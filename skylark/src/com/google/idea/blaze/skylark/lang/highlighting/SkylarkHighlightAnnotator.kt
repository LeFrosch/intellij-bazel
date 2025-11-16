package com.google.idea.blaze.skylark.lang.highlighting

import com.google.idea.blaze.skylark.lang.psi.SkylarkFunccall
import com.google.idea.blaze.skylark.lang.psi.SkylarkKwarg
import com.google.idea.blaze.skylark.lang.psi.SkylarkFuncdef
import com.google.idea.blaze.skylark.lang.psi.SkylarkTokenTypes
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

private val CONSTANT_IDENTIFIER_RX = Regex("^[A-Z0-9_]+$")
private val PROVIDER_IDENTIFIER_RX = Regex("^([A-Z][a-z0-9]*)+$")

/**
 * Explantation for the fun regex:
 * - 32-bit Unicode code point: U[0-9a-fA-F]{8}
 * - 16-bit Unicode code point: u[0-9a-fA-F]{4}
 * - Hexadecimal value: x[0-9a-fA-F]{1,2}
 * - Octal value: [0-7]{1,3}
 * - Any other character: [^Uux0-7]
 */
private val STRING_ESCAPE_RX = Regex("\\\\((U[0-9a-fA-F]{8})|(u[0-9a-fA-F]{4})|(x[0-9a-fA-F]{1,2})|([0-7]{1,3})|[^Uux0-7])")

class SkylarkHighlightAnnotator : Annotator, DumbAware {

  private fun annotateKwarg(element: SkylarkKwarg, holder: AnnotationHolder) {
    val identifier = element.node.findChildByType(SkylarkTokenTypes.IDENTIFIER) ?: return
    val assign = element.node.findChildByType(SkylarkTokenTypes.ASSIGN) ?: return
    holder.highlight(identifier.textRange.union(assign.textRange), SkylarkTextAttributes.NAMED_ARGUMENT)
  }

  private fun annotateFuncdef(element: SkylarkFuncdef, holder: AnnotationHolder) {
    val identifier = element.node.findChildByType(SkylarkTokenTypes.IDENTIFIER) ?: return
    holder.highlight(identifier.textRange, SkylarkTextAttributes.FUNCTION_DECLARATION)
  }

  private fun annotateFuncall(element: SkylarkFunccall, holder: AnnotationHolder) {
    val identifier = when (element.atomExpr.elementType) {
      SkylarkTokenTypes.ATOM -> element.atomExpr.firstChild
      SkylarkTokenTypes.GETATTR -> element.atomExpr.lastChild
      else -> return
    }

    if (identifier.elementType == SkylarkTokenTypes.IDENTIFIER) {
      holder.highlight(identifier.textRange, SkylarkTextAttributes.FUNCTION_CALL)
    }
  }

  private fun annotateIdentifier(element: PsiElement, holder: AnnotationHolder) {
    if (element.text.length < 2) return

    if (CONSTANT_IDENTIFIER_RX.matches(element.text)) {
      holder.highlight(element.textRange, SkylarkTextAttributes.CONSTANT)
    }
    if (PROVIDER_IDENTIFIER_RX.matches(element.text)) {
      holder.highlight(element.textRange, SkylarkTextAttributes.PROVIDER)
    }
  }

  private fun annotateStringEscape(element: PsiElement, holder: AnnotationHolder) {
    val start = element.textRange.startOffset

    for (match in STRING_ESCAPE_RX.findAll(element.text)) {
      val range = TextRange(
        start + match.range.first,
        start + match.range.last + 1,
      )

      holder.highlight(range, SkylarkTextAttributes.STRING_ESCAPE)
    }
  }

  private fun AnnotationHolder.highlight(range: TextRange, attr: SkylarkTextAttributes) {
    newSilentAnnotation(HighlightSeverity.INFORMATION)
      .range(range)
      .textAttributes(attr.attribute)
      .create()
  }

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    when (element.elementType) {
      SkylarkTokenTypes.KWARG -> annotateKwarg(element as SkylarkKwarg, holder)
      SkylarkTokenTypes.FUNCDEF -> annotateFuncdef(element as SkylarkFuncdef, holder)
      SkylarkTokenTypes.FUNCCALL -> annotateFuncall(element as SkylarkFunccall, holder)
      SkylarkTokenTypes.IDENTIFIER -> annotateIdentifier(element, holder)
      SkylarkTokenTypes.STRING -> annotateStringEscape(element, holder)
    }
  }
}