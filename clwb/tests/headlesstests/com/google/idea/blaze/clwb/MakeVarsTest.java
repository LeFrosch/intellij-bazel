package com.google.idea.blaze.clwb;

import static com.google.common.truth.Truth.assertThat;
import static com.google.idea.blaze.clwb.base.Assertions.assertContainsCompilerFlag;
import static com.google.idea.blaze.clwb.base.Assertions.assertContainsHeader;
import static com.google.idea.blaze.clwb.base.Assertions.assertContainsPattern;
import static com.google.idea.blaze.clwb.base.Assertions.assertDefine;
import static com.google.idea.blaze.clwb.base.Utils.parseEchoOutput;
import static com.google.idea.testing.headless.Assertions.abort;

import com.google.common.collect.ImmutableList;
import com.google.idea.blaze.base.async.process.ExternalTask;
import com.google.idea.blaze.base.lang.buildfile.psi.LoadStatement;
import com.google.idea.blaze.base.sync.autosync.ProjectTargetManager.SyncStatus;
import com.google.idea.blaze.clwb.base.ClwbHeadlessTestCase;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.cidr.lang.workspace.compiler.ClangCompilerKind;
import com.jetbrains.cidr.lang.workspace.compiler.GCCCompilerKind;
import com.jetbrains.cidr.lang.workspace.compiler.MSVCCompilerKind;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MakeVarsTest extends ClwbHeadlessTestCase {

  @Test
  public void testClwb() throws Exception {
    final var errors = runSync(defaultSyncParams().build());
    errors.assertNoErrors();

    checkCopts();
    checkMakeVars();
    checkDataDeps();
  }

  private void checkCopts() {
    final var compilerSettings = findFileCompilerSettings("main/main0.cc");
    assertContainsCompilerFlag("-Wall", compilerSettings);
  }

  private List<String> runEcho(String target) throws Exception {
    return parseEchoOutput(runBazelCommand("run", target));
  }

  private void checkMakeVars() throws Exception {
    final var compilerSettings = findFileCompilerSettings("main/main1.cc");

    final var expanded = runEcho("//main:main1");
    assertThat(expanded).hasSize(4);

    assertDefine("EXECPATH", compilerSettings).contains(expanded.get(0));
    assertDefine("ROOTPATH", compilerSettings).contains(expanded.get(1));
    assertDefine("RLOCATIONPATH", compilerSettings).contains(expanded.get(2));
    assertDefine("LOCATION", compilerSettings).contains(expanded.get(3));
  }

  private void checkDataDeps() throws Exception {
    // only check that target can be imported while https://github.com/bazelbuild/bazel/issues/27047 is not fixed
    findFileCompilerSettings("main/main2.cc");

    final var expanded = runEcho("//main:main2");
    assertThat(expanded).hasSize(4);
  }
}
