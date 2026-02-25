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
        bazel_cmd = [bazel]
    else:
        bazel_cmd = [bazel, "--output_user_root=%s" % rctx.path("output")]

    # Use --client_debug to get detailed output about what the Bazel client is doing
    # during extraction and server startup (the phase where it hangs on CI).
    cmd = bazel_cmd + ["--client_debug", "build", "--curses=no", "--color=no"] + list(rctx.attr.jars)

    rctx.report_progress("building: %s" % ", ".join(rctx.attr.jars))

    # print the command for debugging CI issues
    print("bazel_build_jars: running: %s" % " ".join(cmd))
    print("bazel_build_jars: working_directory: %s" % source_dir)

    # Run directly without shell redirect. The Bazel client writes all output to
    # stderr (progress, errors, and --client_debug messages). rctx.execute captures
    # stderr through a pipe, which is more reliable than file redirect + stdbuf.
    result = rctx.execute(cmd, working_directory = source_dir, timeout = 600)

    # Always shut down the nested Bazel server to free resources. The timeout kills
    # the client process, but the Bazel server is a daemon that keeps running.
    rctx.execute(bazel_cmd + ["shutdown"], working_directory = source_dir, timeout = 60)

    if result.return_code != 0:
        timeout_msg = " (TIMEOUT)" if result.return_code == 256 else ""
        output = result.stderr.strip() if result.stderr.strip() else result.stdout.strip()
        if output:
            lines = output.split("\n")
            last_lines = "\n".join(lines[-200:])
        else:
            last_lines = "(no output captured)"
        fail("\n".join([
            "could not build jars (exit code %s%s)" % (result.return_code, timeout_msg),
            "command: %s" % " ".join(cmd),
            "--- build output (last 200 lines) ---",
            last_lines,
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
