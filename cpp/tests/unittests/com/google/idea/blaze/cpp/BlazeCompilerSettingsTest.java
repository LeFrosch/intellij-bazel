/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.cpp;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.idea.blaze.base.BlazeTestCase;
import com.google.idea.blaze.base.bazel.BazelBuildSystemProvider;
import com.google.idea.blaze.base.bazel.BuildSystemProvider;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.model.MockBlazeProjectDataBuilder;
import com.google.idea.blaze.base.model.MockBlazeProjectDataManager;
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot;
import com.google.idea.blaze.base.qsync.settings.QuerySyncSettings;
import com.google.idea.blaze.base.settings.BlazeImportSettings;
import com.google.idea.blaze.base.settings.BlazeImportSettings.ProjectType;
import com.google.idea.blaze.base.settings.BlazeImportSettingsManager;
import com.google.idea.blaze.base.settings.BuildSystemName;
import com.google.idea.blaze.base.sync.data.BlazeProjectDataManager;
import com.google.idea.common.experiments.ExperimentService;
import com.google.idea.common.experiments.MockExperimentService;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.util.registry.Registry;
import com.jetbrains.cidr.lang.CLanguageKind;
import java.io.File;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link BlazeCompilerSettings}.
 */
@RunWith(JUnit4.class)
public class BlazeCompilerSettingsTest extends BlazeTestCase {

  private BlazeProjectData blazeProjectData;
  private WorkspaceRoot workspaceRoot;

  @Override
  protected void initTest(Container applicationServices, Container projectServices) {
    // access to target map from IncludeRootFlagsProcessor requires ExperimentService
    // which is used there to check whether querysync is enabled
    applicationServices.register(ExperimentService.class, new MockExperimentService());

    Registry.get("bazel.sync.resolve.virtual.includes").setValue(true);

    applicationServices.register(QuerySyncSettings.class, new QuerySyncSettings());

    ExtensionPointImpl<BlazeCompilerFlagsProcessor.Provider> ep =
        registerExtensionPoint(
            BlazeCompilerFlagsProcessor.EP_NAME, BlazeCompilerFlagsProcessor.Provider.class);
    ep.registerExtension(new IncludeRootFlagsProcessor.Provider());
    ep.registerExtension(new SysrootFlagsProcessor.Provider());

    BlazeImportSettingsManager importSettingsManager = new BlazeImportSettingsManager(project);
    BlazeImportSettings importSettings =
        new BlazeImportSettings(
            "/root", "", "", "", BuildSystemName.Bazel, ProjectType.ASPECT_SYNC);
    importSettingsManager.setImportSettings(importSettings);
    projectServices.register(BlazeImportSettingsManager.class, importSettingsManager);

    workspaceRoot = WorkspaceRoot.fromImportSettings(importSettings);
    blazeProjectData = MockBlazeProjectDataBuilder.builder(workspaceRoot).build();
    projectServices.register(
        BlazeProjectDataManager.class, new MockBlazeProjectDataManager(blazeProjectData));
  }

  @Override
  protected BuildSystemProvider createBuildSystemProvider() {
    return new BazelBuildSystemProvider();
  }

  private static BlazeCompilerSettings.Builder createCompilerSettingsBuilder() {
    return BlazeCompilerSettings.builder()
        .setCCompiler(new File("bin/c"))
        .setCppCompiler(new File("bin/c++"))
        .setVersion("cc version (trunk r123456)")
        .setName("cc")
        .setCSwitches(ImmutableList.of())
        .setCppSwitches(ImmutableList.of())
        .setBuiltInIncludes(ImmutableList.of())
        .setEnvironment(ImmutableMap.of());
  }

  @Test
  public void testCompilerSwitchesSimple() {
    ImmutableList<String> cFlags = ImmutableList.of("-fast", "-slow");
    BlazeCompilerSettings settings = createCompilerSettingsBuilder()
        .setCSwitches(cFlags)
        .setCppSwitches(cFlags)
        .build();

    assertThat(settings.getCompilerSwitches(CLanguageKind.C, null))
        .containsExactly("-fast", "-slow")
        .inOrder();
  }

  @Test
  public void relativeSysroot_makesAbsolutePathInMainWorkspace() {
    final var flags = List.of("--sysroot=third_party/toolchain/");
    final var result = BlazeCompilerFlagsProcessor.process(project, flags);
    assertThat(result).containsExactly("--sysroot=" + workspaceRoot + "/third_party/toolchain");
  }

  @Test
  public void absoluteSysroot_doesNotChange() {
    final var flags = List.of("--sysroot=/usr");
    final var result = BlazeCompilerFlagsProcessor.process(project, flags);
    assertThat(result).containsExactly("--sysroot=/usr");
  }

  @Test
  public void relativeIsystem_makesAbsolutePathInWorkspaces() {
    final var flags = List.of("-isystem", "external/arm_gcc/include", "-DFOO=1", "-Ithird_party/stl");
    final var result = BlazeCompilerFlagsProcessor.process(project, flags);

    final var outputBase = blazeProjectData.getBlazeInfo().getOutputBase().toString();

    assertThat(result)
        .containsExactly(
            "-isystem",
            outputBase + "/external/arm_gcc/include",
            "-DFOO=1",
            "-I",
            workspaceRoot + "/third_party/stl");
  }

  @Test
  public void relativeIquote_makesAbsolutePathInExecRoot() {
    final var flags = List.of("-iquote", "bazel-out/android-arm64-v8a-opt/bin/external/boringssl");
    final var result = BlazeCompilerFlagsProcessor.process(project, flags);

    final var execRoot = blazeProjectData.getBlazeInfo().getExecutionRoot().toString();

    assertThat(result)
        .containsExactly(
            "-iquote",
            execRoot + "/bazel-out/android-arm64-v8a-opt/bin/external/boringssl");
  }

  @Test
  public void absoluteISystem_doesNotChange() {
    final var flags = List.of("-isystem", "/usr/include");
    final var result = BlazeCompilerFlagsProcessor.process(project, flags);
    assertThat(result).containsExactly("-isystem", "/usr/include");
  }

  @Test
  public void developerDirEnvVar_doesNotChange() {
    BlazeCompilerSettings settings = createCompilerSettingsBuilder()
        .setEnvironment(ImmutableMap.of("DEVELOPER_DIR", "/tmp/foobar"))
        .build();

    assertThat(settings.environment().get("DEVELOPER_DIR")).matches("/tmp/foobar");
  }
}
