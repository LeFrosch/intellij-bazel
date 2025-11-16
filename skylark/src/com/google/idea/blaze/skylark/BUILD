load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

# TODO: maybe worth adding a rule for bundles?
kt_jvm_library(
  name = "bundle",
  srcs = ["SkylarkBundle.kt"],
  deps = ["//intellij_platform_sdk:plugin_api"],
  visibility = ["//skylark:__subpackages__"],
)