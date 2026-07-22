@file:Suppress("UnstableApiUsage")

package com.google.idea.blaze.clwb.run

import com.intellij.cidr.debugger.profiles.CidrDebugProfile
import com.intellij.cidr.debugger.profiles.CidrDebugProfileType
import com.intellij.execution.ExecutionException
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindIntText
import com.jetbrains.cidr.cpp.CLionExecutionBundle
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGDBDriverConfiguration
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGdbProfileDriverConfiguration
import com.jetbrains.cidr.cpp.toolchains.CPPEnvironment
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment
import icons.BlazeIcons
import org.jetbrains.annotations.Nls
import javax.swing.Icon

/** Persisted options for the [BazelGdbServerDebugProfileType]. */
class BazelGdbServerDebugProfileState : BaseState() {
  /** The port the gdbserver spawned by Bazel (`--run_under`) listens on. */
  var port: Int by property(DEFAULT_DEBUG_PORT)

  companion object {
    const val DEFAULT_DEBUG_PORT: Int = 5006
  }
}

/**
 * A user-selectable CLion debug profile representing "debug via Bazel gdbserver".
 *
 * Selecting this profile is how a user opts into gdbserver-based debugging; its single [port]
 * option is the authoritative source of the gdbserver port. The launcher wiring that actually
 * spawns gdbserver and connects the remote debug process using this port is a follow-up.
 *
 * Note: [CidrDebugProfileType] is marked `@ApiStatus.Internal` and gated behind the
 * `cidr.debugger.enableDebugProfiles` registry key; treat it as unstable across CLion releases.
 */
class BazelGdbServerDebugProfileType : CidrDebugProfileType<BazelGdbServerDebugProfileState> {

  companion object {
    const val ID: String = "bazel-gdb-server"
  }

  override fun getId(): String = ID

  override fun getName(): @Nls String = "GDB Server"

  override fun getDefaultDebugProfileName(): String = "GDB Server"

  override fun getTileIcon(): Icon = BlazeIcons.Logo

  override fun createDefaultState(): BazelGdbServerDebugProfileState = BazelGdbServerDebugProfileState()

  override fun createEmptyState(): BazelGdbServerDebugProfileState = BazelGdbServerDebugProfileState()

  override fun createDriverConfiguration(
    project: Project,
    profile: CidrDebugProfile<BazelGdbServerDebugProfileState>,
    isElevated: Boolean,
    isEmulateTerminal: Boolean,
    environment: CidrToolEnvironment,
  ): DebuggerDriverConfiguration {
    if (environment !is CPPEnvironment) {
      throw ExecutionException(CLionExecutionBundle.message("debugProfile.gdb.error.unsupported.execution.environment"))
    }
    return CLionGDBDriverConfiguration(project, environment.toolchain)
  }

  override fun buildSettingsUi(
    project: Project,
    state: BazelGdbServerDebugProfileState,
    panel: Panel,
    disposable: Disposable,
    integratedPanels: MutableList<DialogPanel>,
  ) {
    with(panel) {
      row("Port:") {
        intTextField(range = 0..65535).bindIntText(state::port)
      }
    }
  }
}
