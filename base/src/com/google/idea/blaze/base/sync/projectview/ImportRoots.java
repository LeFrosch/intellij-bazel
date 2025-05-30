/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.base.sync.projectview;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.idea.blaze.base.bazel.BuildSystemProvider;
import com.google.idea.blaze.base.model.primitives.Label;
import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot;
import com.google.idea.blaze.base.projectview.ProjectViewManager;
import com.google.idea.blaze.base.projectview.ProjectViewSet;
import com.google.idea.blaze.base.projectview.section.sections.AutomaticallyDeriveTargetsSection;
import com.google.idea.blaze.base.projectview.section.sections.DirectoryEntry;
import com.google.idea.blaze.base.projectview.section.sections.DirectorySection;
import com.google.idea.blaze.base.projectview.section.sections.TargetSection;
import com.google.idea.blaze.base.projectview.section.sections.ViewProjectRootSection;
import com.google.idea.blaze.base.settings.Blaze;
import com.google.idea.blaze.base.settings.BuildSystemName;
import com.google.idea.blaze.base.sync.data.BlazeDataStorage;
import com.google.idea.blaze.base.util.WorkspacePathUtil;
import com.google.idea.common.experiments.BoolExperiment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import javax.annotation.Nullable;

/** The roots to import. Derived from project view. */
public final class ImportRoots {

  private final BoolExperiment treatProjectTargetsAsSource =
      new BoolExperiment("blaze.treat.project.targets.as.source", true);

  /** Returns the ImportRoots for the project, or null if it's not a blaze project. */
  @Nullable
  public static ImportRoots forProjectSafe(Project project) {
    WorkspaceRoot root = WorkspaceRoot.fromProjectSafe(project);
    if (root == null) {
      return null;
    }

    ProjectViewManager manager = ProjectViewManager.getInstance(project);
    if (manager == null) {
      return null;
    }

    ProjectViewSet projectViewSet = manager.getProjectViewSet();
    if (projectViewSet == null) {
      return null;
    }

    return ImportRoots.builder(root, Blaze.getBuildSystemName(project)).add(projectViewSet).build();
  }

  /** Builder for import roots */
  public static class Builder {
    private final ImmutableCollection.Builder<WorkspacePath> rootDirectoriesBuilder =
        ImmutableList.builder();
    private final ImmutableSet.Builder<WorkspacePath> excludeDirectoriesBuilder =
        ImmutableSet.builder();
    private final ImmutableSet.Builder<WorkspacePath> bazelIgnorePathsBuilder =
        ImmutableSet.builder();
    private final ImmutableList.Builder<TargetExpression> projectTargets = ImmutableList.builder();
    private boolean deriveTargetsFromDirectories = false;

    private final WorkspaceRoot workspaceRoot;
    private final BuildSystemName buildSystemName;
    private Boolean viewProjectRoot = false;

    private Builder(WorkspaceRoot workspaceRoot, BuildSystemName buildSystemName) {
      this.workspaceRoot = workspaceRoot;
      this.buildSystemName = buildSystemName;
    }

    @CanIgnoreReturnValue
    public Builder add(ProjectViewSet projectViewSet) {
      for (DirectoryEntry entry : projectViewSet.listItems(DirectorySection.KEY)) {
        add(entry);
      }
      projectTargets.addAll(projectViewSet.listItems(TargetSection.KEY));
      deriveTargetsFromDirectories =
          projectViewSet.getScalarValue(AutomaticallyDeriveTargetsSection.KEY).orElse(false);
      viewProjectRoot = projectViewSet.getScalarValue(ViewProjectRootSection.KEY).orElse(false);
      return this;
    }

    @CanIgnoreReturnValue
    @VisibleForTesting
    public Builder add(DirectoryEntry entry) {
      if (entry.included) {
        include(entry.directory);
      } else {
        exclude(entry.directory);
      }
      return this;
    }

    @CanIgnoreReturnValue
    public Builder include(WorkspacePath directory) {
      rootDirectoriesBuilder.add(directory);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder exclude(WorkspacePath entry) {
      excludeDirectoriesBuilder.add(entry);
      return this;
    }

    /**
     * For Bazel projects if the root directories include the workspace root, exclude bazel-bin,
     * bazel-out, ... directories to avoid scanning them for Build files by {@link
     * com.google.idea.blaze.qsync.project.ProjectDefinition#deriveQuerySpec}
     */
    private ImmutableSet<WorkspacePath> getBuildSystemExcludes(
        ImmutableCollection<WorkspacePath> rootDirectories) {
      if (buildSystemName == BuildSystemName.Bazel && hasWorkspaceRoot(rootDirectories)) {
        ImmutableSet<WorkspacePath> buildArtifactDirectories =
            BuildSystemProvider.getBuildSystemProvider(buildSystemName)
                .buildArtifactDirectories(workspaceRoot)
                .stream()
                .map(WorkspacePath::new)
                .collect(toImmutableSet());
        WorkspacePath projectDataSubdirectory =
            new WorkspacePath(BlazeDataStorage.PROJECT_DATA_SUBDIRECTORY);
        return ImmutableSet.<WorkspacePath>builder()
            .addAll(buildArtifactDirectories)
            .add(projectDataSubdirectory)
            .build();
      }
      return ImmutableSet.of();
    }

    public ImportRoots build() {
      if (viewProjectRoot) {
        rootDirectoriesBuilder.add(workspaceRoot.workspacePathFor(workspaceRoot.directory()));
      }

      ImmutableCollection<WorkspacePath> rootDirectories = rootDirectoriesBuilder.build();
      if (buildSystemName == BuildSystemName.Bazel) {
        if (hasWorkspaceRoot(rootDirectories)) {
          excludeBuildSystemArtifacts();
          excludeProjectDataSubDirectory();
        }
        excludeBazelIgnoredPaths();
      }

      if (viewProjectRoot) {
          selectExcludes(rootDirectoriesBuilder.build()).forEach(excludeDirectoriesBuilder::add);
      }

      ImmutableSet<WorkspacePath> minimalExcludes =
          WorkspacePathUtil.calculateMinimalWorkspacePaths(excludeDirectoriesBuilder.build());

      // Remove any duplicates, overlapping, or excluded directories
      ImmutableSet<WorkspacePath> minimalRootDirectories =
          WorkspacePathUtil.calculateMinimalWorkspacePaths(rootDirectories, minimalExcludes);

      //Paths in .bazelignore are already excluded by Bazel, excluding them explicitly in "bazel query" produces a warning
      ImmutableSet<WorkspacePath> excludePathsForBazelQuery = Sets.difference(minimalExcludes, bazelIgnorePathsBuilder.build()).immutableCopy();

      ProjectDirectoriesHelper directories =
          new ProjectDirectoriesHelper(minimalRootDirectories, minimalExcludes, excludePathsForBazelQuery);

      TargetExpressionList targets =
          deriveTargetsFromDirectories
              ? TargetExpressionList.createWithTargetsDerivedFromDirectories(
                  projectTargets.build(), directories)
              : TargetExpressionList.create(projectTargets.build());

      ImmutableSet<WorkspacePath> systemExcludes = getBuildSystemExcludes(rootDirectories);
      return new ImportRoots(directories, targets, systemExcludes);
    }

    private @NotNull List<WorkspacePath> selectExcludes(ImmutableCollection<WorkspacePath> rootDirectories) {
      var userDeclaredRootDirectories = rootDirectories.stream().filter(rootDirectory -> !rootDirectory.isWorkspaceRoot()).collect(toImmutableSet());
      Queue<File> files = new LinkedList<>(Collections.singletonList(workspaceRoot.directory()));
      var result = new ArrayList<File>();
      while (!files.isEmpty()) {
        File file = files.poll();
        if (rootDirectories.stream().anyMatch(d -> FileUtil.isAncestor(file, workspaceRoot.fileForPath(d), /*strict=*/ true)) &&
                userDeclaredRootDirectories.stream().noneMatch(d -> FileUtil.filesEqual(file, workspaceRoot.fileForPath(d)))
        ) {
          var children = file.listFiles(File::isDirectory);
          if (children != null) {
            files.addAll(List.of(children));
          }
        } else if (rootDirectories.stream().noneMatch(d -> FileUtil.filesEqual(file, workspaceRoot.fileForPath(d)))) {
          result.add(file);
        }
      }
      return result.stream().map(workspaceRoot::workspacePathFor).toList();
    }

    private void excludeBuildSystemArtifacts() {
      for (String dir :
          BuildSystemProvider.getBuildSystemProvider(buildSystemName)
              .buildArtifactDirectories(workspaceRoot)) {
        exclude(new WorkspacePath(dir));
      }
    }

    private void excludeProjectDataSubDirectory() {
      exclude(new WorkspacePath(BlazeDataStorage.PROJECT_DATA_SUBDIRECTORY));
    }

    private void excludeBazelIgnoredPaths() {
      ImmutableList<WorkspacePath> bazelIgnoredPaths = new BazelIgnoreParser(workspaceRoot).getIgnoredPaths();
      excludeDirectoriesBuilder.addAll(bazelIgnoredPaths);
      bazelIgnorePathsBuilder.addAll(bazelIgnoredPaths);
    }

    private static boolean hasWorkspaceRoot(ImmutableCollection<WorkspacePath> rootDirectories) {
      return rootDirectories.stream().anyMatch(WorkspacePath::isWorkspaceRoot);
    }
  }

  private final ProjectDirectoriesHelper projectDirectories;
  private final TargetExpressionList projectTargets;
  private final ImmutableSet<WorkspacePath> buildSystemExcludes;

  public static Builder builder(Project project) {
    return new Builder(WorkspaceRoot.fromProject(project), Blaze.getBuildSystemName(project));
  }

  public static Builder builder(WorkspaceRoot workspaceRoot, BuildSystemName buildSystemName) {
    return new Builder(workspaceRoot, buildSystemName);
  }

  private ImportRoots(
      ProjectDirectoriesHelper projectDirectories,
      TargetExpressionList projectTargets,
      ImmutableSet<WorkspacePath> buildSystemExcludes) {
    this.projectDirectories = projectDirectories;
    this.projectTargets = projectTargets;
    this.buildSystemExcludes = buildSystemExcludes;
  }

  public Collection<WorkspacePath> rootDirectories() {
    return projectDirectories.rootDirectories;
  }

  /** Returns the import roots, as paths relative to the workspace root. */
  public ImmutableSet<Path> rootPaths() {
    return projectDirectories.rootDirectories.stream()
        .map(WorkspacePath::asPath)
        .collect(toImmutableSet());
  }

  /** Returns the system excluded directories. */
  public ImmutableSet<WorkspacePath> systemExcludes() {
    return ImmutableSet.copyOf(buildSystemExcludes);
  }

  public Set<WorkspacePath> excludeDirectories() {
    return projectDirectories.excludeDirectories;
  }

  public Set<WorkspacePath> excludePathsForBazelQuery() {
    return projectDirectories.excludePathsForBazelQuery;
  }

  public ImmutableSet<Path> excludePaths() {
    return projectDirectories.excludeDirectories.stream()
        .map(WorkspacePath::asPath)
        .collect(toImmutableSet());
  }

  /** Returns true if this rule should be imported as source. */
  public boolean importAsSource(Label label) {
    if (label.isExternal()) {
      return false;
    }
    return projectDirectories.containsWorkspacePath(label.blazePackage())
        || (treatProjectTargetsAsSource.getValue() && targetInProject(label));
  }

  /**
   * Returns true if this target is covered by the project view. Assumes wildcard target patterns
   * (explicit or derived from directories) cover all targets in the relevant packages.
   */
  public boolean targetInProject(Label label) {
    return projectTargets.includesTarget(label);
  }

  public boolean packageInProjectTargets(WorkspacePath packagePath) {
    return projectTargets.includesPackage(packagePath);
  }

  public boolean containsWorkspacePath(WorkspacePath workspacePath) {
    return projectDirectories.containsWorkspacePath(workspacePath);
  }

  static class ProjectDirectoriesHelper {
    private final ImmutableSet<WorkspacePath> rootDirectories;
    private final ImmutableSet<WorkspacePath> excludeDirectories;
    private final ImmutableSet<WorkspacePath> excludePathsForBazelQuery;

    @VisibleForTesting
    ProjectDirectoriesHelper(
        Collection<WorkspacePath> rootDirectories,
        Collection<WorkspacePath> excludeDirectories,
        Collection<WorkspacePath> excludePathsForBazelQuery) {
      this.rootDirectories = ImmutableSet.copyOf(rootDirectories);
      this.excludeDirectories = ImmutableSet.copyOf(excludeDirectories);
      this.excludePathsForBazelQuery = ImmutableSet.copyOf(excludePathsForBazelQuery);
    }

    boolean containsWorkspacePath(WorkspacePath workspacePath) {
      boolean included = false;
      boolean excluded = false;
      for (WorkspacePath rootDirectory : rootDirectories) {
        included = included || isSubdirectory(rootDirectory, workspacePath);
      }
      for (WorkspacePath excludeDirectory : excludeDirectories) {
        excluded = excluded || isSubdirectory(excludeDirectory, workspacePath);
      }
      return included && !excluded;
    }

    private static boolean isSubdirectory(WorkspacePath ancestor, WorkspacePath descendant) {
      if (ancestor.isWorkspaceRoot()) {
        return true;
      }
      Path ancestorPath = FileSystems.getDefault().getPath(ancestor.relativePath());
      Path descendantPath = FileSystems.getDefault().getPath(descendant.relativePath());
      return descendantPath.startsWith(ancestorPath);
    }
  }
}
