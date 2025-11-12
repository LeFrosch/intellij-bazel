load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//build_defs:build_defs.bzl", "intellij_plugin_library", "stamped_plugin_xml")

package(default_visibility = ["//clwb:__subpackages__"])

kt_jvm_library(
    name = "kotlin_lib",
    srcs = glob(["*.kt"]),
    resource_strip_prefix = "../bazel+/src/",
    resources = ["@bazel//src/tools/execlog:parser_deploy.jar"],
    deps = [
        "//base",
        "//intellij_platform_sdk:plugin_api",
    ],
)

intellij_plugin_library(
    name = "plugin_library",
    plugin_xmls = ["optional-plugin.xml"],
    deps = [":kotlin_lib"],
)
