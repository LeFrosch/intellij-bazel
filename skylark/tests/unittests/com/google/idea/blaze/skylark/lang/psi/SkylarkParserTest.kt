package com.google.idea.blaze.skylark.lang.psi

import com.intellij.openapi.application.PathManager
import com.intellij.testFramework.ParsingTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SkylarkParserTest() : ParsingTestCase("testData", "bzl", SkylarkParserDefinition()) {

  override fun getTestDataPath(): String = PathManager.getHomePath()

  @Test
  fun testExpression() = doTest()

  @Test
  fun testFunction() = doTest()

  @Test
  fun testGenrule() = doTest()

  @Test
  fun testLoad() = doTest()

  @Test
  fun testComment() = doTest()

  @Test
  fun testList() = doTest()

  @Test
  fun testTuple() = doTest()

  fun doTest() = doTest(true, true)
}
