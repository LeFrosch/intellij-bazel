package com.google.idea.blaze.clwb;

import com.google.idea.blaze.base.lang.buildfile.language.BuildFileType;
import com.google.idea.blaze.base.settings.Blaze;
import com.google.idea.blaze.base.wizard2.BazelImportCurrentProjectAction;
import com.google.idea.blaze.base.wizard2.BazelNotificationProvider;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationProvider;
import com.jetbrains.cidr.lang.OCLanguageUtilsBase;
import com.jetbrains.cidr.project.ui.popup.ProjectFixesProvider;
import java.util.List;
import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CLionNotificationProvider implements ProjectFixesProvider {
  private static void unregisterGenericProvider(Project project) {
    final var extensionPoint = EditorNotificationProvider.EP_NAME.getPoint(project);

    for (final var extension : extensionPoint.getExtensions()) {
      if (extension instanceof BazelNotificationProvider) {
        extensionPoint.unregisterExtension(extension);
      }
    }
  }

  public static void register(Project project) {
    unregisterGenericProvider(project);

    final var extensionPoint = ProjectFixesProvider.Companion.getEP_NAME().getPoint();
    extensionPoint.registerExtension(new CLionNotificationProvider());
  }

  @NotNull
  @Override
  public List<AnAction> collectFixes(
      @NotNull Project project,
      @Nullable VirtualFile file,
      @NotNull DataContext dataContext) {
    if (file == null) {
      return List.of();
    }
    if (Blaze.isBlazeProject(project)) {
      return List.of();
    }
    if (!OCLanguageUtilsBase.isSupported(file) && file.getFileType() != BuildFileType.INSTANCE) {
      return List.of();
    }
    if (!BazelImportCurrentProjectAction.projectCouldBeImported(project)) {
      return List.of();
    }

    String root = project.getBasePath();
    if (root == null) {
      return List.of();
    }

    return List.of(new BazelImportCurrentProjectAction(new File(root)));
  }
}
