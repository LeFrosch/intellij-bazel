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
package com.google.idea.blaze.base.qsync;

import static com.google.idea.blaze.base.sync.SyncScope.SyncFailedException;

import com.google.idea.blaze.base.bazel.BuildSystem;
import com.google.idea.blaze.base.command.info.BlazeInfo;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.exception.BuildException;

public class BazelInfoHandler {
  private final BuildSystem.BuildInvoker buildInvoker;

  BazelInfoHandler(BuildSystem.BuildInvoker buildInvoker) {
    this.buildInvoker = buildInvoker;
  }

  public BlazeInfo getBazelInfo(BlazeContext context) throws BuildException {
    // TODO: can we cache the results from handlers?
    try {
      return buildInvoker.getBlazeInfo(context);
    } catch (SyncFailedException e) {
      throw new BuildException("Could not get bazel info", e);
    }
  }
}
