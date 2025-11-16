@file:Suppress("DEPRECATION")

package com.google.idea.blaze.skylark.lang

import com.google.idea.blaze.base.bazel.BuildSystemProvider
import com.google.idea.blaze.base.lang.buildfile.language.BuildFileType
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

/**
 * Temporary file type factory for migration from BuildFileLanguage to
 * SkylarkLanguage. Can be toggled with the SkylarkLanguage.ENABLED_KEY.
 */
class SkylarkFileTypeFactory : FileTypeFactory() {

  override fun createFileTypes(consumer: FileTypeConsumer) {
    val fileNameMatchers = BuildSystemProvider.defaultBuildSystem()
      .buildLanguageFileTypeMatchers()
      .toTypedArray()


    if (SkylarkLanguage.enabled) {
      consumer.consume(SkylarkFileType, *fileNameMatchers)
    } else {
      consumer.consume(BuildFileType.INSTANCE, *fileNameMatchers)
    }
  }
}