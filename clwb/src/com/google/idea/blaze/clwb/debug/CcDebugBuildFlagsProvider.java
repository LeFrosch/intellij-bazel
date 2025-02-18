package com.google.idea.blaze.clwb.debug;

import com.google.idea.blaze.base.command.BlazeCommandName;
import com.google.idea.blaze.base.command.BlazeInvocationContext;
import com.google.idea.blaze.base.command.BuildFlagsProvider;
import com.google.idea.blaze.base.projectview.ProjectViewSet;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.clwb.run.BlazeDebuggerKind;
import com.intellij.openapi.project.Project;
import java.util.List;

public class CcDebugBuildFlagsProvider implements BuildFlagsProvider {

  @Override
  public void addBuildFlags(
      Project project,
      ProjectViewSet projectViewSet,
      BlazeCommandName command,
      BlazeInvocationContext invocationContext,
      List<String> flags) {
    if (command.equals(BlazeCommandName.MOD)) {
      return;
    }

    final var isDebugBuild = projectViewSet.getScalarValue(CcDebugBuildSection.KEY).orElse(false);
    if (!isDebugBuild) {
      return;
    }

    flags.addAll(CcDebugBuildFlags.getFlags(project, BlazeDebuggerKind.byHeuristic()));
  }

  @Override
  public void addSyncFlags(
      Project project,
      ProjectViewSet projectViewSet,
      BlazeCommandName command,
      BlazeContext context,
      BlazeInvocationContext invocationContext,
      List<String> flags) {
    addBuildFlags(project, projectViewSet, command, invocationContext, flags);
  }
}
