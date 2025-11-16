package com.google.idea.blaze.skylark.lang.highlighting

import com.google.idea.blaze.skylark.SkylarkBundle
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import org.jetbrains.annotations.PropertyKey

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
  NAMED_ARGUMENT(
    "SKYLARK_NAMED_ARGUMENT",
    null,
    "settings.colors.group.function", "settings.colors.function.named_argument",
  ),
  FUNCTION_CALL(
    "SKYLARK_FUNCTION_CALL",
    DefaultLanguageHighlighterColors.FUNCTION_DECLARATION,
    "settings.colors.group.function", "settings.colors.function.call",
  ),
  FUNCTION_DECLARATION(
    "SKYLARK_FUNCTION_DECLARATION",
    DefaultLanguageHighlighterColors.FUNCTION_DECLARATION,
    "settings.colors.group.function", "settings.colors.function.declaration",
  ),
  CONSTANT(
    "SKYLARK_CONSTANT",
    DefaultLanguageHighlighterColors.STATIC_FIELD,
    "settings.colors.constant",
  ),
  PROVIDER(
    "SKYLARK_PROVIDER",
    DefaultLanguageHighlighterColors.METADATA,
    "settings.colors.provider",
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
    vararg bundleKey: @PropertyKey(resourceBundle = SkylarkBundle.BUNDLE_FQN) String,
  ) : this() {
    attribute = TextAttributesKey.createTextAttributesKey(externalName, fallbackKey)
    descriptor =
      AttributesDescriptor(bundleKey.joinToString(separator = "//", transform = SkylarkBundle::message), attribute)
  }
}