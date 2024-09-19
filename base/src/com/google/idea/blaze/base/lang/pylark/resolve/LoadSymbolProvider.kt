package com.google.idea.blaze.base.lang.pylark.resolve

import com.google.idea.blaze.base.lang.pylark.getBuildFile
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext

class LoadSymbolProvider : PyReferenceResolveProvider {
  override fun resolveName(expression: PyQualifiedExpression, context: TypeEvalContext): List<RatedResolveResult> {
    val file = expression.getBuildFile() ?: return emptyList()
    val reference = file.loadedSymbols()[expression.name] ?: return emptyList()
    val resolved = reference.resolve() ?: return emptyList()

    return listOf(RatedResolveResult(RatedResolveResult.RATE_NORMAL, resolved))
  }
}