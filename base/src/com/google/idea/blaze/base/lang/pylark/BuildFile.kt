package com.google.idea.blaze.base.lang.pylark

import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyFileImpl
import icons.BlazeIcons
import javax.swing.Icon

class BuildFile(viewProvider: FileViewProvider) : PyFileImpl(viewProvider, BuildFileLanguage) {
  enum class Flavor { BUILD, WORKSPACE, MODULE, STARLARK }

  override fun getIcon(flags: Int): Icon = BlazeIcons.BuildFile

  val flavor: Flavor
    get() = when {
      name.startsWith("BUILD") -> Flavor.BUILD
      name.startsWith("WORKSPACE") -> Flavor.WORKSPACE
      name.startsWith("MODULE") -> Flavor.MODULE
      else -> Flavor.STARLARK
    }

  fun findRule(targetName: String): PyExpressionStatement? {
    return children.filterIsInstance<PyExpressionStatement>().firstOrNull {
      isRule(it, targetName)
    }
  }

  private fun isRule(stmt: PyExpressionStatement, targetName: String): Boolean {
    val expr = stmt.expression
    if (expr !is PyCallExpression) return false

    val literal = expr.getKeywordArgument("name") ?: return false
    if (literal !is PyStringLiteralExpression) return false

    return literal.stringValue == targetName
  }

  fun findSymbol(symbolName: String): PsiElement? {
    return children.firstNotNullOfOrNull {
      isAssignment(it, symbolName) ?: isFunction(it, symbolName) // ?: fromLoad()
    }
  }

  private fun isAssignment(stmt: PsiElement, symbolName: String): PsiElement? {
    if (stmt !is PyAssignmentStatement) return null
    return stmt.targets.firstOrNull { it.name == symbolName }
  }

  private fun isFunction(stmt: PsiElement, symbolName: String): PsiElement? {
    if (stmt !is PyFunction || stmt.name != symbolName) return null
    return stmt
  }
}