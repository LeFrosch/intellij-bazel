load("@rules_java//java:defs.bzl", "JavaInfo", "java_common")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

ModuleInfo = provider(
    fields = {
        "name": "The name of this module",
        "plugin_xml": "The plugin xml files for this module",
        "deps_direct": "Depset of direct IntellijModule dependencies",
        "deps_transitive": "Depset of all IntellijModule dependencies",
        "optional": "Whether is module is optional or not",
        "package": "JVM package where all module classes are located",
    },
)

def _intellij_module_impl(ctx):
    deps = [dep[ModuleInfo] for dep in ctx.attr.deps if ModuleInfo in dep]

    module_info = ModuleInfo(
        name = "%s.%s" % (ctx.label.package.replace('/', '.'), ctx.label.name),
        plugin_xml = ctx.file.plugin_xml,
        deps_direct = depset(deps),
        deps_transitive = depset(deps, transitive = [dep.deps_transitive for dep in deps]),
        optional = ctx.attr.optional,
        package = ctx.attr.package,
    )

    # TODO: check that all classes are in the right package ?

    return [ctx.attr.impl[JavaInfo], module_info]

_intellij_module = rule(
    implementation = _intellij_module_impl,
    attrs = {
        "impl": attr.label(mandatory = True, providers = [JavaInfo]),
        "plugin_xml": attr.label(allow_single_file = [".xml"]),
        "deps": attr.label_list(providers = [[ModuleInfo, JavaInfo], [JavaInfo]]),
        "optional": attr.bool(),
        "package": attr.string(mandatory = True),
    }
)

def intellij_module(
        name,
        package,
        deps = [],
        srcs = [],
        plugin_xml = None,
        optional = False):

    impl_name = "%s_impl" % name

    kt_jvm_library(
        name = impl_name,
        srcs = srcs,
        deps = deps,
    )

    _intellij_module(
        name = name,
        impl = impl_name,
        deps = deps,
        plugin_xml = plugin_xml,
        optional = optional,
        package = package,
    )