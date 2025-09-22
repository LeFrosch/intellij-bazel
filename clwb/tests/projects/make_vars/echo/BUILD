load("@rules_cc//cc:defs.bzl", "cc_binary", "cc_library")

package(default_visibility = ["//main:__pkg__"])

cc_library(
    name = "lib",
    srcs = ["echo.cc"],
    hdrs = ["echo.h"],
)

cc_binary(
    name = "bin",
    srcs = ["main.cc"],
    deps = [":lib"],
)