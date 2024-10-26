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
package com.google.idea.blaze.base.wizard2.ui;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.idea.blaze.base.project.ExtendableBazelProjectCreator;
import com.google.idea.blaze.base.wizard2.BlazeNewProjectBuilder;
import com.google.idea.blaze.base.wizard2.BlazeProjectCommitException;
import com.google.idea.blaze.base.wizard2.BlazeSelectWorkspaceOption;
import com.google.idea.blaze.base.wizard2.BlazeWizardOptionProvider;
import com.google.idea.blaze.base.wizard2.BlazeWizardUserSettings;
import com.google.idea.blaze.base.wizard2.TopLevelSelectWorkspaceOption;
import com.google.idea.blaze.base.wizard2.WorkspaceTypeList;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.CancelledConfigurationException;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.IdeBorderFactory.PlainSmallWithoutIndent;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import java.awt.CardLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** UI for selecting a client during the import process. */
public class BlazeSelectWorkspaceControl {

  private final BlazeNewProjectBuilder builder;
  private final BlazeWizardUserSettings userSettings;
  private final ImmutableList<TopLevelSelectWorkspaceOption> availableWorkspaceTypes;

  private final JPanel component;
  private final WorkspaceTypeList workspaceTypeList;
  /** The main options panel for the selected workspace type. */
  private final JPanel cardPanel;

  private final CardLayout cardLayout;

  public BlazeSelectWorkspaceControl(BlazeNewProjectBuilder builder, Disposable parentDisposable) {
    this.builder = builder;
    this.userSettings = builder.getUserSettings();
    availableWorkspaceTypes =
        BlazeWizardOptionProvider.getInstance()
            .getSelectWorkspaceOptions(this.builder, parentDisposable);
    Preconditions.checkState(
        !availableWorkspaceTypes.isEmpty(), "No project workspace types available to be selected.");

    cardLayout = new CardLayout();
    cardPanel = new JPanel(cardLayout);
    cardPanel.putClientProperty("BorderFactoryClass", PlainSmallWithoutIndent.class.getName());
    cardPanel.setOpaque(false);
    availableWorkspaceTypes.forEach(
        type -> cardPanel.add(type.getUiComponent(), type.getOptionName()));
    workspaceTypeList = new WorkspaceTypeList(availableWorkspaceTypes);
    workspaceTypeList.addListSelectionListener(e -> updateSelection());
    component = initPanel();

    selectInitialItem();
  }

  private BlazeSelectWorkspaceOption getSelectedOption() {
    BlazeSelectWorkspaceOption selectedType = workspaceTypeList.getSelectedValue();
    return selectedType != null ? selectedType : availableWorkspaceTypes.get(0);
  }

  private void updateSelection() {
    BlazeSelectWorkspaceOption selectedType = workspaceTypeList.getSelectedValue();
    if (selectedType != null) {
      cardLayout.show(cardPanel, selectedType.getOptionName());
      selectedType.optionSelected();
    }
  }

  private JPanel initPanel() {
    // initially generated by IntelliJ IDEA GUI Designer
    JPanel topLevelPanel = new JPanel(new GridLayoutManager(1, 2, JBUI.emptyInsets(), -1, -1));
    topLevelPanel.add(
        getSideBar(),
        new GridConstraints(
            0,
            0,
            1,
            1,
            GridConstraints.ANCHOR_CENTER,
            GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            null,
            null,
            null,
            0,
            false));
    topLevelPanel.add(
        cardPanel,
        new GridConstraints(
            0,
            1,
            1,
            1,
            GridConstraints.ANCHOR_CENTER,
            GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            null,
            null,
            null,
            0,
            false));
    return topLevelPanel;
  }

  private JPanel getSideBar() {
    // initially generated by IntelliJ IDEA GUI Designer
    JPanel sidebar = new JPanel(new GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1));
    sidebar.putClientProperty("BorderFactoryClass", PlainSmallWithoutIndent.class.getName());
    JBScrollPane scrollPane = new JBScrollPane();
    scrollPane.setViewportView(workspaceTypeList);
    sidebar.add(
        scrollPane,
        new GridConstraints(
            0,
            0,
            1,
            1,
            GridConstraints.ANCHOR_CENTER,
            GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            null,
            null,
            null,
            0,
            false));
    return sidebar;
  }

  public JComponent getUiComponent() {
    return component;
  }

  public void validateAndUpdateBuilder() throws ConfigurationException {
    if (!ExtendableBazelProjectCreator.getInstance()
        .canCreateProject(getSelectedOption().getWorkspaceData().buildSystem())) {
      throw new CancelledConfigurationException();
    }
    getSelectedOption().validateAndUpdateBuilder(builder);
  }

  private static final String OPTION_KEY = "select-workspace.selected-option";

  public void commit() throws BlazeProjectCommitException {
    userSettings.put(OPTION_KEY, getSelectedOption().getOptionName());
    getSelectedOption().commit();
  }

  private void selectInitialItem() {
    // first try to deserialize previous selection
    boolean somethingSelected = initializeSettings();
    if (!somethingSelected) {
      workspaceTypeList.setSelectedIndex(0);
    }
  }

  /** Migrate old settings, apply current settings. */
  private boolean initializeSettings() {
    String selectedOption = userSettings.get(OPTION_KEY, null);
    if (selectedOption == null) {
      return false;
    }
    for (BlazeSelectWorkspaceOption option : availableWorkspaceTypes) {
      if (option.getOptionName().equals(selectedOption)) {
        workspaceTypeList.setSelectedValue(option, false);
        return true;
      }
    }
    return false;
  }
}
