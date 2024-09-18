package com.google.idea.blaze.base.lang.pylark

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement

class BuildFileInspectionSuppressor : InspectionSuppressor {
  override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
    return toolId == "PyInterpreter"
  }

  override fun getSuppressActions(element: PsiElement?, toolId: String): Array<out SuppressQuickFix?> = emptyArray()
}