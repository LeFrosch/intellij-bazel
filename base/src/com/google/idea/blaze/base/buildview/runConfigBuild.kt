package com.google.idea.blaze.base.buildview

import com.google.idea.base.src.com.google.idea.blaze.base.buildview.ContextRoutine
import com.google.idea.blaze.base.command.BlazeCommand
import com.google.idea.blaze.base.command.BlazeCommandName
import com.google.idea.blaze.base.command.BlazeFlags
import com.google.idea.blaze.base.command.BlazeInvocationContext
import com.google.idea.blaze.base.command.buildresult.BuildResult
import com.google.idea.blaze.base.command.buildresult.LocalFileArtifact
import com.google.idea.blaze.base.model.primitives.Label
import com.google.idea.blaze.base.projectview.ProjectViewManager
import com.google.idea.blaze.base.run.BlazeCommandRunConfiguration
import com.google.idea.blaze.base.run.state.BlazeCommandRunConfigurationCommonState
import com.google.idea.blaze.base.scope.BlazeContext
import com.google.idea.blaze.base.sync.aspects.BlazeBuildOutputs
import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import com.intellij.util.PathUtil
import com.intellij.util.asSafely
import java.io.File
import java.nio.file.Path

private const val DEFAULT_OUTPUT_GROUP_NAME = "default"

class RunConfigBuild(
  private val project: Project,
  private val configuration: BlazeCommandRunConfiguration,
  private val invocationContext: BlazeInvocationContext,
  private val target: Label,
) : ContextRoutine<RunConfigBuild.Output> {

  override val name: String = "Building $target"

  @Throws(ExecutionException::class)
  override fun run(ctx: BlazeContext): Output {
    val projectViewSet = ProjectViewManager.getInstance(project).getProjectViewSet()
    val handlerState = configuration.handler.state.asSafely<BlazeCommandRunConfigurationCommonState>()

    val flags = BlazeFlags.blazeFlags(
      project,
      projectViewSet,
      BlazeCommandName.BUILD,
      ctx,
      invocationContext,
    )

    val externalFlags = handlerState
      ?.blazeFlagsState
      ?.flagsForExternalProcesses
      ?: emptyList()

    val cmd = BlazeCommand.builder(BlazeCommandName.BUILD)
      .addTargets(target)
      .addBlazeFlags(flags)
      .addBlazeFlags(externalFlags)
      .addBlazeFlags()

    val result = BazelExecService.of(project).build(ctx, cmd)
    if (result.buildResult().status != BuildResult.Status.SUCCESS) {
      throw ExecutionException("Build failed (${result.buildResult().exitCode})")
    }

    return Output(target = target, executable = findExecutable(result, target))
  }

  data class Output(
    val target: Label,
    val executable: Path,
  )
}

@Throws(ExecutionException::class)
private fun findExecutable(output: BlazeBuildOutputs, target: Label): Path {
  // should only be called if the build succeeds
  require(output.buildResult().status == BuildResult.Status.SUCCESS)

  // manually find local artifacts in favour of LocalFileArtifact.getLocalFiles, to avoid the artifact cache,
  // since atm it does not preserve the executable flag for files (and there might be other issues)
  val artifacts = output.getOutputGroupTargetArtifacts(DEFAULT_OUTPUT_GROUP_NAME, target.toString())
    .filterIsInstance<LocalFileArtifact>()
    .map(LocalFileArtifact::getFile)
    .filter(File::canExecute)
    .toList()

  if (artifacts.isEmpty()) {
    throw ExecutionException("No output artifacts found for build")
  }

  if (artifacts.size == 1) {
    return artifacts.first().toPath()
  }

  val name = PathUtil.getFileName(target.targetName().toString())
  return artifacts.firstOrNull { it.name == name }?.toPath() ?: throw ExecutionException("No executable found for build")
}