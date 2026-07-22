@file:Suppress("UnstableApiUsage")

package com.google.idea.blaze.clwb.run.profile

import com.google.idea.blaze.base.run.BlazeCommandRunConfiguration
import com.google.idea.blaze.clwb.run.BlazeCidrRunConfigState
import com.intellij.cidr.debugger.profiles.CidrDebugProfilesEnabler
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class BazelProfileEnabler : CidrDebugProfilesEnabler {

  override fun debugProfilesEnabled(project: Project, runConfiguration: RunConfiguration?): Boolean {
    return runConfiguration is BlazeCommandRunConfiguration && runConfiguration.handler.state is BlazeCidrRunConfigState
  }
}
