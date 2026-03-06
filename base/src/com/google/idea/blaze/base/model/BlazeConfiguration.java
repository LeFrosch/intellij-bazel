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
package com.google.idea.blaze.base.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.intellij.model.ProjectData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.idea.blaze.base.ideinfo.ProtoWrapper;

/**
 * Represents a Bazel build configuration.
 *
 * <p>Configurations contain platform-specific build information including CPU architecture,
 * platform name, make variables, and whether the configuration is used for building tools vs targets.
 */
@AutoValue
public abstract class BlazeConfiguration implements ProtoWrapper<ProjectData.BlazeConfiguration> {

  public abstract String mnemonic();

  public abstract String platformName();

  public abstract String cpu();

  public abstract ImmutableMap<String, String> makeVariables();

  public abstract boolean isToolConfiguration();

  /** Creates a BlazeConfiguration from a project Configuration proto. */
  public static BlazeConfiguration fromProto(ProjectData.BlazeConfiguration proto) {
    return builder()
        .setMnemonic(proto.getMnemonic())
        .setPlatformName(proto.getPlatformName())
        .setCpu(proto.getCpu())
        .setMakeVariables(ImmutableMap.copyOf(proto.getMakeVariablesMap()))
        .setIsToolConfiguration(proto.getIsTool())
        .build();
  }

  /** Creates a BlazeConfiguration from a BEP Configuration proto. */
  public static BlazeConfiguration fromProto(BuildEventStreamProtos.Configuration proto) {
    return builder()
        .setMnemonic(proto.getMnemonic())
        .setPlatformName(proto.getPlatformName())
        .setCpu(proto.getCpu())
        .setMakeVariables(ImmutableMap.copyOf(proto.getMakeVariableMap()))
        .setIsToolConfiguration(proto.getIsTool())
        .build();
  }

  /** Creates a BlazeConfiguration from a bazel config json entry. */
  public static BlazeConfiguration fromConfigJson(JsonObject json) {
    final var mnemonic = json.has("mnemonic") ? json.get("mnemonic").getAsString() : "";
    final var isExec = json.has("isExec") && json.get("isExec").getAsBoolean();
    final var cpu = extractCpuFromJson(json);

    return builder()
        .setMnemonic(mnemonic)
        .setPlatformName(cpu)
        .setCpu(cpu)
        .setMakeVariables(ImmutableMap.of())
        .setIsToolConfiguration(isExec)
        .build();
  }

  private static String extractCpuFromJson(JsonObject json) {
    if (!json.has("fragmentOptions")) {
      return "";
    }
    JsonArray fragments = json.getAsJsonArray("fragmentOptions");
    for (JsonElement frag : fragments) {
      JsonObject fragObj = frag.getAsJsonObject();
      String name = fragObj.has("name") ? fragObj.get("name").getAsString() : "";
      if (name.contains("CoreOptions")) {
        JsonObject options = fragObj.has("options") ? fragObj.getAsJsonObject("options") : null;
        if (options != null && options.has("cpu")) {
          return options.get("cpu").getAsString();
        }
      }
    }
    return "";
  }

  @Override
  public ProjectData.BlazeConfiguration toProto() {
    return ProjectData.BlazeConfiguration.newBuilder()
        .setMnemonic(mnemonic())
        .setPlatformName(platformName())
        .setCpu(cpu())
        .putAllMakeVariables(makeVariables())
        .setIsTool(isToolConfiguration())
        .build();
  }

  public static Builder builder() {
    return new AutoValue_BlazeConfiguration.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setMnemonic(String mnemonic);

    public abstract Builder setPlatformName(String platformName);

    public abstract Builder setCpu(String cpu);

    public abstract Builder setMakeVariables(ImmutableMap<String, String> makeVariables);

    public abstract Builder setIsToolConfiguration(boolean isToolConfiguration);

    public abstract BlazeConfiguration build();
  }
}
