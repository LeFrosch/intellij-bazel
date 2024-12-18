package com.google.idea.blaze.base.sync.aspects.storage

import com.google.idea.blaze.base.sync.SyncScope
import com.intellij.openapi.project.Project
import java.io.File
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively

class AspectWriterImpl : AspectWriter {

  override fun write(dst: Path, project: Project) {
    val src = AspectRepositoryProvider.findAspectDirectory()
      .map(File::toPath)
      .orElseThrow { SyncScope.SyncFailedException("Couldn't find aspect directory") }

    try {
      @OptIn(ExperimentalPathApi::class)
      src.copyToRecursively(dst, overwrite = true, followLinks = false)
    } catch (e: IOException) {
      throw SyncScope.SyncFailedException("Couldn't copy aspects", e)
    }
  }
}