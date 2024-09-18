package com.google.idea.blaze.base.lang.pylark.resolve

import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyStringLiteralExpression

internal class LabelReferenceProvider : BuildFileReferenceContributor.Provider {
  override fun getElementPattern(): PsiElementPattern.Capture<*> {
    return PlatformPatterns.psiElement(PyStringLiteralExpression::class.java)
  }

  override fun getElementReference(element: PsiElement, ctx: ProcessingContext): PsiReference? {
    if (element !is PyStringLiteralExpression) return null

    val load = getLoadExpression(element)
    if (load == null) {
      return LabelReference(element, true)
    }

    val module = load.getArgument(0, PyStringLiteralExpression::class.java) ?: return null

    val moduleReference = LabelReference(module, false)
    if (element == module) return moduleReference

    // TODO: Symbol reference

    return null
  }

  private fun getLoadExpression(element: PsiElement): PyCallExpression? {
    var parent = element.parent

    if (parent is PyKeywordArgument) {
      parent = parent.parent
    }

    if (parent is PyArgumentList) {
      parent = parent.parent
    }

    if (parent !is PyCallExpression) return null
    if (parent.name != "load") return null

    return parent
  }
}