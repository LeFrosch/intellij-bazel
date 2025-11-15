package com.google.idea.blaze.skylark.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import icons.BlazeIcons
import javax.swing.Icon

object SkylarkFileType : LanguageFileType(SkylarkLanguage) {

  override fun getName(): String = SkylarkLanguage.id

  override fun getDescription(): String = SkylarkLanguage.displayName

  override fun getDefaultExtension(): String = "bzl"

  override fun getIcon(): Icon = BlazeIcons.BuildFile
}
