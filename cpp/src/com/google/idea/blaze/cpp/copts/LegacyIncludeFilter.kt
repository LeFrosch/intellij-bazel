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
package com.google.idea.blaze.cpp.copts

import com.intellij.openapi.util.registry.Registry
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind

/**
 * Legacy filter, disabled by default. Only kept for compatibility reasons.
 *
 * "-include somefile.h" doesn't seem to work for some reason. E.g., "-include cstddef" results in "clang: error: no
 * such file or directory: 'cstddef'"
 */
class LegacyIncludeFilter : CoptsProcessor.Filter() {

  override fun enabled(kind: OCCompilerKind): Boolean {
    return Registry.`is`("bazel.cpp.sync.workspace.filter.out.incompatible.flags")
  }

  override fun drop(option: String): Boolean {
    return option.startsWith("-include ")
  }
}
