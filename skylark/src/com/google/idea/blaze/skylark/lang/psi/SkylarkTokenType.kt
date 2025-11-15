package com.google.idea.blaze.skylark.lang.psi

import com.google.idea.blaze.skylark.lang.SkylarkLanguage
import com.intellij.psi.tree.IElementType

class SkylarkTokenType(debugName: String) : IElementType(debugName, SkylarkLanguage)