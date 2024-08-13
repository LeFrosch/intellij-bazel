package com.google.idea.blaze.base.async.process2;

import com.google.idea.blaze.common.Output;
import com.intellij.execution.process.ProcessHandler;

public record OSProcessOutput(ProcessHandler handler) implements Output { }
