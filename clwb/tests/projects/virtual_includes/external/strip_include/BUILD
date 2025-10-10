load("@rules_cc//cc:defs.bzl", "cc_library")

genrule(
    name = "header",
    outs = ["include/strip_include_lib.h"],
    cmd = "echo 'void strip_include_function();' > $@",
)

cc_library(
    name = "impl",
    srcs = [":lib.cc"],
    hdrs = [":include/strip_include_lib.h"],
    includes = ["include"],
)

cc_library(
    name = "lib",
    deps = [":impl"],
    visibility = ["//visibility:public"],
)