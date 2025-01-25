load(":module.bzl", "intellij_module")
load(":plugin.bzl", "intellij_plugin")

intellij_module(
    name = "mod1",
    package = "intellij.mod1",
    srcs = ["src/intellij/mod1/Mod1.kt"],
    optional = True,
)

intellij_module(
    name = "mod2",
    package = "intellij.mod2",
    srcs = ["src/intellij/mod2/Mod2.kt"],
    plugin_xml = "resources/mod2.xml",
    deps = [":mod1"],
)

intellij_module(
    name = "mod3",
    package = "intellij.mod3",
    srcs = ["src/intellij/mod3/Mod3.kt"],
    plugin_xml = "resources/mod3.xml",
    deps = [":mod2"],
)

intellij_plugin(
    name = "plugin",
    package = "com.test.plugin",
    plugin_id = "com.test.plugin",
    plugin_xml = "resources/plugin.xml",
    deps = [":mod3"],
)
