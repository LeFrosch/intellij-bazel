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
package com.google.idea.blaze.base.qsync;

import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.exception.BuildException;
import com.google.idea.blaze.qsync.deps.CcCompilerInfoMap;
import com.google.idea.blaze.qsync.deps.OutputInfo;
import com.google.idea.blaze.qsync.project.ProjectPath;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import java.io.IOException;

public interface CcCompilerInfoCollectorProvider {

  ExtensionPointName<CcCompilerInfoCollectorProvider> EP_NAME =
    ExtensionPointName.create("com.google.idea.blaze.qsync.CcCompilerInfoCollectorProvider");

  CcCompilerInfoCollector create(Project project, ProjectPath.Resolver resolver);

  /** Default implementation returned used when CLion is not available */
  class Empty implements CcCompilerInfoCollector {

    @Override
    public CcCompilerInfoMap run(BlazeContext context, OutputInfo outputInfo) throws IOException, BuildException {
      return CcCompilerInfoMap.EMPTY;
    }
  }

  /** Gets the {@link CcCompilerInfoCollector} if CLion is available or returns a stub. */
  static CcCompilerInfoCollector get(Project project, ProjectPath.Resolver resolver) {
    return EP_NAME.getExtensionList().stream()
      .map((it) -> it.create(project, resolver))
      .findFirst()
      .orElse(new Empty());
  }
}
