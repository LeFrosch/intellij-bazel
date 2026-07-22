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
@file:Suppress("UnstableApiUsage")

package com.google.idea.blaze.clwb.run.profile

import com.google.idea.blaze.base.run.BlazeCommandRunConfiguration
import com.intellij.cidr.debugger.profiles.CidrDebugProfileManager
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.execution.CidrLauncher

/**
 * Runs and debugs `cc_binary` / `cc_test` targets in CLion. The debug flavour (local GDB, local LLDB, or Bazel
 * gdbserver) is chosen by the currently selected [com.intellij.cidr.debugger.profiles.CidrDebugProfile].
 */
class BazelProfileAwareLauncher(
  private val configuration: BlazeCommandRunConfiguration,
  private val environment: ExecutionEnvironment,
) : CidrLauncher() {

  override fun getProject(): Project = configuration.project

  @Throws(ExecutionException::class)
  override fun createProcess(state: CommandLineState): ProcessHandler {
    return with(resolveLauncherContext(configuration, environment)) {
      createTargetProcess(state, emptyList())
    }
  }

  @Throws(ExecutionException::class)
  override fun createDebugProcess(state: CommandLineState, session: XDebugSession): XDebugProcess {
    val profile = CidrDebugProfileManager.getInstance().getCurrentDebugProfile(getProject())
      ?: throw ExecutionException("No debug profile is selected.")

    return with(resolveLauncherContext(configuration, environment)) {
      if (profile.type.getId() == BazelGdbServerDebugProfileType.ID) {
        val port = (profile.state as BazelGdbServerDebugProfileState).port
        val targetProcess = createTargetProcess(state, getGdbServerFlags(port))
        CidrLauncher.configProcessHandler(targetProcess, false, true, getProject())

        createRemoteDebugProcess(state, session, targetProcess, port)
      } else {
        createLocalDebugProcess(state, session, profile)
      }
    }
  }
}
