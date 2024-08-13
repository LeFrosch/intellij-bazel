package com.google.idea.blaze.base.async.process2;

import com.google.idea.blaze.base.command.BlazeCommand;
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ui.EDT;
import java.util.ArrayList;

public final class OSProcessUtil {
  private static final Logger LOG = Logger.getInstance(OSProcessUtil.class);

  private static void assertNonBlocking() {
    LOG.assertTrue(!EDT.isCurrentThreadEdt(), "runs on background thread");
    LOG.assertTrue(!ApplicationManager.getApplication().isReadAccessAllowed(), "runs without read lock");
  }

  public static int execute(
      BlazeContext context,
      BlazeCommand command,
      WorkspaceRoot root) throws ExecutionException {
    assertNonBlocking();

    final var arguments = new ArrayList<String>();
    arguments.add("bazel");
    arguments.addAll(command.toArgumentList());

    final var commandLine = new PtyCommandLine(arguments)
        .withWorkDirectory(root.path().toFile());

    final var handler = new OSProcessHandler(commandLine);
    context.output(new OSProcessOutput(handler));
    handler.startNotify();
    handler.waitFor();

    return handler.getExitCode();
  }
}
