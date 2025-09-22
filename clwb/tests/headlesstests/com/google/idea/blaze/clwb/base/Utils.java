package com.google.idea.blaze.clwb.base;

import static com.google.common.truth.Truth.assertThat;

import com.intellij.openapi.util.text.Strings;
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches.Format;
import com.jetbrains.cidr.lang.workspace.OCCompilerSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Utils {

  private static final String ECHO_OUTPUT_MARKER = "ECHO_OUTPUT_FILE: ";

  public static List<String> lookupCompilerSwitch(String flag, OCCompilerSettings settings) {
    final var switches = settings.getCompilerSwitches();
    assertThat(switches).isNotNull();

    return switches.getList(Format.BASH_SHELL)
        .stream()
        .map(it -> it.replaceAll("^-+", ""))
        .filter(it -> it.startsWith(flag))
        .map(it -> Strings.trimStart(it.substring(flag.length()), "="))
        .toList();
  }

  public static List<String> parseEchoOutput(String output) throws IOException {
    final var line = output.lines().filter((it) -> it.startsWith(ECHO_OUTPUT_MARKER)).findFirst();
    assertThat(line).isPresent();

    final var path = Path.of(line.get().substring(ECHO_OUTPUT_MARKER.length()));
    assertThat(Files.exists(path)).isTrue();

    return Files.readAllLines(path);
  }
}
