package com.google.idea.blaze.base.qsync;

import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.exception.BuildException;
import com.google.idea.blaze.qsync.deps.CompilerInfoMap;
import com.google.idea.blaze.qsync.deps.OutputInfo;
import java.io.IOException;

/** A query sync service that knows how to retrieve additional information from the compilers */
public interface CompilerInfoCollector {

  CompilerInfoMap run(BlazeContext ctx, OutputInfo outputInfo) throws IOException, BuildException;
}
