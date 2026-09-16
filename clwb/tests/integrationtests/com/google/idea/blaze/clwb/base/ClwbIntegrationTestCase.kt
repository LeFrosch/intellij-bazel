/*
 * Copyright 2026 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.clwb.base

import com.google.idea.testing.headless.SandboxErrorProcessor
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.openapi.application.AccessToken
import com.intellij.testFramework.fixtures.BasePlatformTestCase

abstract class ClwbIntegrationTestCase : BasePlatformTestCase() {

  private var sandboxErrorProcessorToken: AccessToken? = null

  override fun setUp() {
    sandboxErrorProcessorToken = SandboxErrorProcessor.install()
    super.setUp()

    // RadInitialConfigurator (clion-radler) flips AUTO_POPUP_JAVADOC_INFO on the
    // first launch. The tearDown checks compare against default settings.
    setDefaultCodeInsightSettings(CodeInsightSettings.getInstance())
  }

  override fun tearDown() {
    try {
      super.tearDown()
    } finally {
      sandboxErrorProcessorToken?.finish()
      sandboxErrorProcessorToken = null
    }
  }
}
