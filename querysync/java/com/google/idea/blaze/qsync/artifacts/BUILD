load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@protobuf//bazel:proto_library.bzl", "proto_library")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "artifacts",
    srcs = glob(["*.java"]),
    visibility = [
        "//querysync:__subpackages__",
        "//third_party/google/aswb/java/com/google/devtools/intellij/blaze/plugin/aswb:__pkg__",
        "//third_party/google/aswb/javatests/com/google/devtools/intellij/blaze/plugin/aswb:__pkg__",
    ],
    deps = [
        ":build_artifact_java_proto",
        "//querysync/java/com/google/idea/blaze/qsync/deps:artifact_tracker_state_java_proto",
        "//querysync/java/com/google/idea/blaze/qsync/project:project_java_proto",
        "//querysync/java/com/google/idea/blaze/qsync/query",
        "//shared",
        "//shared:artifact",
        "//shared:exception",
        "//third_party/java/auto_value",
        "@com_google_guava_guava//jar",
        "@jsr305_annotations//jar",
        "@protobuf//:protobuf_java",
    ],
)

proto_library(
    name = "build_artifact_proto",
    srcs = ["build_artifact.proto"],
    visibility = [
        "//querysync/java/com/google/idea/blaze/qsync/cc:__pkg__",
        "//querysync/java/com/google/idea/blaze/qsync/java:__pkg__",
        "//third_party/google/aswb/javatests/com/google/devtools/intellij/blaze/plugin/aswb:__subpackages__",
    ],
)

java_proto_library(
    name = "build_artifact_java_proto",
    visibility = [
        "//javatests/com/google/devtools/intellij/blaze/plugin/aswb:__pkg__",
        "//querysync/java/com/google/idea/blaze/qsync/cc:__pkg__",
        "//querysync/java/com/google/idea/blaze/qsync/java:__pkg__",
        "//querysync/javatests:__subpackages__",
    ],
    deps = [":build_artifact_proto"],
)
