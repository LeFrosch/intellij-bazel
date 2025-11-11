package com.google.idea.blaze.clwb.repl

import com.google.idea.blaze.base.lang.buildfile.language.BuildFileLanguage
import com.intellij.execution.console.ConsoleExecuteAction
import com.intellij.execution.console.LanguageConsoleBuilder
import com.intellij.execution.console.ProcessBackedConsoleExecuteActionHandler
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.AbstractConsoleRunnerWithHistory
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import java.awt.BorderLayout
import javax.swing.JPanel

private const val REPL_NAME = "Starlark REPL"
private const val REPL_EXTENSION = ".bzl"

class StarlarkReplAction() : AnAction(), DumbAware {

  @Suppress("UnstableApiUsage")
  override fun actionPerformed(event: AnActionEvent) {
    val project = event.project!!

    val handler = StarlarkConsoleProcessHandler()

    val consoleView = LanguageConsoleBuilder().build(project, BuildFileLanguage.INSTANCE)
    consoleView.virtualFile.rename(this, consoleView.virtualFile.name + REPL_EXTENSION)

    val panel = JPanel(BorderLayout())
    panel.add(consoleView.component, BorderLayout.CENTER)

    val contentDescriptor = RunContentDescriptor(
        consoleView,
        handler,
        panel,
        REPL_NAME
      )

    val actionhandler = ProcessBackedConsoleExecuteActionHandler(handler, false)

    val actions: List<AnAction> = listOf(
      ConsoleExecuteAction(consoleView, actionhandler, actionhandler.emptyExecuteAction, actionhandler)
    )

    AbstractConsoleRunnerWithHistory.registerActionShortcuts(actions, consoleView.consoleEditor.component)
    AbstractConsoleRunnerWithHistory.registerActionShortcuts(actions, panel)

    RunContentManager.getInstance(project).showRunContent(
      DefaultRunExecutor.getRunExecutorInstance(),
      contentDescriptor
    )

    consoleView.attachToProcess(handler)
    handler.startNotify()
  }
}