package com.google.idea.blaze.base.qsync.artifacts;

import com.google.idea.blaze.qsync.artifacts.FileTransform;
import com.google.idea.blaze.qsync.project.ProjectProto.ProjectArtifact.ArtifactTransform;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import java.util.Optional;

/**
 * Provides a {@link FileTransform} that may require language-specific dependencies so they cannot be created directly
 * by the base code. There should only be one implementation for every element in {@link ArtifactTransform}.
 */
public interface ArtifactTransformProvider {

  ExtensionPointName<ArtifactTransformProvider> EP_NAME =
      new ExtensionPointName<>("com.google.idea.blaze.base.qsync.ArtifactTransformProvider");

  static Optional<FileTransform> get(Project project, ArtifactTransform transform) {
    return EP_NAME.getExtensionList().stream()
        .flatMap(it -> it.createTransform(project, transform).stream())
        .findFirst();
  }

  Optional<FileTransform> createTransform(Project project, ArtifactTransform transform);
}
