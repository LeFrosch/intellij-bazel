load("@rules_java//java:defs.bzl", "java_test")
load("@rules_python//python:defs.bzl", "py_test")
load(
    "//aspect/testing/rules:intellij_aspect_test_fixture.bzl",
    "intellij_aspect_test_fixture",
)

licenses(["notice"])

py_test(
    name = "simple",
    srcs = ["simple.py"],
    python_version = "PY3",
    srcs_version = "PY3",
)

intellij_aspect_test_fixture(
    name = "simple_fixture",
    deps = [":simple"],
)

java_test(
    name = "PyTestTest",
    srcs = ["PyTestTest.java"],
    data = [":simple_fixture"],
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
