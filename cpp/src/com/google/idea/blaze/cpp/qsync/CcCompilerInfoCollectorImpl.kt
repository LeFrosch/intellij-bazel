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
package com.google.idea.blaze.cpp.qsync

import com.google.idea.blaze.base.qsync.CcCompilerInfoCollectorProvider
import com.google.idea.blaze.base.util.pluginProjectScope
import com.google.idea.blaze.common.Context
import com.google.idea.blaze.common.PrintOutput
import com.google.idea.blaze.exception.BuildException
import com.google.idea.blaze.qsync.cc.CcCompilerInfoCollector
import com.google.idea.blaze.qsync.deps.CcToolchain
import com.google.idea.blaze.qsync.project.ProjectPath
import com.google.idea.blaze.qsync.project.ProjectProto
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.platform.eel.executeProcess
import com.intellij.platform.eel.getOrThrow
import com.intellij.platform.eel.impl.utils.awaitProcessResult
import com.intellij.platform.eel.provider.getEelDescriptor
import kotlinx.coroutines.async
import java.nio.file.Path

private val LOG = logger<CcCompilerInfoCollectorImpl>()

class CcCompilerInfoCollectorImpl private constructor(
  private val project: Project,
  private val resolver: ProjectPath.Resolver,
) : CcCompilerInfoCollector {

  class Provider : CcCompilerInfoCollectorProvider {
    override fun create(project: Project, resolver: ProjectPath.Resolver): CcCompilerInfoCollector {
      return CcCompilerInfoCollectorImpl(project, resolver)
    }
  }

  @Throws(BuildException::class)
  override fun getCompilerKind(ctx: Context<*>, toolchain: CcToolchain): ProjectProto.CcCompilerKind {
    return runJob(ctx, toolchain, ExecutionContext::getCompilerKindImpl)
  }

  @Throws(BuildException::class)
  override fun getMsvcData(ctx: Context<*>, toolchain: CcToolchain): ProjectProto.MsvcData {
    // TODO: implement msvc data collection (see com.google.idea.blaze.clwb.MSVCEnvironmentProvider)
    return ProjectProto.MsvcData.newBuilder().build()
  }

  @Throws(BuildException::class)
  override fun getXcodeData(ctx: Context<*>, toolchain: CcToolchain): ProjectProto.XcodeData {
    // TODO: implement xcode data collection (see com.google.idea.blaze.cpp.XcodeCompilerSettingsProviderImpl)
    return ProjectProto.XcodeData.newBuilder().build()
  }

  @Throws(BuildException::class)
  private fun <T> runJob(
    ctx: Context<*>,
    toolchain: CcToolchain,
    action: suspend ExecutionContext.() -> T,
  ): T {
    val result = pluginProjectScope(project).async { ExecutionContext(project, toolchain, ctx, resolver).action() }
    ctx.addCancellationHandler { result.cancel() }

    return try {
      runBlockingMaybeCancellable { result.await() }
    } catch (e: BuildException) {
      throw e
    } catch (e: Exception) {
      LOG.error("Unhandled exception", e)
      throw BuildException("Unhandled exception", e)
    }
  }
}

private class ExecutionContext(
  val project: Project,
  val toolchain: CcToolchain,
  private val ctx: Context<*>,
  private val resolver: ProjectPath.Resolver,
) {

  fun resolve(path: ProjectPath): Path {
    return resolver.resolve(path)
  }

  fun log(format: String, vararg args: Any?) {
    val msg = String.format(format, *args)

    LOG.info(msg)
    ctx.output(PrintOutput.log(msg))
  }
}

@Suppress("UnstableApiUsage") // EEL API is still unstable
private suspend fun ExecutionContext.getCompilerKindImpl(): ProjectProto.CcCompilerKind {
  val executable = resolve(toolchain.compilerExecutable())

  val result = project.getEelDescriptor().upgrade().exec
    .executeProcess(executable.toString(), "--version")
    .getOrThrow { BuildException("could not execute compiler: ${it.message}") }
    .awaitProcessResult()

  if (result.exitCode == 0) {
    return guessCompilerKind(result.stdout)
  }

  // MSVC does not know the --version flag and will fail. However, the error message does contain
  // the compiler version.
  if (result.stderr.contains("Microsoft")) {
    return ProjectProto.CcCompilerKind.MSVC
  }

  log("could not detect compiler kind: ${result.stderr}")
  return ProjectProto.CcCompilerKind.CC_COMPILER_KIND_UNKNOWN
}

private fun guessCompilerKind(version: String): ProjectProto.CcCompilerKind {
  return when {
    version.startsWith("Apple clang") -> ProjectProto.CcCompilerKind.APPLE_CLANG
    version.contains("clang") -> ProjectProto.CcCompilerKind.CLANG
    version.contains("gcc") -> ProjectProto.CcCompilerKind.GCC
    version.contains("Microsoft") -> ProjectProto.CcCompilerKind.MSVC
    else -> ProjectProto.CcCompilerKind.CC_COMPILER_KIND_UNKNOWN
  }
}