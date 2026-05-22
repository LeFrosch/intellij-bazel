/*
 * Copyright 2026 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base.sync.aspect2

import com.google.idea.blaze.base.model.primitives.LanguageClass
import com.google.idea.blaze.base.settings.BlazeImportSettingsManager
import com.google.idea.blaze.base.sync.aspects.strategy.AspectStrategy
import com.google.idea.blaze.base.sync.aspects.strategy.AspectStrategyProvider
import com.intellij.aspect.lib.Languages
import com.intellij.aspect.lib.aspectsForLanguages
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import java.util.*

class Aspect2Strategy : AspectStrategy() {

  class Provider : AspectStrategyProvider {
    override fun getStrategy(): AspectStrategy? {
      if (!Registry.`is`("bazel.use.intellij.aspect")) return null
      return Aspect2Strategy()
    }
  }

  override fun getName(): String = "IntelliJStrategy"

  override fun getAspectFlag(project: Project): Optional<String> {
    return getAspectFlag(project, LanguageClass.entries.toSet())
  }

  override fun getAspectFlag(project: Project, activeLanguages: Set<LanguageClass>): Optional<String> {
    val settings = BlazeImportSettingsManager.getInstance(project).importSettings ?: return Optional.empty()
    if (settings.workspaceRoot.isNullOrBlank()) return Optional.empty()

    val sdkLanguages = toSdkLanguages(activeLanguages)
    val deployPrefix = DEPLOY_RELATIVE_PATH.toString().replace('\\', '/')

    val labels = aspectsForLanguages(sdkLanguages).joinToString(",") { entry ->
      "//$deployPrefix/$entry"
    }

    return Optional.of("--aspects=$labels")
  }
}

private fun toSdkLanguages(active: Set<LanguageClass>): Set<Languages> = buildSet {
  if (LanguageClass.C in active) add(Languages.CC)
  if (LanguageClass.PYTHON in active) add(Languages.PYTHON)
  if (LanguageClass.JAVA in active) add(Languages.JAVA)
  if (LanguageClass.KOTLIN in active) add(Languages.KOTLIN)
  if (LanguageClass.SCALA in active) add(Languages.SCALA)
  if (LanguageClass.GO in active) add(Languages.GO)
}
