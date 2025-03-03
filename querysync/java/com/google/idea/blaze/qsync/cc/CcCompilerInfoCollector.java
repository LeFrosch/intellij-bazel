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
package com.google.idea.blaze.qsync.cc;

import com.google.idea.blaze.common.Context;
import com.google.idea.blaze.exception.BuildException;
import com.google.idea.blaze.qsync.deps.CcToolchain;
import com.google.idea.blaze.qsync.project.ProjectProto;

/**
 * It is required to collect some basic information about the compiler itself before running CLion's compiler info
 * collection, like detecting the compiler kind (clang, gcc, or msvc).
 */
public interface CcCompilerInfoCollector {

  ProjectProto.CcCompilerKind getCompilerKind(Context<?> ctx, CcToolchain toolchain) throws BuildException;

  ProjectProto.MsvcData getMsvcData(Context<?> ctx, CcToolchain toolchain) throws BuildException;

  ProjectProto.XcodeData getXcodeData(Context<?> ctx, CcToolchain toolchain) throws BuildException;
}
