/*
 * Copyright 2026 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base.sync.reactive

import com.google.idea.blaze.base.command.buildresult.bepparser.parseBepArtifacts
import com.google.idea.blaze.base.command.buildresult.bepparser.streamBepEvents
import com.google.idea.blaze.base.scope.BlazeContext
import com.google.idea.blaze.base.settings.BlazeImportSettingsManager
import com.google.idea.blaze.base.sync.BlazeSyncManager
import com.google.idea.blaze.base.sync.BlazeSyncParams
import com.google.idea.blaze.base.sync.SyncListener
import com.google.idea.blaze.base.sync.SyncMode
import com.google.idea.blaze.base.sync.aspects.BlazeBuildOutputs
import com.google.idea.blaze.base.sync.aspects.BlazeIdeInterface
import com.google.idea.blaze.base.sync.data.BlazeDataStorage
import com.google.idea.blaze.base.sync.data.BlazeProjectDataManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.lang.typescript.editing.TypeScriptInlayHintsSupportedService.Companion.ENABLED_KEY
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import kotlinx.coroutines.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.time.Duration.Companion.seconds


private const val ENABLED_KEY = "ReactiveSyncEnabled"

private val LOG = logger<ReactiveSyncService>()

private const val REACTIVE_DIRECTORY = "reactiveSync"
private const val BEP_FILE_NAME = "reactive_bep.bin"
private const val MANAGED_BAZELRC_NAME = "reactive.bazelrc"
private const val SYNC_ORIGIN: String = "ReactiveSync"
private val POLL_INTERVAL = 1.seconds

/**
 * Watches a single Bazel BEP file (written by external builds, see [InstallReactiveSyncFlagsAction])
 * and, whenever such a build completes, patches the project model from the changed aspect outputs
 * without running a blaze build of its own.
 *
 * Activation is implicit: the watcher idles until the BEP file appears, which only happens once the
 * reactive flags are installed into the workspace `.bazelrc`. There is no separate user setting.
 */
@Service(Service.Level.PROJECT)
class ReactiveSyncService(private val project: Project, private val scope: CoroutineScope) {

  companion object {

    @JvmStatic
    fun getInstance(project: Project): ReactiveSyncService = project.service()
  }

  init {
    // can we automatically restart this?
    scope.launch(CoroutineName("ReactiveSyncWatcher")) { watchLoop() }
  }

  /** Persistent state whether the reactive sync is enabled. */
  private var userEnabled: Boolean
    get() = PropertiesComponent.getInstance(project).getBoolean(ENABLED_KEY, false)
    set(value) = PropertiesComponent.getInstance(project).setValue(ENABLED_KEY, value)

  /** Non-Persistent state whether the reactive sync is suspended i.e. a sync is in progress. */
  private var suspended: Boolean = false

  /** Compination of [userEnabled] and [suspended]. */
  private val active: Boolean get() = userEnabled && !suspended

  /**
   * The IDE-owned directory holding the reactive BEP file and the managed bazelrc. Resolved the same
   * way as `HeaderCacheService.cacheDirectory` so the location is stable and outside both the user's
   * source tree and `bazel-out`.
   */
  private val reactiveDirectory: Path by lazy {
    val importSettings = BlazeImportSettingsManager.getInstance(project).importSettings

    if (importSettings != null) {
      BlazeDataStorage.getProjectDataDir(importSettings).toPath().resolve(REACTIVE_DIRECTORY)
    } else {
      project.getProjectDataPath(REACTIVE_DIRECTORY)
    }
  }

  /** The file Bazel writes its build event protocol to (`--build_event_binary_file`). */
  private val bepFile: Path get() = reactiveDirectory.resolve(BEP_FILE_NAME)

  /** The IDE-managed bazelrc that is `import`ed from the workspace `.bazelrc`. */
  val managedBazelrc: Path get() = reactiveDirectory.resolve(MANAGED_BAZELRC_NAME)

  private suspend fun watchLoop() {
    val file = bepFile

    // avoid reactive sync on project open
    tryTruncate(file)

    while (scope.isActive) {
      delay(POLL_INTERVAL)

      // wait until bazel created the file if it does not exist yet
      if (!Files.exists(file)) continue

      try {
        val output = parseBepArtifacts(streamBepEvents(file))
        requestReactiveUpdate(BlazeBuildOutputs.fromParsedBepOutput(output))
      } catch (e: Exception) {
        LOG.warn("Reactive sync poll failed", e)
      } finally {
        tryTruncate(file)
      }
    }
  }

  /**
   * Requests a [SyncMode.REACTIVE] update that applies the given outputs to the project model
   * without running a blaze build (see `BuildPhaseSyncTask`).
   */
  private fun requestReactiveUpdate(outputs: BlazeBuildOutputs) {
    // A reactive update is a delta on top of an existing model. Without prior project data there is
    // nothing to merge into, and we must not escalate to a full build (that would compete with the
    // agent), so we simply skip until the project has been synced once.
    val projectData = BlazeProjectDataManager.getInstance(project).blazeProjectData
    if (projectData == null) {
      LOG.info("Skipping reactive update: project has not been synced yet")
      return
    }

    // Only sync if the external build actually changed any IDE-info. An external `bazel build`
    // re-runs the aspect on every invocation, but if no `.intellij-info.txt` changed there is
    // nothing for the model to pick up, so skip the entire sync pipeline.
    if (!BlazeIdeInterface.getInstance().hasUpdatedIdeInfo(projectData, outputs)) {
      LOG.info("Skipping reactive update: no IDE info changed")
      return
    }

    val params = BlazeSyncParams.builder()
      .setTitle("Reactive Sync")
      .setSyncMode(SyncMode.REACTIVE)
      .setSyncOrigin(SYNC_ORIGIN)
      .setBackgroundSync(true)
      .setAddProjectViewTargets(false)
      .setExternalBuildOutputs(outputs)
      .build()

    BlazeSyncManager.getInstance(project).requestProjectSync(params)
  }

  @Throws(IOException::class)
  private fun updateRcFile() {
    if (active) {
      writeReactiveRcFileEnabled(managedBazelrc, project, bepFile)
    } else {
      writeReactiveRcFileDisabled(managedBazelrc)
    }
  }

  /**
   * Toggles the reactive sync state and rewrites the rc file accordingly.
   */
  @Throws(IOException::class)
  fun setEnabled(value: Boolean) {
    if (value == userEnabled) return

    if (value) {
      installReactiveRcImport(managedBazelrc, project)
    }

    updateRcFile()
    userEnabled = value
  }

  private class Listener : SyncListener {
    override fun beforeSyncStart(project: Project, context: BlazeContext, syncMode: SyncMode) {
      if (syncMode == SyncMode.REACTIVE) return

      getInstance(project).apply {
        suspended = true
        runCatching { updateRcFile() }
      }
    }

    override fun afterSyncFinish(project: Project, context: BlazeContext, syncMode: SyncMode) {
      if (syncMode == SyncMode.REACTIVE) return

      getInstance(project).apply {
        suspended = false
        runCatching { updateRcFile() }
      }
    }
  }
}

private fun tryTruncate(path: Path) {
  try {
    Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).close()
  } catch (e: IOException) {
    LOG.warn("Failed to truncate BEP file", e)
  }
}
