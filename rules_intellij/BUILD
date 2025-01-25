load("@protobuf//bazel:proto_library.bzl", "proto_library")
load("@protobuf//bazel:py_proto_library.bzl", "py_proto_library")
load("@rules_python//python:defs.bzl", "py_binary")

load(":module.bzl", "intellij_module")
load(":plugin.bzl", "intellij_plugin")

proto_library(
    name = "manifest_proto",
    srcs = ["manifest.proto"],
)

py_proto_library(
    name = "manifest_py_proto",
    deps = [":manifest_proto"],
)

py_binary(
    name = "plugin_xml_builder",
    srcs = ["plugin_xml_builder.py"],
    deps = [":manifest_py_proto"],
    python_version = "PY3",
)

intellij_module(
    name = "mod1",
    srcs = ["src/intellij/mod1/Mod1.kt"],
)

intellij_module(
    name = "mod2",
    srcs = ["src/intellij/mod2/Mod2.kt"],
    plugin_xml = "resources/mod2.xml",
    deps = [":mod1"],
)

intellij_module(
    name = "mod3",
    srcs = ["src/intellij/mod3/Mod3.kt"],
    plugin_xml = "resources/mod3.xml",
    deps = [":mod2"],
)

intellij_plugin(
    name = "plugin",
    plugin_id = "com.test.plugin",
    plugin_xml = "resources/plugin.xml",
    deps = [":mod3"],
)
