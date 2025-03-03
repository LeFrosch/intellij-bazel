package com.google.idea.blaze.qsync.deps;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/**
 *
 */
@AutoValue
public abstract class CcCompilerInfo {

  public enum Kind {UNKNOWN, CLANG, APPLE_CLANG, GCC, MSVC}

  public record Xcode(String developerDir, String sdkRoot) {}

  public record Msvc(String arch, String version, String vcDirectory) {}

  public abstract String toolchainId();

  public abstract Kind kind();

  @Nullable
  public abstract Xcode xcode();

  @Nullable
  public abstract Msvc msvc();

  public static Builder builder() {
    return new AutoValue_CcCompilerInfo.Builder();
  }

  public static CcCompilerInfo unknown(String toolchainId) {
    return builder().toolchainId(toolchainId).kind(Kind.UNKNOWN).build();
  }

  /** Builder for {@link CcCompilationInfo}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder toolchainId(String value);

    public abstract Builder kind(Kind value);

    public abstract Builder xcode(Xcode value);

    public abstract Builder msvc(Msvc value);

    public abstract CcCompilerInfo build();
  }
}
