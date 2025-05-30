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
package com.google.idea.blaze.base.sync.projectstructure;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.idea.blaze.base.bazel.BuildSystemProvider;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot;
import com.google.idea.blaze.base.projectview.ProjectViewSet;
import com.google.idea.blaze.base.projectview.section.sections.UseExclusionPatternsSection;
import com.google.idea.blaze.base.settings.Blaze;
import com.google.idea.blaze.base.sync.SourceFolderProvider;
import com.google.idea.blaze.base.sync.data.BlazeDataStorage;
import com.google.idea.blaze.base.sync.projectview.ImportRoots;
import com.google.idea.blaze.base.sync.projectview.SourceTestConfig;
import com.google.idea.blaze.base.sync.projectview.WorkspaceFileFinder;
import com.google.idea.blaze.base.util.UrlUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.SourceFolder;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;

/** Modifies content entries based on project data. */
public class ContentEntryEditor {

  public static void createContentEntries(
      Project project,
      WorkspaceRoot workspaceRoot,
      ProjectViewSet projectViewSet,
      BlazeProjectData blazeProjectData,
      DirectoryStructure rootDirectoryStructure,
      ModifiableRootModel modifiableRootModel) {
    ImportRoots importRoots =
        ImportRoots.builder(workspaceRoot, Blaze.getBuildSystemName(project))
            .add(projectViewSet)
            .build();
    Collection<WorkspacePath> rootDirectories = importRoots.rootDirectories();
    Collection<WorkspacePath> excludeDirectories = importRoots.excludeDirectories();
    Multimap<WorkspacePath, WorkspacePath> excludesByRootDirectory =
        sortExcludesByRootDirectory(rootDirectories, excludeDirectories);

    SourceTestConfig testConfig = new SourceTestConfig(projectViewSet);
    SourceFolderProvider provider = SourceFolderProvider.getSourceFolderProvider(blazeProjectData);
    WorkspaceFileFinder finder = WorkspaceFileFinder.Provider.getInstance(project).getWorkspaceFileFinder();

    for (WorkspacePath rootDirectory : rootDirectories) {
      File rootFile = workspaceRoot.fileForPath(rootDirectory);
      ContentEntry contentEntry =
          modifiableRootModel.addContentEntry(UrlUtil.pathToUrl(rootFile.getPath()));
      contentEntry.clearExcludeFolders();

      for (WorkspacePath exclude : excludesByRootDirectory.get(rootDirectory)) {
        File excludeFolder = workspaceRoot.fileForPath(exclude);
        contentEntry.addExcludeFolder(UrlUtil.fileToIdeaUrl(excludeFolder));
      }

      if (projectViewSet.getScalarValue(UseExclusionPatternsSection.KEY).orElse(true)) {
        var exclusionPatternsDirectories = importRoots.systemExcludes();

        for (WorkspacePath workspacePath : exclusionPatternsDirectories) {
          if (!workspacePath.asPath().isAbsolute() && workspacePath.asPath().getNameCount() == 1) {
            contentEntry.addExcludePattern(workspacePath.relativePath());
          }
        }
      }

      File directory = new File(workspaceRoot.toString());
      File[] files = directory
              .listFiles((dir, name) ->
                      finder != null && finder.isInProject(dir) && BlazeDataStorage.ALL_PROJECT_SUBDIRECTORIES.containsValue(name));
      if (files != null) {
        for (File file : files) {
          contentEntry.addExcludeFolder(UrlUtil.fileToIdeaUrl(file));
        }
      }

      ImmutableMap<File, SourceFolder> sourceFolders =
          provider.initializeSourceFolders(contentEntry);
      SourceFolder rootSource = sourceFolders.get(rootFile);
      walkFileSystem(
          workspaceRoot,
          testConfig,
          excludesByRootDirectory.get(rootDirectory),
          contentEntry,
          provider,
          sourceFolders,
          rootSource,
          rootDirectory,
          rootDirectoryStructure.directories.get(rootDirectory));
    }
  }

  private static void walkFileSystem(
      WorkspaceRoot workspaceRoot,
      SourceTestConfig testConfig,
      Collection<WorkspacePath> excludedDirectories,
      ContentEntry contentEntry,
      SourceFolderProvider provider,
      ImmutableMap<File, SourceFolder> sourceFolders,
      @Nullable SourceFolder parent,
      WorkspacePath workspacePath,
      DirectoryStructure directoryStructure) {
    if (excludedDirectories.contains(workspacePath)) {
      return;
    }
    File file = workspaceRoot.fileForPath(workspacePath);
    boolean isTest = testConfig.isTestSource(workspacePath.relativePath());
    SourceFolder current = sourceFolders.get(new File(file.getPath()));
    SourceFolder next;
    if (current == null) {
      if (parent != null && isTest != parent.isTestSource()) {
        next = provider.setSourceFolderForLocation(contentEntry, parent, file, isTest);
      } else {
        next = parent;
      }
    } else {
      if (isTest == current.isTestSource()) {
        next = current;
      } else {
        next = provider.setSourceFolderForLocation(contentEntry, current, file, isTest);
        contentEntry.removeSourceFolder(current);
      }
    }
    for (Map.Entry<WorkspacePath, DirectoryStructure> child :
        directoryStructure.directories.entrySet()) {
      walkFileSystem(
          workspaceRoot,
          testConfig,
          excludedDirectories,
          contentEntry,
          provider,
          sourceFolders,
          next,
          child.getKey(),
          child.getValue());
    }
  }

  private static Multimap<WorkspacePath, WorkspacePath> sortExcludesByRootDirectory(
      Collection<WorkspacePath> rootDirectories, Collection<WorkspacePath> excludedDirectories) {

    Multimap<WorkspacePath, WorkspacePath> result = ArrayListMultimap.create();
    for (WorkspacePath exclude : excludedDirectories) {
      rootDirectories.stream()
          .filter(rootDirectory -> isUnderRootDirectory(rootDirectory, exclude.relativePath()))
          .findFirst()
          .ifPresent(foundWorkspacePath -> result.put(foundWorkspacePath, exclude));
    }
    return result;
  }

  private static boolean isUnderRootDirectory(WorkspacePath rootDirectory, String relativePath) {
    if (rootDirectory.isWorkspaceRoot()) {
      return true;
    }
    String rootDirectoryString = rootDirectory.toString();
    return relativePath.startsWith(rootDirectoryString)
        && (relativePath.length() == rootDirectoryString.length()
            || (relativePath.charAt(rootDirectoryString.length()) == '/'));
  }
}
