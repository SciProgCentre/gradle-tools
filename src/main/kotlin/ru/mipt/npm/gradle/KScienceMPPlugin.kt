package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

open class KScienceMPPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        if (plugins.findPlugin("org.jetbrains.kotlin.multiplatform") == null) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        } else {
            logger.info("Kotlin MPP plugin is already present")
        }
        plugins.apply(KScienceCommonPlugin::class)
     }
}
