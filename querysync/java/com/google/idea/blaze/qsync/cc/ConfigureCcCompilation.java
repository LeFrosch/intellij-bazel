/*
 * Copyright 2023 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.qsync.cc;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.idea.blaze.common.PrintOutput;
import com.google.idea.blaze.exception.BuildException;
import com.google.idea.blaze.qsync.artifacts.BuildArtifact;
import com.google.idea.blaze.qsync.deps.ArtifactDirectories;
import com.google.idea.blaze.qsync.deps.ArtifactDirectoryBuilder;
import com.google.idea.blaze.qsync.deps.ArtifactTracker.State;
import com.google.idea.blaze.qsync.deps.CcCompilationInfo;
import com.google.idea.blaze.qsync.deps.CcToolchain;
import com.google.idea.blaze.qsync.deps.DependencyBuildContext;
import com.google.idea.blaze.qsync.deps.ProjectProtoUpdate;
import com.google.idea.blaze.qsync.deps.ProjectProtoUpdateOperation;
import com.google.idea.blaze.qsync.deps.TargetBuildInfo;
import com.google.idea.blaze.qsync.project.LanguageClassProto.LanguageClass;
import com.google.idea.blaze.qsync.project.ProjectPath;
import com.google.idea.blaze.qsync.project.ProjectProto;
import com.google.idea.blaze.qsync.project.ProjectTarget;
import com.google.idea.blaze.qsync.project.ProjectTarget.SourceType;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/** Adds C/C++ compilation information and headers to the project proto. */
@SuppressWarnings("ResultOfMethodCallIgnored") // ignore unused builder returns
public class ConfigureCcCompilation {

  /** An update operation to configure CC compilation. */
  public static class UpdateOperation implements ProjectProtoUpdateOperation {

    @Override
    public void update(ProjectProtoUpdate update, State artifactState)
      throws BuildException {
      new ConfigureCcCompilation(artifactState, update).update();
    }
  }

  private static final AtomicInteger nextFlagSetId = new AtomicInteger(0);
  private final State artifactState;
  private final ProjectProtoUpdate update;

  /* Map from toolchain ID -> language -> flags for that toolchain & language. */
  private final Map<String, Map<ProjectProto.CcLanguage, List<ProjectProto.CcCompilerFlag>>> toolchainLanguageFlags = Maps.newHashMap();

  /* Map of unique sets of compiler flags to an ID to identify them.
   * We do this as the downstream code turns each set of flags into a CidrCompilerSwitches instance
   * which can have a large memory footprint. */
  private final Map<Set<ProjectProto.CcCompilerFlag>, String> uniqueFlagSetIds = Maps.newHashMap();

  ConfigureCcCompilation(State artifactState, ProjectProtoUpdate projectUpdate) {
    this.artifactState = artifactState;
    this.update = projectUpdate;
  }

  public void update() throws BuildException {

    visitToolchainMap(artifactState.ccToolchainMap());

    for (TargetBuildInfo target : artifactState.depsMap().values()) {
      if (target.ccInfo().isEmpty()) {
        continue;
      }
      CcCompilationInfo ccInfo = target.ccInfo().get();
      visitTarget(ccInfo, target.buildContext());
    }
    if (update.project().getCcWorkspaceBuilder().getContextsCount() > 0) {
      update.project().addActiveLanguages(LanguageClass.LANGUAGE_CLASS_CC);
    }
  }

  private void visitToolchainMap(Map<String, CcToolchain> toolchainInfoMap) throws BuildException {
    for (final var toolchain : toolchainInfoMap.values()) {
      visitToolchain(toolchain);
    }
  }

  private void visitToolchain(CcToolchain toolchain) throws BuildException {
    final var commonFlags = toolchain.builtInIncludeDirectories().stream()
        .map(p -> makePathFlag("-I", p))
        .collect(toImmutableList());

    final var cFlags = Stream.concat(
        commonFlags.stream(),
        toolchain.cOptions().stream().map(f -> makeStringFlag(f, ""))
    ).collect(toImmutableList());

    final var cppFlags = Stream.concat(
        commonFlags.stream(),
        toolchain.cppOptions().stream().map(f -> makeStringFlag(f, ""))
    ).collect(toImmutableList());

    toolchainLanguageFlags.put(
        toolchain.id(),
        ImmutableMap.of(ProjectProto.CcLanguage.C, cFlags, ProjectProto.CcLanguage.CPP, cppFlags)
    );

    final var builder = ProjectProto.CcToolchain.newBuilder()
      .setId(toolchain.id())
      .setName(toolchain.compiler())
      .setCpu(toolchain.cpu())
      .setInfo(toolchain.compilerInfo().toProto());

    update.project().getCcWorkspaceBuilder().addToolchains(builder.build());
  }


  private void visitTarget(CcCompilationInfo ccInfo, DependencyBuildContext buildContext) {
    ProjectTarget projectTarget = update.buildGraph().targetMap().get(ccInfo.target());
    if (projectTarget == null) {
      // This target is no longer present in the project. Ignore it.
      // We should really clean up the dependency cache itself to remove any artifacts relating to
      // no-longer-present targets, but that will be a lot more work. For now, just ensure we
      // don't crash.
      return;
    }
    CcToolchain toolchain =
        Preconditions.checkNotNull(
            artifactState.ccToolchainMap().get(ccInfo.toolchainId()), ccInfo.toolchainId());

    ImmutableList<ProjectProto.CcCompilerFlag> targetFlags =
        ImmutableList.<ProjectProto.CcCompilerFlag>builder()
            .addAll(projectTarget.copts().stream().map(d -> makeStringFlag(d, "")).iterator())
            .addAll(ccInfo.defines().stream().map(d -> makeStringFlag("-D", d)).iterator())
            .addAll(ccInfo.includeDirectories().stream().map(p -> makePathFlag("-I", p)).iterator())
            .addAll(
                ccInfo.quoteIncludeDirectories().stream()
                    .map(p -> makePathFlag("-iquote", p))
                    .iterator())
            .addAll(
                ccInfo.systemIncludeDirectories().stream()
                    .map(p -> makePathFlag("-isystem", p))
                    .iterator())
            .addAll(
                ccInfo.frameworkIncludeDirectories().stream()
                    .map(p -> makePathFlag("-F", p))
                    .iterator())
            .build();

    final var srcsBuilder = ImmutableList.<ProjectProto.CcSourceFile>builder();
    for (Path srcPath : update.buildGraph().getTargetSources(ccInfo.target(), SourceType.all())) {
      final var lang = getLanguage(srcPath);
      if (lang.isEmpty()) {
        continue;
      }

      srcsBuilder.add(
          ProjectProto.CcSourceFile.newBuilder()
              .setLanguage(lang.get())
              .setWorkspacePath(srcPath.toString())
              .build());
    }

    // TODO(mathewi): The handling of flag sets here is not optimal, since we recalculate an
    //  identical flag set for each source of the same language, then immediately de-dupe them in
    //  the addFlagSet call. For large flag sets this may be slow.
    final var compilerSettingsBuilder = ImmutableList.<ProjectProto.CcCompilerSettings>builder();
    for (final var entry : toolchainLanguageFlags.get(toolchain.id()).entrySet()) {
      final var flags = Stream.concat(entry.getValue().stream(), targetFlags.stream()).collect(toImmutableList());

      final var settings = ProjectProto.CcCompilerSettings.newBuilder()
          .setToolchainId(toolchain.id())
          .setLanguage(entry.getKey())
          .setFlagSetId(addFlagSet(flags))
          .build();

      compilerSettingsBuilder.add(settings);
    }

    final var targetContext = ProjectProto.CcCompilationContext.newBuilder()
        .setId(ccInfo.target() + "%" + toolchain.targetGnuSystemName())
        .setHumanReadableName(ccInfo.target() + " - " + toolchain.targetGnuSystemName())
        .addAllSources(srcsBuilder.build())
        .addAllCompilerSettings(compilerSettingsBuilder.build())
        .build();

    update.project().getCcWorkspaceBuilder().addContexts(targetContext);

    ArtifactDirectoryBuilder headersDir =
        update.artifactDirectory(ArtifactDirectories.GEN_CC_HEADERS);
    for (BuildArtifact artifact : ccInfo.genHeaders()) {
      headersDir.addIfNewer(artifact.artifactPath(), artifact, buildContext);
    }
  }

  /** Ensure that the given flagset exists, adding it if necessary, and return its unique ID. */
  private String addFlagSet(Collection<ProjectProto.CcCompilerFlag> flags) {
    // Create a set so that two flags sets are considered equivalent if their flag order differs.
    final var canonicalFlagSet = ImmutableSet.copyOf(flags);
    String flagSetId = uniqueFlagSetIds.get(canonicalFlagSet);

    if (flagSetId == null) {
      flagSetId = Integer.toString(nextFlagSetId.incrementAndGet());
      uniqueFlagSetIds.put(canonicalFlagSet, flagSetId);
      update
          .project()
          .getCcWorkspaceBuilder()
          .putFlagSets(flagSetId, ProjectProto.CcCompilerFlagSet.newBuilder().addAllFlags(flags).build());
    }
    return flagSetId;
  }

  private ProjectProto.CcCompilerFlag makeStringFlag(String flag, String value) {
    return ProjectProto.CcCompilerFlag.newBuilder().setFlag(flag).setPlainValue(value).build();
  }

  private ProjectProto.CcCompilerFlag makePathFlag(String flag, ProjectPath path) {
    return ProjectProto.CcCompilerFlag.newBuilder().setFlag(flag).setPath(path.toProto()).build();
  }

  private static final ImmutableMap<String, ProjectProto.CcLanguage> EXTENSION_TO_LANGUAGE_MAP =
      ImmutableMap.of(
          "c", ProjectProto.CcLanguage.C,
          "cc", ProjectProto.CcLanguage.CPP,
          "cpp", ProjectProto.CcLanguage.CPP,
          "cxx", ProjectProto.CcLanguage.CPP,
          "c++", ProjectProto.CcLanguage.CPP,
          "C", ProjectProto.CcLanguage.C);
  /* Files we ignore because they are not top level source files: */

  private static final ImmutableSet<String> IGNORE_SRC_FILE_EXTENSIONS =
      ImmutableSet.of("h", "hh", "hpp", "hxx", "inc", "inl", "H", "S", "a", "lo", "so", "o");

  private Optional<ProjectProto.CcLanguage> getLanguage(Path srcPath) {
    // logic in here based on https://bazel.build/reference/be/c-cpp#cc_library.srcs
    int lastDot = srcPath.getFileName().toString().lastIndexOf('.');
    if (lastDot < 0) {
      // default to cpp
      update
          .context()
          .output(PrintOutput.log("No extension for c/c++ source file %s; assuming cpp", srcPath));
      return Optional.of(ProjectProto.CcLanguage.CPP);
    }
    String ext = srcPath.getFileName().toString().substring(lastDot + 1);
    if (IGNORE_SRC_FILE_EXTENSIONS.contains(ext)) {
      return Optional.empty();
    }
    if (EXTENSION_TO_LANGUAGE_MAP.containsKey(ext)) {
      return Optional.of(EXTENSION_TO_LANGUAGE_MAP.get(ext));
    }
    update
        .context()
        .output(
            PrintOutput.log(
                "Unrecognized extension %s for c/c++ source file %s; assuming cpp", ext, srcPath));
    return Optional.of(ProjectProto.CcLanguage.CPP);
  }
}
