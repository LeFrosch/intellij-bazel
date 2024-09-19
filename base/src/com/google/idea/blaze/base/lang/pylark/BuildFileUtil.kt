package com.google.idea.blaze.base.lang.pylark

import com.intellij.psi.PsiElement
import com.intellij.util.asSafely

fun PsiElement.getBuildFile(): BuildFile? {
  return containingFile.asSafely<BuildFile>()
}

fun PsiElement.getBuildFileFlavor(): BuildFile.Flavor? {
  return getBuildFile()?.flavor
}

fun PsiElement.isInBuildFile(): Boolean {
  return getBuildFile() != null
}