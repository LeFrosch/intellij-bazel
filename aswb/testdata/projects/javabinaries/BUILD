load("@rules_java//java:java_binary.bzl", "java_binary")
load(
    "//aswb/testdata/projects:test_projects.bzl",
    "test_project_package",
)

package(
    default_visibility = ["//src:__subpackages__"],
)

test_project_package(
    name = "test_data",
    all_targets = [
        ":Binary2",
        ":another_binary",
    ],
    visibility = [
        "//aswb/testdata/projects:__pkg__",
        "//javatests/com/google/devtools/intellij/blaze/plugin/aswb:__pkg__",
    ],
)

java_binary(
    name = "Binary2",
    srcs = glob(["java/com/example/binaries/*.java"]),
)

java_binary(
    name = "another_binary",
    srcs = glob(["java/com/example/binaries/*.java"]),
    main_class = "com.example.binaries.Binary1",
)
