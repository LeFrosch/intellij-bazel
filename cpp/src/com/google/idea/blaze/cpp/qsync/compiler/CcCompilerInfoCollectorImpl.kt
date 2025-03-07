package com.google.idea.blaze.cpp.qsync.compiler

import com.google.common.collect.ImmutableMap
import com.google.idea.blaze.base.buildview.pushJob
import com.google.idea.blaze.base.qsync.CcCompilerInfoCollector
import com.google.idea.blaze.base.qsync.CcCompilerInfoCollectorProvider
import com.google.idea.blaze.base.scope.BlazeContext
import com.google.idea.blaze.base.util.pluginProjectScope
import com.google.idea.blaze.exception.BuildException
import com.google.idea.blaze.qsync.deps.CcCompilerInfo
import com.google.idea.blaze.qsync.deps.CcCompilerInfoMap
import com.google.idea.blaze.qsync.deps.OutputInfo
import com.google.idea.blaze.qsync.project.ProjectPath
import com.intellij.openapi.project.Project

class CcCompilerInfoCollectorImpl private constructor(
  private val project: Project,
  private val resolver: ProjectPath.Resolver,
) : CcCompilerInfoCollector {

  class Provider : CcCompilerInfoCollectorProvider {
    override fun create(project: Project, resolver: ProjectPath.Resolver): CcCompilerInfoCollector {
      return CcCompilerInfoCollectorImpl(project, resolver)
    }
  }

  @Throws(BuildException::class)
  override fun run(ctx: BlazeContext, outputInfo: OutputInfo): CcCompilerInfoMap {
    return try {
      ctx.pushJob(pluginProjectScope(project), "CcCompilerInfoCollector") {
        doRun(CcCompilerInfoContext(project, ctx, resolver), outputInfo)
      }
    } catch (e: Exception) {
      throw BuildException("Unhandled exception", e)
    }
  }

  private suspend fun doRun(ctx: CcCompilerInfoContext, info: OutputInfo): CcCompilerInfoMap {
    val compilerInfos = info.ccCompilationInfo.flatMap { it.toolchainsList }.distinctBy { it.id }

    val builder = ImmutableMap.builder<String, CcCompilerInfo>()
    for (compilerInfo in compilerInfos) {
      builder.put(compilerInfo.id, ctx.collectCompilerInfo(compilerInfo))
    }

    return CcCompilerInfoMap(builder.build())
  }
}

data class CcCompilerInfoContext(
  val project: Project,
  val context: BlazeContext,
  val resolver: ProjectPath.Resolver,
)
