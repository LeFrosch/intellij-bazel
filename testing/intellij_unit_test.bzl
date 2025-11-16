load("@rules_java//java:defs.bzl", "java_test")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load(":test_defs.bzl", "ADD_OPENS")

def intellij_unit_test(test, test_class = None, deps = None, data = None):
    # derive the test name from the class name
    name = test.removesuffix(".kt")

    kt_jvm_library(
        name = name + "_lib",
        testonly = 1,
        srcs = [test],
        deps = (deps or []) + [
            "//intellij_platform_sdk:plugin_api_for_tests",
            "//intellij_platform_sdk:test_libs",
            "//third_party/java/junit",
            "//third_party/java/truth",
            "//testing:runfiles",
        ],
    )

    # simple heuristic for the test class based on the current package
    if not test_class:
        path = native.package_name().split("/")
        test_class = ".".join(path[path.index("unittests") + 1:] + [name])

    java_test(
        name = name,
        jvm_flags = ADD_OPENS + [
            "-Didea.home.path=%s" % native.package_name(),
        ],
        test_class = test_class,
        runtime_deps = [name + "_lib"],
        data = data,
    )
