package com.google.idea.blaze.base.lang.pylark

import com.intellij.psi.FileViewProvider
import com.jetbrains.python.psi.impl.PyFileImpl
import icons.BlazeIcons
import javax.swing.Icon

class BuildFile(viewProvider: FileViewProvider) : PyFileImpl(viewProvider, BuildFileLanguage) {
    override fun getIcon(flags: Int): Icon = BlazeIcons.BuildFile
  override fun getIcon(flags: Int): Icon = BlazeIcons.BuildFile
}