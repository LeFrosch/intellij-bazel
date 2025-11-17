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
package com.google.idea.blaze.base.util

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@Service(Service.Level.APP)
class RunJarService : Disposable {

  companion object {
    @Throws(ExecutionException::class)
    suspend fun run(jar: String, vararg args: String): OSProcessHandler = service<RunJarService>().run(jar, *args)
  }

  private val mutex = Mutex()
  private val cache = mutableMapOf<String, Path>()

  @Throws(ExecutionException::class)
  private suspend fun run(jar: String, vararg args: String): OSProcessHandler {
    val path = try {
      getCachedJar(jar)
    } catch (e: IOException) {
      throw ExecutionException("could not materialize jar: $jar", e)
    }

    val java = try {
      findJavaExecutable()
    } catch (e: IOException) {
      throw ExecutionException("could not find java executable", e)
    }

    val cmdLine = GeneralCommandLine()
      .withExePath(java.toString())
      .withParameters("-jar", path.toString(), *args)

    return withContext(Dispatchers.IO) {
      OSProcessHandler(cmdLine)
    }
  }

  @Throws(IOException::class)
  private suspend fun getCachedJar(jar: String): Path {
    return mutex.withLock {
      val cached = cache[jar]
      if (cached != null) return cached

      val inputStream = javaClass.classLoader.getResourceAsStream(jar)
        ?: throw IOException("jar not found")

      val path = Files.createTempFile("runjar", ".jar").toAbsolutePath()

      withContext(Dispatchers.IO) {
        inputStream.use { input ->
          Files.newOutputStream(path).use { output ->
            input.transferTo(output)
          }
        }
      }

      path.also { cache[jar] = it }
    }
  }

  @Throws(IOException::class)
  private fun findJavaExecutable(): Path {
    val home = System.getProperty("java.home") ?: throw IOException("java.home not found")
    val java = Path.of(home, "bin", "java")

    if (!Files.exists(java)) throw IOException("java executable not found: $java")
    if (!Files.isExecutable(java)) throw IOException("java executable is not executable: $java")

    return java
  }

  override fun dispose() {
    /* should I acquire the mutex here? Is the current scope already disposed? */
    for (path in cache.values) {
      Files.deleteIfExists(path)
    }
  }
}