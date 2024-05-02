/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.aspect.cpp.custom_toolchain;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Lists;
import com.google.devtools.intellij.IntellijAspectTestFixtureOuterClass.IntellijAspectTestFixture;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CToolchainIdeInfo;
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo;
import com.google.idea.blaze.BazelIntellijAspectTest;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests cc_toolchain and cc_toolchain_suite */
@RunWith(JUnit4.class)
public class CcCustomToolchainTest extends BazelIntellijAspectTest {

  @Test
  public void testCustomToolchain() throws Exception {
    IntellijAspectTestFixture testFixture = loadTestFixture(":fixture");
    List<TargetIdeInfo> toolchains = findToolchainTarget(testFixture);

    for (TargetIdeInfo toolchain : toolchains) {
      CToolchainIdeInfo toolchainInfo = toolchain.getCToolchainIdeInfo();
      assertThat(toolchainInfo.getCCompiler()).isEqualTo("/path/to/c/compiler");
      assertThat(toolchainInfo.getCppCompiler()).isEqualTo("/path/to/cpp/compiler");
    }
  }

  private static List<TargetIdeInfo> findToolchainTarget(IntellijAspectTestFixture testFixture) {
    List<TargetIdeInfo> result = Lists.newArrayList();
    for (TargetIdeInfo target : testFixture.getTargetsList()) {
      if (target.hasCToolchainIdeInfo()) {
        result.add(target);
      }
    }
    return result;
  }
}
