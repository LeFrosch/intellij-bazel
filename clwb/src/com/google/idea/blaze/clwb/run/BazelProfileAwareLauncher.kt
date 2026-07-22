@file:Suppress("UnstableApiUsage")

package com.google.idea.blaze.clwb.run

import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.command.BlazeCommand
import com.google.idea.blaze.base.command.BlazeCommandName
import com.google.idea.blaze.base.command.BlazeFlags
import com.google.idea.blaze.base.command.BlazeInvocationContext
import com.google.idea.blaze.base.command.buildresult.BuildResultHelperBep
import com.google.idea.blaze.base.ideinfo.TargetIdeInfo
import com.google.idea.blaze.base.issueparser.ToolWindowTaskIssueOutputFilter
import com.google.idea.blaze.base.model.BlazeProjectData
import com.google.idea.blaze.base.model.primitives.Label
import com.google.idea.blaze.base.model.primitives.TargetExpression
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot
import com.google.idea.blaze.base.projectview.ProjectViewSet
import com.google.idea.blaze.base.run.BlazeCommandRunConfiguration
import com.google.idea.blaze.base.run.ExecutorType
import com.google.idea.blaze.base.run.smrunner.BlazeTestEventsHandler
import com.google.idea.blaze.base.run.smrunner.BlazeTestUiSession
import com.google.idea.blaze.base.run.testlogs.LocalBuildEventProtocolTestFinderStrategy
import com.google.idea.blaze.base.scope.BlazeContext
import com.google.idea.blaze.base.sync.data.BlazeProjectDataManager
import com.google.idea.sdkcompat.clion.OSTypeCompat
import com.intellij.cidr.debugger.profiles.CidrDebugProfile
import com.intellij.cidr.debugger.profiles.CidrDebugProfileManager
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.UrlFilter
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.cpp.toolchains.CPPEnvironment
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cidr.execution.CidrConsoleBuilder
import com.jetbrains.cidr.execution.CidrLauncher
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import java.nio.file.Path

private val PROC_CWD = Path.of("/proc", "self", "cwd")

private data class BazelDefaultLauncherContext(
  val project: Project,
  val configuration: BlazeCommandRunConfiguration,
  val environment: ExecutionEnvironment,
  val projectData: BlazeProjectData,
  val projectView: ProjectViewSet,
  val configState: BlazeCidrRunConfigState,
)

context(ctx: BazelDefaultLauncherContext)
private fun getExecutionRoot(): Path {
  return ctx.projectData.blazeInfo().executionRoot.toPath()
}

context(ctx: BazelDefaultLauncherContext)
private fun getWorkspaceRoot(): Path {
  return WorkspaceRoot.fromProject(ctx.project).path()
}

context(ctx: BazelDefaultLauncherContext)
private fun isTest(): Boolean {
  return BlazeCommandName.TEST != ctx.configState.commandState.command
}

context(ctx: BazelDefaultLauncherContext)
private fun createConsoleBuilder(testUiSession: BlazeTestUiSession?): CidrConsoleBuilder {
  if (isTest() && testUiSession != null) {
    // TODO: fix this
    // return BlazeCidrLauncher.GoogleTestConsoleBuilder(ctx.project, testUiSession)
  }

  return CidrConsoleBuilder(ctx.project, null, null)
}

context(ctx: BazelDefaultLauncherContext)
private fun getConsoleFilters(): ImmutableList<Filter> {
  return ImmutableList.of(
    UrlFilter(),
    ToolWindowTaskIssueOutputFilter.createWithDefaultParsers(
      ctx.project,
      WorkspaceRoot.fromProject(ctx.project),
      BlazeInvocationContext.ContextType.RunConfiguration
    )
  )
}

context(ctx: BazelDefaultLauncherContext)
private fun createTargetProcess(state: CommandLineState, context: BlazeContext, extraFlags: List<String>) {
  val testUiSession = createTestUiSession()

  val projectViewFlags = BlazeFlags.blazeFlags(
    ctx.project,
    ctx.projectView,
    ctx.configState.commandState.command,
    context,
    BlazeInvocationContext.runConfigContext(
      /* executorType = */ ExecutorType.fromExecutor(ctx.environment.executor),
      /* configurationType = */ ctx.configuration.type,
      /* beforeRunTask = */ false
    )
  )

  val commandBuilder = BlazeCommand.builder(ctx.configState.commandState.command)
    .addTargets(ctx.configuration.targets)
    .addBlazeFlags(extraFlags)
    .addBlazeFlags(projectViewFlags)
    .addBlazeFlags(testUiSession?.blazeFlags ?: ImmutableList.of())
    .addExeFlags(ctx.configState.exeFlagsState.getFlagsForExternalProcesses())

  val testFilterFlag = ctx.configState.testFilterFlag
  if (testFilterFlag != null && isTest()) {
    commandBuilder.addBlazeFlags(testFilterFlag, BlazeFlags.DISABLE_TEST_SHARDING)
  }

  state.consoleBuilder = createConsoleBuilder(testUiSession)
  state.addConsoleFilters(*getConsoleFilters().toTypedArray<Filter?>())
}

context(ctx: BazelDefaultLauncherContext)
private fun createTestUiSession(): BlazeTestUiSession? {
  if (!isTest()) {
    return null
  }

  if (!BlazeTestEventsHandler.targetsSupported(ctx.project, ctx.configuration.targets)) {
    return null
  }

  // copied from BlazeCidrLauncher, but looks really hacky
  return BuildResultHelperBep().use { helper ->
    BlazeTestUiSession.create(
      ImmutableList.builder<String>()
        // we have users complaining about these hardocded flags
        .add("--runs_per_test=1")
        .add("--flaky_test_attempts=1")
        .addAll(helper.buildFlags)
        .build(),
      LocalBuildEventProtocolTestFinderStrategy(helper.outputFile)
    )
  }
}

context(ctx: BazelDefaultLauncherContext)
private fun createDebugDriverConfiguration(debugProfile: CidrDebugProfile<*>): DebuggerDriverConfiguration {
  return debugProfile.createDriverConfiguration(
    ctx.project,
    isElevated = false,
    isEmulateTerminal = false,
    environment = CPPEnvironment(CPPToolchains.Toolchain(OSTypeCompat.getCurrent()))
  )
}

context(ctx: BazelDefaultLauncherContext)
private fun createDebugPathMapping(debugProfile: CidrDebugProfile<*>): Map<Path, Path> {
  // order matters, external paths need to be mapped first
  return mapOf(
    PROC_CWD.resolve("external") to ctx.executionRoot.resolve("external"),
    PROC_CWD to ctx.workspaceRoot,
  )
}

class BazelProfileAwareLauncher(
  private val configuration: BlazeCommandRunConfiguration,
  private val environment: ExecutionEnvironment,
) : CidrLauncher() {

  override fun getProject(): Project = configuration.project

  @Throws(ExecutionException::class)
  override fun createProcess(state: CommandLineState): ProcessHandler? {
    TODO("Not yet implemented")
  }

  @Throws(ExecutionException::class)
  private fun createDefaultProcess() {

  }

  @Throws(ExecutionException::class)
  override fun createDebugProcess(state: CommandLineState, session: XDebugSession): XDebugProcess {
    val debuggerProfile = CidrDebugProfileManager.getInstance().getCurrentDebugProfile(project)
      ?: throw ExecutionException("Debug profile is not set")
    val handler = configuration.handler.state as? BlazeCidrRunConfigState
      ?: throw ExecutionException("Invalid configuration handler.")
    val target = configuration.getSingleTarget()
      ?: throw ExecutionException("Cannot parse run configuration target.")
    val projectData = BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
      ?: throw ExecutionException("Cannot get Blaze project data.")
    val executable = BlazeCidrRunConfigurationRunner.getDebugExecutable(environment)
      ?: throw ExecutionException("No debug binary found.")

    /*
    val executionRoot = projectData.blazeInfo().executionRoot.toPath()
    val workspaceRoot = WorkspaceRoot.fromProject(project).path()

    val commandLine = GeneralCommandLine().apply {
      withExePath(executable.toString())
      withWorkDirectory(executionRoot.toFile())
      addParameters(getTargetArguments(target, projectData))
      addParameters(handler.exeFlagsState.getFlagsForExternalProcesses())

      // TODO: support for env vars
    }

    // the test filter needs to be passed manually since the binary is not run by Bazel
    val testFilter = handler.testFilterForExternalProcesses
    if (!testFilter.isNullOrBlank()) {
      commandLine.withEnvironment(BlazeCidrLauncher.TEST_FILTER_ENV_VARIABLE, testFilter)
    }

    val parameters = TrivialRunParameters(debuggerDriverConfiguration, TrivialInstaller(commandLine))

    val process = runBlockingCancellable {
      withContext(Dispatchers.EDT) {
        CidrLocalDebugProcess(parameters, session, state.consoleBuilder)
      }
    }


    // cannot await postCommand here, since it would deadlock
    process.postCommand { driver ->
      when (debuggerProfile.type.getId()) {
        CLionLldbDebugProfileType.ID -> configureLldbDriver(driver, sourceMappings)
        CLionGdbDebugProfileType.ID -> configureGdbDriver(driver, sourceMappings)
        else -> throw ExecutionException("Unsupported debugger profile type: ${debuggerProfile.type.getId()}")
      }
    }

    return process
  }

  private fun createRemoteDebugProcess(
    state: CommandLineState,
    session: XDebugSession,
    debuggerProfile: CidrDebugProfile<*>,
    handler: BlazeCidrRunConfigState,
  ): XDebugProcess {

    val extraDebugFlags = BlazeGDBServerProvider.getFlagsForDebugging(handler, configuration)

    val targetProcess: ProcessHandler = createProcess(state, extraDebugFlags)

    configProcessHandler(targetProcess, false, true, getProject())


    // CidrRemoteDebugParameters can't be constructed with a null sysroot, so pass in the default
    // value "target:". Causes paths/files to be resolved in the context of the target.
    val parameters =
      CidrRemoteDebugParameters(
        "tcp:localhost:" + handlerState.getDebugPortState().port,
        executable.toString(),
        "target:",
        ImmutableList.of<CidrRemotePathMapping?>(
          CidrRemotePathMapping("/proc/self/cwd", workspaceRootDirectory.getParent())
        )
      )

    val debuggerDriverConfiguration =
      BlazeCLionGDBDriverConfiguration(project)

    return runOnEDT({
      BlazeCidrRemoteDebugProcess(
        targetProcess, debuggerDriverConfiguration, parameters, session, state.getConsoleBuilder()
      )
    })
     */
    return null!!
  }

  private fun configureLldbDriver(driver: DebuggerDriver, sourceMappings: Map<Path, Path>) {
    val command = mutableListOf("settings", "set", "--global", "target.source-map")
    sourceMappings.flatMap { listOf(it.key, it.value) }.forEach { command.add("\"$it\"") }

    driver.executeInterpreterCommand(command.joinToString(" "))
  }

  private fun configureGdbDriver(driver: DebuggerDriver, sourceMappings: Map<Path, Path>) {
    for ((from, to) in sourceMappings) {
      driver.executeInterpreterCommand("set substitute-path \"$from\" \"$to\"")
    }
  }
}

private fun getTargetArguments(target: TargetExpression?, projectData: BlazeProjectData): List<String> {
  if (target !is Label) {
    return emptyList()
  }

  return projectData.targetMap().get(target)
    .asSequence()
    .mapNotNull(TargetIdeInfo::getcIdeInfo)
    .flatMap { it.ruleContext().args() }
    .toList()
}
