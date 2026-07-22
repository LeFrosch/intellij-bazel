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
import com.google.idea.blaze.clwb.ToolchainUtils
import com.google.idea.blaze.clwb.run.BazelDebugFlagsBuilder
import com.google.idea.blaze.clwb.run.BlazeDebuggerKind
import com.google.idea.blaze.clwb.run.RunConfigurationUtils
import com.google.idea.blaze.clwb.sync.shouldInjectDebugFlags
import com.google.idea.common.util.Datafiles
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.cidr.cpp.toolchains.CPPDebugger
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cidr.execution.debugger.CidrDebuggerPathManager
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

private val LOG = logger<BazelGdbServerDebugProfileType>()

/**
 * A script shipped with the plugin that makes gdbserver behave the way the environment expects: it forwards
 * signals, exits with the same code as the inferior, and escapes parameters correctly.
 */
private val GDBSERVER_WRAPPER: Path by Datafiles.resolveLazy("gdb/gdbserver")

/** Bazel flags that run the target under gdbserver listening on [port], so a local gdb can attach to it. */
context(ctx: BazelDefaultLauncherContext)
fun getGdbServerFlags(port: Int): ImmutableList<String> {
  val builder = BazelDebugFlagsBuilder.fromDefaults(
    BlazeDebuggerKind.GDB_SERVER,
    RunConfigurationUtils.getCompilerKind(ctx.configuration),
  )

  if (shouldInjectDebugFlags(ctx.project)) {
    builder.withBuildFlags()
  }

  if (ctx.isTest) {
    builder.withTestFlags()
  }

  // if gdbserver could not be found, fall back to trying PATH
  val gdbServerPath = resolveGdbServerPath(ToolchainUtils.getToolchain()) ?: "gdbserver"
  builder.withRunUnderGDBServer(gdbServerPath, port, GDBSERVER_WRAPPER.toString())

  return builder.build()
}

private fun resolveGdbServerPath(toolchain: CPPToolchains.Toolchain): String? {
  val gdbPath = when (toolchain.debuggerKind) {
    CPPDebugger.Kind.CUSTOM_GDB -> toolchain.customGDBExecutablePath
    CPPDebugger.Kind.BUNDLED_GDB -> CidrDebuggerPathManager.getBundledGDBBinary().path
    else -> {
      LOG.error("Trying to resolve gdbserver executable for ${toolchain.debuggerKind}")
      return null
    }
  }

  // there is no dedicated toolchain setting for gdbserver, so try appending "server" to the gdb path
  val gdbServer = Path.of(gdbPath + "server")
  if (!Files.exists(gdbServer)) {
    return null
  }

  return gdbServer.absolutePathString()
}
