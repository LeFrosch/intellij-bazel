package com.google.idea.blaze.base.sync.aspects.storage

import java.nio.file.Path

abstract class LegacyAspectWriter : AspectWriter {

  override fun prefix(): Path = Path.of("legacy")

  override fun enabled(): Boolean = true
}