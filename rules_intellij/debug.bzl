# file suffix hardcoded in com.google.idea.blaze.plugin.run.BlazeIntellijPluginDeployer
SUFFIX = "intellij-plugin-debug-target-deploy-info"

# proto file proto/intellij_plugin_target_deploy_info.proto
_PluginDeployInfo = provider(fields = ["deploy_files"])

# collect by the aspect for plugin dependencies
_AspectPluginDeployInfo = provider(fields = ["input_files", "deploy_info"])

def _deploy_info_file(file):
    return struct(
        execution_path = file.path,
        deploy_location = file.basename,
    )

def _deploy_aspect_impl(target, ctx):
    if _PluginDeployInfo in target:
        deploy_files = target[_PluginDeployInfo].deploy_files
    else:
        deploy_files = [_deploy_info_file(file) for file in target.files.to_list()]

    return _AspectPluginDeployInfo(
        input_files = target.files,
        deploy_info = _PluginDeployInfo(deploy_files = deploy_files),
    )

_deploy_aspect = aspect(implementation = _deploy_aspect_impl)

def _rule_impl(ctx):
    input_files = depset()
    deploy_files = []

    for dep in ctx.attr.deps:
        info = dep[_AspectPluginDeployInfo]

        input_files = depset(transitive = [input_files, info.input_files])
        deploy_files.extend(info.deploy_info.deploy_files)

    info = _PluginDeployInfo(deploy_files = deploy_files)

    output = ctx.actions.declare_file("%s.%s" % (ctx.label.name, SUFFIX))
    ctx.actions.write(output, proto.encode_text(info))

    input_files = depset([output], transitive = [input_files])

    return [DefaultInfo(files = input_files), info]

# name hardcoded in com.google.idea.blaze.base.model.primitives.GenericBlazeRules
intellij_plugin_debug_target = rule(
    implementation = _rule_impl,
    attrs = {
        "deps": attr.label_list(aspects = [_deploy_aspect]),
    },
)
