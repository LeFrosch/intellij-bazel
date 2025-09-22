load("@rules_cc//cc:defs.bzl", "cc_binary", "cc_test")

cc_binary(
    name = "main0",
    srcs = ["main0.cc"],
    deps = ["//echo:lib"],
    copts = [
        "-Wall",
        "$(STACK_FRAME_UNLIMITED)",  # empty make variable should be stripped by make variable tokenization
    ],
)

cc_binary(
    name = "main1",
    srcs = ["main1.cc"],
    deps = ["//echo:lib"],
    copts = [
        '-D EXECPATH=\\"$(execpath main1.cc)\\"',
        '-D ROOTPATH=\\"$(rootpath main1.cc)\\"',
        '-D RLOCATIONPATH=\\"$(rlocationpath main1.cc)\\"',
        '-D LOCATION=\\"$(location main1.cc)\\"',
    ],
)

cc_binary(
    name = "main2",
    srcs = ["main2.cc"],
    deps = ["//echo:lib"],
    data = ["//echo:bin"],
    copts = [
        '-D EXECPATH=\\"$(execpath //echo:bin)\\"',
        '-D ROOTPATH=\\"$(rootpath //echo:bin)\\"',
        '-D RLOCATIONPATH=\\"$(rlocationpath //echo:bin)\\"',
        '-D LOCATION=\\"$(location //echo:bin)\\"',
    ],
)
