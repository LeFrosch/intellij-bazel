package com.google.idea.blaze.cpp.qsync.compiler

import com.google.idea.blaze.exception.BuildException
import com.google.idea.blaze.qsync.deps.CcCompilerInfo
import com.google.idea.blaze.qsync.java.cc.CcCompilationInfoOuterClass.CcToolchainInfo

suspend fun CcCompilerInfoContext.collectCompilerInfo(toolchain: CcToolchainInfo): CcCompilerInfo {
  val kind = try {
    getCompilerKind(toolchain)
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

@Throws(BuildException::class)
private suspend fun CcCompilerInfoContext.getCompilerKind(toolchain: CcToolchainInfo): CcCompilerInfo.Kind {
  val executable = resolve(toolchain.compilerExecutable)

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
