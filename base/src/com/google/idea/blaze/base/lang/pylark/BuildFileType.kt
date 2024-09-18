package com.google.idea.blaze.base.lang.pylark

import com.intellij.openapi.util.NlsSafe
import com.jetbrains.python.PythonFileType
import icons.BlazeIcons
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

object BuildFileType : PythonFileType(BuildFileLanguage) {
  override fun getName(): @NonNls String = BuildFileLanguage.id

  override fun getDescription(): @NlsSafe String = "Bazel BUILD language"

  override fun getDefaultExtension(): String = ""

  override fun getIcon(): Icon = BlazeIcons.BuildFile
}