/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.base.lang.projectview.completion

import com.google.idea.blaze.base.lang.projectview.language.ProjectViewLanguage
import com.google.idea.blaze.base.lang.projectview.lexer.ProjectViewTokenType
import com.google.idea.blaze.base.lang.projectview.psi.ProjectViewElementTypes
import com.google.idea.blaze.base.lang.projectview.psi.ProjectViewPsiScalarSection
import com.google.idea.blaze.base.projectview.section.ScalarSectionParser
import com.google.idea.blaze.base.projectview.section.sections.Sections
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class ScalarCompletionContributor : CompletionContributor() {
  init {
    val predicate = psiElement()
      .withLanguage(ProjectViewLanguage.INSTANCE)
      .withElementType(ProjectViewTokenType.IDENTIFIER)
      .inside(psiElement(ProjectViewElementTypes.SCALAR_ITEM))

    extend(CompletionType.BASIC, predicate, Provider)
  }
}

private object Provider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet
  ) {
    val section = PsiTreeUtil.getParentOfType(parameters.position, ProjectViewPsiScalarSection::class.java) ?: return
    val sectionName = section.name

    Sections.getParsers()
      .asSequence()
      .filterIsInstance<ScalarSectionParser<*>>()
      .filter { it.sectionKey.name == sectionName }
      .flatMap { it.variants }
      .map(LookupElementBuilder::create)
      .forEach(result::addElement)
  }
}