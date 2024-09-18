package com.google.idea.blaze.base.lang.pylark

import com.intellij.psi.FileViewProvider
import com.jetbrains.python.psi.impl.PyFileImpl
import icons.BlazeIcons
import javax.swing.Icon

class BuildFile(viewProvider: FileViewProvider) : PyFileImpl(viewProvider, BuildFileLanguage) {
  enum class Flavor { BUILD, WORKSPACE, MODULE, STARLARK }

  override fun getIcon(flags: Int): Icon = BlazeIcons.BuildFile

  val flavor: Flavor
    get() = when {
      name.startsWith("BUILD") -> Flavor.BUILD
      name.startsWith("WORKSPACE") -> Flavor.WORKSPACE
      name.startsWith("MODULE") -> Flavor.MODULE
      else -> Flavor.STARLARK
    }
}