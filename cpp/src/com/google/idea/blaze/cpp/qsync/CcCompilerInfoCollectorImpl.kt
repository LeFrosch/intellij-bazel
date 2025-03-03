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
package com.google.idea.blaze.cpp.qsync;

import com.google.idea.blaze.base.qsync.CcCompilerInfoCollectorProvider;
import com.google.idea.blaze.common.Context;
import com.google.idea.blaze.exception.BuildException;
import com.google.idea.blaze.qsync.cc.CcCompilerInfoCollector;
import com.google.idea.blaze.qsync.deps.CcToolchain;
import com.google.idea.blaze.qsync.project.ProjectProto.CcCompilerKind;
import com.google.idea.blaze.qsync.project.ProjectProto.MsvcData;
import com.google.idea.blaze.qsync.project.ProjectProto.XcodeData;
import com.intellij.openapi.project.Project;

public class CcCompilerInfoCollectorImpl implements CcCompilerInfoCollector {

  public static class Provider implements CcCompilerInfoCollectorProvider {

    @Override
    public CcCompilerInfoCollector create(Project project) {
      return new CcCompilerInfoCollectorImpl(project);
    }
  }

  private final Project project;

  private CcCompilerInfoCollectorImpl(Project project) {
    this.project = project;
  }

  @Override
  public CcCompilerKind getCompilerKind(Context<?> ctx, CcToolchain toolchain) throws BuildException {
    // TODO: implement compiler kind detection (see com.google.idea.blaze.cpp.CompilerVersionCheckerImpl)
    return CcCompilerKind.CC_COMPILER_KIND_UNKNOWN;
  }

  @Override
  public MsvcData getMsvcData(Context<?> ctx, CcToolchain toolchain) throws BuildException {
    // TODO: implement msvc data collection (see com.google.idea.blaze.clwb.MSVCEnvironmentProvider)
    return MsvcData.newBuilder().build();
  }

  @Override
  public XcodeData getXcodeData(Context<?> ctx, CcToolchain toolchain) throws BuildException {
    // TODO: implement xcode data collection (see com.google.idea.blaze.cpp.XcodeCompilerSettingsProviderImpl)
    return XcodeData.newBuilder().build();
  }
}
