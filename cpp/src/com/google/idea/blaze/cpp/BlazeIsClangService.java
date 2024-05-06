package com.google.idea.blaze.cpp;

import com.google.common.collect.ImmutableMap;
import com.google.idea.blaze.base.model.primitives.Label;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Property;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

@State(name = "BlazeCompilerInfoMap", storages = @Storage(value = "blazeInfo.xml", roamingType = RoamingType.DISABLED))
final public class BlazeIsClangService implements PersistentStateComponent<BlazeIsClangService> {

  private final static Logger logger = Logger.getInstance(BlazeIsClangService.class);

  // State cannot be an ImmutableMap, because the IDE uses Map.clear() internally,
  // which ImmutableMap doesn't support. Annotate with property to be saved using
  // PersistentStateComponent interface.
  @NotNull
  @Property
  Map<String, Boolean> targetMap = new HashMap<>();

  @Override
  public BlazeIsClangService getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull BlazeIsClangService state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @NotNull
  public static Boolean isClangTarget(Project project, Label label) {
    final Map<String, Boolean> targetMap = project.getService(BlazeIsClangService.class).targetMap;
    final Boolean result = targetMap.get(label.toString());

    if (result == null) {
      logger.warn(String.format("Could not find target %s", label));
      return false;
    }

    return result;
  }

  public static void update(Project project, ImmutableMap<String, Boolean> targetMap) {
    project.getService(BlazeIsClangService.class).targetMap =
        Collections.checkedMap(targetMap, String.class, Boolean.class);
  }
}
