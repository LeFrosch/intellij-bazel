load("@bazel_tools//tools/cpp:cc_toolchain_config_lib.bzl", "action_config", "feature", "flag_group", "flag_set", "tool", "tool_path")
load("@bazel_tools//tools/build_defs/cc:action_names.bzl", "ACTION_NAMES")

def _action_configs_for(names, path):
    return [
        action_config(
            action_name = name,
            tools = [tool(path = path)],
        )
        for name in names
    ]

def _impl(ctx):
    action_configs = []

    action_configs += _action_configs_for(
        names = [
            ACTION_NAMES.preprocess_assemble,
            ACTION_NAMES.assemble,
            ACTION_NAMES.cc_flags_make_variable,
            ACTION_NAMES.cpp_header_parsing,
            ACTION_NAMES.cpp_link_dynamic_library,
            ACTION_NAMES.cpp_link_executable,
            ACTION_NAMES.cpp_link_nodeps_dynamic_library,
        ],
        path = "/usr/bin/false",
    )

    action_configs += _action_configs_for(
        names = [ ACTION_NAMES.c_compile ],
        path = "/path/to/c/compiler",
    )

    action_configs += _action_configs_for(
        names = [ ACTION_NAMES.cpp_compile ],
        path = "/path/to/cpp/compiler",
    )

    return cc_common.create_cc_toolchain_config_info(
        ctx = ctx,
        toolchain_identifier = "k8-toolchain",
        host_system_name = "local",
        target_system_name = "local",
        target_cpu = "k8",
        target_libc = "unknown",
        compiler = "compiler",
        abi_version = "unknown",
        abi_libc_version = "unknown",
        action_configs = action_configs,
    )

cc_toolchain_config = rule(
    implementation = _impl,
    attrs = {},
    provides = [CcToolchainConfigInfo],
)
