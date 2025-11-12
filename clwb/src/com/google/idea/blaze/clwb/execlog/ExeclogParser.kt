package com.google.idea.blaze.clwb.execlog

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.DumbAware
import com.intellij.testFramework.LightVirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.Attributes
import java.util.jar.JarFile


class ExeclogParser : AnAction(), DumbAware {

  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project ?: return

    val descriptor = FileChooserDescriptorFactory.singleFile()
      .withTitle("Select execlog")
      .withDescription("Select execlog file to parse")

    val files = FileChooserFactory.getInstance()
      .createFileChooser(descriptor, project, null)
      .choose(project)

    val inputFile = files.singleOrNull() ?: return

    currentThreadCoroutineScope().launch {
      val content = withContext(Dispatchers.IO) {
        val temp = Files.createTempFile("execlog", ".txt")

        jarRun(
          ctx = ExeclogParser::class.java,
          path = "tools/execlog/parser_deploy.jar",
          args = listOf(
            "--log_path", inputFile.path,
            "--output_path", temp.toString()
          ),
        )

        Files.readString(temp)
      }

      withContext(Dispatchers.EDT) {
        val file = LightVirtualFile(inputFile.name, PlainTextFileType.INSTANCE, content)
        FileEditorManager.getInstance(project).openFile(file, false)
      }
    }
  }
}

@Throws(IOException::class)
private fun jarLoad(ctx: Class<*>, path: String): File {
  val stream = ctx.classLoader.getResourceAsStream(path)
    ?: throw IOException("jar not found: $path")

  return stream.use {
    val temp = Files.createTempFile("extracted", ".jar")
    Files.copy(it, temp, StandardCopyOption.REPLACE_EXISTING)

    val file = temp.toFile()
    file.deleteOnExit()

    return file
  }
}

@Throws(IOException::class)
private suspend fun jarRun(ctx: Class<*>, path: String, args: List<String>) {
  val jar = jarLoad(ctx, path)

  val mainClass = JarFile(jar).use {
    val manifest = it.manifest
      ?: throw IOException("jar has no manifest: $path")
    manifest.mainAttributes.getValue(Attributes.Name.MAIN_CLASS)
      ?: throw IOException("jar has no main class: $path")
  }

  val classLoader = URLClassLoader(arrayOf(jar.toURI().toURL()), ctx.classLoader)

  val thread = Thread {
    Class
      .forName(mainClass, true, classLoader)
      .getMethod("main", Array<String>::class.java)
      .invoke(null, args.toTypedArray())
  }
  thread.contextClassLoader = classLoader
  thread.start()

  suspendCancellableCoroutine { cont ->
    thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
      cont.resumeWith(Result.failure(e))
    }
    cont.invokeOnCancellation {
      thread.interrupt()
    }

    try {
      thread.join()
    } catch (e: InterruptedException) {
      cont.resumeWith(Result.failure(e))
    } finally {
      cont.resumeWith(Result.success(Unit))
    }
  }
}