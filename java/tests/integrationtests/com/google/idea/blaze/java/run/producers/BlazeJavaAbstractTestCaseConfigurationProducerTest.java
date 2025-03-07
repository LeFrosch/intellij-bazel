/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.java.run.producers;

import static com.google.common.truth.Truth.assertThat;

import com.google.idea.blaze.base.command.BlazeFlags;
import com.google.idea.blaze.base.ideinfo.TargetIdeInfo;
import com.google.idea.blaze.base.ideinfo.TargetMapBuilder;
import com.google.idea.blaze.base.lang.buildfile.psi.util.PsiUtils;
import com.google.idea.blaze.base.model.MockBlazeProjectDataBuilder;
import com.google.idea.blaze.base.model.MockBlazeProjectDataManager;
import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.google.idea.blaze.base.run.BlazeCommandRunConfiguration;
import com.google.idea.blaze.base.sync.data.BlazeProjectDataManager;
import com.google.idea.blaze.java.run.producers.BlazeJUnitTestFilterFlags.JUnitVersion;
import com.google.idea.blaze.java.utils.BlazeJUnitRunConfigurationProducerTestCase;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Integration tests for {@link BlazeJavaAbstractTestCaseConfigurationProducer}.
 *  Parameters are provided by the base class.
 */
@RunWith(Parameterized.class)
public class BlazeJavaAbstractTestCaseConfigurationProducerTest
    extends BlazeJUnitRunConfigurationProducerTestCase {

  @Test
  public void testIgnoreTestClassWithNoTestSubclasses() throws Throwable {
    PsiFile javaFile = createAndIndexGenericJUnitTestFile();

    PsiClass javaClass = ((PsiClassOwner) javaFile).getClasses()[0];
    assertThat(javaClass).isNotNull();

    ConfigurationContext context = createContextFromPsi(javaClass);
    ConfigurationFromContext fromContext =
        new BlazeJavaAbstractTestCaseConfigurationProducer()
            .createConfigurationFromContext(context);
    assertThat(fromContext).isNull();
  }

  @Test
  public void testIgnoreAbstractTestClassWithNoTestSubclasses() throws Throwable {
    PsiFile javaFile = createAndIndexGenericJUnitTestFile();

    PsiClass javaClass = ((PsiClassOwner) javaFile).getClasses()[0];
    assertThat(javaClass).isNotNull();

    ConfigurationContext context = createContextFromPsi(javaClass);
    ConfigurationFromContext fromContext =
        new BlazeJavaAbstractTestCaseConfigurationProducer()
            .createConfigurationFromContext(context);
    assertThat(fromContext).isNull();
  }

  @Test
  public void testHandlesNonAbstractClassWithTestSubclass() throws Throwable {
    workspace.createPsiDirectory(new WorkspacePath("java/com/google/test"));
    PsiFile superClassFile =
        createAndIndexFile(
            new WorkspacePath("java/com/google/test/NonAbstractSuperClassTestCase.java"),
            "package com.google.test;",
            "@org.junit.runner.RunWith(org.junit.runners.JUnit4.class)",
            "public class NonAbstractSuperClassTestCase {",
            "  @org.junit.Test",
            "  public void testMethod() {}",
            "}");

    createAndIndexFile(
        new WorkspacePath("java/com/google/test/TestClass.java"),
        "package com.google.test;",
        "import com.google.test.NonAbstractSuperClassTestCase;",
        "@org.junit.runner.RunWith(org.junit.runners.JUnit4.class)",
        "public class TestClass extends NonAbstractSuperClassTestCase {",
        "  @org.junit.Test",
        "  public void anotherTestMethod() {}",
        "}");

    PsiClass javaClass = ((PsiClassOwner) superClassFile).getClasses()[0];
    assertThat(javaClass).isNotNull();

    ConfigurationContext context = createContextFromPsi(superClassFile);
    List<ConfigurationFromContext> configurations = context.getConfigurationsFromContext();
    assertThat(configurations).hasSize(1);

    ConfigurationFromContext fromContext = configurations.get(0);
    assertThat(fromContext.isProducedBy(BlazeJavaAbstractTestCaseConfigurationProducer.class))
        .isTrue();
    assertThat(fromContext.getSourceElement()).isEqualTo(javaClass);

    RunConfiguration config = fromContext.getConfiguration();
    assertThat(config).isInstanceOf(BlazeCommandRunConfiguration.class);
    BlazeCommandRunConfiguration blazeConfig = (BlazeCommandRunConfiguration) config;
    assertThat(blazeConfig.getTargets()).isEmpty();
    assertThat(blazeConfig.getName())
        .isEqualTo("Choose subclass for NonAbstractSuperClassTestCase");
  }

  @Test
  public void testConfigurationCreatedFromAbstractClass() throws Throwable {
    workspace.createPsiDirectory(new WorkspacePath("java/com/google/test"));
    PsiFile abstractClassFile =
        createAndIndexFile(
            new WorkspacePath("java/com/google/test/AbstractTestCase.java"),
            "package com.google.test;",
            "public abstract class AbstractTestCase {}");

    if (jUnitVersionUnderTest == JUnitVersion.JUNIT_5) {
      createAndIndexFile(
          new WorkspacePath("java/com/google/test/TestClass.java"),
          "package com.google.test;",
          "import com.google.test.AbstractTestCase;",
          "@org.junit.platform.commons.annotation.Testable",
          "public class TestClass extends AbstractTestCase {",
          "  @org.junit.jupiter.api.Test",
          "  public void testMethod1() {}",
          "  @org.junit.jupiter.api.Test",
          "  public void testMethod2() {}",
          "}");
    } else if (jUnitVersionUnderTest == JUnitVersion.JUNIT_4) {
      createAndIndexFile(
          new WorkspacePath("java/com/google/test/TestClass.java"),
          "package com.google.test;",
          "import com.google.test.AbstractTestCase;",
          "@org.junit.runner.RunWith(org.junit.runners.JUnit4.class)",
          "public class TestClass extends AbstractTestCase {",
          "  @org.junit.Test",
          "  public void testMethod1() {}",
          "  @org.junit.Test",
          "  public void testMethod2() {}",
          "}");
    } else {
      throw new RuntimeException("JUnit Version (" + jUnitVersionUnderTest.toString() + ") should never be tested!");
    }

    PsiClass javaClass = ((PsiClassOwner) abstractClassFile).getClasses()[0];
    assertThat(javaClass).isNotNull();

    ConfigurationContext context = createContextFromPsi(abstractClassFile);
    List<ConfigurationFromContext> configurations = context.getConfigurationsFromContext();
    assertThat(configurations).hasSize(1);

    ConfigurationFromContext fromContext = configurations.get(0);
    assertThat(fromContext.isProducedBy(BlazeJavaAbstractTestCaseConfigurationProducer.class))
        .isTrue();
    assertThat(fromContext.getSourceElement()).isEqualTo(javaClass);

    RunConfiguration config = fromContext.getConfiguration();
    assertThat(config).isInstanceOf(BlazeCommandRunConfiguration.class);
    BlazeCommandRunConfiguration blazeConfig = (BlazeCommandRunConfiguration) config;
    assertThat(blazeConfig.getTargets()).isEmpty();
    assertThat(blazeConfig.getName()).isEqualTo("Choose subclass for AbstractTestCase");

    MockBlazeProjectDataBuilder builder = MockBlazeProjectDataBuilder.builder(workspaceRoot);
    builder.setTargetMap(
        TargetMapBuilder.builder()
            .addTarget(
                TargetIdeInfo.builder()
                    .setKind("java_test")
                    .setLabel("//java/com/google/test:TestClass")
                    .addSource(sourceRoot("java/com/google/test/TestClass.java"))
                    .build())
            .build());
    registerProjectService(
        BlazeProjectDataManager.class, new MockBlazeProjectDataManager(builder.build()));

    BlazeJavaAbstractTestCaseConfigurationProducer.chooseSubclass(
        fromContext, context, EmptyRunnable.INSTANCE);

    assertThat(blazeConfig.getTargets())
        .containsExactly(TargetExpression.fromStringSafe("//java/com/google/test:TestClass"));
    String junit4Hash = (jUnitVersionUnderTest == JUnitVersion.JUNIT_4 ? "#" : "");
    assertThat(getTestFilterContents(blazeConfig))
        .isEqualTo(BlazeFlags.TEST_FILTER + "=com.google.test.TestClass" + junit4Hash);
  }

  @Test
  public void testConfigurationCreatedFromMethodInAbstractClass() throws Throwable {
    PsiFile abstractClassFile = null;
    if (jUnitVersionUnderTest == JUnitVersion.JUNIT_5) {
      abstractClassFile = createAndIndexFile(
              new WorkspacePath("java/com/google/test/AbstractTestCase.java"),
              "package com.google.test;",
              "public abstract class AbstractTestCase {",
              "  @org.junit.jupiter.api.Test",
              "  public void testMethod() {}",
              "}");
      createAndIndexFile(
          new WorkspacePath("java/com/google/test/TestClass.java"),
          "package com.google.test;",
          "import com.google.test.AbstractTestCase;",
          "@org.junit.platform.commons.annotation.Testable",
          "public class TestClass extends AbstractTestCase {}");
    } else if (jUnitVersionUnderTest == JUnitVersion.JUNIT_4) {
      abstractClassFile = createAndIndexFile(
          new WorkspacePath("java/com/google/test/AbstractTestCase.java"),
          "package com.google.test;",
          "public abstract class AbstractTestCase {",
          "  @org.junit.Test",
          "  public void testMethod() {}",
          "}");
      createAndIndexFile(
          new WorkspacePath("java/com/google/test/TestClass.java"),
          "package com.google.test;",
          "import com.google.test.AbstractTestCase;",
          "import org.junit.runner.RunWith;",
          "import org.junit.runners.JUnit4;",
          "@org.junit.runner.RunWith(org.junit.runners.JUnit4.class)",
          "public class TestClass extends AbstractTestCase {}");
    } else {
      throw new RuntimeException("JUnit Version (" + jUnitVersionUnderTest.toString() + ") should never be tested!");
    }

    setUpRepositoryAndTarget();

    PsiClass javaClass = ((PsiClassOwner) abstractClassFile).getClasses()[0];
    PsiMethod method = PsiUtils.findFirstChildOfClassRecursive(javaClass, PsiMethod.class);
    assertThat(method).isNotNull();

    ConfigurationContext context = createContextFromPsi(method);
    List<ConfigurationFromContext> configurations = context.getConfigurationsFromContext();
    assertThat(configurations).hasSize(1);

    ConfigurationFromContext fromContext = configurations.get(0);
    assertThat(fromContext.isProducedBy(BlazeJavaAbstractTestCaseConfigurationProducer.class))
        .isTrue();
    assertThat(fromContext.getSourceElement()).isEqualTo(method);

    RunConfiguration config = fromContext.getConfiguration();
    assertThat(config).isInstanceOf(BlazeCommandRunConfiguration.class);
    BlazeCommandRunConfiguration blazeConfig = (BlazeCommandRunConfiguration) config;
    assertThat(blazeConfig.getTargets()).isEmpty();
    assertThat(blazeConfig.getName()).isEqualTo("Choose subclass for AbstractTestCase.testMethod");

    BlazeJavaAbstractTestCaseConfigurationProducer.chooseSubclass(
        fromContext, context, EmptyRunnable.INSTANCE);

    assertThat(blazeConfig.getTargets())
        .containsExactly(TargetExpression.fromStringSafe("//java/com/google/test:TestClass"));
    String junit4Dollar = (jUnitVersionUnderTest == JUnitVersion.JUNIT_4 ? "$" : "");
    assertThat(getTestFilterContents(blazeConfig))
        .isEqualTo(BlazeFlags.TEST_FILTER + "=com.google.test.TestClass#testMethod" + junit4Dollar);
  }

  @Test
  public void testConfigurationCreatedFromIdentifierOfMethodInAbstractClass() throws Throwable {
    PsiFile abstractClassFile =
            createAndIndexFile(
                    new WorkspacePath("java/com/google/test/AbstractTestCase.java"),
                    "package com.google.test;",
                    "public abstract class AbstractTestCase {",
                    "  @org.junit.Test",
                    "  public void testMethod() {}",
                    "}");

    createAndIndexFile(
            new WorkspacePath("java/com/google/test/TestClass.java"),
            "package com.google.test;",
            "import com.google.test.AbstractTestCase;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@org.junit.runner.RunWith(org.junit.runners.JUnit4.class)",
            "public class TestClass extends AbstractTestCase {}");

    setUpRepositoryAndTarget();

    PsiClass javaClass = ((PsiClassOwner) abstractClassFile).getClasses()[0];
    PsiMethod method = PsiUtils.findFirstChildOfClassRecursive(javaClass, PsiMethod.class);
    assertThat(method).isNotNull();

    PsiElement identifyingElement = method.getIdentifyingElement();
    assertThat(identifyingElement).isNotNull();
    ConfigurationContext context = createContextFromPsi(identifyingElement);
    List<ConfigurationFromContext> configurations = context.getConfigurationsFromContext();
    assertThat(configurations).hasSize(1);

    ConfigurationFromContext fromContext = configurations.get(0);
    assertThat(fromContext.isProducedBy(BlazeJavaAbstractTestCaseConfigurationProducer.class))
            .isTrue();
    assertThat(fromContext.getSourceElement()).isEqualTo(method);

    RunConfiguration config = fromContext.getConfiguration();
    assertThat(config).isInstanceOf(BlazeCommandRunConfiguration.class);
    BlazeCommandRunConfiguration blazeConfig = (BlazeCommandRunConfiguration) config;
    assertThat(blazeConfig.getTargets()).isEmpty();
    assertThat(blazeConfig.getName()).isEqualTo("Choose subclass for AbstractTestCase.testMethod");


    BlazeJavaAbstractTestCaseConfigurationProducer.chooseSubclass(
            fromContext, context, EmptyRunnable.INSTANCE);

    assertThat(blazeConfig.getTargets())
            .containsExactly(TargetExpression.fromStringSafe("//java/com/google/test:TestClass"));
    assertThat(getTestFilterContents(blazeConfig))
            .isEqualTo(BlazeFlags.TEST_FILTER + "=com.google.test.TestClass#testMethod$");
  }
}
