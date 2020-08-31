package ru.mipt.npm.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KScienceNativePlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        if (plugins.findPlugin(KScienceMPPlugin::class) == null) {
            pluginManager.apply(KScienceMPPlugin::class)
        }
        configure<KotlinMultiplatformExtension> {
            val hostOs = System.getProperty("os.name")
            val isMingwX64 = hostOs.startsWith("Windows")

            val nativeTarget = when {
                hostOs == "Mac OS X" -> macosX64("native")
                hostOs == "Linux" -> linuxX64("native")
                isMingwX64 -> mingwX64("native")
                else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
            }

            sourceSets.invoke {
                val nativeMain by getting
                val nativeTest by getting
            }
        }
    }
}