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

import com.google.idea.blaze.base.actions.BlazeProjectAction
import com.google.idea.blaze.base.sync.data.BlazeProjectDataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.IOException

private const val TITLE = "Enable Reactive Sync"

/**
 * Writes an IDE-managed bazelrc with the aspect + build-event flags required for reactive sync and
 * `import`s it from the workspace `.bazelrc`. After this, every external `bazel build` runs the
 * IntelliJ aspect and streams its build events to the file watched by [ReactiveSyncService].
 *
 * Installing these flags is the on-switch for reactive sync; removing the managed `import` line
 * turns it off.
 */
class InstallReactiveSyncFlagsAction : BlazeProjectAction() {

  override fun actionPerformedInBlazeProject(project: Project, e: AnActionEvent) {
    val projectData = BlazeProjectDataManager.getInstance(project).blazeProjectData
    if (projectData == null) {
      Messages.showErrorDialog(
        project, "Sync the project at least once before enabling reactive sync.", TITLE)
      return
    }

    val confirmed =
      Messages.showYesNoDialog(
        project,
        "This adds Bazel flags imported from the project's .bazelrc.\n\n" +
          "Every 'bazel build' will then run the IntelliJ aspect and stream build events to the " +
          "IDE, so external builds update the project model automatically.\n\nContinue?",
        TITLE,
        Messages.getQuestionIcon())
    if (confirmed != Messages.YES) {
      return
    }

    try {
      ReactiveSyncService.getInstance(project).setEnabled(true)
    } catch (ex: IOException) {
      Messages.showErrorDialog(project, "Failed to install reactive sync flags: ${ex.message}", TITLE)
      return
    }

    Messages.showInfoMessage(
      project,
      "Reactive sync is enabled. External 'bazel build' invocations will now update the project " +
        "model without an explicit sync.",
      TITLE)
  }
}
