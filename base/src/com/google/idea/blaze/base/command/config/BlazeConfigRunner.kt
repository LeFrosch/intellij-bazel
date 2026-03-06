package com.google.idea.blaze.base.command.config

import com.google.common.collect.ImmutableMap
import com.google.gson.JsonParser
import com.google.idea.blaze.base.bazel.BuildSystem.BuildInvoker
import com.google.idea.blaze.base.buildview.BazelExecService
import com.google.idea.blaze.base.command.BlazeCommand
import com.google.idea.blaze.base.command.BlazeCommandName
import com.google.idea.blaze.base.model.BlazeConfiguration
import com.google.idea.blaze.base.model.BlazeConfigurationData
import com.google.idea.blaze.base.scope.BlazeContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

private val LOG = Logger.getInstance(BlazeConfigRunner::class.java)

object BlazeConfigRunner {

  /**
   * Runs `bazel config --dump_all --output=json` and parses the result into [BlazeConfigurationData].
   *
   * This captures ALL configurations from Skyframe, including transition configurations
   * that BEP does not emit. Must be called in the same Bazel server session as the build.
   *
   * @return parsed configuration data, or null if the command fails
   */
  @JvmStatic
  fun fetchAllConfigurations(
    project: Project,
    invoker: BuildInvoker,
    ctx: BlazeContext,
  ): BlazeConfigurationData? {
    return try {
      val cmdBuilder = BlazeCommand.builder(invoker, BlazeCommandName.CONFIG)
        .addBlazeFlags("--dump_all", "--output=json")

      val json = BazelExecService.instance(project).exec(ctx, cmdBuilder)
      val configs = parseConfigJson(json)
      BlazeConfigurationData.create(configs)
    } catch (e: Exception) {
      LOG.warn("Failed to run bazel config --dump_all", e)
      null
    }
  }

  private fun parseConfigJson(json: String): ImmutableMap<String, BlazeConfiguration> {
    val configs = JsonParser.parseString(json).asJsonArray
    val result = ImmutableMap.builder<String, BlazeConfiguration>()
    for (element in configs) {
      val obj = element.asJsonObject
      val configHash = obj.get("configHash").asString
      result.put(configHash, BlazeConfiguration.fromConfigJson(obj))
    }
    return result.buildKeepingLast()
  }
}
