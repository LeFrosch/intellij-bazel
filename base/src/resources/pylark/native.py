def cc_library(
    name,
    deps=[],
    srcs=[],
    data=[],
    hdrs=[],
    additional_compiler_inputs=[],
    additional_linker_inputs=[],
    alwayslink=False,
    compatible_with=[],
    copts=[],
    defines=[],
    deprecation=None,
    distribs=[],
    exec_compatible_with=[],
    exec_properties={},
    features=[],
    hdrs_check="",
    implementation_deps=[],
    include_prefix="",
    includes=[],
    licenses=["none"],
    linkopts=[],
    linkstamp=None,
    linkstatic=False,
    local_defines=[],
    module_interfaces=[],
    restricted_to=[],
    strip_include_prefix="",
    tags=[],
    target_compatible_with=[],
    testonly=False,
    textual_hdrs=[],
    toolchains=[],
    visibility=[],
    win_def_file=None):
  """
  Use cc_library() for C++-compiled libraries. The result is either a .so, .lo, or .a, depending on what is needed.

  If you build something with static linking that depends on a cc_library, the output of a depended-on library rule is the .a file.
  If you specify alwayslink=True, you get the .lo file.

  The actual output file name is libfoo.so for the shared library, where foo is the name of the rule.
  The other kinds of libraries end with .lo and .a, respectively.
  If you need a specific shared library name, for example, to define a Python module, use a genrule to copy the library to the desired name.

  :param name: A unique name for this target.
  :param deps: The list of other libraries that the library target depends upon.
  :param srcs: The list of C and C++ files that are processed to create the library target. These are C/C++ source and header files, either non-generated (normal source code) or generated.
  :param data: The list of files needed by this library at runtime.
  :param hdrs: The list of header files published by this library to be directly included by sources in dependent rules.
  :param additional_compiler_inputs: Any additional files you might want to pass to the compiler command line, such as sanitizer ignorelists, for example. Files specified here can then be used in copts with the $(location) function.
  :param additional_linker_inputs: Pass these files to the C++ linker command.
  :param alwayslink: If 1, any binary that depends (directly or indirectly) on this C++ library will link in all the object files for the files listed in srcs, even if some contain no symbols referenced by the binary. This is useful if your code isn't explicitly called by code in the binary, e.g., if your code registers to receive some callback provided by some service.
  :param compatible_with: The list of environments this target can be built for, in addition to default-supported environments.
  :param copts: Add these options to the C++ compilation command. Subject to "Make variable" substitution and Bourne shell tokenization.
  :param defines: List of defines to add to the compile line. Subject to "Make" variable substitution and Bourne shell tokenization.
  :param deprecation: An explanatory warning message associated with this target.
  :param distribs: A list of distribution-method strings to be used for this particular target. This is part of a deprecated licensing API that Bazel no longer uses. Don't use this.
  :param exec_compatible_with: A list of constraint_values that must be present in the execution platform for this target. This is in addition to any constraints already set by the rule type.
  :param exec_properties: A dictionary of strings that will be added to the exec_properties of a platform selected for this target.
  :param features: A feature is string tag that can be enabled or disabled on a target. The meaning of a feature depends on the rule itself.
  :param hdrs_check: Deprecated, no-op.
  :param implementation_deps: The list of other libraries that the library target depends on. Unlike with deps, the headers and include paths of these libraries (and all their transitive deps) are only used for compilation of this library, and not libraries that depend on it.
  :param include_prefix: The prefix to add to the paths of the headers of this rule.
  :param includes: List of include dirs to be added to the compile line. Subject to "Make variable" substitution.
  :param licenses: A list of license-type strings to be used for this particular target. This is part of a deprecated licensing API that Bazel no longer uses. Don't use this.
  :param linkopts: See cc_binary.linkopts. The linkopts attribute is also applied to any target that depends, directly or indirectly, on this library via deps attributes (or via other attributes that are treated similarly: the malloc attribute of cc_binary).
  :param linkstamp: Simultaneously compiles and links the specified C++ source file into the final binary. This trickery is required to introduce timestamp information into binaries; if we compiled the source file to an object file in the usual way, the timestamp would be incorrect.
  :param linkstatic: If enabled and this is a binary or test, this option tells the build tool to link in .a's instead of .so's for user libraries whenever possible.
  :param local_defines: List of defines to add to the compile line. Subject to "Make" variable substitution and Bourne shell tokenization. Each string,
  :param module_interfaces: The list of files are regarded as C++20 Modules Interface.
  :param restricted_to: The list of environments this target can be built for, instead of default-supported environments.
  :param strip_include_prefix: The prefix to strip from the paths of the headers of this rule.
  :param tags:  Tags can be used on any rule. Tags on test and test_suite rules are useful for categorizing the tests. Tags on non-test targets are used to control sandboxed execution of genrules and Starlark actions, and for parsing by humans and/or external tools.
  :param target_compatible_with: A list of constraint_values that must be present in the target platform for this target to be considered compatible. This is in addition to any constraints already set by the rule type. If the target platform does not satisfy all listed constraints then the target is considered incompatible.
  :param testonly: If True, only testonly targets (such as tests) can depend on this target.
  :param textual_hdrs: The list of header files published by this library to be textually included by sources in dependent rules.
  :param toolchains: The set of targets whose Make variables this target is allowed to access. These targets are either instances of rules that provide TemplateVariableInfo or special targets for toolchain types built into Bazel.
  :param visibility: The visibility attribute on a target controls whether the target can be used in other packages.
  :param win_def_file: The Windows DEF file to be passed to linker.
  :return:
  """
  pass
