package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

open class KScienceJSPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        if (plugins.findPlugin("org.jetbrains.kotlin.js") == null) {
            pluginManager.apply("org.jetbrains.kotlin.js")
        } else {
            logger.info("Kotlin JS plugin is already present")
        }
        plugins.apply(KScienceCommonPlugin::class)
    }
}