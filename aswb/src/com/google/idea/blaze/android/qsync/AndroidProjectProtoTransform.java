/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.android.qsync;

import com.google.common.collect.ImmutableList;
import com.google.idea.blaze.android.manifest.ManifestParser;
import com.google.idea.blaze.base.qsync.ProjectProtoTransformProvider;
import com.google.idea.blaze.common.Context;
import com.google.idea.blaze.exception.BuildException;
import com.google.idea.blaze.qsync.ProjectProtoTransform;
import com.google.idea.blaze.qsync.deps.ArtifactTracker;
import com.google.idea.blaze.qsync.deps.ProjectProtoUpdate;
import com.google.idea.blaze.qsync.deps.ProjectProtoUpdateOperation;
import com.google.idea.blaze.qsync.java.AarPackageNameExtractor;
import com.google.idea.blaze.qsync.java.AddAndroidResPackages;
import com.google.idea.blaze.qsync.java.AddDependencyAars;
import com.google.idea.blaze.qsync.project.BuildGraphData;
import com.google.idea.blaze.qsync.project.ProjectDefinition;
import com.google.idea.blaze.qsync.project.ProjectProto.Project;
import java.util.List;

/** A {@link ProjectProtoTransform} that adds android specific information to the project proto. */
public class AndroidProjectProtoTransform implements ProjectProtoTransform {

  /**
   * Provides a {@link ProjectProtoTransform} that adds android specific information to the project
   * proto.
   */
  public static class Provider implements ProjectProtoTransformProvider {

    @Override
    public List<ProjectProtoTransform> createTransforms(ProjectDefinition projectDef) {
      return ImmutableList.of(new AndroidProjectProtoTransform(projectDef));
    }
  }

  private final ImmutableList<ProjectProtoUpdateOperation> updateOperations;

  private AndroidProjectProtoTransform(ProjectDefinition projectDefinition) {
    updateOperations =
        ImmutableList.of(
            new AddDependencyAars(
                projectDefinition,
                new AarPackageNameExtractor(
                    in -> ManifestParser.parseManifestFromInputStream(in).packageName)),
            new AddAndroidResPackages());
  }

  @Override
  public Project apply(
      Project proto, BuildGraphData graph, ArtifactTracker.State artifactState, Context<?> context)
      throws BuildException {
    ProjectProtoUpdate update = new ProjectProtoUpdate(proto, graph, context);
    for (var op : updateOperations) {
      op.update(update, artifactState, context);
    }
    return update.build();
  }
}
