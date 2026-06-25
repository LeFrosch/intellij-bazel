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
package com.google.idea.blaze.base.sync.reactive

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Forces construction of the [ReactiveSyncService] when a Bazel project opens.
 *
 * The service is a lazily-instantiated project service whose watcher is launched from its `init`
 * block, so it never starts unless something requests it. Touching it here bootstraps the watcher
 * on project open instead of waiting for the user to invoke [InstallReactiveSyncFlagsAction].
 */
class ReactiveSyncStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    ReactiveSyncService.getInstance(project)
  }
}
