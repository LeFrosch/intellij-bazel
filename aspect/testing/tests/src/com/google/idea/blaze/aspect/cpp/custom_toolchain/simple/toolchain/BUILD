filegroup(name = "empty")

cc_toolchain(
    name = "cc_custom_toolchain",
    toolchain_identifier = "custom-toolchain",
    toolchain_config = ":custom_toolchain_config",
    all_files = ":empty",
    compiler_files = ":empty",
    dwp_files = ":empty",
    linker_files = ":empty",
    objcopy_files = ":empty",
    strip_files = ":empty",
    supports_param_files = 0,
)

toolchain(
    name = "custom_toolchain",
    toolchain = ":cc_custom_toolchain",
    toolchain_type = "@bazel_tools//tools/cpp:toolchain_type",
    exec_compatible_with = [
        "@platforms//cpu:x86_64",
        "@platforms//os:linux",
    ],
    target_compatible_with = [
        "@platforms//cpu:x86_64",
        "@platforms//os:linux",
    ],
)

package(default_visibility = ["//visibility:public"])

load(":cc_toolchain_config.bzl", "cc_toolchain_config")

cc_toolchain_config(name = "custom_toolchain_config")
