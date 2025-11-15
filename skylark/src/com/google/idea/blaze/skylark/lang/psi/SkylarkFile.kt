package com.google.idea.blaze.skylark.lang.psi

import com.google.idea.blaze.skylark.lang.SkylarkFileType
import com.google.idea.blaze.skylark.lang.SkylarkLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class SkylarkFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, SkylarkLanguage) {

  override fun getFileType(): FileType = SkylarkFileType

  override fun toString(): String = "Skylark file"
}
