package com.google.idea.blaze.base.lang.pylark

import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.jetbrains.python.PythonParserDefinition

class BuildFileParserDefinition : PythonParserDefinition() {
  override fun getFileNodeType(): IFileElementType = BuildFileElementType

  override fun createFile(viewProvider: FileViewProvider): PsiFile = BuildFile(viewProvider)
}