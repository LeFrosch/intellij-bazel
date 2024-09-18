package com.google.idea.blaze.base.lang.pylark

import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.python.psi.LanguageLevel

class BuildFileSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
  private val highlighter = PyHighlighter(LanguageLevel.getDefault())

  override fun getSyntaxHighlighter(project: Project?, file: VirtualFile?): SyntaxHighlighter = highlighter
}