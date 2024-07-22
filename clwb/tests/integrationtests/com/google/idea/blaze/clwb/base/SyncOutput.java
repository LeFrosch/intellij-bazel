package com.google.idea.blaze.clwb.base;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.base.scope.OutputSink;
import com.google.idea.blaze.base.scope.output.IssueOutput;
import com.google.idea.blaze.common.Output;
import com.google.idea.blaze.common.PrintOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class SyncOutput {
  private static class BufferSink<T extends Output> implements OutputSink<T> {
    private final List<T> buffer = new ArrayList<>();

    @Override
    public Propagation onOutput(@NotNull T output) {
      buffer.add(output);
      return Propagation.Continue;
    }
  }

  private final BufferSink<IssueOutput> issues = new BufferSink<>();
  private final BufferSink<PrintOutput> prints = new BufferSink<>();

  void install(BlazeContext context) {
    context.addOutputSink(IssueOutput.class, issues);
    context.addOutputSink(PrintOutput.class, prints);
  }

  public String collectLog() {
    return prints.buffer.stream()
        .map(PrintOutput::getText)
        .collect(Collectors.joining("\n"));
  }

  public void assertNoErrors() {
    final var log = collectLog();

    assertWithMessage("There where errors during the sync, check this log:\n" + log)
        .that(issues.buffer)
        .isEmpty();
  }
}
