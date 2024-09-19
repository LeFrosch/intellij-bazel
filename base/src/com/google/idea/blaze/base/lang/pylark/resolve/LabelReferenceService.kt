package com.google.idea.blaze.base.lang.pylark.resolve

import com.google.idea.blaze.base.lang.pylark.BuildFile
import com.google.idea.blaze.base.model.primitives.Label
import com.google.idea.blaze.base.sync.workspace.WorkspaceHelper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager

private val BUILD_FILE_NAMES = listOf("BUILD.bazel", "BUILD")

@Service(Service.Level.PROJECT)
class LabelReferenceService(private val project: Project) {
  companion object {
    fun of(project: Project): LabelReferenceService = project.service()
  }

  fun resolve(label: Label, excludeRules: Boolean = false): PsiElement? {
    val packageDir = findPackageDir(label) ?: return null

    val targetName = label.targetName().toString()
    if (targetName == "__pkg__") {
      return findBuildFile(packageDir)
    }

    if (!excludeRules) {
      val rule = findRule(packageDir, targetName)
      if (rule != null) return rule
    }

    // try a direct file reference (e.g. ":a.java")
    val file = packageDir.findFile(targetName) ?: return null
    return PsiManager.getInstance(project).findFile(file)
  }

  private fun findPackageDir(label: Label): VirtualFile? {
    val file = WorkspaceHelper.resolveBlazePackage(project, label) ?: return null
    return VfsUtil.findFileByIoFile(file, true)
  }

  private fun findBuildFile(packageDir: VirtualFile): BuildFile? {
    val virtualFile = BUILD_FILE_NAMES.firstNotNullOfOrNull { packageDir.findChild(it) } ?: return null

    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
    if (psiFile !is BuildFile) return null

    return psiFile
  }

  private fun findRule(packageDir: VirtualFile, targetName: String): PsiElement? {
    val buildFile = findBuildFile(packageDir) ?: return null
    return buildFile.findRule(targetName)
  }
}