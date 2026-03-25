package com.google.idea.common.aquery;

import com.google.devtools.build.lib.analysis.AnalysisProtosV2
import com.google.protobuf.TextFormat
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import kotlin.jvm.Throws

class ActionGraph(
  private val ruleClassMap: Map<Int, String>,
  private val configurationMap: Map<Int, AnalysisProtosV2.Configuration>,
  private val pathFragmentMap: Map<Int, AnalysisProtosV2.PathFragment>,
  private val artifactMap: Map<Int, AnalysisProtosV2.Artifact>,
  private val targetMap: Map<Int, AnalysisProtosV2.Target>,
  private val allActions: List<AnalysisProtosV2.Action>,
) {

  companion object {

    @Throws(IOException::class)
    fun fromProto(text: String): ActionGraph {
      val builder = AnalysisProtosV2.ActionGraphContainer.newBuilder()
      TextFormat.Parser.newBuilder().build().merge(text, builder)

      return fromProto(builder.build())
    }

    @Throws(IOException::class)
    fun fromProto(stream: InputStream): ActionGraph {
      val builder = AnalysisProtosV2.ActionGraphContainer.newBuilder()
      generateSequence { AnalysisProtosV2.ActionGraphContainer.parseDelimitedFrom(stream) }.forEach(builder::mergeFrom)

      return fromProto(builder.build())
    }

    fun fromProto(input: AnalysisProtosV2.ActionGraphContainer): ActionGraph = ActionGraph(
      ruleClassMap = input.ruleClassesList.associate { it.id to it.name },
      configurationMap = input.configurationList.associateBy { it.id },
      pathFragmentMap = input.pathFragmentsList.associateBy { it.id },
      artifactMap = input.artifactsList.associateBy { it.id },
      targetMap = input.targetsList.associateBy { it.id },
      allActions = input.actionsList,
    )
  }

  val targets: Sequence<Target> get() = targetMap.values.asSequence().map { Target(it) }

  val defaultTarget: Target get() = Target(targetMap.values.minBy { it.id })

  val defaultConfiguration: String get() = configurationMap.values.minBy { it.id }.checksum

  private fun resolvePath(path: Int): Path {
    return generateSequence(pathFragmentMap.getValue(path)) { pathFragmentMap.getOrDefault(it.parentId, null) }
      .fold(Path.of("")) { acc, fragment -> Path.of(fragment.label).resolve(acc) }
  }

  inner class Target(private val src: AnalysisProtosV2.Target) {

    val label: String get() = src.label

    val ruleClass: String get() = ruleClassMap.getValue(src.ruleClassId)

    val actions: Sequence<Action> get() = allActions.asSequence().filter { it.targetId == src.id }.map { Action(it) }
  }

  inner class Artifact(private val src: AnalysisProtosV2.Artifact) {

    val path: Path get() = resolvePath(src.pathFragmentId)

    val isTreeArtifact: Boolean get() = src.isTreeArtifact
  }

  inner class Action(private val src: AnalysisProtosV2.Action) {

    val target: Target get() = Target(targetMap.getValue(src.targetId))

    val mnemonic: String get() = src.mnemonic

    val arguments: List<String> get() = src.argumentsList

    val configuration: String get() = configurationMap.getValue(src.configurationId).checksum

    val outputs: Sequence<Artifact> get() = src.outputIdsList.asSequence().map { Artifact(artifactMap.getValue(it)) }

    val primaryOutput: Artifact get() = Artifact(artifactMap.getValue(src.primaryOutputId))
  }
}