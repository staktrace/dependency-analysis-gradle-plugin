// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Ignore
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Runs if both java-library and kotlin-jvm plugins have been applied. Checks for presence of java
 * and kotlin source. Suggests removing kotlin-jvm if there is no Kotlin source.
 */
@CacheableTask
abstract class DetectRedundantJvmPluginTask : DefaultTask() {

  init {
    description = "Produces a report about redundant jvm plugins that have been applied"
  }

  @get:Input
  abstract val hasJava: Property<Boolean>

  @get:Input
  abstract val hasKotlin: Property<Boolean>

  @get:Input
  abstract val redundantPluginsBehavior: Property<Behavior>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    // Outputs
    val outputFile = output.getAndDelete()

    val behavior = redundantPluginsBehavior.get()
    val shouldIgnore = behavior is Ignore

    logger.quiet("behavior: $behavior")
    logger.quiet("shouldIgnore: $shouldIgnore")
    logger.quiet("hasKotlin: ${hasKotlin.get()}")
    val pluginAdvices =
      if (!hasKotlin.get() && !shouldIgnore) mutableSetOf(PluginAdvice.redundantKotlinJvm())
      else mutableSetOf()

    pluginAdvices.removeIf {
      behavior.filter.contains(it.redundantPlugin)
    }

    outputFile.bufferWriteJsonSet(pluginAdvices)
  }
}
