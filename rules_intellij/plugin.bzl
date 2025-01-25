load(":module.bzl", "ModuleInfo")

def _write_module_xml(ctx, info):
    plugin_id = ctx.attr.plugin_id
    plugin_xml_file = info.plugin_xml

    module_xml_name = "%s.%s.xml" % (plugin_id, info.name)
    module_xml_file = ctx.actions.declare_file(module_xml_name)

    deps = [
        struct(
            name = "%s.%s" % (plugin_id, dep.name),
            optional = dep.optional,
        )
        for dep in info.deps_direct.to_list()
    ]

    input = proto.encode_text(struct(
        plugin_id = plugin_id,
        output = module_xml_file.path,
        module = struct(
            name = info.name,
            package = info.package,
            plugin_xml = plugin_xml_file.path if plugin_xml_file else "",
            deps = deps,
        ),
    ))

    ctx.actions.run(
        executable = ctx.executable._module_xml_builder,
        arguments = [input],
        inputs = [plugin_xml_file] if plugin_xml_file else [],
        outputs = [module_xml_file],
        progress_message = "Building module xml file",
        mnemonic = "BuildModuleXml",
    )

    return module_xml_file

def _write_plugin_xml(ctx):
    plugin_id = ctx.attr.plugin_id
    plugin_xml_file = ctx.file.plugin_xml

    module_xml_name = "%s.xml" % plugin_id
    module_xml_file = ctx.actions.declare_file(module_xml_name)

    deps = [
        struct(
            name = "%s.%s" % (plugin_id, dep[ModuleInfo].name),
            optional = dep[ModuleInfo].optional,
        )
        for dep in ctx.attr.deps
    ]

    input = proto.encode_text(struct(
        output = module_xml_file.path,
        plugin = struct(
            id = plugin_id,
            package = ctx.attr.package,
            plugin_xml = plugin_xml_file.path,
            deps = deps,
        )
    ))

    ctx.actions.run(
        executable = ctx.executable._plugin_xml_builder,
        arguments = [input],
        inputs = [plugin_xml_file],
        outputs = [module_xml_file],
        progress_message = "Building plugin xml file",
        mnemonic = "BuildPluginXml",
    )

    return module_xml_file

def _intellij_plugin_impl(ctx):
    deps = [dep[ModuleInfo] for dep in ctx.attr.deps]
    modules = depset(direct = deps, transitive = [dep.deps_transitive for dep in deps]).to_list()

    files = [_write_module_xml(ctx, module) for module in modules]
    files.append(_write_plugin_xml(ctx))

    return [DefaultInfo(files = depset(files))]

intellij_plugin = rule(
    implementation = _intellij_plugin_impl,
    attrs = {
        "deps": attr.label_list(providers = [ModuleInfo]),
        "package": attr.string(mandatory = True),
        "plugin_xml": attr.label(mandatory = True, allow_single_file = [".xml"]),
        "plugin_id": attr.string(mandatory = True),
        "_module_xml_builder": attr.label(
            default = Label("//rules_intellij/xml_builder:module"),
            executable = True,
            cfg = "exec",
        ),
        "_plugin_xml_builder": attr.label(
            default = Label("//rules_intellij/xml_builder:plugin"),
            executable = True,
            cfg = "exec",
        ),
    },
)
