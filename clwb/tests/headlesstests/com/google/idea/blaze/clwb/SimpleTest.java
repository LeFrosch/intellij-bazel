package com.google.idea.blaze.clwb;

import static com.google.common.truth.Truth.assertThat;
import static com.google.idea.blaze.clwb.base.Assertions.assertContainsCompilerFlag;
import static com.google.idea.blaze.clwb.base.Assertions.assertContainsHeader;
import static com.google.idea.blaze.clwb.base.Assertions.assertContainsPattern;

import com.google.idea.blaze.base.lang.buildfile.psi.BuildFile;
import com.google.idea.blaze.base.lang.buildfile.psi.LoadStatement;
import com.google.idea.blaze.base.run.producers.BinaryContextRunConfigurationProducer;
import com.google.idea.blaze.base.run.producers.BlazeBuildFileRunConfigurationProducer;
import com.google.idea.blaze.clwb.base.ClwbHeadlessTestCase;
import com.google.idea.blaze.clwb.run.BlazeCppRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.runners.ProgramRunner.Callback;
import com.intellij.execution.ui.ExecutionUiService;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.jetbrains.cidr.lang.workspace.compiler.ClangCompilerKind;
import com.jetbrains.cidr.lang.workspace.compiler.GCCCompilerKind;
import com.jetbrains.cidr.lang.workspace.compiler.MSVCCompilerKind;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SimpleTest extends ClwbHeadlessTestCase {

  @Test
  public void testClwb() throws Exception {
    final var errors = runSync(defaultSyncParams().build());
    errors.assertNoErrors();

    checkCompiler();
    checkTest();
    checkXcode();
    checkResolveRulesCC();
    checkDebugger();
  }

  private void checkCompiler() {
    final var compilerSettings = findFileCompilerSettings("main/hello-world.cc");

    if (SystemInfo.isMac) {
      assertThat(compilerSettings.getCompilerKind()).isEqualTo(ClangCompilerKind.INSTANCE);
    } else if (SystemInfo.isLinux) {
      assertThat(compilerSettings.getCompilerKind()).isEqualTo(GCCCompilerKind.INSTANCE);
    } else if (SystemInfo.isWindows) {
      assertThat(compilerSettings.getCompilerKind()).isEqualTo(MSVCCompilerKind.INSTANCE);
    }

    assertContainsHeader("iostream", compilerSettings);
    assertContainsCompilerFlag("-Wall", compilerSettings);
  }

  private void checkTest() {
    final var compilerSettings = findFileCompilerSettings("main/test.cc");

    assertContainsHeader("iostream", compilerSettings);
    assertContainsHeader("catch2/catch_test_macros.hpp", compilerSettings);
  }

  private void checkXcode() throws IOException {
    if (!SystemInfo.isMac) {
      return;
    }

    final var compilerSettings = findFileCompilerSettings("main/test.cc");

    final var compilerExecutable = compilerSettings.getCompilerExecutable();
    assertThat(compilerExecutable).isNotNull();

    final var scriptLines = Files.readAllLines(compilerSettings.getCompilerExecutable().toPath());

    assertContainsPattern("export DEVELOPER_DIR=/.*/Xcode.*.app/Contents/Developer", scriptLines);
    assertContainsPattern("export SDKROOT=/.*/Xcode.*.app/Contents/Developer/.*", scriptLines);
  }

  private void checkResolveRulesCC() {
    final var file = findProjectPsiFile("main/BUILD");

    final var load = PsiTreeUtil.findChildOfType(file, LoadStatement.class);
    assertThat(load).isNotNull();
    assertThat(load.getImportedPath()).isEqualTo("@rules_cc//cc:defs.bzl");

    for (final var symbol : load.getLoadedSymbols()) {
      final var reference = symbol.getReference();
      assertThat(reference).isNotNull();
      assertThat(reference.resolve()).isNotNull();
    }
  }

  private void checkDebugger() throws Exception {
    final var buildFile = (BuildFile) findProjectPsiFile("main/BUILD");

    final var element = buildFile.findRule("hello-world");

    final var context = ConfigurationContext.getFromContext(SimpleDataContext.builder()
        .add(CommonDataKeys.PROJECT, getProject())
        .add(PlatformCoreDataKeys.MODULE, ModuleUtilCore.findModuleForPsiElement(element))
        .add(Location.DATA_KEY, PsiLocation.fromPsiElement(element))
        .build(), ActionPlaces.UNKNOWN);

    final var executor = ExecutorRegistry.getInstance().getExecutorById(DefaultDebugExecutor.EXECUTOR_ID);
    final var producer = RunConfigurationProducer.getInstance(BlazeBuildFileRunConfigurationProducer.class);
    final var configuration = producer.createConfigurationFromContext(context);
    final var environment = ExecutionUtil.createEnvironment(executor, configuration.getConfigurationSettings()).build();

    final var future = new CompletableFuture<Integer>();
    environment.setCallback(new Callback() {
      @Override
      public void processStarted(RunContentDescriptor runContentDescriptor) {
        runContentDescriptor.getProcessHandler().addProcessListener(new ProcessListener() {
          @Override
          public void startNotified(@NotNull ProcessEvent event) {
            ProcessListener.super.startNotified(event);
          }

          @Override
          public void processNotStarted() {
            future.completeExceptionally(new ExecutionException("Process not started"));
          }

          @Override
          public void processTerminated(@NotNull ProcessEvent event) {
            future.complete(event.getExitCode());
          }
        });
      }

      @Override
      public void processNotStarted(@Nullable Throwable error) {
        future.completeExceptionally(error);
      }
    });

    ((ExecutionManagerImpl) ExecutionManager.getInstance(getProject())).setForceCompilationInTests(true);
    new BlazeCppRunner().execute(environment);

    while (!future.isDone()) {
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    assertThat(future.getNow(404)).isEqualTo(0);
  }
}
