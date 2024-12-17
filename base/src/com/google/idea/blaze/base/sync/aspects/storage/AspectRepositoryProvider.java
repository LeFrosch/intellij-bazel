package com.google.idea.blaze.base.sync.aspects.storage;

import com.intellij.openapi.extensions.ExtensionPointName;

import java.io.File;
import java.util.Optional;

public interface AspectRepositoryProvider {

  ExtensionPointName<AspectRepositoryProvider> EP_NAME =
      ExtensionPointName.create("com.google.idea.blaze.AspectRepositoryProvider");

  Optional<File> aspectDirectory();

  static Optional<File> findAspectDirectory() {
    return EP_NAME.getExtensionsIfPointIsRegistered().stream()
        .map(AspectRepositoryProvider::aspectDirectory)
        .filter(Optional::isPresent)
        .findFirst()
        .orElse(Optional.empty());
  }
}
