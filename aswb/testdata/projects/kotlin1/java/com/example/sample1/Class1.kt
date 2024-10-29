/*
 * Copyright 2023 The Bazel Authors. All rights reserved.
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
package com.example.sample1

import com.example.lib.LibClass
import com.example.sample1.nested.NestedClass
import com.google.common.collect.ImmutableList

/** Class1 test class */
public class Class1 {
  val libClass = LibClass("hello")
  val nestedClass = NestedClass(1)
  val list: ImmutableList<Any> = ImmutableList.of()
  val kotlinStdLibList: List<Int> = listOf(1, 2, 3)

  fun method() {
    println(libClass::class.java)
    println(nestedClass::class.java)
    println(list::class.java)
    println(kotlinStdLibList::class.java)
  }
}
