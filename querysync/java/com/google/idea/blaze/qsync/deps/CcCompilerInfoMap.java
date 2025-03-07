package com.google.idea.blaze.qsync.deps;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public class CcCompilerInfoMap {

  public static final CcCompilerInfoMap EMPTY = new CcCompilerInfoMap(ImmutableMap.of());

  private final ImmutableMap<String, CcCompilerInfo> infoMap;

  public CcCompilerInfoMap(ImmutableMap<String, CcCompilerInfo> infoMap) {
    this.infoMap = infoMap;
  }

  public CcCompilerInfo get(String toolchainId) {
    return Optional.ofNullable(infoMap.get(toolchainId)).orElse(CcCompilerInfo.UNKNOWN);
  }
}
