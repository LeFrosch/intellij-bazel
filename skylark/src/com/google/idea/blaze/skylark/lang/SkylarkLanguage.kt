package com.google.idea.blaze.skylark.lang

import com.intellij.lang.Language
import com.intellij.openapi.util.registry.Registry

object SkylarkLanguage : Language("Skylark", "text/python") {

  const val ENABLED_KEY = "bazel.starlark.lsp.enabled"

  @JvmStatic
  val enabled: Boolean get() = Registry.`is`(ENABLED_KEY)

  override fun getDisplayName(): String = "Starlark"

  override fun isCaseSensitive(): Boolean = true
}