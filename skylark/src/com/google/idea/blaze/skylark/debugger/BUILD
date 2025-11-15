load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "debugger",
    srcs = glob(["**/*.java"]),
    visibility = ["//skylark:__subpackages__"],
    deps = [
        "//base",
        "//common/experiments",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//proto:proto_deps",
    ],
)
