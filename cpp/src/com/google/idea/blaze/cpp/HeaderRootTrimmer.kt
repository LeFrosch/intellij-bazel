/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.cpp

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.idea.blaze.base.ideinfo.CToolchainIdeInfo
import com.google.idea.blaze.base.ideinfo.TargetIdeInfo
import com.google.idea.blaze.base.ideinfo.TargetKey
import com.google.idea.blaze.base.model.BlazeProjectData
import com.google.idea.blaze.base.scope.BlazeContext
import com.google.idea.blaze.base.sync.workspace.ExecutionRootPathResolver
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.util.function.Predicate

interface HeaderRootTrimmer {

  companion object {

    @JvmStatic
    fun getInstance(project: Project): HeaderRootTrimmer = project.service()
  }

  fun getValidHeaderRoots(
    parentContext: BlazeContext,
    projectData: BlazeProjectData,
    toolchainLookupMap: ImmutableMap<TargetKey, CToolchainIdeInfo>,
    targetFilter: Predicate<TargetIdeInfo>,
    executionRootPathResolver: ExecutionRootPathResolver,
  ): ImmutableSet<Path>
}