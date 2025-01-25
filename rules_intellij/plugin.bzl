load(":module.bzl", "ModuleInfo")

def _write_module_xml(ctx, info):
    plugin_id = ctx.attr.plugin_id
    plugin_xml_file = info.plugin_xml

    module_xml_name = "%s.%s.xml" % (plugin_id, info.name)
    module_xml_file = ctx.actions.declare_file(module_xml_name)

    input = proto.encode_test(struct(
        plugin_id = plugin_id,
        output = module_xml_file.path,
        module = struct(
            name = info.name,
            package = info.package,
            plugin_xml = plugin_xml_file.path if plugin_xml_file else "",
            optional = info.optional,
            deps = [dep.name for dep in info.deps],
        ),
    ))

    ctx.actions.run(
        executable = ctx.executable._plugin_xml_builder,
        arguments = [input],
        inputs = [plugin_xml_file] if plugin_xml_file else [],
        outputs = [module_xml_file],
        progress_message = "Building module xml file",
        mnemonic = "BuildModuleXml",
    )

    return module_xml_file

def _intellij_plugin_impl(ctx):
    deps = [dep[ModuleInfo] for dep in ctx.attr.deps]
    modules = depset(direct = deps, transitive = [dep.deps for dep in deps]).to_list()

    manifest = []
    input_files = []

    for module in modules:
        if not module.plugin_xml:
            continue

        manifest.append(struct(
            optional = module.optional,
            plugins = module.plugins,
            path = module.plugin_xml.path,
        ))

        input_files.append(module.plugin_xml)

    manifest_file = ctx.actions.declare_file("%s_manifest.txt" % ctx.label.name)
    ctx.actions.write(manifest_file, proto.encode_text(struct(files = manifest)))

    plugin_xml_file = ctx.actions.declare_file("%s_plugin.xml" % ctx.label.name)
    ctx.actions.run(
        executable = ctx.executable._plugin_xml_builder,
        arguments = [manifest_file.path, plugin_xml_file.path],
        inputs = [manifest_file] + input_files,
        outputs = [plugin_xml_file],
        progress_message = "Building plugin xml file",
        mnemonic = "BuildPluginXml",
    )

    return [DefaultInfo(files = depset([plugin_xml_file]))]

intellij_plugin = rule(
    implementation = _intellij_plugin_impl,
    attrs = {
        "deps": attr.label_list(providers = [ModuleInfo]),
        "plugin_xml": attr.label(mandatory = True, allow_single_file = [".xml"]),
        "plugin_id": attr.string(mandatory = True),
        "_plugin_xml_builder": attr.label(
            default = Label("//rules_intellij:plugin_xml_builder"),
            executable = True,
            cfg = "exec",
        ),
    },
)
