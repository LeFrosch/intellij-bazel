package com.google.idea.testing.headless;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.base.scope.OutputSink.Propagation;
import com.google.idea.blaze.base.scope.output.IssueOutput;
import com.google.idea.blaze.base.scope.output.StatusOutput;
import com.google.idea.blaze.base.scope.output.SummaryOutput;
import com.google.idea.blaze.common.Output;
import com.google.idea.blaze.common.PrintOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public class SyncOutput {
  private final List<IssueOutput> issues = new ArrayList<>();
  private final List<String> messages = new ArrayList<>();

  @Nullable
  private BlazeContext context;

  void install(BlazeContext context) {
    this.context = context;

    addOutputSink(context, IssueOutput.class, issues::add);
    addOutputSink(context, PrintOutput.class, (it) -> messages.add(it.getText()));
    addOutputSink(context, StatusOutput.class, (it) -> messages.add(it.getStatus()));
    addOutputSink(context, SummaryOutput.class, (it) -> messages.add(it.getText()));
  }

  private <T extends Output> void addOutputSink(BlazeContext context, Class<T> clazz, Consumer<T> consumer) {
    context.addOutputSink(clazz, (it) -> {
      consumer.accept(it);
      return Propagation.Continue;
    });
  }

  public String collectLog() {
    final var builder = new StringBuilder();
    final var separator = String.format("%n%s%n", "=".repeat(100));

    builder.append(separator);
    for (final var element : System.getenv().entrySet()) {
      builder.append(String.format("%s: %s%n", element.getKey(), element.getValue()));
    }

    builder.append(separator);
    for (int i = 0; i < messages.size(); i++) {
      builder.append(String.format("%03d: %s%n", i, messages.get(i)));
    }

    builder.append(separator);
    for (final var issue : issues) {
      builder.append(String.format("%s: %s%n%s%n", issue.getKind(), issue.getTitle(), issue.getDescription()));
    }
    if (issues.isEmpty()) {
      builder.append("No issues during sync\n");
    }

    builder.append(separator);
    return builder.toString();
  }

  public void assertNoErrors() {
    final var log = String.format("check this log: %s", collectLog());

    assertWithMessage(log).that(context).isNotNull();
    assertWithMessage(log).that(context.hasErrors()).isFalse();
    assertWithMessage(log).that(context.hasWarnings()).isFalse();
    assertWithMessage(log).that(context.isCancelled()).isFalse();

    assertWithMessage(log).that(issues).isEmpty();
  }
}
