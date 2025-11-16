load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//third_party/grammar_kit:grammar_kit_lexer.bzl", "grammar_kit_lexer")
load("//third_party/grammar_kit:grammar_kit_parser.bzl", "grammar_kit_parser")

grammar_kit_lexer(
    name = "lexer",
    src = "lexer/lexer.flex",
    class_name = "SkylarkLexer",
)

grammar_kit_parser(
    name = "parser",
    src = "psi/parser.bnf",
)

kt_jvm_library(
    name = "lang",
    srcs = [
        ":lexer",
        ":parser",
    ] + glob(["**/*.kt"]),
    visibility = ["//skylark:__subpackages__"],
    deps = [
        "//base",  # only required for migration from BuildFileLanguage
        "//intellij_platform_sdk:plugin_api",
        "//skylark/src/com/google/idea/blaze/skylark:bundle",
    ]
)
