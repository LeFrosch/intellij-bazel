package com.google.idea.blaze.skylark.lang.psi

import com.google.idea.blaze.skylark.lang.SkylarkLanguage
import com.google.idea.blaze.skylark.lang.lexer.SkylarkIndentLexer
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

private val FILE = IFileElementType(SkylarkLanguage)

class SkylarkParserDefinition : ParserDefinition {

  override fun createLexer(project: Project): Lexer = SkylarkIndentLexer()

  override fun createParser(project: Project?): PsiParser = SkylarkParser()

  override fun getFileNodeType(): IFileElementType = FILE

  override fun getCommentTokens(): TokenSet = TokenSet.create(SkylarkTokenTypes.COMMENT)

  override fun getStringLiteralElements(): TokenSet = TokenSet.create(SkylarkTokenTypes.STRING)

  override fun createElement(node: ASTNode?): PsiElement = SkylarkTokenTypes.Factory.createElement(node)

  override fun createFile(viewProvider: FileViewProvider): PsiFile = SkylarkFile(viewProvider)
}
