package com.google.idea.blaze.base.lang.pylark.resolve

import com.google.idea.blaze.base.lang.buildfile.references.LabelUtils
import com.google.idea.blaze.base.lang.pylark.BuildFile
import com.google.idea.blaze.base.lang.pylark.getBuildFileFlavor
import com.google.idea.blaze.base.model.primitives.Label
import com.google.idea.blaze.base.sync.workspace.WorkspaceHelper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.jetbrains.python.psi.PyStringLiteralExpression
import java.io.File

class LabelReference(element: PyStringLiteralExpression, soft: Boolean) : PsiReferenceBase<PyStringLiteralExpression>(element, soft) {
  override fun resolve(): PsiElement? {
    val label = getLabel() ?: return null

    val inStarlarkFile = element.getBuildFileFlavor() == BuildFile.Flavor.STARLARK

    return BuildReferenceService.of(element.project).resolveLabel(
      label,
      excludeRules = !label.isAbsolut || inStarlarkFile,
    )
  }

  private fun getLabel(): Label? {
    val labelString = element.stringValue

    // don't handle globs yet
    if (labelString.contains('*')) return null

    val packageLabel = getContainingPackageLabel(element)
    return LabelUtils.createLabelFromString(packageLabel, labelString)
  }

  private fun getContainingPackageLabel(element: PsiElement): Label? {
    val file = element.containingFile?.virtualFile ?: return null
    return WorkspaceHelper.getBuildLabel(element.project, File(file.path));
  }
}