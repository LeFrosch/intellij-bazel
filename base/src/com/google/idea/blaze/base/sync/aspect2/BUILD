load("//build_defs:build_defs.bzl", "intellij_plugin_library")
load("@rules_java//java:defs.bzl", "java_import")

intellij_plugin_library(
    name = "aspect2",
    srcs = glob(["*.kt"]),
    plugin_xmls = ["library.xml"],
    visibility = ["//base:__subpackages__"],
    deps = [
        "//base",
        "@intellij_aspect_sdk//sdk",
    ],
)
