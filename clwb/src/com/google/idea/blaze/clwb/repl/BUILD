load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//build_defs:build_defs.bzl", "intellij_plugin_library", "stamped_plugin_xml")

package(default_visibility = ["//clwb:__subpackages__"])

kt_jvm_library(
    name = "kotlin_lib",
    srcs = glob(["*.kt"]),
    deps = [
        "//base",
        "//intellij_platform_sdk:plugin_api",
        "@bazel//src/main/java/net/starlark/java/eval",
        "@bazel//src/main/java/net/starlark/java/syntax",
    ],
)

intellij_plugin_library(
    name = "plugin_library",
    plugin_xmls = ["optional-plugin.xml"],
    deps = [":kotlin_lib"],
)
