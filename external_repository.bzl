def _impl(ctx):
    path = "{}/{}".format(ctx.workspace_root, ctx.attr.path)

    ctx.execute(["cp", "-r", path + "/.", "."])
    ctx.watch_tree(path)

to_external_repository = repository_rule(
    implementation = _impl,
    local = True,
    attrs = {
        "path": attr.string(mandatory = True),
    },
)
