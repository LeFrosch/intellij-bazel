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

import com.google.idea.blaze.common.Context;
import com.google.idea.blaze.exception.BuildException;
import com.google.idea.blaze.qsync.cc.CcCompilerInfoCollector;
import com.google.idea.blaze.qsync.deps.CcToolchain;
import com.google.idea.blaze.qsync.project.ProjectProto;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;

public interface CcCompilerInfoCollectorProvider {

  ExtensionPointName<CcCompilerInfoCollectorProvider> EP_NAME =
    ExtensionPointName.create("com.google.idea.blaze.qsync.CcCompilerInfoCollectorProvider");

  CcCompilerInfoCollector create(Project project);

  /** Default implementation returned used when CLion is not available */
  class Unavailable implements CcCompilerInfoCollector {

    @Override
    public ProjectProto.CcCompilerKind getCompilerKind(Context<?> ctx, CcToolchain toolchain) throws BuildException {
      return ProjectProto.CcCompilerKind.CC_COMPILER_KIND_UNKNOWN;
    }

    @Override
    public ProjectProto.MsvcData getMsvcData(Context<?> ctx, CcToolchain toolchain) throws BuildException {
      throw new UnsupportedOperationException("CPP support is not available");
    }

    @Override
    public ProjectProto.XcodeData getXcodeData(Context<?> ctx, CcToolchain toolchain) throws BuildException {
      throw new UnsupportedOperationException("CPP support is not available");
    }
  }

  /** Gets the {@link CcCompilerInfoCollector} if CLion is available or returns a stub. */
  static CcCompilerInfoCollector get(Project project) {
    return EP_NAME.getExtensionList().stream()
      .map((it) -> it.create(project))
      .findFirst()
      .orElse(new Unavailable());
  }
}

