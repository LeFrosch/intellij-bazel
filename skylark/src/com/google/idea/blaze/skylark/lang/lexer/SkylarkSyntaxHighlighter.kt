package com.google.idea.blaze.skylark.lang.lexer

import com.google.idea.blaze.skylark.lang.psi.SkylarkTokenSets
import com.google.idea.blaze.skylark.lang.psi.SkylarkTokenTypes
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class SkylarkSyntaxHighlighter : SyntaxHighlighterBase() {

  class Factory : SyntaxHighlighterFactory() {

    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
      return SkylarkSyntaxHighlighter()
    }
  }

  override fun getHighlightingLexer(): Lexer = FlexAdapter(SkylarkLexer(null))

  private fun pack(vararg attr: SkylarkTextAttributes): Array<TextAttributesKey> {
    return attr.map { it.attribute }.toTypedArray()
  }

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
    if (tokenType in SkylarkTokenSets.numbers) return pack(SkylarkTextAttributes.NUMBER)
    if (tokenType in SkylarkTokenSets.operators) return pack(SkylarkTextAttributes.OPERATOR)
    if (tokenType in SkylarkTokenSets.keywords) return pack(SkylarkTextAttributes.KEYWORD)

    return when (tokenType) {
      SkylarkTokenTypes.STRING -> pack(SkylarkTextAttributes.STRING)
      SkylarkTokenTypes.COMMENT -> pack(SkylarkTextAttributes.COMMENT)
      SkylarkTokenTypes.SEMICOLON -> pack(SkylarkTextAttributes.SEMICOLON)
      SkylarkTokenTypes.COMMA -> pack(SkylarkTextAttributes.COMMA)
      SkylarkTokenTypes.LBRACKET, SkylarkTokenTypes.RBRACKET -> pack(SkylarkTextAttributes.BRACKETS)
      SkylarkTokenTypes.LPAREN, SkylarkTokenTypes.RPAREN -> pack(SkylarkTextAttributes.PARENTHESES)
      SkylarkTokenTypes.LBRACE, SkylarkTokenTypes.RBRACE -> pack(SkylarkTextAttributes.BRACES)
      TokenType.BAD_CHARACTER -> pack(SkylarkTextAttributes.BAD_CHARACTER)

      else -> pack(null)
    }
  }
}
