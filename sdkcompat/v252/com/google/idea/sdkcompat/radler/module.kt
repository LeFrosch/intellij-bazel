package com.google.idea.sdkcompat.radler

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.settings.OCResolveContextSettings
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration

typealias RadSymbolHost = com.jetbrains.cidr.radler.protocol.RadSymbolsHost
typealias RadTestPsiElement = com.jetbrains.cidr.radler.testing.RadTestPsiElement

typealias RadProjectFallbackConfigDisabler = com.jetbrains.cidr.radler.projectmodel.RadProjectFallbackConfigDisabler

// #api252
fun findPriorityConfiguration(project: Project, configs: Collection<OCResolveConfiguration>): OCResolveConfiguration? {
  return OCResolveContextSettings.getInstance(project).findPriorityConfiguration(configs)?.first
}

// #api252
fun setSelectedConfiguration(project: Project, config: OCResolveConfiguration) {
  OCResolveContextSettings.getInstance(project).setSelectedConfiguration(config)
}

// #api252
fun resetConfigurationPriorities(project: Project) {
  OCResolveContextSettings.getInstance(project).resetConfigurationPriorities()
}