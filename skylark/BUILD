load("@rules_java//java:defs.bzl", "java_library")
load("//build_defs:build_defs.bzl", "intellij_plugin_library")

java_library(
    name = "resources",
    resource_strip_prefix = "skylark/src",
    resources = glob(["src/resources/**/*"]),
)

intellij_plugin_library(
    name = "plugin_library",
    plugin_xmls = ["src/META-INF/blaze-skylark.xml"],
    visibility = ["//visibility:public"],
    deps = [
        ":resources",
        "//skylark/src/com/google/idea/blaze/skylark/debugger",
        "//skylark/src/com/google/idea/blaze/skylark/lang",
    ],
)
