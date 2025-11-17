load("//build_defs:build_defs.bzl", "intellij_plugin_library")

intellij_plugin_library(
    name = "repl",
    srcs = glob(["*.kt"]),
    plugin_xmls = ["plugin.xml"],
    visibility = ["//skylark:__pkg__"],
    deps = [
        "//base",
        "//third_party/bazel/jars:starlark_repl",
    ],
)
