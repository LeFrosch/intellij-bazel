package com.google.idea.blaze.qsync.deps;

import com.google.auto.value.AutoValue;
import com.google.idea.blaze.qsync.deps.AutoValue_CcCompilerInfo.Builder;
import com.google.idea.blaze.qsync.project.ProjectProto;
import com.google.idea.blaze.qsync.project.ProjectProto.CcCompilerInfo.Kind;
import com.google.idea.blaze.qsync.project.ProjectProto.CcCompilerInfo.Msvc;
import com.google.idea.blaze.qsync.project.ProjectProto.CcCompilerInfo.Xcode;
import javax.annotation.Nullable;

/**
 *
 */
@AutoValue
public abstract class CcCompilerInfo {

  public enum Kind {
    UNKNOWN, CLANG, APPLE_CLANG, GCC, MSVC;

    public static Kind fromProto(ProjectProto.CcCompilerInfo.Kind proto) {
      return switch (proto) {
        case CLANG -> CLANG;
        case APPLE_CLANG -> APPLE_CLANG;
        case GCC -> GCC;
        case MSVC -> MSVC;
        default -> UNKNOWN;
      };
    }

    public ProjectProto.CcCompilerInfo.Kind toProto() {
      return switch (this) {
        case UNKNOWN -> ProjectProto.CcCompilerInfo.Kind.CC_COMPILER_KIND_UNKNOWN;
        case CLANG -> ProjectProto.CcCompilerInfo.Kind.CLANG;
        case APPLE_CLANG -> ProjectProto.CcCompilerInfo.Kind.APPLE_CLANG;
        case GCC -> ProjectProto.CcCompilerInfo.Kind.GCC;
        case MSVC -> ProjectProto.CcCompilerInfo.Kind.MSVC;
      };
    }
  }

  public record Xcode(String developerDir, String sdkRoot) {

    public static Xcode fromProto(ProjectProto.CcCompilerInfo.Xcode proto) {
      return new Xcode(proto.getDeveloperDir(), proto.getSdkRoot());
    }

    public ProjectProto.CcCompilerInfo.Xcode toProto() {
      return ProjectProto.CcCompilerInfo.Xcode.newBuilder()
        .setDeveloperDir(developerDir())
        .setSdkRoot(sdkRoot())
        .build();
    }
  }

  public record Msvc(String arch, String version, String vcDirectory) {

    public static Msvc fromProto(ProjectProto.CcCompilerInfo.Msvc msvc) {
      return new Msvc(msvc.getArch(), msvc.getVersion(), msvc.getVcDirectory());
    }

    public ProjectProto.CcCompilerInfo.Msvc toProto() {
      return ProjectProto.CcCompilerInfo.Msvc.newBuilder()
        .setArch(arch())
        .setVersion(version())
        .setVcDirectory(vcDirectory())
        .build();
    }
  }

  public static CcCompilerInfo fromProto(ProjectProto.CcCompilerInfo proto) {
    final var builder = builder();
    builder.kind(CcCompilerInfo.Kind.fromProto(proto.getKind())).executable(proto.getExecutable());

    if (proto.hasXcode()) {
      builder.xcode(CcCompilerInfo.Xcode.fromProto(proto.getXcode()));
    }

    if (proto.hasMsvc()) {
      builder.msvc(CcCompilerInfo.Msvc.fromProto(proto.getMsvc()));
    }

    return builder.build();
  }

  public ProjectProto.CcCompilerInfo toProto() {
    final var builder = ProjectProto.CcCompilerInfo.newBuilder();
    builder.setKind(kind().toProto()).setExecutable(executable());

    if (xcode() != null) {
      builder.setXcode(xcode().toProto());
    }

    if (msvc() != null) {
      builder.setMsvc(msvc().toProto());
    }

    return builder.build();
  }

  public abstract Kind kind();

  public abstract String executable();

  @Nullable
  public abstract Xcode xcode();

  @Nullable
  public abstract Msvc msvc();

  public static Builder builder() {
    return new AutoValue_CcCompilerInfo.Builder();
  }

  public static CcCompilerInfo unknown(String executable) {
    return CcCompilerInfo.builder().executable(executable).kind(Kind.UNKNOWN).build();
  }

  /** Builder for {@link CcCompilationInfo}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder kind(Kind value);

    public abstract Builder executable(String value);

    public abstract Builder xcode(Xcode value);

    public abstract Builder msvc(Msvc value);

    public abstract CcCompilerInfo build();
  }
}
