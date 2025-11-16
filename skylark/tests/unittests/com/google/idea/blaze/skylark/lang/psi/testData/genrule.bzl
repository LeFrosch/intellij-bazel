genrule(
    name = "generate",
    srcs = [],
    outs = ["generated.h"],
    cmd = r"""echo '#define EXTERNAL_GENERATED_MACRO 0' > $@""",
)
