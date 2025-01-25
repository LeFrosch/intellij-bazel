load("@protobuf//bazel:proto_library.bzl", "proto_library")
load("@protobuf//bazel:py_proto_library.bzl", "py_proto_library")
load("@rules_python//python:defs.bzl", "py_binary")

proto_library(
    name = "module_arguments",
    srcs = ["module.proto"],
)

py_proto_library(
    name = "module_arguments_pb2",
    deps = [":module_arguments"],
)

py_binary(
    name = "module",
    srcs = ["module.py", "utils.py"],
    deps = [":module_arguments_pb2"],
    python_version = "PY3",
    visibility = ["//visibility:public"],
)

proto_library(
    name = "plugin_arguments",
    srcs = ["plugin.proto"],
)

py_proto_library(
    name = "plugin_arguments_pb2",
    deps = [":plugin_arguments"],
)

py_binary(
    name = "plugin",
    srcs = ["plugin.py", "utils.py"],
    deps = [":plugin_arguments_pb2"],
    python_version = "PY3",
    visibility = ["//visibility:public"],
)

proto_library(
    name = "jar_arguments",
    srcs = ["jar.proto"],
)

py_proto_library(
    name = "jar_arguments_pb2",
    deps = [":jar_arguments"],
)

py_binary(
    name = "jar",
    srcs = ["jar.py", "utils.py"],
    deps = [":jar_arguments_pb2"],
    python_version = "PY3",
    visibility = ["//visibility:public"],
)
