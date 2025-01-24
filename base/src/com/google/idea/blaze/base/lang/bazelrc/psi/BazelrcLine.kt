package com.google.idea.blaze.base.lang.bazelrc.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.google.idea.blaze.base.lang.bazelrc.elements.BazelrcTokenTypes

class BazelrcLine(node: ASTNode) : BazelrcBaseElement(node) {
  override fun acceptVisitor(visitor: BazelrcElementVisitor) = visitor.visitLine(this)

  val config: String? = this.findChildByType<PsiElement>(BazelrcTokenTypes.CONFIG)?.text
  val command: String = this.findChildByType<PsiElement>(COMMAND_TOKENS)?.text ?: ""

  companion object {
    val COMMAND_TOKENS = TokenSet.create(BazelrcTokenTypes.COMMAND)
  }
}
