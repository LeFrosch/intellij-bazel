/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.java.run.hotswap;

import com.google.common.collect.ImmutableList;
import com.google.idea.blaze.base.model.BlazeVersionData;
import com.google.idea.blaze.base.settings.BuildSystemName;
import com.google.idea.blaze.base.sync.aspects.storage.AspectStorageService;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import java.util.Arrays;
import javax.annotation.Nullable;

/** A strategy for attaching the java_classpath aspect during a build invocation. */
public interface JavaClasspathAspectStrategy {

  String OUTPUT_GROUP = "runtime_classpath";

  ExtensionPointName<JavaClasspathAspectStrategy> EP_NAME =
      ExtensionPointName.create("com.google.idea.blaze.JavaClasspathAspectStrategy");

  @Nullable
  static JavaClasspathAspectStrategy findStrategy(BlazeVersionData versionData) {
    return Arrays.stream(EP_NAME.getExtensions())
        .filter(s -> s.isApplicable(versionData))
        .findFirst()
        .orElse(null);
  }

  boolean isApplicable(BlazeVersionData versionData);

  ImmutableList<String> getBuildFlags(BlazeVersionData versionData, Project project);

  /** A strategy for attaching the java_classpath aspect during a bazel build invocation. */
  class BazelStrategy implements JavaClasspathAspectStrategy {

    @Override
    public boolean isApplicable(BlazeVersionData versionData) {
      return versionData.buildSystem() == BuildSystemName.Bazel
          && versionData.bazelIsAtLeastVersion(0, 5, 0);
    }

    @Override
    public ImmutableList<String> getBuildFlags(BlazeVersionData versionData, Project project) {
      return AspectStorageService.of(project).resolve("java_classpath.bzl")
          .map(label ->
              ImmutableList.of(
                  String.format("--aspects=%s%%java_classpath_aspect", label),
                  "--output_groups=" + OUTPUT_GROUP
              )
          ).orElseGet(ImmutableList::of);
    }
  }
}
