/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.base.execlog

import com.google.idea.common.util.Datafiles
import com.google.idea.common.util.RunJarService
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.progress.currentThreadCoroutineScope
import com.intellij.openapi.project.DumbAware
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.testFramework.LightVirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val LOG = Logger.getInstance(ExeclogParserAction::class.java)

val PARSER_JAR_PATH by Datafiles.resolveLazy("bazel/execlog_parser.jar")

class ExeclogParserAction : AnAction(), DumbAware {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return

    val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
      .withTitle("Select Execution Log")
      .withDescription("Select a Bazel execution log file to parse")

    val selected = FileChooser.chooseFile(descriptor, project, null) ?: return

    currentThreadCoroutineScope().launch(Dispatchers.Default) {
      withBackgroundProgress(project, "Parsing Execution Log") {
        val handler = RunJarService.run(PARSER_JAR_PATH, "--log_path", selected.path)
        val adapter = CapturingProcessAdapter()
        handler.addProcessListener(adapter)
        handler.startNotify()
        handler.waitFor()

        val output = adapter.output
        if (output.exitCode != 0) {
          LOG.warn("Execlog parser failed with exit code ${output.exitCode}: ${output.stderr}")
        }

        val text = output.stdout
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("textproto")
          .takeIf { it !is UnknownFileType } ?: PlainTextFileType.INSTANCE

        withContext(Dispatchers.EDT) {
          val file = LightVirtualFile("${selected.nameWithoutExtension}.textproto", fileType, text)
          file.isWritable = false
          FileEditorManager.getInstance(project).openFile(file, true)
        }
      }
    }
  }
}
