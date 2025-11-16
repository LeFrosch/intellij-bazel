def _wasi_sdk_sysroots(ctx):
    ctx.download_and_extract()

    ctx.file("empty/BUILD.bazel", _SYSROOT_BUILD.format(
        name = repr("empty"),
    ))
