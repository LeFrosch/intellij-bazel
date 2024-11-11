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
package com.google.idea.blaze.qsync.artifacts;

import com.google.idea.blaze.qsync.project.ProjectProto.ProjectArtifact.ArtifactTransform;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simple registry for artifact transforms that also supplies implementations for copy and unzip.
 */
public class ArtifactTransformRegistry {

  private final Map<ArtifactTransform, FileTransform> transforms = new HashMap<>();

  public void add(ArtifactTransform key, FileTransform transform) {
    this.transforms.put(key, transform);
  }

  public void addAll(Map<ArtifactTransform, FileTransform> transforms) {
    this.transforms.putAll(transforms);
  }

  public Optional<FileTransform> get(ArtifactTransform key) {
    return switch (key) {
      case NONE, UNRECOGNIZED -> Optional.empty();
      case COPY -> Optional.of(FileTransform.COPY);
      case UNZIP -> Optional.of(FileTransform.UNZIP);
      default -> Optional.of(transforms.get(key));
    };
  }
}
