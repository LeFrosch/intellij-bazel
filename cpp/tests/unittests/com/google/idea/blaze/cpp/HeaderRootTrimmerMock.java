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
package com.google.idea.blaze.cpp;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.idea.blaze.base.ideinfo.CToolchainIdeInfo;
import com.google.idea.blaze.base.ideinfo.TargetIdeInfo;
import com.google.idea.blaze.base.ideinfo.TargetKey;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.base.sync.workspace.ExecutionRootPathResolver;
import java.nio.file.Path;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

public class HeaderRootTrimmerMock implements HeaderRootTrimmer {

  @Override
  public @NotNull ImmutableSet<Path> getValidHeaderRoots(
      @NotNull BlazeContext parentContext,
      @NotNull BlazeProjectData projectData,
      @NotNull ImmutableMap<TargetKey, CToolchainIdeInfo> toolchainLookupMap,
      @NotNull Predicate<TargetIdeInfo> targetFilter,
      @NotNull ExecutionRootPathResolver executionRootPathResolver
  ) {
    return ImmutableSet.of();
  }
}
