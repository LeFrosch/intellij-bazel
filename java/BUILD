load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load(
    "//build_defs:build_defs.bzl",
    "intellij_plugin",
    "intellij_plugin_library",
    "optional_plugin_xml",
    "stamped_plugin_xml",
)
load(
    "//testing:test_defs.bzl",
    "intellij_integration_test_suite",
    "intellij_unit_test_suite",
)

java_library(
    name = "java",
    srcs = glob(["src/**/*.java"]),
    javacopts = ["-Xep:FutureReturnValueIgnored:OFF"],
    visibility = ["//visibility:public"],
    deps = [
        "//base",
        "//base/src/com/google/idea/blaze/base/command/buildresult/bepparser",
        "//common/actions",
        "//common/experiments",
        "//common/settings",
        "//common/util:concurrency",
        "//common/util:platform",
        "//common/util:transactions",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:junit",
        "//intellij_platform_sdk:plugin_api",
        "//proto:proto_deps",
        "//sdkcompat",
        "//shared",
        "//shared:artifact",
        "//third_party/java/auto_value",
        "@error_prone_annotations//jar",
    ],
)

optional_plugin_xml(
    name = "optional_java",
    module = ["JUnit"],
    plugin_xml = "src/META-INF/java-contents.xml",
)

optional_plugin_xml(
    name = "optional_coverage",
    module = ["com.intellij.modules.coverage"],
    plugin_xml = "src/META-INF/coverage-contents.xml",
)

optional_plugin_xml(
    name = "optional_java_coverage",
    module = [
        "com.intellij.modules.coverage",
        "com.intellij.java",
    ],
    plugin_xml = "src/META-INF/java-coverage-contents.xml",
)

intellij_plugin_library(
    name = "plugin_library",
    optional_plugin_xmls = [
        ":optional_java",
        ":optional_coverage",
        ":optional_java_coverage",
    ],
    plugin_xmls = ["src/META-INF/blaze-java.xml"],
    visibility = ["//visibility:public"],
    deps = [":java"],
)

intellij_unit_test_suite(
    name = "unit_tests",
    srcs = glob(["tests/unittests/**/*.java"]),
    tags = ["noci:studio-win"],
    test_package_root = "com.google.idea.blaze.java",
    visibility = ["//visibility:public"],
    deps = [
        ":java",
        "//base",
        "//base:unit_test_utils",
        "//common/experiments",
        "//common/experiments:unit_test_utils",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api_for_tests",
        "//intellij_platform_sdk:test_libs",
        "//intellij_platform_sdk:truth",
        "//proto:proto_deps",
        "//shared",
        "//testing:lib",
        "//third_party/java/junit",
        "@com_google_guava_guava//jar",
        "@org_opentest4j_opentest4j//jar",
    ],
)

stamped_plugin_xml(
    name = "java_plugin_xml",
    testonly = 1,
    plugin_id = "com.google.idea.blaze.java",
    plugin_name = "com.google.idea.blaze.java",
)

intellij_plugin(
    name = "java_integration_test_plugin",
    testonly = 1,
    plugin_xml = ":java_plugin_xml",
    deps = [
        ":plugin_library",
        "//base:plugin_library",
    ],
)

intellij_integration_test_suite(
    name = "integration_tests",
    srcs = glob(["tests/integrationtests/**/*.java"]),
    jvm_flags = [
        "--add-exports java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED",
    ],
    required_plugins = "com.google.idea.blaze.java",
    tags = ["noci:studio-win"],
    test_package_root = "com.google.idea.blaze.java",
    visibility = ["//visibility:public"],
    runtime_deps = [":java_integration_test_plugin"],
    deps = [
        ":java",
        "//base",
        "//base:integration_test_utils",
        "//base:unit_test_utils",
        "//common/experiments",
        "//common/experiments:unit_test_utils",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api_for_tests",
        "//intellij_platform_sdk:test_libs",
        "//proto:proto_deps",
        "//shared",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "@com_google_guava_guava//jar",
        "@org_opentest4j_opentest4j//jar",
    ],
)
