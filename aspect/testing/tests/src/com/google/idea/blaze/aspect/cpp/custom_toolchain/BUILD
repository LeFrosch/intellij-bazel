licenses(["notice"])

load(
    "//aspect/testing/rules:intellij_aspect_test_fixture.bzl",
    "intellij_aspect_test_fixture",
)

intellij_aspect_test_fixture(
    name = "fixture",
    deps = ["@test_cpp_custom_toolchain//src:simple"],
)

java_test(
    name = "CcCustomToolchainTest",
    srcs = ["CcCustomToolchainTest.java"],
    data = [":fixture"],
    deps = [
        "//aspect/testing:BazelIntellijAspectTest",
        "//aspect/testing:guava",
        "//aspect/testing/rules:IntellijAspectTest",
        "//aspect/testing/rules:intellij_aspect_test_fixture_java_proto",
        "//intellij_platform_sdk:test_libs",
        "//proto:intellij_ide_info_java_proto",
        "@junit//jar",
    ],
)
