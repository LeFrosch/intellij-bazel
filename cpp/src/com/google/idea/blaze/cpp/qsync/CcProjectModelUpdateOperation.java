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
package com.google.idea.blaze.cpp.qsync;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.idea.blaze.base.scope.output.IssueOutput;
import com.google.idea.blaze.base.util.UrlUtil;
import com.google.idea.blaze.common.Context;
import com.google.idea.blaze.cpp.CppSupportChecker;
import com.google.idea.blaze.qsync.cc.FlagResolver;
import com.google.idea.blaze.qsync.project.ProjectPath;
import com.google.idea.blaze.qsync.project.ProjectProto.CcCompilationContext;
import com.google.idea.blaze.qsync.project.ProjectProto.CcCompilerFlagSet;
import com.google.idea.blaze.qsync.project.ProjectProto.CcCompilerSettings;
import com.google.idea.blaze.qsync.project.ProjectProto.CcLanguage;
import com.google.idea.blaze.qsync.project.ProjectProto.CcSourceFile;
import com.google.idea.blaze.qsync.project.ProjectProto.CcToolchain;
import com.google.idea.blaze.qsync.project.ProjectProto.CcWorkspace;
import com.intellij.build.events.MessageEvent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.containers.MultiMap;
import com.jetbrains.cidr.lang.CLanguageKind;
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches;
import com.jetbrains.cidr.lang.toolchains.CidrToolEnvironment;
import com.jetbrains.cidr.lang.workspace.OCCompilerSettings;
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration;
import com.jetbrains.cidr.lang.workspace.OCWorkspace;
import com.jetbrains.cidr.lang.workspace.compiler.ClangCompilerKind;
import com.jetbrains.cidr.lang.workspace.compiler.CompilerInfoCache;
import com.jetbrains.cidr.lang.workspace.compiler.CompilerInfoCache.Message;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Updates the IJ project model based a {@link CcWorkspace} proto message. */
public class CcProjectModelUpdateOperation implements Disposable {

  private static final String CLIENT_KEY = "ASwB";
  private static final int CLIENT_VERSION = 1;

  private static final Logger logger = Logger.getInstance(CcProjectModelUpdateOperation.class);
  private final Context<?> context;
  private final ProjectPath.Resolver pathResolver;
  private final FlagResolver flagResolver;
  private final OCWorkspace.ModifiableModel modifiableOcWorkspace;
  private final Map<String, CidrCompilerSwitches> compilerSwitches = Maps.newHashMap();
  private final Map<String, OCResolveConfiguration.ModifiableModel> resolveConfigs =
      Maps.newLinkedHashMap();
  private final File compilerWorkingDir;

  CcProjectModelUpdateOperation(
      Context<?> context, OCWorkspace readonlyOcWorkspace, ProjectPath.Resolver pathResolver) {
    this.context = context;
    this.pathResolver = pathResolver;
    this.flagResolver = new FlagResolver(pathResolver);
    // TODO(mathewi) should we use clear=false here and do the diff instead?
    modifiableOcWorkspace = readonlyOcWorkspace.getModifiableModel(CLIENT_KEY, /* clear= */ true);
    modifiableOcWorkspace.setClientVersion(CLIENT_VERSION);
    compilerWorkingDir = pathResolver.resolve(ProjectPath.WORKSPACE_ROOT).toFile();
  }

  /** Visit a {@link CcWorkspace} proto. Should be called from a background thread. */
  public void visitWorkspace(CcWorkspace proto) {
    visitSwitchesMap(proto.getFlagSetsMap());
    for (CcCompilationContext compilationContext : proto.getContextsList()) {
      visitCompilationContext(proto.getToolchainsList(), compilationContext);
    }
  }

  private void visitSwitchesMap(Map<String, CcCompilerFlagSet> flagsetMap) {
    for (Map.Entry<String, CcCompilerFlagSet> e : flagsetMap.entrySet()) {
      compilerSwitches.put(
          e.getKey(), new CidrCompilerSwitches(flagResolver.resolveAll(e.getValue())));
    }
  }

  private void visitCompilationContext(List<CcToolchain> toolchains, CcCompilationContext ccCc) {
    final var config = modifiableOcWorkspace.addConfiguration(ccCc.getId(), ccCc.getHumanReadableName());

    final var compilerSettings = ccCc.getCompilerSettingsList();
    visitLanguageCompilerSettingsMap(toolchains, compilerSettings, config);

    for (CcSourceFile source : ccCc.getSourcesList()) {
      visitSourceFile(toolchains, compilerSettings, source, config);
    }

    resolveConfigs.put(ccCc.getId(), config);
  }

  private void visitLanguageCompilerSettingsMap(
      List<CcToolchain> toolchains,
      List<CcCompilerSettings> list,
      OCResolveConfiguration.ModifiableModel config
  ) {
    for (CcCompilerSettings e : list) {
      final var toolchain = toolchains.stream()
          .filter((it) -> it.getId().equals(e.getToolchainId()))
          .findFirst()
          .orElse(null);
      if (toolchain == null) {
        continue;
      }

      final var switches = checkNotNull(compilerSwitches.get(e.getFlagSetId()));
      if (!CppSupportChecker.isSupportedCppConfiguration(switches, compilerWorkingDir.toPath())) {
        return;
      }

      CLanguageKind lang = getLanguageKind(e.getLanguage(), "compiler settings");
      OCCompilerSettings.ModifiableModel compilerSettings = config.getLanguageCompilerSettings(lang);
      compilerSettings.setCompiler(
          ClangCompilerKind.INSTANCE, // TODO: get kind from toolchain.getKind()
          getCompilerExecutable(toolchain),
          compilerWorkingDir);
      compilerSettings.setCompilerSwitches(switches);
    }
  }

  private void visitSourceFile(
      List<CcToolchain> toolchains,
      List<CcCompilerSettings> compilerSettings,
      CcSourceFile source,
      OCResolveConfiguration.ModifiableModel config
  ) {
    final var compilerSetting = compilerSettings.stream()
        .filter((it) -> it.getLanguage().equals(source.getLanguage()))
        .findFirst()
        .orElse(null);
    if (compilerSetting == null) {
      return;
    }

    final var toolchain = toolchains.stream()
        .filter((it) -> it.getId().equals(compilerSetting.getToolchainId()))
        .findFirst()
        .orElse(null);
    if (toolchain == null) {
      return;
    }

    CidrCompilerSwitches switches = checkNotNull(compilerSwitches.get(compilerSetting.getFlagSetId()));
    if (!CppSupportChecker.isSupportedCppConfiguration(switches, pathResolver.resolve(ProjectPath.WORKSPACE_ROOT))) {
      // Ignore the file if it's not supported by the current IDE.
      return;
    }

    Path srcPath = Path.of(source.getWorkspacePath());
    CLanguageKind language = getLanguageKind(source.getLanguage(), "Source file " + source.getWorkspacePath());
    srcPath = pathResolver.resolve(ProjectPath.workspaceRelative(srcPath));
    if (!Files.exists(srcPath)) {
      logger.warn("Src file not found: " + srcPath);
    }

    // TODO: can this logic be deduplicated with visitLanguageCompilerSettingsMap
    OCCompilerSettings.ModifiableModel perSourceCompilerSettings =
        config.addSource(UrlUtil.pathToIdeaDirectoryUrl(srcPath), language);
    perSourceCompilerSettings.setCompilerSwitches(switches);
    perSourceCompilerSettings.setCompiler(
        ClangCompilerKind.INSTANCE, // TODO: get kind from toolchain.getKind()
        getCompilerExecutable(toolchain),
        compilerWorkingDir);
  }

  private static final ImmutableMap<CcLanguage, CLanguageKind> LANGUAGE_MAP =
      ImmutableMap.of(
          CcLanguage.C, CLanguageKind.C,
          CcLanguage.CPP, CLanguageKind.CPP,
          CcLanguage.OBJ_C, CLanguageKind.OBJ_C,
          CcLanguage.OBJ_CPP, CLanguageKind.OBJ_CPP);

  private CLanguageKind getLanguageKind(CcLanguage language, String whatFor) {
    return Preconditions.checkNotNull(
        LANGUAGE_MAP.get(language), "Invalid language " + language + " for " + whatFor);
  }

  private File getCompilerExecutable(CcToolchain toolchain) {
    return pathResolver
        .resolve(ProjectPath.create(toolchain.getExecutable()))
        .toFile();
  }

  /** Pre-commits the project update. Should be called from a background thread. */
  public void preCommit() {
    processCompilerSettings();
    modifiableOcWorkspace.preCommit();
  }

  /** Commits the project update. Must be called from the write thread. */
  public void commit() {
    boolean didChange = WriteAction.compute(modifiableOcWorkspace::commit);
    if (!didChange) {
      logger.warn("Project model update did not result in any changes.");
    }
  }

  private void processCompilerSettings() {
    EmptyProgressIndicator indicator = new EmptyProgressIndicator();
    CompilerInfoCache cache = new CompilerInfoCache();
    var session = cache.<String>createSession(indicator);
    boolean sessionClosed = false;
    try {
      // TODO: create environment from toolchain
      var toolEnvironment = new CidrToolEnvironment();
      session.setExpectedJobsCount(resolveConfigs.size());
      for (Map.Entry<String, OCResolveConfiguration.ModifiableModel> e : resolveConfigs.entrySet()) {
        session.schedule(
            e.getKey(),
            e.getValue(),
            toolEnvironment,
            pathResolver.resolve(ProjectPath.WORKSPACE_ROOT).toString());
      }

      // Compute all configurations. Block until complete.
      var messages = new MultiMap<String, Message>();
      session.waitForAll(messages);
      sessionClosed = true;

      ImmutableList<Message> frozenMessages =
          messages.freezeValues().values().stream()
              .flatMap(Collection::stream)
              .collect(ImmutableList.toImmutableList());

      for (final var message : frozenMessages) {
        final var kind = switch (message.getType()) {
          case ERROR -> MessageEvent.Kind.ERROR;
          case WARNING -> MessageEvent.Kind.WARNING;
        };

        IssueOutput.issue(kind, "COMPILER INFO COLLECTION")
            .withDescription(message.getText())
            .submit(context);
      }
    } finally {
      if (!sessionClosed) {
        session.dispose();
      }
    }
  }

  @Override
  public void dispose() {
    Disposer.dispose(modifiableOcWorkspace);
  }
}
