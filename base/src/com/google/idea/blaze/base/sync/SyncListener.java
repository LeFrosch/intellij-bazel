/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base.sync;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.google.idea.blaze.base.projectview.ProjectViewSet;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.base.settings.BlazeImportSettings;
import com.google.idea.blaze.base.sync.SyncScope.SyncCanceledException;
import com.google.idea.blaze.base.sync.SyncScope.SyncFailedException;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;

/** Extension interface for listening to syncs. */
public interface SyncListener {
  ExtensionPointName<SyncListener> EP_NAME = ExtensionPointName.create("com.google.idea.blaze.SyncListener");

  Topic<SyncListener> TOPIC = Topic.create("Bazel sync events", SyncListener.class);

  /**
   * Called once at the very start of a sync request, before any blaze invocation -- including the
   * force-full-sync language probe that precedes the initial directory update. Paired with {@link
   * #afterSyncFinish}. Unlike the per-pass {@link #onSyncStart}, this fires exactly once per sync
   * request.
   */
  default void beforeSyncStart(Project project, BlazeContext context, SyncMode syncMode) {}

  /**
   * Called exactly once per sync request, after the whole operation completes -- including all
   * internal passes -- regardless of success, failure, or cancellation. Paired with {@link
   * #beforeSyncStart}. Unlike the per-pass {@link #afterSync}, this fires exactly once per sync
   * request, so it is the right place to undo state established in {@link #beforeSyncStart}.
   */
  default void afterSyncFinish(Project project, BlazeContext context, SyncMode syncMode) {}

  /**
   * Called after open documents have been saved, prior to starting the blaze sync.
   *
   * <p><b>May be invoked more than once per user-triggered sync, by design.</b> A foreground sync
   * runs as two internal passes -- a {@link SyncMode#NO_BUILD} "initial directory update" followed
   * by the real build-based sync -- and this hook is dispatched once per pass, because each pass
   * runs blaze and guards it with its own {@code onSyncStart}. Implementations must therefore be
   * idempotent. For a hook guaranteed to run exactly once per sync request (before any blaze
   * invocation), use {@link #beforeSyncStart}.
   */
  default void onSyncStart(Project project, BlazeContext context, SyncMode syncMode)
      throws SyncFailedException, SyncCanceledException {}

  /**
   * Called just prior to starting a blaze build during sync.
   *
   * @param fullProjectSync true if all project targets are being synced.
   * @param buildId a unique ID associated with each sync build.
   */
  default void buildStarted(
      Project project,
      BlazeContext context,
      boolean fullProjectSync,
      int buildId,
      ImmutableList<TargetExpression> targets) {}

  /** Called on successful (or partially successful) completion of a sync */
  default void onSyncComplete(
      Project project,
      BlazeContext context,
      BlazeImportSettings importSettings,
      ProjectViewSet projectViewSet,
      ImmutableSet<Integer> buildIds,
      BlazeProjectData blazeProjectData,
      SyncMode syncMode,
      SyncResult syncResult) {}

  /**
   * Called once per internal sync pass, regardless of whether that pass completed successfully --
   * so it may fire more than once per user-triggered sync (see {@link #onSyncStart}). For a hook
   * guaranteed to run exactly once per sync request, use {@link #afterSyncFinish}.
   */
  default void afterSync(
      Project project,
      BlazeContext context,
      SyncMode syncMode,
      SyncResult syncResult,
      ImmutableSet<Integer> buildIds) {}
}
