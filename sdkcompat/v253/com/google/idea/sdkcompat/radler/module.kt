package com.google.idea.sdkcompat.radler

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.settings.OCResolveContextSettings
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration

typealias RadSymbolHost = com.intellij.clion.radler.core.protocol.RadSymbolsHost
typealias RadTestPsiElement = com.intellij.clion.radler.testing.RadTestPsiElement

typealias RadProjectFallbackConfigDisabler = com.intellij.rider.cpp.core.projectModel.RadProjectFallbackConfigDisabler

// #api252
fun findPriorityConfiguration(project: Project, configs: Collection<OCResolveConfiguration>): OCResolveConfiguration? {
  return OCResolveContextSettings.getInstance(project).findPriorityConfiguration(configs)
}

// #api252
fun setSelectedConfiguration(project: Project, config: OCResolveConfiguration) {
  OCResolveContextSettings.getInstance(project).setSelectedConfiguration(config)
}

// #api252
fun resetConfigurationPriorities(project: Project) {
  OCResolveContextSettings.getInstance(project).resetConfigurationPriorities()
}