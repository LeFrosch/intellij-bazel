load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//build_defs:build_defs.bzl", "intellij_plugin_library")

licenses(["notice"])

kt_jvm_library(
    name = "lib",
    srcs = glob(["**/*.java", "**/*.kt"]),
    deps = [
        "//base",
        "//intellij_platform_sdk:plugin_api",
        "//intellij_platform_sdk:jsr305",
    ],
)

intellij_plugin_library(
    name = "bazelrc",
    deps = [":lib"],
    plugin_xmls = ["plugin.xml"],
    visibility = ["//visibility:public"],
)
