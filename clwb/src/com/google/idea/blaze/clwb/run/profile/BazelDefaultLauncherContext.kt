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
package com.google.idea.blaze.clwb.run.profile

import com.google.idea.blaze.base.command.BlazeCommandName
import com.google.idea.blaze.base.model.BlazeProjectData
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot
import com.google.idea.blaze.base.projectview.ProjectViewManager
import com.google.idea.blaze.base.projectview.ProjectViewSet
import com.google.idea.blaze.base.run.BlazeCommandRunConfiguration
import com.google.idea.blaze.base.sync.data.BlazeProjectDataManager
import com.google.idea.blaze.clwb.run.BlazeCidrRunConfigState
import com.intellij.execution.ExecutionException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import java.nio.file.Path

/** Everything the launcher helpers need, resolved once up front and passed implicitly as a context. */
data class BazelDefaultLauncherContext(
  val project: Project,
  val configuration: BlazeCommandRunConfiguration,
  val environment: ExecutionEnvironment,
  val projectData: BlazeProjectData,
  val projectView: ProjectViewSet,
  val configState: BlazeCidrRunConfigState,
)

val BazelDefaultLauncherContext.executionRoot: Path
  get() = projectData.blazeInfo().executionRoot.toPath()

val BazelDefaultLauncherContext.workspaceRoot: Path
  get() = WorkspaceRoot.fromProject(project).path()

val BazelDefaultLauncherContext.isTest: Boolean
  get() = configState.commandState.command == BlazeCommandName.TEST

@Throws(ExecutionException::class)
fun resolveLauncherContext(
  configuration: BlazeCommandRunConfiguration,
  environment: ExecutionEnvironment,
): BazelDefaultLauncherContext {
  val project = configuration.project

  val configState = configuration.handler.state as? BlazeCidrRunConfigState
    ?: throw ExecutionException("Invalid run configuration handler.")

  val projectData = BlazeProjectDataManager.getInstance(project).blazeProjectData
    ?: throw ExecutionException("Cannot get Blaze project data.")

  val projectView = ProjectViewManager.getInstance(project).projectViewSet
    ?: throw ExecutionException("Cannot get project view.")

  return BazelDefaultLauncherContext(
    project = project,
    configuration = configuration,
    environment = environment,
    projectData = projectData,
    projectView = projectView,
    configState = configState,
  )
}
