package com.google.idea.blaze.base.lang.pylark.resolve

import com.google.idea.blaze.base.lang.pylark.BuildFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

private val PROVIDERS: List<BuildFileReferenceContributor.Provider> = listOf(
  LabelReferenceProvider()
)

class BuildFileReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    val buildFilePattern = PlatformPatterns.psiFile(BuildFile::class.java)

    for (provider in PROVIDERS) {
      registrar.registerReferenceProvider(
        provider.getElementPattern().inFile(buildFilePattern),
        ProviderAdapter(provider),
      )
    }
  }

  internal interface Provider {
    fun getElementPattern(): PsiElementPattern.Capture<*>

    fun getElementReference(element: PsiElement, ctx: ProcessingContext): PsiReference?
  }
}

private class ProviderAdapter(private val provider: BuildFileReferenceContributor.Provider) : PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference?> {
    val reference = provider.getElementReference(element, context) ?: return PsiReference.EMPTY_ARRAY
    return arrayOf(reference)
  }
}