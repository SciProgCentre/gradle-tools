package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

open class KScienceMPPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        if (plugins.findPlugin("org.jetbrains.kotlin.multiplatform") == null) {
            logger.info("Kotlin multiplatform plugin is not resolved. Adding it automatically")
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        }
        plugins.apply(KScienceCommonPlugin::class)
     }
}
