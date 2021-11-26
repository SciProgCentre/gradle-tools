package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.hasPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin

public open class KScienceMPPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        if (!plugins.hasPlugin(KotlinMultiplatformPlugin::class)) {
            //apply<KotlinMultiplatformPlugin>() for some reason it does not work
            plugins.apply("org.jetbrains.kotlin.multiplatform")
        } else {
            logger.info("Kotlin MPP plugin is already present")
        }

        apply<KScienceCommonPlugin>()
    }
}
