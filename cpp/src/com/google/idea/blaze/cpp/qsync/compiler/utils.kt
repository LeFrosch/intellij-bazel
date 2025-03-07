package com.google.idea.blaze.cpp.qsync.compiler

import com.google.idea.blaze.base.buildview.BazelExecService
import com.google.idea.blaze.base.command.BlazeCommand
import com.google.idea.blaze.base.command.BlazeCommandName
import com.google.idea.blaze.base.scope.output.IssueOutput
import com.google.idea.blaze.exception.BuildException
import com.google.idea.blaze.qsync.project.ProjectPath
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.logger
import com.intellij.platform.eel.executeProcess
import com.intellij.platform.eel.getOrThrow
import com.intellij.platform.eel.impl.utils.awaitProcessResult
import com.intellij.platform.eel.provider.getEelDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Path
import java.util.*

private val LOG = logger<CcCompilerInfoContext>()

@Throws(BuildException::class)
suspend fun CcCompilerInfoContext.resolve(path: String): Path {
  val resolvedPath = resolver.resolve(ProjectPath.execrootRelative(path))

  return try {
    withContext(Dispatchers.IO) { resolvedPath.toRealPath() }
  } catch (e: IOException) {
    throw BuildException("could not resolve path: $path", e)
  }
}

fun CcCompilerInfoContext.warn(title: String, e: BuildException) {
  LOG.warn(title, e)
  IssueOutput.warn(title).withDescription(e.toString()).submit(context)
}

@Throws(BuildException::class)
@Suppress("UnstableApiUsage") // EEL API is still unstable
suspend fun CcCompilerInfoContext.exec(executable: Path, vararg args: String): ProcessOutput {
  return project.getEelDescriptor().upgrade().exec
    .executeProcess(executable.toString(), *args)
    .getOrThrow { BuildException("could not execute $executable: ${it.message}") }
    .awaitProcessResult()
}

@Throws(BuildException::class)
suspend fun CcCompilerInfoContext.execBazel(cmd: BlazeCommandName, configure: BlazeCommand.Builder.() -> Unit): ProcessOutput {
  return BazelExecService.instance(project).execute(context, cmd, configure = configure)
}
