package com.google.idea.blaze.base.sync.aspects.storage

import com.google.idea.blaze.base.scope.BlazeContext
import com.google.idea.blaze.base.scope.Scope
import com.google.idea.blaze.base.scope.scopes.ToolWindowScope
import com.google.idea.blaze.base.settings.BlazeImportSettings
import com.google.idea.blaze.base.settings.BlazeImportSettingsManager
import com.google.idea.blaze.base.sync.SyncScope.SyncFailedException
import com.google.idea.blaze.base.sync.aspects.strategy.AspectRepositoryProvider
import com.google.idea.blaze.base.sync.data.BlazeDataStorage
import com.google.idea.blaze.base.toolwindow.Task
import com.google.idea.blaze.common.Label
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import java.io.IOException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively

private const val TASK_TITLE = "Print Aspects"

@Service(Service.Level.PROJECT)
class AspectStorageService(private val project: Project) {

  companion object {
    @JvmStatic
    fun of(project: Project): AspectStorageService = project.service()
  }

  @Throws(SyncFailedException::class)
  fun prepare(ctx: BlazeContext) {
    val task = ToolWindowScope.Builder(project, Task(project, TASK_TITLE, Task.Type.SYNC)).build()

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

      for (printer in AspectPrinter.EP_NAME.extensionList) {
        printer.print(directory, project)
      }
    }
  }

  fun resolve(file: String): Label? {
    val settings = BlazeImportSettingsManager.getInstance(project).importSettings ?: return null
    val directory = aspectDirectory(settings) ?: return null

    val file = AspectPrinter.EP_NAME.extensionList.asSequence()
      .map { it.resolve(directory, file) }
      .firstOrNull { Files.exists(it) }
      ?: return null

    val path = Path.of(settings.workspaceRoot).relativize(file)
    return Label.fromWorkspacePackageAndName("", path.parent, path.fileName)
  }

  private fun aspectDirectory(settings: BlazeImportSettings): Path? {
    val projectPath = project.basePath?.let(Path::of) ?: return null
    val workspacePath = settings.workspaceRoot.takeIf(String::isNotBlank)?.let(Path::of) ?: return null

    // if the project data path is contained in the workspace, the aspects can be placed in there
    if (projectPath.startsWith(workspacePath)) {
      return projectPath
    }

    // if this is not the case, fallback to .ijwb_aspects or .clwb_aspects
    return projectPath.resolve(BlazeDataStorage.PROJECT_DATA_SUBDIRECTORY + "_aspects")
  }
}

interface AspectPrinter {
  companion object {
    val EP_NAME = ExtensionPointName.create<AspectPrinter>("com.google.idea.blaze.AspectPrinter");
  }

  @Throws(SyncFailedException::class)
  fun print(root: Path, project: Project)

  fun resolve(root: Path, file: String): Path
}

class DefaultAspectPrinter : AspectPrinter {
  private val SUBDIRECTORY = "sync";

  override fun print(root: Path, project: Project) {
    val aspects = AspectRepositoryProvider.findAspectDirectory()
      .map(File::toPath)
      .orElseThrow { SyncFailedException("Couldn't find aspect directory") }

    val realizedAspectsPath = root.resolve(SUBDIRECTORY);

    try {
      Files.createDirectories(realizedAspectsPath)
    } catch (e: IOException) {
      throw SyncFailedException("Couldn't create realized aspects", e)
    }

    try {
      @OptIn(ExperimentalPathApi::class)
      aspects.copyToRecursively(realizedAspectsPath, overwrite = true, followLinks = false)
    } catch (e: IOException) {
      throw SyncFailedException("Couldn't copy aspects", e)
    }
  }

  override fun resolve(root: Path, file: String): Path {
    return root.resolve(SUBDIRECTORY).resolve(file);
  }
}