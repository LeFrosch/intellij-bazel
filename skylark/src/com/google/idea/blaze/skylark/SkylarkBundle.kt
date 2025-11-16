package com.google.idea.blaze.skylark

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

object SkylarkBundle {

  const val BUNDLE_FQN: @NonNls String = "resources.messages.SkylarkBundle"

  private val BUNDLE = DynamicBundle(javaClass, BUNDLE_FQN)

  @Nls
  fun message(
    @NonNls @PropertyKey(resourceBundle = BUNDLE_FQN) key: String,
    vararg params: Any,
  ): String = BUNDLE.getMessage(key, *params)

  fun messagePointer(
    @NonNls @PropertyKey(resourceBundle = BUNDLE_FQN) key: String,
    vararg params: Any,
  ): Supplier<String> = BUNDLE.getLazyMessage(key, *params)
}
