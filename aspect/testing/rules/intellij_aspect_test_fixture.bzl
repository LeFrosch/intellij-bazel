"""Rules for writing tests for the IntelliJ aspect."""

load("@bazel_skylib//rules:copy_file.bzl", "copy_file")
load(
    "//aspect:intellij_info.bzl",
    "intellij_info_aspect",
)
load(
    "//aspect:intellij_info_impl.bzl",
    "IntelliJInfo",
    "update_set_in_dict",
)

def _impl(ctx):
    """Implementation method for _intellij_aspect_test_fixture."""
    output_groups = dict()
    inputs = depset()
    deps = [dep for dep in ctx.attr.deps if IntelliJInfo in dep]
    for dep in deps:
        for k, v in dep[IntelliJInfo].output_groups.items():
            update_set_in_dict(output_groups, k, v)
            inputs = depset(
                [f for f in v.to_list() if f.short_path.endswith(".intellij-info.txt")],
                transitive = [inputs],
            )

    output_name = ctx.attr.output
    output = ctx.actions.declare_file(output_name)

    args = [output.path]
    args.append(":".join([f.path for f in inputs.to_list()]))
    for k, v in output_groups.items():
        args.append(k)
        args.append(":".join([f.short_path for f in v.to_list()]))
    argfile = ctx.actions.declare_file(
        output_name + ".params",
    )

    ctx.actions.write(output = argfile, content = "\n".join(args))
    ctx.actions.run(
        inputs = inputs.to_list() + [argfile],
        outputs = [output],
        executable = ctx.executable._intellij_aspect_test_fixture_builder,
        arguments = ["@" + argfile.path],
        mnemonic = "IntellijAspectTestFixtureBuilder",
        progress_message = "Building Intellij Aspect Test Fixture",
    )
    return DefaultInfo(
        files = depset([output]),
        runfiles = ctx.runfiles(
            files = [output],
        ),
    )

_intellij_aspect_test_fixture = rule(
    _impl,
    attrs = {
        "deps": attr.label_list(aspects = [intellij_info_aspect]),
        "output": attr.string(mandatory = True),
        "_intellij_aspect_test_fixture_builder": attr.label(
            default = Label("//aspect/testing/rules:IntellijAspectTestFixtureBuilder"),
            cfg = "exec",
            executable = True,
            allow_files = True,
        ),
    },
)

def intellij_aspect_test_fixture(name, deps, transitive_configs = []):
    _intellij_aspect_test_fixture(
        name = name,
        output = name + ".intellij-aspect-test-fixture",
        deps = deps,
        testonly = 1,
        transitive_configs = transitive_configs,
    )

def test_sources(outs):
    for out in outs:
        copy_file(
            name = out + ".genrule",
            src = out + ".testdata",
            out = out,
        )
