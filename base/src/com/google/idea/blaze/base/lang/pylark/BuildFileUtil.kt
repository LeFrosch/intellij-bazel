package com.google.idea.blaze.base.lang.pylark

import com.intellij.psi.PsiElement

fun PsiElement.getBuildFileFlavor(): BuildFile.Flavor? {
  val file = containingFile ?: return null
  if (file !is BuildFile) return null

  return file.flavor
}

fun PsiElement.isInBuildFile(): Boolean {
  return getBuildFileFlavor() != null
}