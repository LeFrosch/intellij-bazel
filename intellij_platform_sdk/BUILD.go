load("@rules_java//java:defs.bzl", "java_import")

java_import(
    name = "go",
    jars = glob([
	    "go-plugin/lib/*.jar",
	    "go-plugin/lib/modules/*.jar",
    ]),
    visibility = ["//visibility:public"],
)
