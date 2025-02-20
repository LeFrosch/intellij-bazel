#
# Description:
# Proto dependencies from bazel.
#
load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@protobuf//bazel:proto_library.bzl", "proto_library")
load("@rules_java//java:defs.bzl", "java_library")
load(
    "//:build-visibility.bzl",
    "ASPECT_PROTO_VISIBILITY",
    "PLUGIN_PACKAGES_VISIBILITY",
    "create_proto_visibility_group",
)
load(
    "//build_defs:build_defs.bzl",
    "combine_visibilities",
)

licenses(["notice"])

create_proto_visibility_group()

proto_library(
    name = "common_proto",
    srcs = ["common.proto"],
    visibility = ["//visibility:private"],
)

java_proto_library(
    name = "common_java_proto",
    visibility = combine_visibilities(
        ASPECT_PROTO_VISIBILITY,
        PLUGIN_PACKAGES_VISIBILITY,
    ),
    deps = [":common_proto"],
)

proto_library(
    name = "fast_build_info_proto",
    srcs = ["fast_build_info.proto"],
    visibility = PLUGIN_PACKAGES_VISIBILITY,
    deps = [":common_proto"],
)

java_proto_library(
    name = "fast_build_info_java_proto",
    visibility = PLUGIN_PACKAGES_VISIBILITY,
    deps = [":fast_build_info_proto"],
)

proto_library(
    name = "intellij_ide_info_proto",
    srcs = ["intellij_ide_info.proto"],
    visibility = combine_visibilities(
        ASPECT_PROTO_VISIBILITY,
        PLUGIN_PACKAGES_VISIBILITY,
    ),
    deps = [
        ":common_proto",
    ],
)

java_proto_library(
    name = "intellij_ide_info_java_proto",
    visibility = combine_visibilities(
        ASPECT_PROTO_VISIBILITY,
        PLUGIN_PACKAGES_VISIBILITY,
    ),
    deps = [":intellij_ide_info_proto"],
)

proto_library(
    name = "project_data_proto",
    srcs = ["project_data.proto"],
    deps = [
        ":common_proto",
        ":intellij_ide_info_proto",
        "//third_party/bazel/src/main/protobuf:build_proto",
    ],
)

java_proto_library(
    name = "project_data_java_proto",
    deps = [":project_data_proto"],
)

proto_library(
    name = "intellij_plugin_target_deploy_info_proto",
    srcs = ["intellij_plugin_target_deploy_info.proto"],
)

java_proto_library(
    name = "intellij_plugin_target_deploy_info_java_proto",
    deps = [":intellij_plugin_target_deploy_info_proto"],
)

# TODO delete this build rule. Dependants should use the proto targets they use
# directly instead.
java_library(
    name = "proto_deps",
    visibility = PLUGIN_PACKAGES_VISIBILITY,
    exports = [
        # do not sort: to get a consistent version of the protobuf-java library across all proto users
        "@protobuf//:protobuf_java",
        ":common_java_proto",
        ":fast_build_info_java_proto",
        ":intellij_ide_info_java_proto",
        ":intellij_plugin_target_deploy_info_java_proto",
        ":project_data_java_proto",
        "//third_party/bazel/src/main/java/com/google/devtools/build/lib/buildeventstream/proto:build_event_stream_java_proto",
        "//third_party/bazel/src/main/java/com/google/devtools/build/lib/starlarkdebug/proto:starlark_debugging_java_proto",
        "//third_party/bazel/src/main/protobuf:android_deploy_info_java_proto",
        "//third_party/bazel/src/main/protobuf:build_java_proto",
        "//third_party/bazel/src/main/protobuf:command_line_java_proto",
        "//third_party/bazel/src/main/protobuf:deps_java_proto",
    ],
)
