load("@rules_java//java:defs.bzl", "java_library")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load(
    "//:build-visibility.bzl",
    "DEFAULT_TEST_VISIBILITY",
    "G3PLUGINS_VISIBILITY",
    "INTELLIJ_PLUGINS_VISIBILITY",
    "PLUGIN_PACKAGES_VISIBILITY",
)
load(
    "//build_defs:build_defs.bzl",
    "intellij_plugin",
    "intellij_plugin_library",
    "stamped_plugin_xml",
)
load("//intellij_platform_sdk:build_defs.bzl", "java_version_flags")
load(
    "//testing:test_defs.bzl",
    "intellij_integration_test_suite",
    "intellij_unit_test_suite",
)

kt_jvm_library(
    name = "base",
    srcs = glob([
        "src/**/*.java",
        "src/**/*.kt",
    ]),
    resource_strip_prefix = "base/src",
    resources = glob(["src/resources/**/*"]),
    visibility = PLUGIN_PACKAGES_VISIBILITY,
    deps = [
        "//aspect:aspect_lib",
        "//aspect:aspect_qsync_lib",
        "//aspect:aspect_template_lib",
        "//base/src/com/google/idea/blaze/base/command/buildresult/bepparser",
        "//common/actions",
        "//common/experiments",
        "//common/settings",
        "//common/ui/properties",
        "//common/util:concurrency",
        "//common/util:platform",
        "//common/util:process",
        "//common/util:transactions",
        "//intellij_platform_sdk:jsr305",  # unuseddeps: keep for @Nullable
        "//intellij_platform_sdk:plugin_api",
        "//proto:proto_deps",
        "//querysync",
        "//sdkcompat",
        "//shared",
        "//shared:artifact",
        "//shared:exception",
        "//shared:proto",
        "//shared:vcs",
        "//third_party/java/auto_value",
        "@error_prone_annotations//jar",
        "@gson//jar",
    ],
)

intellij_plugin_library(
    name = "plugin_library",
    plugin_xmls = [
        "src/META-INF/dependency-on-lvcs-impl.xml",
        "src/META-INF/blaze-base.xml",
    ],
    visibility = PLUGIN_PACKAGES_VISIBILITY,
    deps = [":base"],
)

java_library(
    name = "proto_wrapper",
    srcs = [
        "src/com/google/idea/blaze/base/ideinfo/ProjectDataInterner.java",
        "src/com/google/idea/blaze/base/ideinfo/ProtoWrapper.java",
    ],
    neverlink = 1,
    visibility = ["//visibility:private"],
    deps = [
        ":base",
        "//common/experiments",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//proto:proto_deps",
    ],
)

# self-contained custom formatter API, exposed so other plugins can optionally depend on it
java_library(
    name = "formatter_api",
    srcs = glob(["src/com/google/idea/blaze/base/formatter/*.java"]),
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//sdkcompat",
    ],
)

# formatter_api for tests.
java_library(
    name = "formatter_api_for_test",
    testonly = True,
    srcs = glob(["src/com/google/idea/blaze/base/formatter/*.java"]),
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        "//intellij_platform_sdk:jsr305",  # unuseddeps: keep (b/146560321)
        "//intellij_platform_sdk:plugin_api",
    ],
)

# self-contained label parsing API, exposed so other plugins can optionally depend on it
java_library(
    name = "label_api",
    srcs = [
        "src/com/google/idea/blaze/base/ideinfo/ArtifactLocation.java",
        "src/com/google/idea/blaze/base/ideinfo/Tags.java",
        "src/com/google/idea/blaze/base/model/primitives/InvalidTargetException.java",
        "src/com/google/idea/blaze/base/model/primitives/Kind.java",
        "src/com/google/idea/blaze/base/model/primitives/Label.java",
        "src/com/google/idea/blaze/base/model/primitives/LanguageClass.java",
        "src/com/google/idea/blaze/base/model/primitives/PackagePathValidator.java",
        "src/com/google/idea/blaze/base/model/primitives/RuleType.java",
        "src/com/google/idea/blaze/base/model/primitives/TargetExpression.java",
        "src/com/google/idea/blaze/base/model/primitives/TargetName.java",
        "src/com/google/idea/blaze/base/model/primitives/WildcardTargetPattern.java",
        "src/com/google/idea/blaze/base/model/primitives/WorkspacePath.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        ":proto_wrapper",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//proto:proto_deps",
        "//shared",
        "//third_party/java/auto_value",
        "@error_prone_annotations//jar",
    ],
)

# self-contained sync listener API, exposed so other plugins can optionally depend on it
java_library(
    name = "sync_listener_api",
    srcs = [
        "src/com/google/idea/blaze/base/sync/SimpleSyncListener.java",
        "src/com/google/idea/blaze/base/sync/SyncMode.java",
        "src/com/google/idea/blaze/base/sync/SyncResult.java",
    ],
    neverlink = 1,
    visibility = INTELLIJ_PLUGINS_VISIBILITY,
    deps = [
        "//intellij_platform_sdk:plugin_api",
    ],
)

# self-contained VCS state listener API, exposed so other plugins can optionally depend on it
java_library(
    name = "vcs_sync_listener_api",
    srcs = [
        "src/com/google/idea/blaze/base/vcs/VcsSyncListener.java",
    ],
    neverlink = 1,
    visibility = INTELLIJ_PLUGINS_VISIBILITY,
    deps = [
        "//intellij_platform_sdk:plugin_api",
    ],
)

# neverlink library exposed so other plugins can optionally depend on it
java_library(
    name = "vfs_api",
    srcs = [
        "src/com/google/idea/blaze/base/io/VfsUtils.java",
        "src/com/google/idea/blaze/base/io/VirtualFileSystemProvider.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
    ],
)

# neverlink library exposed so other plugins can optionally depend on it
java_library(
    name = "absolute_path_patcher_api",
    srcs = [
        "src/com/google/idea/blaze/base/io/AbsolutePathPatcher.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        ":vfs_api",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
    ],
)

# neverlink library exposed so other plugins can optionally depend on it
java_library(
    name = "workspace_path_resolver_api",
    srcs = [
        "src/com/google/idea/blaze/base/model/primitives/WorkspacePath.java",
        "src/com/google/idea/blaze/base/model/primitives/WorkspaceRoot.java",
        "src/com/google/idea/blaze/base/settings/BlazeImportSettings.java",
        "src/com/google/idea/blaze/base/settings/BlazeImportSettingsManager.java",
        "src/com/google/idea/blaze/base/settings/BuildSystemName.java",
        "src/com/google/idea/blaze/base/sync/workspace/WorkspacePathResolver.java",
        "src/com/google/idea/blaze/base/sync/workspace/WorkspacePathResolverProvider.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        ":proto_wrapper",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//proto:proto_deps",
    ],
)

# neverlink library exposed so other plugins can optionally depend on it
java_library(
    name = "workspace_file_finder_api",
    srcs = [
        "src/com/google/idea/blaze/base/sync/projectview/WorkspaceFileFinder.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
    ],
)

# neverlink library exposed so other plugins can optionally depend on it
java_library(
    name = "workspace_language_checker_api",
    srcs = [
        "src/com/google/idea/blaze/base/ideinfo/Tags.java",
        "src/com/google/idea/blaze/base/model/primitives/LanguageClass.java",
        "src/com/google/idea/blaze/base/sync/projectview/WorkspaceLanguageChecker.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        ":proto_wrapper",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
    ],
)

# neverlink library exposed so other plugins can optionally depend on it
java_library(
    name = "logging_api",
    srcs = [
        "src/com/google/idea/blaze/base/logging/EventLogger.java",
        "src/com/google/idea/blaze/base/logging/LoggedDirectoryProvider.java",
        "src/com/google/idea/blaze/base/logging/LoggedSettingsProvider.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        "//common/experiments",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//third_party/java/auto_value",
    ],
)

# neverlink library exposed so other plugins can optionally depend on it
java_library(
    name = "library_to_target_api",
    srcs = [
        "src/com/google/idea/blaze/base/model/LegacyLibraryToTargetResolver.java",
        "src/com/google/idea/blaze/base/model/LibraryKey.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        ":label_api",
        ":proto_wrapper",
        "//intellij_platform_sdk:jsr305",  # unuseddeps: keep for @Immutable
        "//intellij_platform_sdk:plugin_api",
    ],
)

# neverlink library exposed so other plugins can optionally depend on it
java_library(
    name = "blaze_user_settings_api",
    srcs = [
        "src/com/google/idea/blaze/base/settings/BlazeUserSettings.java",
    ],
    neverlink = 1,
    deps = [
        ":logging_api",
        "//intellij_platform_sdk:jsr305",  # unuseddeps: keep for @Nullable
        "//intellij_platform_sdk:plugin_api",
    ],
)

# neverlink library exposed so other plugins can provide dependency information
java_library(
    name = "dep_finder_api",
    srcs = [
        "src/com/google/idea/blaze/base/dependencies/DependencyFinder.java",
        "src/com/google/idea/blaze/base/dependencies/SourceToTargetProvider.java",
        "src/com/google/idea/blaze/base/dependencies/TargetInfo.java",
        "src/com/google/idea/blaze/base/dependencies/TestSize.java",
        "src/com/google/idea/blaze/base/run/SourceToTargetFinder.java",
        "src/com/google/idea/blaze/base/run/targetfinder/FuturesUtil.java",
        "src/com/google/idea/blaze/base/run/targetfinder/TargetFinder.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        ":base",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//proto:proto_deps",
        "//shared",
        "@error_prone_annotations//jar",
    ],
)

# neverlink library exposed so other plugins can provide dependency information
java_library(
    name = "build_batching_service_api",
    srcs = [
        "src/com/google/idea/blaze/base/bazel/BuildSystem.java",
        "src/com/google/idea/blaze/base/logging/utils/ShardStats.java",
        "src/com/google/idea/blaze/base/sync/sharding/BuildBatchingService.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    exports = [
        ":label_api",
    ],
    deps = [
        ":base",
        "//base/src/com/google/idea/blaze/base/command/buildresult/bepparser",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//shared/java/com/google/idea/blaze/exception",
        "//third_party/java/auto_value",
        "@error_prone_annotations//jar",
    ],
)

# neverlink library exposed so other plugins can optionally depend on it
java_library(
    name = "blaze_sync_modification_tracker_api",
    srcs = ["src/com/google/idea/blaze/base/sync/BlazeSyncModificationTracker.java"],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        ":base",
        "//intellij_platform_sdk:plugin_api",
    ],
)

# neverlink library exposed so other plugins can optionally depend on it
java_library(
    name = "buildifier_binary_provider_api",
    srcs = [
        "src/com/google/idea/blaze/base/buildmodifier/BuildifierBinaryProvider.java",
    ],
    neverlink = 1,
    deps = [
        "//intellij_platform_sdk:jsr305",  # unuseddeps: keep for @Nullable
        "//intellij_platform_sdk:plugin_api",
    ],
)

# exposed so g3plugins tests can optionally depend on it
java_library(
    name = "remote_file_prefetcher_for_tests",
    testonly = 1,
    srcs = [
        "src/com/google/idea/blaze/base/async/FutureUtil.java",
        "src/com/google/idea/blaze/base/command/buildresult/RemoteOutputArtifact.java",
        "src/com/google/idea/blaze/base/ideinfo/ProtoWrapper.java",
        "src/com/google/idea/blaze/base/prefetch/DefaultPrefetcher.java",
        "src/com/google/idea/blaze/base/prefetch/FetchExecutor.java",
        "src/com/google/idea/blaze/base/prefetch/RemoteArtifactPrefetcher.java",
    ],
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        ":base",
        "//base/src/com/google/idea/blaze/base/command/buildresult/bepparser",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//proto:proto_deps",
        "//shared:artifact",
        "@error_prone_annotations//jar",
    ],
)

java_library(
    name = "file_size_scanner",
    srcs = [
        "src/com/google/idea/blaze/base/io/FileSizeScanner.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        ":base",
        "//intellij_platform_sdk:plugin_api",
    ],
)

java_library(
    name = "remote_file_prefetcher",
    srcs = [
        "src/com/google/idea/blaze/base/async/FutureUtil.java",
        "src/com/google/idea/blaze/base/command/buildresult/RemoteOutputArtifact.java",
        "src/com/google/idea/blaze/base/prefetch/DefaultPrefetcher.java",
        "src/com/google/idea/blaze/base/prefetch/FetchExecutor.java",
        "src/com/google/idea/blaze/base/prefetch/RemoteArtifactPrefetcher.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        ":base",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//proto:proto_deps",
        "//shared:artifact",
        "@error_prone_annotations//jar",
    ],
)

java_library(
    name = "artifact_fetcher",
    srcs = [
        "src/com/google/idea/blaze/base/command/buildresult/LocalFileOutputArtifact.java",
        "src/com/google/idea/blaze/base/command/buildresult/LocalFileOutputArtifactWithoutDigest.java",
        "src/com/google/idea/blaze/base/qsync/cache/ArtifactFetchers.java",
        "src/com/google/idea/blaze/base/qsync/cache/FileApiArtifactFetcher.java",
        "src/com/google/idea/blaze/base/settings/BuildBinaryType.java",
    ],
    neverlink = 1,
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        ":base",
        "//common/experiments",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//shared",
        "//shared:artifact",
        "@error_prone_annotations//jar",
    ],
)

java_library(
    name = "blaze_executor",
    srcs = [
        "src/com/google/idea/blaze/base/async/executor/BlazeExecutor.java",
    ],
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        "//intellij_platform_sdk:plugin_api",
        "@com_google_guava_guava//jar",
    ],
)

java_library(
    name = "version_checker",
    srcs = [
        "src/com/google/idea/blaze/base/util/VersionChecker.java",
    ],
    visibility = G3PLUGINS_VISIBILITY,
    deps = [
        "//intellij_platform_sdk:plugin_api",
    ],
)

java_library(
    name = "unit_test_utils",
    testonly = 1,
    srcs = glob(["tests/utils/unit/**/*.java"]),
    visibility = PLUGIN_PACKAGES_VISIBILITY,
    deps = [
        ":base",
        "//base/src/com/google/idea/blaze/base/command/buildresult/bepparser",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api_for_tests",
        "//intellij_platform_sdk:test_libs",
        "//proto:proto_deps",
        "//shared:artifact",
        "//shared:exception",
        "//shared/java/com/google/idea/blaze/common",
        "//testing:lib",
        "//third_party/java/auto_value",
        "//third_party/java/junit",
        "@com_google_guava_guava//jar",
        "@error_prone_annotations//jar",
    ],
)

java_library(
    name = "integration_test_utils",
    testonly = 1,
    srcs = glob(["tests/utils/integration/**/*.java"]),
    data = [
        "//base/tests/utils/testdata/java/com/test",
        "//base/tests/utils/testdata/java/com/test:test_srcs",
        "//base/tests/utils/testdata/java/com/test2",
        "//base/tests/utils/testdata/java/com/test2:test2_srcs",
    ],
    javacopts = java_version_flags(),
    visibility = PLUGIN_PACKAGES_VISIBILITY,
    deps = [
        ":base",
        ":unit_test_utils",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api_for_tests",
        "//intellij_platform_sdk:test_libs",
        "//proto:proto_deps",
        "//sdkcompat",
        "//shared:artifact",
        "//shared:vcs",
        "//testing:lib",
        "//third_party/java/junit",
        "@bazel_tools//tools/java/runfiles",
        "@com_google_guava_guava//jar",
    ],
)

intellij_unit_test_suite(
    name = "unit_tests",
    srcs = glob(["tests/unittests/**/*.java"]),
    tags = ["noci:studio-win"],
    test_package_root = "com.google.idea.blaze.base",
    visibility = DEFAULT_TEST_VISIBILITY,
    deps = [
        ":base",
        ":unit_test_utils",
        "//base/src/com/google/idea/blaze/base/command/buildresult/bepparser",
        "//common/experiments",
        "//common/experiments:unit_test_utils",
        "//common/util:process",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api_for_tests",
        "//intellij_platform_sdk:test_libs",
        "//proto:proto_deps",
        "//querysync",
        "//querysync/javatests/com/google/idea/blaze/qsync:TestUtils",
        "//querysync/javatests/com/google/idea/blaze/qsync/testdata",
        "//shared",
        "//shared:artifact",
        "//shared/java/com/google/idea/blaze/common",
        "//shared/javatests/com/google/idea/blaze/common:test_utils",
        "//shared/javatests/com/google/idea/blaze/common/artifact:test_utils",
        "//testing:lib",
        "//third_party/java/auto_value",
        "//third_party/java/junit",
        "@com_google_guava_guava//jar",
    ],
)

stamped_plugin_xml(
    name = "base_plugin_xml",
    testonly = 1,
    plugin_id = "com.google.idea.blaze.base",
    plugin_name = "com.google.idea.blaze.base",
)

intellij_plugin(
    name = "base_integration_test_plugin",
    testonly = 1,
    plugin_xml = ":base_plugin_xml",
    deps = [":plugin_library"],
)

intellij_integration_test_suite(
    name = "integration_tests",
    srcs = glob(["tests/integrationtests/**/*.java"]),
    plugins = [":com.google.idea.blaze.base"],
    required_plugins = "com.google.idea.blaze.base",
    tags = ["noci:studio-win"],
    test_package_root = "com.google.idea.blaze.base",
    visibility = DEFAULT_TEST_VISIBILITY,
    runtime_deps = [
        ":base_integration_test_plugin",
    ],
    deps = [
        ":base",
        ":integration_test_utils",
        ":unit_test_utils",
        "//common/experiments",
        "//common/experiments:unit_test_utils",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//intellij_platform_sdk:plugin_api_for_tests",  # unuseddeps: keep
        "//intellij_platform_sdk:test_libs",
        "//proto:proto_deps",
        "//querysync",
        "//querysync/javatests/com/google/idea/blaze/qsync/artifacts:mock_artifact_cache",
        "//shared:exception",
        "//shared/java/com/google/idea/blaze/common",
        "//shared/java/com/google/idea/blaze/common/artifact",
        "//shared/javatests/com/google/idea/blaze/common:test_utils",
        "//third_party/java/auto_value",
        "//third_party/java/junit",
        "@com_google_guava_guava//jar",
        "@error_prone_annotations//jar",
        "@org_opentest4j_opentest4j//jar",
    ],
)
