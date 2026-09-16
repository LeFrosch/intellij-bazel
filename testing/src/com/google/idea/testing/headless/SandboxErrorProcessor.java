/*
 * Copyright 2026 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.idea.testing.headless;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.LoggedErrorProcessor;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Ignores IDE errors which are caused by the sandboxed test environment and cannot be avoided, since every logged error
 * fails the test it was logged in.
 */
public final class SandboxErrorProcessor extends LoggedErrorProcessor {

  private static final SandboxErrorProcessor INSTANCE = new SandboxErrorProcessor();

  /**
   * Installs the processor until the returned token is finished. Needs to be installed before the project is opened,
   * because the radler backend is started during project open.
   */
  public static AccessToken install() {
    return LoggedErrorProcessor.executeWith(INSTANCE);
  }

  private SandboxErrorProcessor() { }

  @Override
  public Set<Action> processError(String category, String message, String[] details, @Nullable Throwable t) {
    // The Rider backend tries to access ~/Library/Application Support/Symbols
    // disregarding any configuration. This directory does not reside inside
    // the sandbox and thus cannot be accessed during tests. #api262
    if (SystemInfo.isMac && message.contains("Application Support/Symbols")) {
      return Action.NONE;
    }

    return Action.ALL;
  }
}
