load("@rules_cc//cc:cc_library.bzl", "cc_library")

cc_library(
    name = "testclass",
    srcs = [
        "TestClass.cc",
    ],
    hdrs = [
        "TestClass.h",
    ],
    visibility = ["//querysync/javatests/com/google/idea/blaze/qsync/testdata:__subpackages__"],
)

cc_library(
    name = "testclass2",
    srcs = [
        "TestClass2.cpp",
    ],
    hdrs = [
        "TestClass2.h",
    ],
    visibility = ["//querysync/javatests/com/google/idea/blaze/qsync/testdata:__subpackages__"],
)
