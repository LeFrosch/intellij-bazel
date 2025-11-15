package com.google.idea.blaze.skylark.lang.psi

import com.intellij.psi.tree.TokenSet

object SkylarkTokenSets {

  val operators = TokenSet.create(
    SkylarkTokenTypes.LES,
    SkylarkTokenTypes.GRE,
    SkylarkTokenTypes.PLUS,
    SkylarkTokenTypes.MINUS,
    SkylarkTokenTypes.STAR,
    SkylarkTokenTypes.STARSTAR,
    SkylarkTokenTypes.DIV,
    SkylarkTokenTypes.FLOORDIV,
    SkylarkTokenTypes.MOD,
    SkylarkTokenTypes.TILDE,
    SkylarkTokenTypes.AND,
    SkylarkTokenTypes.OR,
    SkylarkTokenTypes.XOR,
    SkylarkTokenTypes.SHL,
    SkylarkTokenTypes.SHR,
  )

  val assignments = TokenSet.create(
    SkylarkTokenTypes.ASSIGN,
    SkylarkTokenTypes.NEQ,
    SkylarkTokenTypes.MINUS_EQ,
    SkylarkTokenTypes.MUL_EQ,
    SkylarkTokenTypes.DIV_EQ,
    SkylarkTokenTypes.FLOORDIV_EQ,
    SkylarkTokenTypes.MOD_EQ,
    SkylarkTokenTypes.AND_EQ,
    SkylarkTokenTypes.OR_EQ,
    SkylarkTokenTypes.XOR_EQ,
    SkylarkTokenTypes.SHL_EQ,
    SkylarkTokenTypes.SHR_EQ,
  )

  val numbers = TokenSet.create(
    SkylarkTokenTypes.FLOAT,
    SkylarkTokenTypes.INT,
  )

  val brackets = TokenSet.create(
    SkylarkTokenTypes.LPAREN,
    SkylarkTokenTypes.RPAREN,
    SkylarkTokenTypes.LBRACKET,
    SkylarkTokenTypes.RBRACKET,
    SkylarkTokenTypes.LBRACE,
    SkylarkTokenTypes.RBRACE,
  )

  val keywords = TokenSet.create(
    SkylarkTokenTypes.BREAK,
    SkylarkTokenTypes.CONTINUE,
    SkylarkTokenTypes.DEF,
    SkylarkTokenTypes.ELIF,
    SkylarkTokenTypes.ELSE,
    SkylarkTokenTypes.FOR,
    SkylarkTokenTypes.IF,
    SkylarkTokenTypes.IN,
    SkylarkTokenTypes.LAMBDA,
    SkylarkTokenTypes.LOAD,
    SkylarkTokenTypes.NOT,
    SkylarkTokenTypes.L_AND,
    SkylarkTokenTypes.L_OR,
    SkylarkTokenTypes.PASS,
    SkylarkTokenTypes.RETURN,
    SkylarkTokenTypes.NONE,
    SkylarkTokenTypes.TRUE,
    SkylarkTokenTypes.FALSE,
  )
}