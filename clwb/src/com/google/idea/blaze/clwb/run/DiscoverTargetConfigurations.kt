package com.google.idea.clwb.src.com.google.idea.blaze.clwb.run

import com.google.idea.base.src.com.google.idea.blaze.base.buildview.ContextRoutine
import com.google.idea.blaze.base.buildview.BazelExecService
import com.google.idea.blaze.base.command.BlazeCommand
import com.google.idea.blaze.base.command.BlazeCommandName
import com.google.idea.blaze.base.model.primitives.Label
import com.google.idea.blaze.base.scope.BlazeContext
import com.google.idea.common.aquery.ActionGraph
import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import java.io.IOException
import kotlin.jvm.Throws

private const val CC_COMPILE_MNEMONIC = "CppCompile"

class DiscoverTargetConfigurations(
  private val project: Project,
  private val target: Label,
) : ContextRoutine<DiscoverTargetConfigurations.Output> {

  override val name: String = "Discover CC Target Configurations"

  override val propagateErrors: Boolean = false

  @Throws(ExecutionException::class)
  override fun run(ctx: BlazeContext): Output {
    val cmd = BlazeCommand.builder(BlazeCommandName.AQUERY)
      .addBlazeFlags("--output=streamed_proto")
      .addTargets(target)

    BazelExecService.of(project).exec(ctx, cmd).use { result ->
      result.throwOnFailure()

      val graph = try {
        ActionGraph.fromProto(result.stdout)
      } catch (e: IOException) {
        throw ExecutionException("failed to parse action graph", e)
      }

      val configurations = graph.targets.mapNotNull { target ->
        target.compileAction()?.let { Label.create(target.label) to it }
      }.toMap()

      return Output(
        mainTarget = Label.create(graph.defaultTarget.label),
        mainConfiguration = graph.defaultConfiguration,
        compileActions = configurations,
      )
    }
  }

  data class Output(
    val mainTarget: Label,
    val mainConfiguration: String,
    val compileActions: Map<Label, ActionGraph.Action>,
  )
}

private fun ActionGraph.Target.compileAction(): ActionGraph.Action? {
  return actions.firstOrNull { it.mnemonic == CC_COMPILE_MNEMONIC }
}