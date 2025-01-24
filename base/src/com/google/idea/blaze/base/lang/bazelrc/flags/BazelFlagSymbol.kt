package com.google.idea.blaze.base.lang.bazelrc.flags

import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationSymbol
import com.google.idea.blaze.base.lang.bazelrc.documentation.BazelFlagDocumentationTarget

@Suppress("UnstableApiUsage")
class BazelFlagSymbol(val flag: Flag, val project: Project) :
  DocumentationSymbol,
  Pointer<BazelFlagSymbol> {
  override fun createPointer() = this

  override fun dereference() = this

  override fun getDocumentationTarget() = BazelFlagDocumentationTarget(this)
}
