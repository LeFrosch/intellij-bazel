load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@protobuf//bazel:proto_library.bzl", "proto_library")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "deps",
    srcs = glob(["*.java"]),
    visibility = [
        "//javatests/com/google/devtools/intellij/blaze/plugin/aswb:__pkg__",
        "//querysync:__subpackages__",
    ],
    deps = [
        ":artifact_tracker_state_java_proto",
        "//common/experiments",
        "//intellij_platform_sdk:plugin_api",
        "//querysync/java/com/google/idea/blaze/qsync/artifacts",
        "//querysync/java/com/google/idea/blaze/qsync/cc:cc_compilation_info_java_proto",
        "//querysync/java/com/google/idea/blaze/qsync/java:java_target_info_java_proto",
        "//querysync/java/com/google/idea/blaze/qsync/project",
        "//querysync/java/com/google/idea/blaze/qsync/project:project_java_proto",
        "//shared",
        "//shared:artifact",
        "//shared:proto",
        "//shared:vcs",
        "//third_party/java/auto_value",
        "@com_google_guava_guava//jar",
        "@error_prone_annotations//jar",
        "@jetbrains_annotations//jar",
        "@jsr305_annotations//jar",
        "@protobuf//:protobuf_java",
    ],
)

proto_library(
    name = "artifact_tracker_state_proto",
    srcs = ["artifact_tracker_state.proto"],
    deps = [
        "//querysync/java/com/google/idea/blaze/qsync/project:project_proto",
        "//querysync/java/com/google/idea/blaze/qsync/project:snapshot_proto",
    ],
)

java_proto_library(
    name = "artifact_tracker_state_java_proto",
    visibility = [
        "//querysync:__subpackages__",
    ],
    deps = [":artifact_tracker_state_proto"],
)
