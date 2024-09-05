package com.google.idea.blaze.base.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.terminal.AppendableTerminalDataStream
import com.intellij.terminal.JBTerminalSystemSettingsProviderBase
import com.intellij.terminal.JBTerminalWidget
import com.jediterm.core.util.TermSize
import com.jediterm.terminal.TerminalStarter
import com.jediterm.terminal.TtyConnector
import com.jediterm.terminal.model.JediTerminal
import java.io.IOException

private val LOG = Logger.getInstance(PtyConsoleView::class.java)

class PtyConsoleView(project: Project) : Disposable.Default {
  private val terminalStream = AppendableTerminalDataStream()
  private val terminal = Terminal(project, this, terminalStream)

  init {
    terminal.start(Connector())
  }

  fun write(content: String) {
    try {
      terminalStream.append(content)
    } catch (e: IOException) {
      LOG.error("could not append to terminal", e)
    }
  }
}

private class Terminal(
  project: Project,
  parent: Disposable,
  private val stream: AppendableTerminalDataStream,
) : JBTerminalWidget(project, JBTerminalSystemSettingsProviderBase(), parent) {

  override fun createTerminalStarter(terminal: JediTerminal, connector: TtyConnector): TerminalStarter {
    return TerminalStarter(
      terminal,
      connector,
      stream,
      typeAheadManager,
      executorServiceManager,
    )
  }
}

private class Connector : TtyConnector {
  override fun read(buf: CharArray, offset: Int, length: Int): Int = -1

  override fun write(bytes: ByteArray) { }

  override fun write(string: String) { }

  override fun isConnected(): Boolean = true

  override fun waitFor(): Int = 0

  override fun ready(): Boolean = true

  override fun getName(): String = "bazel-console"

  override fun close() { }

  override fun resize(termSize: TermSize) { }
}