#
# Description: Builds ijwb
#

load("//:version.bzl", "VERSION")
load(
    "//build_defs:build_defs.bzl",
    "intellij_plugin",
    "intellij_plugin_library",
    "stamped_plugin_xml",
)
load(
    "//build_defs:intellij_plugin_debug.bzl",
    "intellij_plugin_debug_target",
)
load("//build_defs:intellij_plugin_zip.bzl", "intellij_plugin_zip")

licenses(["notice"])

intellij_plugin_library(
    name = "plugin_library",
    plugin_xmls = ["src/META-INF/ijwb.xml"],
    visibility = ["//visibility:public"],
)

stamped_plugin_xml(
    name = "stamped_plugin_xml",
    changelog_file = "//:changelog",
    description_file = "src/META-INF/description.html",
    plugin_id = "com.google.idea.bazel.ijwb",
    plugin_name = "Bazel for IntelliJ",
    since_build_numbers = {"212": "212.5080.55"},
    stamp_since_build = True,
    stamp_until_build = True,
    version = VERSION,
)

intellij_plugin(
    name = "ijwb_bazel",
    plugin_icons = ["//common:pluginIcon.svg"],
    plugin_xml = ":stamped_plugin_xml",
    deps = [
        ":plugin_library",
        "//base:plugin_library",
        # core language support
        "//java:plugin_library",
        "//kotlin:plugin_library",
        # optional language support (activated when the corresponding IDE plugin is installed)
        "//cpp:plugin_library",
        "//dart:plugin_library",
        "//gazelle:plugin_library",
        "//golang:plugin_library",
        "//javascript:plugin_library",
        "//python:plugin_library",
        "//scala:plugin_library",
        "//skylark:plugin_library",
        "//terminal:plugin_library",
    ],
)

intellij_plugin_debug_target(
    name = "ijwb_bazel_dev",
    prefix = "ijwb",
    deps = [":ijwb_bazel"],
)

intellij_plugin_zip(
    name = "ijwb_bazel_zip",
    filename = "ijwb_bazel.zip",
    prefix = "ijwb",
    deps = [":ijwb_bazel"],
)
