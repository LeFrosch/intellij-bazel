package com.google.idea.blaze.base.lang.pylark.resolve

import com.google.idea.blaze.base.lang.pylark.BuildFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.asSafely
import com.jetbrains.python.psi.PyStringLiteralExpression

class LoadSymbolReference(
  element: PyStringLiteralExpression,
  private val moduleReference: LabelReference,
) : PsiReferenceBase<PyStringLiteralExpression>(element, false) {

  override fun resolve(): PsiElement? {
    val module = moduleReference.resolve().asSafely<BuildFile>() ?: return null
    return module.findSymbol(element.stringValue)
  }
}