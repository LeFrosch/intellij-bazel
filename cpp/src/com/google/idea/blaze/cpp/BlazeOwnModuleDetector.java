/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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

import static com.google.idea.blaze.base.settings.Blaze.isBlazeProject;

import com.intellij.openapi.module.Module;
import com.jetbrains.cidr.project.workspace.CidrOwnModuleDetector;
import org.jetbrains.annotations.NotNull;

/** A module to mark blaze's module as the project's own module. */
public class BlazeOwnModuleDetector implements CidrOwnModuleDetector {
  @Override
  public boolean isOwnModule(@NotNull Module module) {
    return isBlazeProject(module.getProject());
  }
}
