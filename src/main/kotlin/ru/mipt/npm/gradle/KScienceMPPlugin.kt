package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.findPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin

open class KScienceMPPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        if (plugins.findPlugin(KotlinMultiplatformPlugin::class) == null) {
            logger.info("Kotlin multiplatform plugin is not resolved. Adding it automatically")
            pluginManager.apply(KotlinMultiplatformPlugin::class)
        }
        plugins.apply(KScienceCommonPlugin::class)
     }
}
