# Description: Indirections for SDK changes to the underlying platform library.

load("@rules_java//java:defs.bzl", "java_library")
load(
    "//:build-visibility.bzl",
    "SDK_COMPAT_VISIBILITY",
    "create_sdkcompat_visibility_package",
)
load("//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

licenses(["notice"])

create_sdkcompat_visibility_package()

java_library(
    name = "sdkcompat",
    visibility = SDK_COMPAT_VISIBILITY,
    exports = select_for_plugin_api({
        "intellij-ue-2025.1": ["//sdkcompat/v251"],
        "intellij-ue-2025.2": ["//sdkcompat/v252"],
        "clion-2025.1": ["//sdkcompat/v251"],
        "clion-2025.2": ["//sdkcompat/v252"],
        "default": [],
    }),
)
