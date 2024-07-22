package com.google.idea.blaze.clwb.base;

import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import com.jetbrains.cidr.lang.workspace.headerRoots.HeadersSearchRoot;
import com.jetbrains.cidr.lang.workspace.headerRoots.HeadersSearchRootProcessor;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;

public class ClwbIntegrationTestAsserts {

  public static void assertContainsHeader(SoftAssertions softly, String fileName, List<HeadersSearchRoot> roots) {
    assertContainsHeader(softly, fileName, true, roots);
  }

  public static void assertDoesntContainHeader(SoftAssertions softly, String fileName, List<HeadersSearchRoot> roots) {
    assertContainsHeader(softly, fileName, false, roots);
  }

  private static void assertContainsHeader(SoftAssertions softly, String fileName, boolean shouldContain,
      List<HeadersSearchRoot> roots) {
    final var found = new Ref<VirtualFile>();
    final var foundIn = new Ref<PsiFileSystemItem>();

    for (final var root : roots) {
      root.processChildren(new HeadersSearchRootProcessor() {
        @Override
        public boolean process(@NotNull VirtualFile file) {
          if (file.isDirectory() || !FileUtil.namesEqual(file.getName(), fileName)) {
            return true;
          }

          found.set(file);
          foundIn.set(root);
          return false;
        }
      });

      if (!found.isNull()) {
        break;
      }
    }

    if (shouldContain) {
      softly.assertThat(found.isNull())
          .overridingErrorMessage(String.format("%s not found in:\n%s", fileName, StringUtil.join(roots, "\n")))
          .isFalse();
    } else {
      softly.assertThat(found.isNull())
          .overridingErrorMessage(String.format("%s found in: %s", fileName, foundIn.get()))
          .isTrue();
    }
  }
}
