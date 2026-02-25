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

    # --- Diagnostics (print() appears in outer Bazel log even if nested build hangs) ---
    if "windows" not in rctx.os.name.lower():
        mem = rctx.execute(["free", "-m"])
        disk = rctx.execute(["df", "-h", source_dir])
        print("bazel_build_jars: memory:\n%s" % mem.stdout.strip())
        print("bazel_build_jars: disk:\n%s" % disk.stdout.strip())

    # Step 1: Check that the Bazel binary works (--version doesn't require extraction)
    rctx.report_progress("checking Bazel binary...")
    version = rctx.execute([bazel, "--version"], timeout = 30)
    print("bazel_build_jars: version: %s (exit: %s)" % (version.stdout.strip(), version.return_code))

    # Step 2: Test extraction + server startup separately (where the hang occurs on CI).
    # Use --client_debug for detailed client output. Write to file since rctx.execute
    # discards pipe output on timeout (replaces stderr with "Timed out").
    rctx.report_progress("starting nested Bazel server...")
    server_log = str(rctx.path("server.log"))
    server_cmd = " ".join(bazel_cmd + ["--client_debug", "info", "server_pid"]) + " > " + server_log + " 2>&1"
    server_result = rctx.execute(["bash", "-c", server_cmd], working_directory = source_dir, timeout = 300)
    server_log_content = rctx.execute(["cat", server_log])
    print("bazel_build_jars: server startup (exit: %s):\n%s" % (server_result.return_code, server_log_content.stdout.strip()[-2000:]))

    if server_result.return_code != 0:
        timeout_msg = " (TIMEOUT)" if server_result.return_code == 256 else ""
        fail("\n".join([
            "could not start nested Bazel server (exit code %s%s)" % (server_result.return_code, timeout_msg),
            "--- server.log ---",
            server_log_content.stdout if server_log_content.return_code == 0 else "(could not read log)",
        ]))

    # Step 3: Run the actual build (server is already running from step 2)
    cmd = bazel_cmd + ["build", "--curses=no", "--color=no"] + list(rctx.attr.jars)
    log_file = str(rctx.path("build.log"))

    rctx.report_progress("building: %s" % ", ".join(rctx.attr.jars))
    print("bazel_build_jars: running: %s" % " ".join(cmd))
    print("bazel_build_jars: working_directory: %s" % source_dir)

    shell_cmd = " ".join(cmd) + " > " + log_file + " 2>&1"
    result = rctx.execute(["bash", "-c", shell_cmd], working_directory = source_dir, timeout = 600)

    # Always shut down the nested Bazel server to free resources.
    rctx.execute(bazel_cmd + ["shutdown"], working_directory = source_dir, timeout = 60)

    if result.return_code != 0:
        log = rctx.execute(["tail", "-200", log_file])
        timeout_msg = " (TIMEOUT)" if result.return_code == 256 else ""
        fail("\n".join([
            "could not build jars (exit code %s%s)" % (result.return_code, timeout_msg),
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
