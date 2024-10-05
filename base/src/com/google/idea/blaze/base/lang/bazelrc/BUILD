load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//build_defs:build_defs.bzl", "optional_plugin_xml")

licenses(["notice"])

package(default_visibility = ["//visibility:public"])

optional_plugin_xml(
    name = "plugin_xml",
    module = ["org.jetbrains.plugins.clion.radler"],
    plugin_xml = "plugin.xml",
)

kt_jvm_library(
    name = "lib",
    srcs = glob(["**/*.java", "**/*.kt"]),
    deps = [
        "//base",
        "//intellij_platform_sdk:plugin_api",
        "//intellij_platform_sdk:jsr305",  # unuseddeps: keep for @Nullable
    ],
)

