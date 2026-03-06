package com.google.idea.blaze.cpp

import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration

private const val MARKER = "BAZEL_ID"

/**
 * Wrapper class for the unique identifier derived from a
 */
data class BlazeResolveConfigurationID(
  val identifier: String,
  val configurationId: String,
) {

  companion object {

    @JvmStatic
    fun fromBlazeResolveConfigurationData(data: BlazeResolveConfigurationData): BlazeResolveConfigurationID {
      return BlazeResolveConfigurationID(
        identifier = data.hashCode().toString(),
        configurationId = data.configurationId(),
      )
    }

    @JvmStatic
    fun fromOCResolveConfiguration(config: OCResolveConfiguration): BlazeResolveConfigurationID? {
      val parts = config.uniqueId.split(':')
      if (parts.size != 3) return null

      val (marker, identifier, configurationId) = parts
      if (marker != MARKER) return null

      return BlazeResolveConfigurationID(identifier, configurationId)
    }
  }

  override fun toString(): String {
    return "${MARKER}:${identifier}:${configurationId}"
  }
}