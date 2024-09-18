package com.google.idea.blaze.base.lang.pylark

import com.intellij.lang.Language
import com.jetbrains.python.PythonLanguage

object BuildFileLanguage : Language(PythonLanguage.getInstance(), "BUILD")