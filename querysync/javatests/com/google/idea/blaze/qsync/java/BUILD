load("@rules_java//java:defs.bzl", "java_test")
load("//:build-visibility.bzl", "DEFAULT_TEST_VISIBILITY")

package(default_visibility = DEFAULT_TEST_VISIBILITY)

java_test(
    name = "PackageStatementParserTest",
    size = "small",
    srcs = ["PackageStatementParserTest.java"],
    deps = [
        "//querysync/java/com/google/idea/blaze/qsync/java",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "@com_google_guava_guava//jar",
    ],
)

java_test(
    name = "AddCompiledJavaDepsTest",
    size = "small",
    srcs = ["AddCompiledJavaDepsTest.java"],
    deps = [
        "//querysync/java/com/google/idea/blaze/qsync/artifacts",
        "//querysync/java/com/google/idea/blaze/qsync/deps",
        "//querysync/java/com/google/idea/blaze/qsync/java",
        "//querysync/java/com/google/idea/blaze/qsync/project",
        "//querysync/java/com/google/idea/blaze/qsync/project:project_java_proto",
        "//querysync/javatests/com/google/idea/blaze/qsync/testdata",
        "//querysync/javatests/com/google/idea/blaze/qsync/testdata:project_protos",
        "//shared",
        "//shared/javatests/com/google/idea/blaze/common:test_utils",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "@com_google_guava_guava//jar",
    ],
)

java_test(
    name = "AddDependencyGenSrcsJarsTest",
    size = "small",
    srcs = ["AddDependencyGenSrcsJarsTest.java"],
    deps = [
        "//querysync/java/com/google/idea/blaze/qsync",
        "//querysync/java/com/google/idea/blaze/qsync/artifacts",
        "//querysync/java/com/google/idea/blaze/qsync/deps",
        "//querysync/java/com/google/idea/blaze/qsync/java",
        "//querysync/java/com/google/idea/blaze/qsync/project:project_java_proto",
        "//querysync/javatests/com/google/idea/blaze/qsync:TestUtils",
        "//querysync/javatests/com/google/idea/blaze/qsync/testdata",
        "//shared",
        "//shared:artifact",
        "//shared/javatests/com/google/idea/blaze/common:test_utils",
        "//shared/javatests/com/google/idea/blaze/common/artifact:test_utils",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "@com_google_guava_guava//jar",
        "@mockito//jar",
    ],
)

java_test(
    name = "AddDependencySrcJarsTest",
    size = "small",
    srcs = ["AddDependencySrcJarsTest.java"],
    deps = [
        "//querysync/java/com/google/idea/blaze/qsync",
        "//querysync/java/com/google/idea/blaze/qsync/deps",
        "//querysync/java/com/google/idea/blaze/qsync/java",
        "//querysync/java/com/google/idea/blaze/qsync/project",
        "//querysync/java/com/google/idea/blaze/qsync/project:project_java_proto",
        "//querysync/javatests/com/google/idea/blaze/qsync:TestUtils",
        "//querysync/javatests/com/google/idea/blaze/qsync/testdata",
        "//shared",
        "//shared/javatests/com/google/idea/blaze/common:test_utils",
        "//shared/javatests/com/google/idea/blaze/common/artifact:test_utils",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "@com_google_guava_guava//jar",
    ],
)

java_test(
    name = "AddProjectGenSrcJarsTest",
    size = "small",
    srcs = ["AddProjectGenSrcJarsTest.java"],
    deps = [
        "//querysync/java/com/google/idea/blaze/qsync",
        "//querysync/java/com/google/idea/blaze/qsync/artifacts",
        "//querysync/java/com/google/idea/blaze/qsync/deps",
        "//querysync/java/com/google/idea/blaze/qsync/java",
        "//querysync/java/com/google/idea/blaze/qsync/project:project_java_proto",
        "//querysync/javatests/com/google/idea/blaze/qsync:TestUtils",
        "//querysync/javatests/com/google/idea/blaze/qsync/testdata",
        "//shared",
        "//shared:artifact",
        "//shared/javatests/com/google/idea/blaze/common:test_utils",
        "//shared/javatests/com/google/idea/blaze/common/artifact:test_utils",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "@com_google_guava_guava//jar",
        "@mockito//jar",
        "@protobuf//:protobuf_java",
    ],
)

java_test(
    name = "AddProjectGenSrcsTest",
    size = "small",
    srcs = ["AddProjectGenSrcsTest.java"],
    deps = [
        "//querysync/java/com/google/idea/blaze/qsync",
        "//querysync/java/com/google/idea/blaze/qsync/artifacts",
        "//querysync/java/com/google/idea/blaze/qsync/deps",
        "//querysync/java/com/google/idea/blaze/qsync/java",
        "//querysync/java/com/google/idea/blaze/qsync/project:project_java_proto",
        "//querysync/javatests/com/google/idea/blaze/qsync:TestUtils",
        "//querysync/javatests/com/google/idea/blaze/qsync/testdata",
        "//shared",
        "//shared:artifact",
        "//shared/javatests/com/google/idea/blaze/common:test_utils",
        "//shared/javatests/com/google/idea/blaze/common/artifact:test_utils",
        "//third_party/java/junit",
        "//third_party/java/truth",
        "@com_google_guava_guava//jar",
        "@mockito//jar",
        "@protobuf//:protobuf_java",
    ],
)
