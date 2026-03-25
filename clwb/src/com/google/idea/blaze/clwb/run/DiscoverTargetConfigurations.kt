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

private val CC_MNEMONICS = listOf("CppCompile", "CppLink")

class DiscoverTargetConfigurations(
  private val project: Project,
  private val target: Label,
) : ContextRoutine<DiscoverTargetConfigurations.Output> {

  override val name: String = "Discover CC Target Configurations"

  @Throws(ExecutionException::class)
  override fun run(ctx: BlazeContext): Output {
    val cmd = BlazeCommand.builder(BlazeCommandName.AQUERY)
    cmd.addBlazeFlags("--output=streamed_proto", target.toString())

    val result = BazelExecService.of(project).exec(ctx, cmd)
    result.throwOnFailure()

    val graph = try {
      ActionGraph.fromProto(result.stdout)
    } catch (e: IOException) {
      throw ExecutionException("failed to parse action graph", e)
    }

    val configurations = graph.targets.mapNotNull { target ->
      target.actions.firstOrNull { it.mnemonic in CC_MNEMONICS }?.let {
        Label.create(target.label) to it.configuration
      }
    }.toMap()

    return Output(
      mainTarget = Label.create(graph.defaultTarget.label),
      mainConfiguration = graph.defaultConfiguration,
      targetConfigurations = configurations,
    )
  }

  data class Output(
    val mainTarget: Label,
    val mainConfiguration: String,
    val targetConfigurations: Map<Label, String>,
  )
}

