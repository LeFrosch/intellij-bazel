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
package com.google.idea.blaze.base.sync.aspects.impl.intellij

import com.google.idea.blaze.base.model.primitives.Label
import com.google.idea.blaze.base.model.primitives.LanguageClass
import com.google.idea.blaze.base.sync.aspects.storage.AspectStorageService
import com.google.idea.blaze.base.sync.aspects.storage.AspectWriter
import com.google.idea.blaze.base.sync.aspects.strategy.AspectStrategy
import com.google.idea.blaze.base.sync.aspects.strategy.AspectStrategyProvider
import com.intellij.aspect.lib.Languages
import com.intellij.aspect.lib.aspectsForLanguages
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import java.nio.file.Path
import java.util.Optional

/**
 * The IntelliJ split aspect strategy: the `intellij_aspect_sdk` archive is materialized by
 * [IntelliJAspectWriter] into the `intellij` prefix, and the `--aspects` flag is built by resolving
 * each aspect entry against that same deployed location, so the flag and the files always agree.
 */
class IntelliJAspectStrategy : AspectStrategy() {

  class Provider : AspectStrategyProvider {
    override fun getStrategy(): AspectStrategy? {
      if (!Registry.`is`("bazel.sync.use.intellij.aspect")) return null
      return IntelliJAspectStrategy()
    }
  }

  override fun getName(): String = "IntelliJStrategy"

  override fun prefix(): Path = Path.of("intellij")

  override fun writers(): List<AspectWriter> = listOf(IntelliJAspectWriter())

  override fun resolve(project: Project, relativePath: String): Optional<Label> =
    AspectStorageService.of(project).resolve(relativePath, prefix())

  override fun getAspectFlag(project: Project): Optional<String> =
    getAspectFlag(project, LanguageClass.entries.toSet())

  override fun getAspectFlag(project: Project, activeLanguages: Set<LanguageClass>): Optional<String> {
    val labels = aspectsForLanguages(toSdkLanguages(activeLanguages)).map { entry ->
      // entry has the form "pkgdir:file.bzl%aspect_name", relative to the deployed prefix directory
      val (loadable, aspectName) = entry.split('%', limit = 2)
      val (pkgDir, file) = loadable.split(':', limit = 2)

      // resolve the file against the deployed location; bail out if the aspect was not materialized
      val label = resolve(project, "$pkgDir/$file").orElse(null) ?: return Optional.empty()

      "$label%$aspectName"
    }

    if (labels.isEmpty()) return Optional.empty()
    return Optional.of("--aspects=" + labels.joinToString(","))
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
