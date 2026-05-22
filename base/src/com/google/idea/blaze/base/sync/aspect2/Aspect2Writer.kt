/*
 * Copyright 2026 JetBrains s.r.o.
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
package com.google.idea.blaze.base.sync.aspect2

import com.google.idea.blaze.base.settings.BlazeImportSettingsManager
import com.google.idea.blaze.base.sync.SyncProjectState
import com.google.idea.blaze.base.sync.SyncScope.SyncFailedException
import com.google.idea.blaze.base.sync.aspects.storage.AspectWriter
import com.intellij.aspect.lib.AspectConfig
import com.intellij.aspect.lib.deployAspectZip
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import java.io.IOException
import java.nio.file.Path

/**
 * Materializes the intellij_aspect_sdk archive into <workspaceRoot>/<PROJECT_DATA_SUBDIRECTORY>/aspect2/.
 * Ignores the `dst` passed by AspectStorageService, we deploy to our own location based on the
 * workspace root rather than the project data directory.
 */
class Aspect2Writer : AspectWriter {

  override fun name(): String = "IntelliJ Aspect (materialized)"

  override fun prefix(): Path = Path.of("intellij")

  override fun enabled(): Boolean = Registry.`is`("bazel.use.intellij.aspect")

  override fun write(dst: Path, project: Project, state: SyncProjectState) {
    val normalized = dst.toAbsolutePath().normalize()

    val workspaceRoot = state.workspacePathResolver.findWorkspaceRoot(normalized.toFile())
      ?: throw SyncFailedException("could not determine workspace root")

    try {
      deployAspectZip(
          workspaceRoot = workspaceRoot.path(),
          relativeDestination = workspaceRoot.relativize(normalized),
          config = AspectConfig(
              bazelVersion = formatBazelVersion(state),
              repoMapping = emptyMap(),
              useBuiltin = false,
          ),
          archiveZip = null,
      )
    } catch (e: IOException) {
      throw SyncFailedException("could not deploy the IntelliJ aspect", e)
    }
  }

  private fun formatBazelVersion(state: SyncProjectState): String {
    return state.blazeVersionData.bazelVersion?.toString().orEmpty()
  }
}
