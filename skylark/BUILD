load("//build_defs:build_defs.bzl", "intellij_plugin_library")

intellij_plugin_library(
    name = "plugin_library",
    plugin_xmls = ["src/META-INF/blaze-skylark.xml"],
    visibility = ["//visibility:public"],
    deps = [
        "//skylark/src/com/google/idea/blaze/skylark/debugger",
        "//skylark/src/com/google/idea/blaze/skylark/lang",
    ],
)
