package com.google.idea.blaze.clwb.debug;

import com.google.common.collect.ImmutableList;
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot;
import com.google.idea.blaze.clwb.run.BlazeDebuggerKind;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;

public class CcDebugBuildFlags {

  private static final ImmutableList<String> DEFAULT_FLAGS = ImmutableList.of(
      "--compilation_mode=dbg",
      "--copt=-O0",
      "--copt=-g",
      "--strip=never",
      "--dynamic_mode=off"
  );

  // refer to #2101 for context
  private static ImmutableList<String> lldbStripAbsolutPathsFlags(Project project) {
    if (Registry.is("bazel.trim.absolute.path.disabled")) {
      return ImmutableList.of();
    }

    final var root = WorkspaceRoot.fromProjectSafe(project);
    if (root == null) {
      return ImmutableList.of();
    }

    final var builder = ImmutableList.<String>builder();
    builder.add(String.format("--copt=-fdebug-compilation-dir=%s", root.path()));

    if (SystemInfo.isMac) {
      builder.add("--linkopt=-Wl,-oso_prefix,.");
    }

    return builder.build();
  }

  // refer to #5604 for context
  private static ImmutableList<String> fissionFlags() {
    if (Registry.is("bazel.clwb.debug.fission.disabled")) {
      return ImmutableList.of();
    } else {
      return ImmutableList.of("--fission=yes");
    }
  }

  public static ImmutableList<String> getFlags(Project project, BlazeDebuggerKind kind) {
    final var builder = ImmutableList.<String>builder();
    builder.addAll(DEFAULT_FLAGS);
    builder.addAll(fissionFlags());

    if (kind.equals(BlazeDebuggerKind.BUNDLED_LLDB)) {
      builder.addAll(lldbStripAbsolutPathsFlags(project));
    }

    return builder.build();
  }
}
