load("@rules_java//java:defs.bzl", "java_import")

java_import(
    name = "scala",
    jars = glob([
	    "Scala/lib/*.jar",
	    "Scala/lib/modules/*.jar",
    ]),
    visibility = ["//visibility:public"],
)
