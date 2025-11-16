package com.google.idea.blaze.skylark.lang.lexer

import com.intellij.lexer.Lexer
import com.intellij.testFramework.LexerTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SkylarkLexerTest : LexerTestCase() {

  override fun createLexer(): Lexer = SkylarkIndentLexer()

  override fun getDirPath(): String = "testData"

  override fun shouldTrim(): Boolean = false

  @Test
  fun testDefine() = doTest()

  @Test
  fun testFunction() = doTest()

  @Test
  fun testCquery() = doTest()

  @Test
  fun testWasm() = doTest()

  @Test
  fun testComment() = doTest()

  private fun doTest() = doFileTest("bzl")
}
