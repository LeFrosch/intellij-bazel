package com.google.idea.blaze.clwb.repl

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import net.starlark.java.eval.*
import net.starlark.java.syntax.FileOptions
import net.starlark.java.syntax.ParserInput
import net.starlark.java.syntax.SyntaxError
import java.io.OutputStream

private val OPTIONS = FileOptions.DEFAULT.toBuilder().allowToplevelRebinding(true).loadBindsGlobally(true).build()

class StarlarkConsoleProcessHandler() : ProcessHandler() {

  private val module: Module
  private val thread: StarlarkThread

  init {
    val mu = Mutability.create(javaClass.simpleName)
    thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT)
    thread.setPrintHandler { _: StarlarkThread, msg: String -> notifyTextAvailable(msg, ProcessOutputTypes.STDOUT) }

    module = Module.create()
    module.documentation = "<REPL>"
  }

  private val inputBuffer = StringBuilder()

  override fun isSilentlyDestroyOnClose(): Boolean = true

  override fun destroyProcessImpl() = notifyProcessDetached()

  override fun detachProcessImpl() {}

  override fun detachIsDefault(): Boolean = true

  override fun getProcessInput(): OutputStream = object : OutputStream() {
    override fun write(byte: Int) {
      val c = byte.toChar()
      inputBuffer.append(c)

      if (c == '\n') {
        handleReplMessage(inputBuffer.toString())
        inputBuffer.clear()
      }
    }
  }

  private fun handleReplMessage(text: String) {
    val input = ParserInput.fromString(text, "<stdin>")

    try {
      val result = Starlark.execFile(input, OPTIONS, module, thread)

      if (result != Starlark.NONE) {
        notifyTextAvailable(Starlark.repr(result) + "\n", ProcessOutputTypes.STDOUT);
      }
    } catch (e: SyntaxError.Exception) {
      notifyTextAvailable(e.message + "\n", ProcessOutputTypes.STDERR);
    }
  }
}