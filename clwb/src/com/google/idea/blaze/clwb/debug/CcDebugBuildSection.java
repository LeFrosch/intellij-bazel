package com.google.idea.blaze.clwb.debug;

import com.google.idea.blaze.base.projectview.section.ScalarSection;
import com.google.idea.blaze.base.projectview.section.SectionKey;
import com.google.idea.blaze.base.projectview.section.SectionParser;
import com.google.idea.blaze.base.projectview.section.sections.BooleanSectionParser;

public class CcDebugBuildSection {

  private static final String DOCUMENTATION = "Enables debug builds for the entire project, "
      + "required when using debug run configurations. "
      + "However, can cause cache invalidation when toggled.";

  public static final SectionKey<Boolean, ScalarSection<Boolean>> KEY = SectionKey.of("cc_debug_build");

  public static final SectionParser PARSER = new BooleanSectionParser(KEY, DOCUMENTATION);
}
