licenses(["notice"])

load("@rules_java//java:defs.bzl", "java_test")
load(
    "//aspect/testing/rules:intellij_aspect_test_fixture.bzl",
    "intellij_aspect_test_fixture",
)

cc_library(
    name = "simple",
    srcs = ["simple.cc"],
    hdrs = ["simple.h"],
)

intellij_aspect_test_fixture(
    name = "fixture",
    deps = [":simple"],
)

java_test(
    name = "CcToolchainTest",
    srcs = ["CcToolchainTest.java"],
    data = [":fixture"],
    deps = [
        "//aspect/testing:BazelIntellijAspectTest",
        "//aspect/testing:guava",
        "//aspect/testing/rules:IntellijAspectTest",
        "//aspect/testing/rules:intellij_aspect_test_fixture_java_proto",
        "//intellij_platform_sdk:test_libs",
        "//proto:intellij_ide_info_java_proto",
        "//third_party/java/junit",
    ],
)
