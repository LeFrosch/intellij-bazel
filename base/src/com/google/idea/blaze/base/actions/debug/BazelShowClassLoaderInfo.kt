/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.base.actions.debug

import com.google.idea.blaze.base.model.BlazeProjectData
import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.project.Project

private val TEST_CLASSES = listOf(
  "java.lang.Exception",
  "com.google.idea.blaze.base.actions.debug.BazelShowClassLoaderInfo",
  "kotlinx.coroutines.guava.ListenableFutureKt",
)

private val LOCATION_RX = "(jar:file:)((/[^!]+)+)!".toPattern()

@Suppress("UnstableApiUsage")
class BazelShowClassLoaderInfo : BazelDebugAction() {

  override suspend fun exec(project: Project, data: BlazeProjectData): String {
    val builder = StringBuilder()

    val classLoader = this.javaClass.classLoader
    if (classLoader !is PluginClassLoader) fail("unexpected classloader: ${classLoader.javaClass.name}")

    builder.appendLine("DESCRIPTOR: ${classLoader.javaClass.name}")
    builder.appendLine()

    builder.appendLine("TEST CLASSES:")
    TEST_CLASSES.forEach { builder.appendLine("  $it: ${tryLoad(classLoader, it)}") }
    builder.appendLine()

    builder.appendLine("PARENTS:")
    classLoader.getAllParentsClassLoaders().filterIsInstance<PluginClassLoader>().forEach {
      builder.appendLine("  ${it.pluginDescriptor}")
    }

    return builder.toString()
  }

  override fun shouldShowOutputInEditor(): Boolean = true
}

@Suppress("UnstableApiUsage")
private fun tryLoad(classLoader: ClassLoader, name: String): String {
  val clazz = try {
    classLoader.loadClass(name)
  } catch (_: ClassNotFoundException) {
    return "not found"
  }

  val actualClassLoader = clazz.classLoader ?: return "null classloader"

  val plugin = if (actualClassLoader is PluginClassLoader) {
    actualClassLoader.pluginDescriptor
  } else {
    "unknown classloader"
  }

  val resource = actualClassLoader.getResource(name.replace('.', '/') + ".class")
  val location = if (resource != null) {
    val matcher = LOCATION_RX.matcher(resource.toString())
    if (matcher.find()) matcher.group(2) else "unknown location"
  } else {
    "unknown location"
  }

  return "$location by $plugin"
}