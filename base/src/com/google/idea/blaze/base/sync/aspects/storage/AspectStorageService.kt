package com.google.idea.blaze.base.sync.aspects.storage

import com.google.idea.blaze.base.scope.BlazeContext
import com.google.idea.blaze.base.scope.Scope
import com.google.idea.blaze.base.scope.scopes.ToolWindowScope
import com.google.idea.blaze.base.settings.BlazeImportSettings
import com.google.idea.blaze.base.settings.BlazeImportSettingsManager
import com.google.idea.blaze.base.sync.SyncScope.SyncFailedException
import com.google.idea.blaze.base.sync.data.BlazeDataStorage
import com.google.idea.blaze.base.toolwindow.Task
import com.google.idea.blaze.common.Label
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional

private const val ASPECT_TASK_TITLE = "Print Aspects"
private const val ASPECT_DIRECTORY = "aspects"

@Service(Service.Level.PROJECT)
class AspectStorageService(private val project: Project) {

  companion object {
    @JvmStatic
    fun of(project: Project): AspectStorageService = project.service()
  }

  /**
   * Copies all bundled aspects to a workspace relative directory.
   * This should be called as one of the first steps in the sync workflow.
   *
   * Register a [AspectWriter] to provide aspect files.
   */
  @Throws(SyncFailedException::class)
  fun prepare(ctx: BlazeContext) {
    val task = ToolWindowScope.Builder(project, Task(project, ASPECT_TASK_TITLE, Task.Type.SYNC)).build()

    Scope.push(ctx) { ctx ->
      ctx.push(task)

      val settings = BlazeImportSettingsManager.getInstance(project).importSettings
        ?: throw SyncFailedException("No import settings found")

      val directory = aspectDirectory(settings)
        ?: throw SyncFailedException("Could not determine aspect directory")

      try {
        if (!Files.exists(directory)) {
          Files.createDirectories(directory)
        }
      } catch (e: IOException) {
        throw SyncFailedException("Could not create aspect directory", e)
      }

      for (printer in AspectWriter.EP_NAME.extensionList) {
        printer.write(directory, project)
      }
    }
  }

  fun resolve(file: String): Optional<Label> {
    val settings = BlazeImportSettingsManager.getInstance(project).importSettings ?: return Optional.empty()
    val directory = aspectDirectory(settings) ?: return Optional.empty()

    val file = directory.resolve(file)
    if (!Files.exists(file)) return Optional.empty()

    val path = Path.of(settings.workspaceRoot).relativize(file)
    return Optional.of(Label.fromWorkspacePackageAndName("", path.parent, path.fileName))
  }

  private fun aspectDirectory(settings: BlazeImportSettings): Path? {
    val projectPath = project.basePath?.let(Path::of) ?: return null
    val workspacePath = settings.workspaceRoot.takeIf(String::isNotBlank)?.let(Path::of) ?: return null

    // if the project data path is contained in the workspace, the aspects can be placed in there
    if (projectPath.startsWith(workspacePath)) {
      return projectPath.resolve(ASPECT_DIRECTORY)
    }

    // if this is not the case, fallback to .ijwb_aspects or .clwb_aspects
    return projectPath.resolve(BlazeDataStorage.PROJECT_DATA_SUBDIRECTORY + "_aspects")
  }
}
