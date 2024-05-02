filegroup(name = "empty")

# custom constraint for toolchain, so that toolchain is only available when for this test
constraint_setting(name = "aspect_test")

constraint_value(
    name = "use_for_aspect_test",
    constraint_setting = ":aspect_test",
)

platform(
    name = "platform_for_aspect_test",
    constraint_values = [
        "@platforms//cpu:x86_64",
        "@platforms//os:linux",
        ":use_for_aspect_test",
    ],
)

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
    exec_compatible_with = [":use_for_aspect_test"],
    target_compatible_with = [":use_for_aspect_test"],
)

package(default_visibility = ["//visibility:public"])

load(":cc_toolchain_config.bzl", "cc_toolchain_config")

cc_toolchain_config(name = "custom_toolchain_config")
