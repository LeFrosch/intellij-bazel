load("@rules_java//java:java_library.bzl", "java_library")
load("//intellij_platform_sdk:build_defs.bzl", "select_for_version")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

java_library(
    name = "scala",
    neverlink = 1,
    exports = select_for_version(
        versions = {"2026.1": ["@scala_2026_1//:scala"]},
    ),
)

java_library(
    name = "scala_for_tests",
    testonly = 1,
    exports = select_for_version(
        versions = {"2026.1": ["@scala_2026_1//:scala"]},
    ),
)
