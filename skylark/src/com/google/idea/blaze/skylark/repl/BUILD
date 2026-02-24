load("//build_defs:build_defs.bzl", "intellij_plugin_library")

intellij_plugin_library(
    name = "repl",
    srcs = glob(["**/*.kt"]),
    plugin_xmls = ["library.xml"],
    visibility = ["//skylark:__subpackages__"],
    data = {
        "intentionDescriptions/SkylarkSendLineToRepl/description.html": ":send/send_line_description.xml",
        "intentionDescriptions/SkylarkSendSelectionToRepl/description.html": ":send/send_selection_description.xml",
    },
    deps = [
        "//base",
        "//common/util:datafiles",
        "//common/util:runjar",
        "//third_party/bazel/jars:starlark_repl",
    ],
)
