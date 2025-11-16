load("//testing:intellij_unit_test.bzl", "intellij_unit_test")

intellij_unit_test(
    data = glob(["testData/*"]),
    test = "SkylarkLexerTest.kt",
    deps = ["//skylark/src/com/google/idea/blaze/skylark/lang"],
)
