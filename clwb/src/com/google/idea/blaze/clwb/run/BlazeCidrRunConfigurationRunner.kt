/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.clwb.run

import androidx.compose.ui.graphics.CloseSegment
import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.command.BlazeCommandName
import com.google.idea.blaze.base.command.BlazeInvocationContext
import com.google.idea.blaze.base.command.buildresult.BuildResultParser
import com.google.idea.blaze.base.command.buildresult.GetArtifactsException
import com.google.idea.blaze.base.model.primitives.Label
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot
import com.google.idea.blaze.base.run.BlazeBeforeRunCommandHelper
import com.google.idea.blaze.base.run.BlazeCommandRunConfiguration
import com.google.idea.blaze.base.run.ExecutorType
import com.google.idea.blaze.base.run.confighandler.BlazeCommandRunConfigurationRunner
import com.google.idea.blaze.base.util.SaveUtil
import com.google.idea.blaze.clwb.run.BlazeGDBServerProvider.getFlagsForDebugging
import com.google.idea.blaze.clwb.run.BlazeGDBServerProvider.getOptionalFissionArguments
import com.google.idea.blaze.common.Interners
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.RunCanceledByUserException
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.PathUtil
import com.intellij.util.asSafely
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.lang.toolchains.CidrSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.ClangClCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.ClangClSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.ClangCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.ClangSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.GCCSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.MSVCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.MSVCSwitchBuilder
import java.io.File
import java.util.concurrent.CancellationException
import java.util.function.Consumer

/** CLion-specific handler for [BlazeCommandRunConfiguration]s.  */
class BlazeCidrRunConfigurationRunner(private val configuration: BlazeCommandRunConfiguration) :
  BlazeCommandRunConfigurationRunner {

  /** Calculated during the before-run task, and made available to the debugger.  */
  private var executableToDebug: File? = null

  override fun getRunProfileState(executor: Executor, env: ExecutionEnvironment): RunProfileState {
    return CidrCommandLineState(env, BlazeCidrLauncher(configuration, this, env))
  }

  override fun executeBeforeRunTask(env: ExecutionEnvironment): Boolean {
    executableToDebug = null
    if (!isDebugging(env)) return true

    try {
      val executable = getExecutableToDebug(env)
      executableToDebug = executable
      return true
    } catch (e: ExecutionException) {
      ExecutionUtil.handleExecutionError(env.project, env.executor.toolWindowId, env.runProfile, e)
      return false
    }
  }

  private fun getExtraDebugFlags(env: ExecutionEnvironment): ImmutableList<String> {
    if (Registry.`is`("bazel.clwb.debug.extraflags.disabled")) {
      return ImmutableList.of()
    }

    val debuggerKind = RunConfigurationUtils.getDebuggerKind(configuration)
    if (debuggerKind == BlazeDebuggerKind.GDB_SERVER) {
      return getFlagsForDebugging(configuration.handler.getState())
    }

    val switchBuilder = when (RunConfigurationUtils.getCompilerKind(configuration)) {
      MSVCCompilerKind -> MSVCSwitchBuilder()
      ClangClCompilerKind -> ClangClSwitchBuilder()
      ClangCompilerKind -> ClangSwitchBuilder()
      else -> GCCSwitchBuilder() // default to GCC, as usual
    }

    if (Registry.`is`("bazel.clwb.debug.enforce.dbg.compilation.mode")) {
      switchBuilder.withSwitches()
      flagsBuilder.add("--compilation_mode=dbg")
      flagsBuilder.add("--strip=never")
      flagsBuilder.add("--dynamic_mode=off")
    }

    val compilerKind = RunConfigurationUtils.getCompilerKind(configuration)
    if (compilerKind === ClangClCompilerKind || compilerKind === MSVCCompilerKind) {
      val sb = CidrSwitchBuilder()
      val sb2 = compilerKind.getSwitchBuilder(sb)
      sb2
        .withDebugInfo(-1) // ignored for msvc/clangcl
        .withDisableOptimization()

      sb2.buildRaw().forEach(Consumer { opt: String? ->
        flagsBuilder.add("--copt=$opt")
      })
    } else {
      if (debuggerKind == BlazeDebuggerKind.BUNDLED_LLDB && !Registry.`is`("bazel.trim.absolute.path.disabled")) {
        flagsBuilder.add(
          "--copt=-fdebug-compilation-dir=" + WorkspaceRoot.fromProject(env.project)
        )

        if (SystemInfo.isMac) {
          flagsBuilder.add("--linkopt=-Wl,-oso_prefix,.")
        }
      }

      flagsBuilder.add("--copt=-O0")
      flagsBuilder.add("--copt=-g")
      flagsBuilder.addAll(getOptionalFissionArguments())
    }
    return flagsBuilder.build()
  }

  private fun getExtraCompilerSwitches(env: ExecutionEnvironment): CidrSwitchBuilder {

  }

  /**
   * Builds blaze C/C++ target in debug mode, and returns the output build artifact.
   *
   * @throws ExecutionException if no unique output artifact was found.
   */
  @Throws(ExecutionException::class)
  private fun getExecutableToDebug(env: ExecutionEnvironment): File {
    SaveUtil.saveAllFiles()

    // TODO: migrate this to use the BazelExecService and the new build/sync view
    val eventStreamFuture = BlazeBeforeRunCommandHelper.runBlazeCommand(
      BlazeCommandName.BUILD,
      configuration,
      ImmutableList.of(),
      getExtraDebugFlags(env),
      BlazeInvocationContext.runConfigContext(ExecutorType.fromExecutor(env.executor), configuration.type, true),
      "Building debug binary"
    )

    val target: Label = getSingleTarget(configuration)
    try {
      eventStreamFuture.get().use { bepStream ->
        val candidateFiles = BuildResultParser.getExecutableArtifacts(
          bepStream,
          Interners.STRING,
          target.toString()
        )

        return findExecutable(target, candidateFiles)
          ?: throw ExecutionException("More than 1 executable was produced when building $target; don't know which to debug")
      }
    } catch (_: InterruptedException) {
      eventStreamFuture.cancel(true)
      throw RunCanceledByUserException()
    } catch (_: CancellationException) {
      eventStreamFuture.cancel(true)
      throw RunCanceledByUserException()
    } catch (e: java.util.concurrent.ExecutionException) {
      throw ExecutionException(e)
    } catch (e: GetArtifactsException) {
      throw ExecutionException("Failed to get output artifacts when building $target: ${e.message}")
    }
  }

  companion object {
    private fun isDebugging(environment: ExecutionEnvironment): Boolean {
      return environment.executor is DefaultDebugExecutor
    }

    @Throws(ExecutionException::class)
    private fun getSingleTarget(config: BlazeCommandRunConfiguration): Label {
      return config.targets.firstOrNull()?.asSafely<Label>()
        ?: throw ExecutionException("Invalid configuration: doesn't have a single target label")
    }

    /**
     * Basic heuristic for choosing between multiple output files. Currently just looks for a filename
     * matching the target name.
     */
    private fun findExecutable(target: Label, outputs: List<File>): File? {
      if (outputs.size == 1) return outputs[0]

      val name = PathUtil.getFileName(target.targetName().toString())
      return outputs.firstOrNull { file -> file.getName() == name }
    }
  }
}
