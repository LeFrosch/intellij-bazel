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
package com.google.idea.blaze.cpp.qsync

import com.google.common.collect.ImmutableMap
import com.google.idea.blaze.base.buildview.pushJob
import com.google.idea.blaze.base.projectview.ProjectViewManager
import com.google.idea.blaze.base.projectview.section.sections.BazelBinarySection
import com.google.idea.blaze.base.qsync.CompilerInfoCollector
import com.google.idea.blaze.base.qsync.CompilerInfoCollectorProvider
import com.google.idea.blaze.base.scope.BlazeContext
import com.google.idea.blaze.base.scope.output.IssueOutput
import com.google.idea.blaze.base.settings.BlazeUserSettings
import com.google.idea.blaze.base.sync.aspects.storage.AspectRepositoryProvider
import com.google.idea.blaze.base.util.pluginProjectScope
import com.google.idea.blaze.common.PrintOutput
import com.google.idea.blaze.exception.BuildException
import com.google.idea.blaze.qsync.deps.CcCompilerInfo
import com.google.idea.blaze.qsync.deps.CompilerInfoMap
import com.google.idea.blaze.qsync.deps.OutputInfo
import com.google.idea.blaze.qsync.java.cc.CcCompilationInfoOuterClass.CcToolchainInfo
import com.google.idea.blaze.qsync.project.ProjectPath
import com.google.idea.blaze.qsync.project.ProjectProto
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.eel.executeProcess
import com.intellij.platform.eel.getOrThrow
import com.intellij.platform.eel.impl.utils.awaitProcessResult
import com.intellij.platform.eel.provider.getEelDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*

private val LOG = logger<CompilerInfoCollectorImpl>()

class CompilerInfoCollectorImpl private constructor(
  private val project: Project,
  private val resolver: ProjectPath.Resolver,
) : CompilerInfoCollector {

  class Provider : CompilerInfoCollectorProvider {
    override fun create(project: Project, resolver: ProjectPath.Resolver): CompilerInfoCollector {
      return CompilerInfoCollectorImpl(project, resolver)
    }
  }

  @Throws(BuildException::class)
  override fun run(ctx: BlazeContext, outputInfo: OutputInfo): CompilerInfoMap {
    return try {
      ctx.pushJob(pluginProjectScope(project), "CompilerInfoCollector") {
        run(ExecutionContext(project, ctx, resolver), outputInfo)
      }
    } catch (e: Exception) {
      throw BuildException("Unhandled exception", e)
    }
  }

  private suspend fun run(ctx: ExecutionContext, info: OutputInfo): CompilerInfoMap {
    val compilerInfos = info.ccCompilationInfo.flatMap { it.toolchainsList }.distinctBy { it.id }

    val builder = ImmutableMap.builder<String, CcCompilerInfo>()
    for (compilerInfo in compilerInfos) {
      builder.put(compilerInfo.id, ctx.run(compilerInfo))
    }

    return CompilerInfoMap(builder.build())
  }
}

private suspend fun ExecutionContext.run(toolchain: CcToolchainInfo): CcCompilerInfo {
  val kind = try {
    getCompilerKindImpl(toolchain)
  } catch (e: BuildException) {
    warn("could not detect compiler kind", "Exception: %s", e.toString())
    CcCompilerInfo.Kind.UNKNOWN
  }

  val builder = CcCompilerInfo.builder().kind(kind)

  if (kind == CcCompilerInfo.Kind.APPLE_CLANG) {
    try {
      builder.xcode(getXcodeData())
    } catch (e: BuildException) {
      warn("could not get xcode info", "Exception: %s", e.toString())
    }
  }

  if (kind == CcCompilerInfo.Kind.MSVC) {
    try {
      // builder.msvc(getMsvcData())
    } catch (e: BuildException) {
      warn("could not get msvc info", "Exception: %s", e.toString())
    }
  }

  return builder.build()
}

private class ExecutionContext(
  val project: Project,
  private val ctx: BlazeContext,
  private val resolver: ProjectPath.Resolver,
) {

  fun resolve(path: String): Path {
    return resolver.resolve(ProjectPath.execrootRelative(path))
  }

  fun warn(title: String, format: String = "", vararg args: Any?) {
    val msg = String.format(format, *args)

    LOG.warn(String.format("%s: %s", title, msg))
    IssueOutput.warn(title).withDescription(msg).submit(ctx)
  }

  val bazelBinary: Path by lazy {
    val projectSpecificBinary = getProjectSpecificBazelBinary()
    if (projectSpecificBinary != null) {
      return@lazy projectSpecificBinary
    }

    try {
      Path.of(BlazeUserSettings.getInstance().bazelBinaryPath)
    } catch (e: InvalidPathException) {
      throw BuildException("invalid bazel path", e)
    }
  }

  private fun getProjectSpecificBazelBinary(): Path? {
    val projectView = ProjectViewManager.getInstance(project).projectViewSet ?: return null

    return projectView.getScalarValue(BazelBinarySection.KEY)
      .map(File::toPath)
      .orElse(null)
  }
}

@Throws(BuildException::class)
@Suppress("UnstableApiUsage") // EEL API is still unstable
private suspend fun ExecutionContext.getCompilerKindImpl(toolchain: CcToolchainInfo): CcCompilerInfo.Kind {
  val executable = resolve(toolchain.compilerExecutable)

  val result = project.getEelDescriptor().upgrade().exec
    .executeProcess(executable.toString(), "--version")
    .getOrThrow { BuildException("could not execute compiler: ${it.message}") }
    .awaitProcessResult()

  if (result.exitCode == 0) {
    return guessCompilerKind(result.stdout)
  }

  // MSVC does not know the --version flag and will fail. However, the error message does contain
  // the compiler version.
  if (result.stderr.contains("Microsoft")) {
    return CcCompilerInfo.Kind.MSVC
  }

  throw BuildException("could not detect compiler kind: ${result.stderr}")
}

private fun guessCompilerKind(version: String): CcCompilerInfo.Kind {
  return when {
    version.startsWith("Apple clang") -> CcCompilerInfo.Kind.APPLE_CLANG
    version.contains("clang") -> CcCompilerInfo.Kind.CLANG
    version.contains("gcc") -> CcCompilerInfo.Kind.GCC
    version.contains("Microsoft") -> CcCompilerInfo.Kind.MSVC
    else -> throw BuildException("unknown compiler version: $version")
  }
}

private val XCODE_QUERY_TEMPLATE =
  "cquery 'deps(@bazel_tools//tools/osx:current_xcode_config)' --output=starlark --starlark:file='%s'"

@Suppress("UnstableApiUsage") // EEL API is still unstable
private suspend fun ExecutionContext.getXcodeData(): CcCompilerInfo.Xcode {
  val queryFileSource = AspectRepositoryProvider.aspectQSyncFile("xcode_query.bzl")

  val queryFile = try {
    withContext(Dispatchers.IO) {
      val tmpFile = Files.createTempFile("xcode_query", ".bzl")
      Files.copy(queryFileSource.openStream(), tmpFile, StandardCopyOption.REPLACE_EXISTING)

      tmpFile
    }
  } catch (e: IOException) {
    throw BuildException("could not copy xcode query file", e)
  }

  val result = project.getEelDescriptor().upgrade().exec
    .executeProcess(bazelBinary.toString(), String.format(XCODE_QUERY_TEMPLATE, queryFile.toString()))
    .getOrThrow { BuildException("could not execute xcode query: ${it.message}") }
    .awaitProcessResult()

  if (result.exitCode == 37) {
    // if the target doesn't exist, bazel will exit with code 37
    throw BuildException("@bazel_tools//tools/osx:xcode-locator not found")
  }

  if (result.exitCode != 0) {
    throw BuildException("xcode query failed: ${result.stderr}")
  }

  val config = parseXcodeConfig(result.stdout)

  val developerDir = if (config.commandLineToolsOnly) {
    Path.of("/Library/Developer/CommandLineTools")
  } else {
    getDeveloperDir(config.version!!)
  }

  val sdkRoot = Path.of(developerDir.toString(), "SDKs", "MacOSX.sdk")

  return CcCompilerInfo.Xcode(developerDir.toString(), sdkRoot.toString())
}

private data class XcodeConfig(val version: String?, val sdkVersion: String, val commandLineToolsOnly: Boolean)

private fun parseXcodeConfig(stdout: String): XcodeConfig {
  for (line in stdout.lines()) {
    if (line.isBlank()) continue

    val versions = line.split(' ')
    if (versions.size != 2) {
      throw BuildException("could not parse xcode versions: $line")
    }

    // if you only have CommandLineTools installed the query will fail to fetch an xcode version,
    // but it will still return an SDK version
    if (versions[0] == "None") {
      return XcodeConfig(null, versions[1], true)
    }

    // Using the first occurrence is fine because the target we query is an alias for the current config anyway
    return XcodeConfig(versions[0], versions[1], false)
  }

  throw BuildException("no usable scode version returned by query: $stdout")
}

/**
 * Pass the version to the xcode locator, so that it returns the developer dir. This is a mirror
 * of Bazel's own behavior: Ref:
 * https://github.com/bazelbuild/bazel/blob/1811e82ca4e68c2dd52eed7907c3d1926237e18a/src/main/java/com/google/devtools/build/lib/exec/local/XcodeLocalEnvProvider.java#L241
 */
private suspend fun ExecutionContext.getDeveloperDir(version: String): Path {
  // TODO: add flags from project view to avoid invalidation
  val result = project.getEelDescriptor().upgrade().exec
    .executeProcess(bazelBinary.toString(), "run @bazel_tools//tools/osx:xcode-locator -- $version")
    .getOrThrow { BuildException("could not execute xcode query: ${it.message}") }
    .awaitProcessResult()

  if (result.exitCode != 0) {
    throw BuildException("could not locate xcode: ${result.stderr}")
  }

  return try {
    Path.of(result.stdout.trim())
  } catch (e: InvalidPathException) {
    throw BuildException("invalid xcode location: ${result.stdout}")
  }
}
