/*
 * Copyright 2026 The Bazel Authors. All rights reserved.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.idea.blaze.base.command.buildresult.BuildResult;
import com.google.idea.blaze.base.sync.aspects.BlazeBuildOutputs;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for the {@code externalBuildOutputs} field of {@link BlazeSyncParams}, which carries
 * pre-parsed outputs into the build phase for {@link SyncMode#REACTIVE}.
 */
@RunWith(JUnit4.class)
public class BlazeSyncParamsExternalOutputsTest {

  @Test
  public void externalBuildOutputs_defaultsToNull() {
    BlazeSyncParams params =
        BlazeSyncParams.builder()
            .setTitle("Sync")
            .setSyncMode(SyncMode.INCREMENTAL)
            .setSyncOrigin("test")
            .build();
    assertThat(params.externalBuildOutputs()).isNull();
  }

  @Test
  public void externalBuildOutputs_roundTripsThroughBuilder() {
    BlazeBuildOutputs outputs = BlazeBuildOutputs.noOutputsForTesting("build-id", BuildResult.SUCCESS);
    BlazeSyncParams params =
        BlazeSyncParams.builder()
            .setTitle("Reactive Sync")
            .setSyncMode(SyncMode.REACTIVE)
            .setSyncOrigin("ReactiveSync")
            .setExternalBuildOutputs(outputs)
            .build();
    assertThat(params.syncMode()).isEqualTo(SyncMode.REACTIVE);
    assertThat(params.externalBuildOutputs()).isSameInstanceAs(outputs);
  }

  @Test
  public void combine_carriesExternalBuildOutputs_regardlessOfOrder() {
    BlazeBuildOutputs outputs = BlazeBuildOutputs.noOutputsForTesting("build-id", BuildResult.SUCCESS);
    BlazeSyncParams reactive =
        BlazeSyncParams.builder()
            .setTitle("Reactive Sync")
            .setSyncMode(SyncMode.REACTIVE)
            .setSyncOrigin("ReactiveSync")
            .setBackgroundSync(true)
            .setExternalBuildOutputs(outputs)
            .build();
    BlazeSyncParams other =
        BlazeSyncParams.builder()
            .setTitle("Sync")
            .setSyncMode(SyncMode.INCREMENTAL)
            .setSyncOrigin("test")
            .setBackgroundSync(true)
            .build();

    assertThat(BlazeSyncParams.combine(reactive, other).externalBuildOutputs())
        .isSameInstanceAs(outputs);
    assertThat(BlazeSyncParams.combine(other, reactive).externalBuildOutputs())
        .isSameInstanceAs(outputs);
  }
}
