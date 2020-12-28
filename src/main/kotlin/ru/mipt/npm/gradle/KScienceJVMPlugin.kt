package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

open class KScienceJVMPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        if (plugins.findPlugin("org.jetbrains.kotlin.jvm") == null) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
        } else {
            logger.info("Kotlin JVM plugin is already present")
        }
        plugins.apply(KScienceCommonPlugin::class)
    }
}
