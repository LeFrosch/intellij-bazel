package com.google.idea.blaze.qsync.deps;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import autovalue.shaded.com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class CompilerInfoMap {

  public static final CompilerInfoMap EMPTY = new CompilerInfoMap(ImmutableMap.of());

  private final ImmutableMap<String, CcCompilerInfo> infoMap;

  public CompilerInfoMap(ImmutableMap<String, CcCompilerInfo> infoMap) {
    this.infoMap = infoMap;
  }

  public CcCompilerInfo get(String toolchainId) {
    return Optional.ofNullable(infoMap.get(toolchainId)).orElse(CcCompilerInfo.UNKNOWN);
  }
}
