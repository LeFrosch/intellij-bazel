load("@rules_cc//cc:cc_binary.bzl", "cc_binary")
load("@rules_java//java:defs.bzl", "java_test")
load(
    "//aspect/testing/rules:intellij_aspect_test_fixture.bzl",
    "intellij_aspect_test_fixture",
)

cc_binary(
    name = "simple",
    srcs = ["simple/simple.cc"],
    deps = ["//aspect/testing/tests/src/com/google/idea/blaze/aspect/cpp/foreigncc/cmake:lib"],
)

intellij_aspect_test_fixture(
    name = "aspect_fixture",
    deps = ["simple"],
)

java_test(
    name = "ForeignCcTest",
    srcs = ["ForeignCcTest.java"],
    data = [":aspect_fixture"],
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
