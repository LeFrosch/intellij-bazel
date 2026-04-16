load("@rules_java//java:java_library.bzl", "java_library")
load("//intellij_platform_sdk:build_defs.bzl", "select_for_version")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

java_library(
    name = "go",
    neverlink = 1,
    exports = select_for_version(
        versions = {"2026.1": ["@go_2026_1//:go"]},
    ),
)

java_library(
    name = "go_for_tests",
    testonly = 1,
    exports = select_for_version(
        versions = {"2026.1": ["@go_2026_1//:go"]},
    ),
)
