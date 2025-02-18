package com.google.idea.blaze.clwb.debug;

import com.google.idea.blaze.base.projectview.ProjectViewManager;
import com.google.idea.blaze.base.projectview.ProjectViewSet;
import com.google.idea.blaze.base.projectview.section.ScalarSection;
import com.google.idea.blaze.base.projectview.section.SectionKey;
import com.google.idea.blaze.base.projectview.section.SectionParser;
import com.google.idea.blaze.base.projectview.section.sections.BooleanSectionParser;
import com.intellij.openapi.project.Project;

public class CcDebugBuildSection {

  private static final String DOCUMENTATION = "Enables debug builds for the entire project, "
      + "required when using debug run configurations. "
      + "However, can cause cache invalidation when toggled.";

  public static final SectionKey<Boolean, ScalarSection<Boolean>> KEY = SectionKey.of("cc_debug_build");

  public static final SectionParser PARSER = new BooleanSectionParser(KEY, DOCUMENTATION);

  public static boolean isEnabled(Project project) {
    final var projectViewSet = ProjectViewManager.getInstance(project).getProjectViewSet();
    if (projectViewSet == null) {
      return false;
    }

    return isEnabled(projectViewSet);
  }

  public static boolean isEnabled(ProjectViewSet projectViewSet) {
    return projectViewSet.getScalarValue(KEY).orElse(false);
  }
}
