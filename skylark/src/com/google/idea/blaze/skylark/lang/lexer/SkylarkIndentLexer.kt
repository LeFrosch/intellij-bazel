package com.google.idea.blaze.skylark.lang.lexer

import com.google.idea.blaze.skylark.lang.psi.SkylarkTokenTypes
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.lexer.LookAheadLexer
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet
import java.util.*

private val OPEN_BRACKETS = TokenSet.create(
  SkylarkTokenTypes.LPAREN,
  SkylarkTokenTypes.LBRACKET,
  SkylarkTokenTypes.LBRACE,
)

private val CLOSE_BRACKETS = TokenSet.create(
  SkylarkTokenTypes.RPAREN,
  SkylarkTokenTypes.RBRACKET,
  SkylarkTokenTypes.RBRACE,
)

/**
 * Tracks indentation and converts INDENT tokens into WHITE_SPACE or DEDENT
 * tokens. This lexer cannot be used for incremental lexing and thus can only
 * be used for parsing.
 */
class SkylarkIndentLexer : LookAheadLexer(FlexAdapter(SkylarkLexer(null))) {

  // simple bracket depth to disable INDENT/DEDENT inside brackets
  private var bracketLevel = 0

  // indentation stack; only emit at most one INDENT/DEDENT per line
  private val indentStack = ArrayDeque<Int>(listOf(0))

  // assert that the lexer is not used for incremental lexing
  override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
    assert(startOffset == 0)
    super.start(buffer, startOffset, endOffset, initialState)
  }

  override fun lookAhead(baseLexer: Lexer) {
    when (baseLexer.tokenType) {
      null -> processEOF()
      SkylarkTokenTypes.NEWLINE -> processNewline(baseLexer)
      SkylarkTokenTypes.INDENT -> processIndentation(baseLexer)

      // track the current nesting level of brackets (and parentheses and braces)
      in OPEN_BRACKETS -> {
        bracketLevel++
        advanceLexer(baseLexer)
      }

      in CLOSE_BRACKETS -> {
        if (bracketLevel > 0) bracketLevel--
        advanceLexer(baseLexer)
      }

      else -> advanceLexer(baseLexer)
    }
  }

  /**
   * Main processing function; converts INDENT tokens into WHITE_SPACE or
   * DEDENT tokens. Has to advance the lexer.
   */
  private fun processIndentation(baseLexer: Lexer) {
    if (bracketLevel != 0) {
      advanceAs(baseLexer, TokenType.WHITE_SPACE)
      return
    }

    val len = baseLexer.tokenEnd - baseLexer.tokenStart
    val prv = indentStack.peek()

    when {
      prv == null || len > prv -> {
        indentStack.push(len)
        advanceAs(baseLexer, SkylarkTokenTypes.INDENT)
      }

      len < prv -> {
        while (indentStack.peek() > len) {
          indentStack.pop()
          addToken(SkylarkTokenTypes.DEDENT)
        }
        baseLexer.advance()
      }

      else -> advanceAs(baseLexer, TokenType.WHITE_SPACE)
    }
  }

  /**
   * Convert newline tokens inside parentheses into WHITE_SPACE tokens. Has to
   * advance the lexer.
   */
  private fun processNewline(baseLexer: Lexer) {
    if (bracketLevel > 0) {
      advanceAs(baseLexer, TokenType.WHITE_SPACE)
    } else {
      advanceLexer(baseLexer)
    }
  }

  /**
   * Make sure the file always ends with a newline and the appropriate dedent;
   * this is required by the parser. Has to add the null token to indicate the
   * end of the file.
   */
  private fun processEOF() {
    addToken(SkylarkTokenTypes.NEWLINE)

    if (indentStack.peek() > 0 && bracketLevel == 0) {
      addToken(SkylarkTokenTypes.DEDENT)
    }

    addToken(null)
  }
}