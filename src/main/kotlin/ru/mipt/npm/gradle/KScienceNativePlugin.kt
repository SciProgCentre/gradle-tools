package ru.mipt.npm.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

private fun KotlinMultiplatformExtension.sourceSets(configure: Action<NamedDomainObjectContainer<KotlinSourceSet>>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("sourceSets", configure)

class KScienceNativePlugin : Plugin<Project> {
    override fun apply(project: Project) = project.run {
        //Apply multiplatform plugin is not applied, apply it
        if (plugins.findPlugin("org.jetbrains.kotlin.multiplatform") == null) {
            logger.info("Kotlin multiplatform plugin is not resolved. Adding it automatically")
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        }
        if (plugins.findPlugin(KScienceCommonPlugin::class) == null) {
            logger.info("KScience plugin is not resolved. Adding it automatically")
            pluginManager.apply(KScienceCommonPlugin::class)
        }

        configure<KotlinMultiplatformExtension> {
            val hostOs = System.getProperty("os.name")

            val isLinux = hostOs == "Linux"
            val isMinGw = hostOs.startsWith("Windows")
            val isMacOs = hostOs == "Mac OS X"

            if (isLinux || isMinGw) {
                linuxX64()
            }
            if (isMinGw) {
                mingwX64()
            }
            if (isMacOs) {
                macosX64()
            }

            sourceSets {
                val commonMain = findByName("commonMain")!!
                val commonTest = findByName("commonTest")!!

                val nativeMain = create("nativeMain").apply {
                    dependsOn(commonMain)
                }

                val nativeTest = create("nativeTest").apply {
                    dependsOn(commonTest)
                }

                findByName("linuxX64Main")?.dependsOn(nativeMain)
                findByName("linuxX64Test")?.dependsOn(nativeTest)

                findByName("mingwX64Main")?.dependsOn(nativeMain)
                findByName("mingwX64Test")?.dependsOn(nativeTest)

                findByName("macosX64Main")?.dependsOn(nativeMain)
                findByName("macosX64Test")?.dependsOn(nativeTest)
            }
        }
    }
}