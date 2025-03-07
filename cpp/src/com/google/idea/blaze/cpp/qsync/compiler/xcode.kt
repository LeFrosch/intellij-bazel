package com.google.idea.blaze.cpp.qsync.compiler

import com.google.idea.blaze.base.command.BlazeCommandName
import com.google.idea.blaze.base.sync.aspects.storage.AspectRepositoryProvider
import com.google.idea.blaze.exception.BuildException
import com.google.idea.blaze.qsync.deps.CcCompilerInfo
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private data class XcodeConfig(val version: String?, val sdkVersion: String, val commandLineToolsOnly: Boolean)

@Throws(BuildException::class)
suspend fun CcCompilerInfoContext.getXcodeData(): CcCompilerInfo.Xcode {
  val config = getConfig()

  val developerDir = if (config.commandLineToolsOnly) {
    Path.of("/Library/Developer/CommandLineTools")
  } else {
    getDeveloperDir(config.version!!)
  }

  val sdkRoot = Path.of(developerDir.toString(), "SDKs", "MacOSX.sdk")

  return CcCompilerInfo.Xcode(developerDir.toString(), sdkRoot.toString())
}

@Throws(BuildException::class)
private suspend fun CcCompilerInfoContext.getConfig(): XcodeConfig {
  val queryFile = try {
    val source = AspectRepositoryProvider.aspectQSyncFile("xcode_query.bzl")

    withContext(Dispatchers.IO) {
      val tmpFile = Files.createTempFile("xcode_query", ".bzl")
      Files.copy(source.openStream(), tmpFile, StandardCopyOption.REPLACE_EXISTING)

      tmpFile
    }
  } catch (e: IOException) {
    throw BuildException("could not copy xcode query file", e)
  }

  val result = execBazel(BlazeCommandName.CQUERY) {
    addBlazeFlags("deps(@bazel_tools//tools/osx:current_xcode_config)")
    addBlazeFlags("--output=starlark")
    addBlazeFlags("--starlark:file='$queryFile'")
  }

  if (result.exitCode == 37) {
    // if the target doesn't exist, bazel will exit with code 37
    throw BuildException("@bazel_tools//tools/osx:xcode-locator not found")
  }

  if (result.exitCode != 0) {
    throw BuildException("xcode query failed: ${result.stderr}")
  }

  return parseXcodeConfig(result.stdout)
}

@Throws(BuildException::class)
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
private suspend fun CcCompilerInfoContext.getDeveloperDir(version: String): Path {
  val result = execBazel(BlazeCommandName.RUN) {
    addBlazeFlags("@bazel_tools//tools/osx:xcode-locator")
    addExeFlags(version)
  }

  if (result.exitCode != 0) {
    throw BuildException("could not locate xcode: ${result.stderr}")
  }

  return try {
    Path.of(result.stdout.trim())
  } catch (e: InvalidPathException) {
    throw BuildException("invalid xcode location: ${result.stdout}")
  }
}
