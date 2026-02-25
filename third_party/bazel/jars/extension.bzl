load("@bazel_skylib//lib:paths.bzl", "paths")

def _resolve_target_artifact(rctx, target, source_dir):
    """Finds the generated artifact in the local bazel-bin directory."""
    return rctx.path(paths.join(
        source_dir,
        "bazel-bin",
        target.removeprefix("//").replace(":", "/"),
    ))

def _resolve_target_jar_name(target):
    """Derives the jar name from the target name."""
    return target.split(":")[1]

def _bazel_build_jars_impl(rctx):
    bazel = rctx.getenv("BAZEL_REAL")
    if not bazel:
        fail("BAZEL_REAL environment variable is not set, are you using bazelisk?")

    rctx.extract(rctx.path(rctx.attr.srcs), type = "tar.gz")
    source_dir = rctx.execute(["ls"]).stdout.strip()

    # windows requires non-hermetic build to avoid long paths issues :(
    if "windows" in rctx.os.name.lower():
        build_cmd = [bazel, "build"]
    else:
        build_cmd = [bazel, "--output_user_root=%s" % rctx.path("output"), "build"]

    rctx.report_progress("building: %s" % ", ".join(rctx.attr.jars))
    cmd = build_cmd + list(rctx.attr.jars)
    log_file = str(rctx.path("build.log"))

    # print the command for debugging CI issues
    print("bazel_build_jars: running: %s" % " ".join(cmd))
    print("bazel_build_jars: working_directory: %s" % source_dir)

    # redirect output to a log file so we can read it even after a timeout
    shell_cmd = " ".join(cmd) + " > " + log_file + " 2>&1"
    result = rctx.execute(["bash", "-c", shell_cmd], working_directory = source_dir, timeout = 3600)

    if result.return_code != 0:
        log = rctx.execute(["tail", "-200", log_file])
        fail("\n".join([
            "could not build jars (exit code %s)" % result.return_code,
            "command: %s" % " ".join(cmd),
            "--- build.log (last 200 lines) ---",
            log.stdout if log.return_code == 0 else "(could not read log file: %s)" % log.stderr,
        ]))

    for target in rctx.attr.jars:
        rctx.symlink(_resolve_target_artifact(rctx, target, source_dir), _resolve_target_jar_name(target))

    files = ", ".join(["'%s'" % _resolve_target_jar_name(target) for target in rctx.attr.jars])
    rctx.file("BUILD", content = "exports_files([%s])" % files)

bazel_build_jars = repository_rule(
    implementation = _bazel_build_jars_impl,
    attrs = {
        "srcs": attr.label(mandatory = True),
        "jars": attr.string_list(mandatory = True),
    },
)
