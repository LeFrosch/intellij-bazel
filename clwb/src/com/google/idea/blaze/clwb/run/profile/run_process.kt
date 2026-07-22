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
package com.google.idea.blaze.clwb.run.profile

import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.async.process.LineProcessingOutputStream
import com.google.idea.blaze.base.command.BlazeCommand
import com.google.idea.blaze.base.command.BlazeFlags
import com.google.idea.blaze.base.command.BlazeInvocationContext
import com.google.idea.blaze.base.command.buildresult.BuildResultHelperBep
import com.google.idea.blaze.base.console.BlazeConsoleLineProcessorProvider
import com.google.idea.blaze.base.issueparser.ToolWindowTaskIssueOutputFilter
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot
import com.google.idea.blaze.base.run.ExecutorType
import com.google.idea.blaze.base.run.processhandler.LineProcessingProcessAdapter
import com.google.idea.blaze.base.run.processhandler.ScopedBlazeProcessHandler
import com.google.idea.blaze.base.run.smrunner.BlazeTestEventsHandler
import com.google.idea.blaze.base.run.smrunner.BlazeTestUiSession
import com.google.idea.blaze.base.run.testlogs.LocalBuildEventProtocolTestFinderStrategy
import com.google.idea.blaze.base.scope.BlazeContext
import com.google.idea.blaze.base.scope.scopes.ProblemsViewScope
import com.google.idea.blaze.base.settings.Blaze
import com.google.idea.blaze.base.settings.BlazeUserSettings
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.UrlFilter
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.jetbrains.cidr.execution.CidrConsoleBuilder

context(ctx: BazelDefaultLauncherContext)
@Throws(ExecutionException::class)
fun createTargetProcess(state: CommandLineState, extraFlags: List<String>): ProcessHandler {
  val blazeContext = BlazeContext.create()
  val testUiSession = createTestUiSession()
  val command = ctx.configState.commandState.command

  val builder = BlazeCommand
    .builder(Blaze.getBuildSystemProvider(ctx.project).getBinaryPath(ctx.project), command)
    .addTargets(ctx.configuration.targets)
    .addBlazeFlags(extraFlags)
    .addBlazeFlags(
      BlazeFlags.blazeFlags(
        ctx.project,
        ctx.projectView,
        command,
        blazeContext,
        BlazeInvocationContext.runConfigContext(
          ExecutorType.fromExecutor(ctx.environment.executor),
          ctx.configuration.type,
          false,
        ),
      )
    )
    .addBlazeFlags(testUiSession?.blazeFlags ?: ImmutableList.of<String>())

  val testFilterFlag = ctx.configState.testFilterFlag
  if (testFilterFlag != null && ctx.isTest) {
    builder.addBlazeFlags(testFilterFlag, BlazeFlags.DISABLE_TEST_SHARDING)
  }

  builder.addExeFlags(ctx.configState.exeFlagsState.getFlagsForExternalProcesses())

  state.consoleBuilder = createConsoleBuilder(testUiSession)
  state.addConsoleFilters(*getConsoleFilters().toTypedArray())

  val commandLine = GeneralCommandLine(builder.build().toList())
  applyEnvironment(commandLine)

  return ScopedBlazeProcessHandler(
    ctx.project,
    commandLine,
    WorkspaceRoot.fromProject(ctx.project),
    object : ScopedBlazeProcessHandler.ScopedProcessHandlerDelegate {
      override fun onBlazeContextStart(context: BlazeContext) {
        context.push(ProblemsViewScope(ctx.project, BlazeUserSettings.getInstance().showProblemsViewOnRun))
      }

      override fun createProcessListeners(context: BlazeContext): ImmutableList<ProcessListener> {
        val outputStream = LineProcessingOutputStream.of(
          BlazeConsoleLineProcessorProvider.getAllStderrLineProcessors(context)
        )
        return ImmutableList.of<ProcessListener>(LineProcessingProcessAdapter(outputStream))
      }
    },
  )
}

context(ctx: BazelDefaultLauncherContext)
fun createTestUiSession(): BlazeTestUiSession? {
  if (!ctx.isTest) {
    return null
  }

  if (!BlazeTestEventsHandler.targetsSupported(ctx.project, ctx.configuration.targets)) {
    return null
  }

  return BuildResultHelperBep().use { helper ->
    BlazeTestUiSession.create(
      ImmutableList.builder<String>()
        .add("--runs_per_test=1")
        .add("--flaky_test_attempts=1")
        .addAll(helper.buildFlags)
        .build(),
      LocalBuildEventProtocolTestFinderStrategy(helper.outputFile),
    )
  }
}

context(ctx: BazelDefaultLauncherContext)
fun createConsoleBuilder(testUiSession: BlazeTestUiSession?): CidrConsoleBuilder {
  if (ctx.isTest) {
    return BazelTestConsoleBuilder(ctx.configuration, ctx.environment, testUiSession)
  }

  return CidrConsoleBuilder(ctx.project, null, null)
}

context(ctx: BazelDefaultLauncherContext)
fun getConsoleFilters(): ImmutableList<Filter> {
  return ImmutableList.of<Filter>(
    UrlFilter(),
    ToolWindowTaskIssueOutputFilter.createWithDefaultParsers(
      ctx.project,
      WorkspaceRoot.fromProject(ctx.project),
      BlazeInvocationContext.ContextType.RunConfiguration,
    ),
  )
}

context(ctx: BazelDefaultLauncherContext)
fun applyEnvironment(commandLine: GeneralCommandLine) {
  val environmentData = ctx.configState.envVarsState.data
  commandLine.withParentEnvironmentType(
    if (environmentData.isPassParentEnvs) {
      GeneralCommandLine.ParentEnvironmentType.SYSTEM
    } else {
      GeneralCommandLine.ParentEnvironmentType.NONE
    }
  )
  commandLine.environment.putAll(environmentData.envs)
}
