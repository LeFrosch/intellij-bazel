package com.google.idea.blaze.cpp.qsync.compiler

import com.google.idea.blaze.exception.BuildException
import com.google.idea.blaze.qsync.deps.CcCompilerInfo
import com.google.idea.blaze.qsync.java.cc.CcCompilationInfoOuterClass.CcToolchainInfo
import java.io.IOException
import java.nio.file.Path

@Throws(BuildException::class)
suspend fun CcCompilerInfoContext.collectCompilerInfo(toolchain: CcToolchainInfo): CcCompilerInfo {
  val executable = resolve(toolchain.compilerExecutable)
  val kind = try {
    getCompilerKind(executable)
  } catch (e: BuildException) {
    warn("could not detect compiler kind", e)
    CcCompilerInfo.Kind.UNKNOWN
  }

  val builder = CcCompilerInfo.builder()
    .executable(executable.toString())
    .kind(kind)

  if (kind == CcCompilerInfo.Kind.APPLE_CLANG) {
    try {
      builder.xcode(getXcodeData())
    } catch (e: BuildException) {
      warn("could not get xcode info", e)
    }
  }

  if (kind == CcCompilerInfo.Kind.MSVC) {
    try {
      // builder.msvc(getMsvcData())
    } catch (e: BuildException) {
      warn("could not get msvc info", e)
    }
  }

  return builder.build()
}

@Throws(BuildException::class)
private suspend fun CcCompilerInfoContext.getCompilerKind(executable: Path): CcCompilerInfo.Kind {
  val result = exec(executable, "--version")

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

@Throws(BuildException::class)
private fun guessCompilerKind(version: String): CcCompilerInfo.Kind {
  return when {
    version.startsWith("Apple clang") -> CcCompilerInfo.Kind.APPLE_CLANG
    version.contains("clang") -> CcCompilerInfo.Kind.CLANG
    version.contains("gcc") -> CcCompilerInfo.Kind.GCC
    version.contains("Microsoft") -> CcCompilerInfo.Kind.MSVC
    else -> throw BuildException("unknown compiler version: $version")
  }
}
