package com.google.idea.blaze.base.lang.pylark.resolve

import com.google.idea.blaze.base.lang.pylark.BuildFile
import com.google.idea.blaze.base.lang.pylark.getBuildFileFlavor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.impl.PyBuiltinCache
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import java.nio.file.Path

private val LOG = Logger.getInstance(BuildFileBuiltinsProvider::class.java)
private val BUILTINS_PATH = Path.of("resources", "pylark")

class BuildFileBuiltinsProvider : PyReferenceResolveProvider {
  override fun resolveName(expression: PyQualifiedExpression, context: TypeEvalContext): List<RatedResolveResult?> {
    val flavor = expression.getBuildFileFlavor() ?: return emptyList()
    val cache = CacheCacheService.of(expression.project, flavor) ?: return emptyList()
    val element = cache.getByName(expression.name) ?: return emptyList()

    return listOf(RatedResolveResult(RatedResolveResult.RATE_NORMAL, element))
  }
}

@Service(Service.Level.PROJECT)
private class CacheCacheService(private val project: Project) {
  companion object {
    fun of(project: Project, flavor: BuildFile.Flavor): PyBuiltinCache? {
      val service = project.service<CacheCacheService>()

      return when(flavor) {
        BuildFile.Flavor.BUILD -> service.build
        BuildFile.Flavor.STARLARK -> service.starlark
        else -> service.default
      }
    }
  }

  private val default: PyBuiltinCache? by lazy { createBuiltinCache(project, "default") }
  private val starlark: PyBuiltinCache? by lazy { createBuiltinCache(project, "starlark") }
  private val build: PyBuiltinCache? by lazy { createBuiltinCache(project, "build") }

  private fun createBuiltinCache(project: Project, name: String): PyBuiltinCache? {
    val path = BUILTINS_PATH.resolve("$name.py")

    val url = this.javaClass.classLoader.getResource(path.toString())
    if (url == null) {
      LOG.error("could not get builtin resource: $name")
      return null
    }

    val file = VfsUtil.findFileByURL(url)
    if (file == null) {
      LOG.error("could not find builtin file: $name")
      return null
    }

    var psiFile = PsiManager.getInstance(project).findFile(file)
    if (psiFile !is PyFile) {
      LOG.error("could not get builtin psi file: $name")
      return null
    }

    return PyBuiltinCache(psiFile, null)
  }
}