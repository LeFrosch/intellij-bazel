load("@rules_cc//cc:defs.bzl", "cc_binary")

cc_binary(
    name = "simple",
    srcs = ["simple.cc"],
    visibility = ["//visibility:public"],
    exec_compatible_with = ["//toolchain:use_for_aspect_test"],
)