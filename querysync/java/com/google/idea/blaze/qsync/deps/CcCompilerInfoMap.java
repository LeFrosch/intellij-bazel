package com.google.idea.blaze.qsync.deps;

import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;

public class CcCompilerInfoMap {

  public static final CcCompilerInfoMap EMPTY = new CcCompilerInfoMap(ImmutableMap.of());

  private final ImmutableMap<String, CcCompilerInfo> infoMap;

  public CcCompilerInfoMap(ImmutableMap<String, CcCompilerInfo> infoMap) {
    this.infoMap = infoMap;
  }

  @Nullable
  public CcCompilerInfo get(String toolchainId) {
    return infoMap.get(toolchainId);
  }
}
