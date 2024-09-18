package com.google.idea.blaze.base.lang.pylark

import com.jetbrains.python.psi.PyFileElementType

object BuildFileElementType : PyFileElementType(BuildFileLanguage) {
  override fun getExternalId(): String = "BUILD.FILE"
}