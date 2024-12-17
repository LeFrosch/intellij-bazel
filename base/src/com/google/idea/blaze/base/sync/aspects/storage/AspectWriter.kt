package com.google.idea.blaze.base.sync.aspects.storage

import com.google.idea.blaze.base.sync.SyncScope
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import java.nio.file.Path

/**
 * Extension point for writing aspect files to the workspace.
 */
interface AspectWriter {
  companion object {
    val EP_NAME = ExtensionPointName.Companion.create<AspectWriter>("com.google.idea.blaze.AspectWriter");
  }

  /**
   * Write all aspect files to the destination directory.
   * Files are resolved from the root of the destination directory.
   */
  @Throws(SyncScope.SyncFailedException::class)
  fun write(dst: Path, project: Project)
}