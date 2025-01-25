load("@rules_java//java:defs.bzl", "java_binary", "java_library")
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
            vendor = ctx.attr.vendor,
            version = ctx.attr.version,
            package = ctx.attr.package,
            plugin_xml = plugin_xml_file.path,
            deps = deps,
        ),
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

def _write_plugin_jar(ctx, module_xmls, plugin_xml):
    jar_name = "%s.jar" % ctx.label.name
    jar_file = ctx.actions.declare_file(jar_name)

    input = proto.encode_text(struct(
        output = jar_file.path,
        plugin = struct(
            impl_jar = ctx.file.impl.path,
            module_xmls = [file.path for file in module_xmls],
            plugin_xml = plugin_xml.path,
        ),
    ))

    ctx.actions.run(
        executable = ctx.executable._jar_builder,
        arguments = [input],
        inputs = [ctx.file.impl, plugin_xml] + module_xmls,
        outputs = [jar_file],
        progress_message = "Building plugin jar",
        mnemonic = "BuildPluginJar",
    )

    return jar_file

def _intellij_plugin_impl(ctx):
    deps = [dep[ModuleInfo] for dep in ctx.attr.deps]
    modules = depset(direct = deps, transitive = [dep.deps_transitive for dep in deps]).to_list()

    module_xmls = [_write_module_xml(ctx, module) for module in modules]
    plugin_xml = _write_plugin_xml(ctx)
    plugin_jar = _write_plugin_jar(ctx, module_xmls, plugin_xml)

    return [DefaultInfo(files = depset([plugin_jar]))]

_intellij_plugin = rule(
    implementation = _intellij_plugin_impl,
    attrs = {
        "deps": attr.label_list(providers = [ModuleInfo]),
        "impl": attr.label(mandatory = True, allow_single_file = [".jar"]),
        "package": attr.string(mandatory = True),
        "plugin_xml": attr.label(mandatory = True, allow_single_file = [".xml"]),
        "plugin_id": attr.string(mandatory = True),
        "vendor": attr.string(mandatory = True),
        "version": attr.string(mandatory = True),
        "_module_xml_builder": attr.label(
            default = Label("//rules_intellij/builder:module"),
            executable = True,
            cfg = "exec",
        ),
        "_plugin_xml_builder": attr.label(
            default = Label("//rules_intellij/builder:plugin"),
            executable = True,
            cfg = "exec",
        ),
        "_jar_builder": attr.label(
            default = Label("//rules_intellij/builder:jar"),
            executable = True,
            cfg = "exec",
        )
    },
)

def intellij_plugin(
        name,
        package,
        plugin_xml,
        plugin_id,
        vendor,
        version,
        deps = []):

    impl_name = "%s_impl" % name

    java_binary(
        name = impl_name,
        runtime_deps = deps,
        create_executable = 0,
    )

    _intellij_plugin(
        name = name,
        deps = deps,
        impl = "%s_deploy.jar" % impl_name,
        package = package,
        plugin_xml = plugin_xml,
        plugin_id = plugin_id,
        vendor = vendor,
        version = version,
    )
