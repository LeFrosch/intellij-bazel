/*
 * Copyright 2026 The Bazel Authors. All rights reserved.
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

@file:Suppress("UnstableApiUsage")

package com.google.idea.blaze.clwb.run.profile

import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.ideinfo.TargetIdeInfo
import com.google.idea.blaze.base.model.primitives.Label
import com.google.idea.blaze.clwb.ToolchainUtils
import com.google.idea.blaze.clwb.run.BlazeCLionGDBDriverConfiguration
import com.google.idea.blaze.clwb.run.BlazeCidrRemoteDebugProcess
import com.google.idea.blaze.clwb.run.BlazeCidrRunConfigurationRunner
import com.intellij.cidr.debugger.profiles.CidrDebugProfile
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGdbDebugProfileType
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionLldbDebugProfileType
import com.jetbrains.cidr.cpp.toolchains.CPPEnvironment
import com.jetbrains.cidr.execution.TrivialInstaller
import com.jetbrains.cidr.execution.TrivialRunParameters
import com.jetbrains.cidr.execution.debugger.CidrLocalDebugProcess
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteDebugParameters
import com.jetbrains.cidr.execution.debugger.remote.CidrRemotePathMapping
import com.jetbrains.cidr.execution.runOnEDT
import java.nio.file.Path

private val PROC_CWD: Path = Path.of("/proc", "self", "cwd")

private const val TEST_FILTER_ENV_VARIABLE = "TESTBRIDGE_TEST_ONLY"

@Throws(ExecutionException::class)
context(ctx: BazelDefaultLauncherContext)
fun createLocalDebugProcess(
  state: CommandLineState,
  session: XDebugSession,
  profile: CidrDebugProfile<*>,
): XDebugProcess {
  val debugDriver = profile.createDriverConfiguration(
    project = ctx.project,
    isElevated = false,
    isEmulateTerminal = false,
    environment = CPPEnvironment(ToolchainUtils.getToolchain()),
  )

  // external paths must be mapped before the workspace root, since one is a prefix of the other
  val sourceMappings = linkedMapOf(
    PROC_CWD.resolve("external") to ctx.executionRoot.resolve("external"),
    PROC_CWD to ctx.workspaceRoot,
  )

  val parameters = TrivialRunParameters(
    debugDriver,
    TrivialInstaller(buildLocalDebugCommandLine(getDebugExecutable())),
  )

  state.consoleBuilder = createConsoleBuilder(null)
  state.addConsoleFilters(*getConsoleFilters())

  val process = runOnEDT {
    CidrLocalDebugProcess(parameters, session, state.consoleBuilder)
  }

  process.postCommand { driver ->
    when (profile.type.getId()) {
      CLionLldbDebugProfileType.ID -> configureLldbDriver(driver, sourceMappings)
      CLionGdbDebugProfileType.ID -> configureGdbDriver(driver, sourceMappings)
    }
  }

  return process
}

@Throws(ExecutionException::class)
context(ctx: BazelDefaultLauncherContext)
fun createRemoteDebugProcess(
  state: CommandLineState,
  session: XDebugSession,
  targetProcess: ProcessHandler,
  port: Int,
): XDebugProcess {
  // sysroot "target:" resolves paths in the context of the debugged target; it must not be null.
  val parameters = CidrRemoteDebugParameters(
    "tcp:localhost:$port",
    getDebugExecutable().toString(),
    "target:",
    ImmutableList.of(CidrRemotePathMapping(PROC_CWD.toString(), ctx.workspaceRoot.parent.toString())),
  )

  val driverConfiguration = BlazeCLionGDBDriverConfiguration(ctx.project)

  return runOnEDT {
    BlazeCidrRemoteDebugProcess(targetProcess, driverConfiguration, parameters, session, state.consoleBuilder)
  }
}

@Throws(ExecutionException::class)
context(ctx: BazelDefaultLauncherContext)
private fun getDebugExecutable(): Path {
  return BlazeCidrRunConfigurationRunner.getDebugExecutable(ctx.environment)
    ?: throw ExecutionException("No debug binary found.")
}

context(ctx: BazelDefaultLauncherContext)
private fun buildLocalDebugCommandLine(executable: Path): GeneralCommandLine {
  val commandLine = GeneralCommandLine(executable.toString()).withWorkingDirectory(ctx.executionRoot)

  commandLine.addParameters(getTargetArguments())
  commandLine.addParameters(ctx.configState.exeFlagsState.getFlagsForExternalProcesses())
  applyEnvironment(commandLine)

  val testFilter = ctx.configState.testFilterForExternalProcesses
  if (!testFilter.isNullOrBlank()) {
    commandLine.withEnvironment(TEST_FILTER_ENV_VARIABLE, testFilter)
  }

  return commandLine
}

context(ctx: BazelDefaultLauncherContext)
private fun getTargetArguments(): List<String> {
  val target = ctx.configuration.singleTarget as? Label ?: return emptyList()

  return ctx.projectData.targetMap().get(target)
    .asSequence()
    .mapNotNull(TargetIdeInfo::getcIdeInfo)
    .flatMap { it.ruleContext().args() }
    .toList()
}

private fun configureGdbDriver(driver: DebuggerDriver, sourceMappings: Map<Path, Path>) {
  for ((from, to) in sourceMappings) {
    driver.executeInterpreterCommand("set substitute-path \"$from\" \"$to\"")
  }
}

private fun configureLldbDriver(driver: DebuggerDriver, sourceMappings: Map<Path, Path>) {
  val command = buildString {
    append("settings set --global target.source-map")
    for ((from, to) in sourceMappings) {
      append(" \"").append(from).append("\" \"").append(to).append('"')
    }
  }

  driver.executeInterpreterCommand(command)
}
