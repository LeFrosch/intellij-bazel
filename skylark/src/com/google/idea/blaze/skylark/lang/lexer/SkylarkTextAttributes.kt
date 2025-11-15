package com.google.idea.blaze.skylark.lang.lexer

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor

enum class SkylarkTextAttributes() {
  STRING(
    "SKYLARK_STRING",
    DefaultLanguageHighlighterColors.STRING,
    "settings.colors.group.string", "settings.colors.string.text",
  ),
  STRING_ESCAPE(
    "SKYLARK_STRING_ESCAPE",
    DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE,
    "settings.colors.group.string", "settings.colors.string.escape",
  ),
  NUMBER(
    "SKYLARK_NUMBER",
    DefaultLanguageHighlighterColors.NUMBER,
    "settings.colors.number",
  ),
  COMMENT(
    "SKYLARK_COMMENT",
    DefaultLanguageHighlighterColors.LINE_COMMENT,
    "settings.colors.comment",
  ),
  KEYWORD(
    "SKYLARK_KEYWORD",
    DefaultLanguageHighlighterColors.KEYWORD,
    "settings.colors.keyword",
  ),
  PARENTHESES(
    "SKYLARK_PARENTHESES",
    DefaultLanguageHighlighterColors.PARENTHESES,
    "settings.colors.group.bao", "settings.colors.bao.parentheses",
  ),
  BRACES(
    "SKYLARK_BRACES",
    DefaultLanguageHighlighterColors.BRACES,
    "settings.colors.group.bao", "settings.colors.bao.braces",
  ),
  BRACKETS(
    "SKYLARK_BRACKETS",
    DefaultLanguageHighlighterColors.BRACKETS,
    "settings.colors.group.bao", "settings.colors.bao.brackets",
  ),
  OPERATOR(
    "SKYLARK_OPERATOR",
    DefaultLanguageHighlighterColors.OPERATION_SIGN,
    "settings.colors.group.bao", "settings.colors.bao.operators",
  ),
  COMMA(
    "SKYLARK_COMMA",
    DefaultLanguageHighlighterColors.COMMA,
    "settings.colors.group.bao", "settings.colors.bao.comma",
  ),
  SEMICOLON(
    "SKYLARK_SEMICOLON",
    DefaultLanguageHighlighterColors.SEMICOLON,
    "settings.colors.group.bao", "settings.colors.bao.semicolon",
  ),
  BAD_CHARACTER(
    "SKYLARK_BAD_CHARACTER",
    HighlighterColors.BAD_CHARACTER,
    "settings.colors.bad_char",
  );

  lateinit var attribute: TextAttributesKey
    private set

  lateinit var descriptor: AttributesDescriptor
    private set

  constructor(
    externalName: String,
    fallbackKey: TextAttributesKey?,
    vararg bundleKey: /* @PropertyKey(resourceBundle = DtsBundle.BUNDLE) */ String,
  ) : this() {
    attribute = TextAttributesKey.createTextAttributesKey(externalName, fallbackKey)
    // descriptor = AttributesDescriptor(StringUtil.join(bundleKey.map { DtsBundle.message(it) }, "//"), attribute)
  }
}