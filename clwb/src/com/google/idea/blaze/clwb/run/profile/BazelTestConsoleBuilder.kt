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

package com.google.idea.blaze.clwb.run.profile

import com.google.idea.blaze.base.run.BlazeCommandRunConfiguration
import com.google.idea.blaze.base.run.smrunner.BlazeTestUiSession
import com.google.idea.blaze.base.run.smrunner.SmRunnerUtils
import com.google.idea.blaze.clwb.run.BlazeCidrTestOutputFilter
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.jetbrains.cidr.execution.CidrConsoleBuilder
import com.jetbrains.cidr.execution.testing.google.CidrGoogleTestConsoleProperties

/** Wires the Google Test tree UI into the C/C++ run/debug console. */
class BazelTestConsoleBuilder(
  private val configuration: BlazeCommandRunConfiguration,
  private val environment: ExecutionEnvironment,
  private val testUiSession: BlazeTestUiSession?,
) : CidrConsoleBuilder(configuration.project, null, null) {

  init {
    addFilter(BlazeCidrTestOutputFilter(configuration.project))
  }

  override fun createConsole(): ConsoleView {
    testUiSession?.let {
      return SmRunnerUtils.getConsoleView(configuration.project, configuration, environment.executor, it)
    }

    // when launching the debugger directly, the blaze test runners aren't involved
    val consoleProperties = CidrGoogleTestConsoleProperties(configuration, environment.executor, environment.executionTarget)
    return createConsole(configuration.type, consoleProperties)
  }
}
