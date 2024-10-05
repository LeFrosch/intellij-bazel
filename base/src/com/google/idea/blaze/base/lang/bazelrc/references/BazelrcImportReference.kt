package com.google.idea.blaze.base.lang.bazelrc.references

import com.google.idea.blaze.base.lang.bazelrc.BazelrcFileType
import com.google.idea.blaze.base.lang.bazelrc.psi.BazelrcImport
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet

class BazelrcImportReference(
  referenceSet: FileReferenceSet,
  range: TextRange,
  index: Int,
  text: String,
) : FileReference(referenceSet, range, index, text) {
  override fun innerResolve(caseSensitive: Boolean, containingFile: PsiFile): Array<out ResolveResult?> =
    when {
      text == "%workspace%" && index == 0 -> {
        val rootDirPath = WorkspaceRoot.fromProject(element.project).path()
        val rootDir = VirtualFileManager.getInstance().findFileByNioPath(rootDirPath)
        arrayOf(findFileSystemItem(element.project, rootDir)).filterNotNull()
      }

      else -> {
        val virtualFiles = contexts.mapNotNull { it.virtualFile?.findChild(text) }
        virtualFiles.mapNotNull { findFileSystemItem(element.project, it) }
      }
    }.let(PsiElementResolveResult::createResults)
}

class BazelrcImportFileReferenceSet(importSpec: BazelrcImport) :
  FileReferenceSet(
    importSpec.getImportPath()?.text ?: "",
    importSpec,
    importSpec.getImportPath()?.startOffsetInParent ?: 0,
    null,
    true,
    true,
    arrayOf(BazelrcFileType),
  ) {
  override fun createFileReference(
    range: TextRange,
    index: Int,
    text: String,
  ): FileReference? = BazelrcImportReference(this, range, index, text)

  override fun computeDefaultContexts(): Collection<PsiFileSystemItem?> = listOf(element.containingFile.originalFile.parent)

  override fun getReferenceCompletionFilter(): Condition<PsiFileSystemItem>? =
    Condition {
      (it.isDirectory && !it.name.startsWith(".")) || it.name.endsWith(".bazelrc")
    }
}

private fun findFileSystemItem(project: Project?, file: VirtualFile?): PsiFileSystemItem? {
  if (project == null || file == null) return null
  if (project.isDisposed || !file.isValid) return null
  val psiManager = PsiManager.getInstance(project)
  return if (file.isDirectory) psiManager.findDirectory(file) else psiManager.findFile(file)
}


