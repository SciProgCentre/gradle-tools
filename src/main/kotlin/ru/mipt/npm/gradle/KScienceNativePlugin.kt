package ru.mipt.npm.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KScienceNativePlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
        //Apply multiplatform plugin is not applied, apply it
        if (plugins.findPlugin(KScienceMPPlugin::class) == null) {
            logger.info("Multiplatform KScience plugin is not resolved. Adding it automatically")
            pluginManager.apply(KScienceMPPlugin::class)
        }

        configure<KotlinMultiplatformExtension> {
            val ideaActive = System.getProperty("idea.active") == "true"

            if (ideaActive) {
                //development mode
                val hostOs = System.getProperty("os.name")

                when {
                    hostOs == "Mac OS X" -> macosX64("native")
                    hostOs == "Linux" -> linuxX64("native")
                    hostOs.startsWith("Windows") -> mingwX64("native")
                    else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
                }
            } else {
                //deploy mode
                linuxX64()
                mingwX64()
                macosX64()

                sourceSets{
                    val commonMain by getting
                    val nativeMain by creating{
                        dependsOn(commonMain)
                    }

                    val commonTest by getting

                    val nativeTest by creating{
                        dependsOn(nativeMain)
                        dependsOn(commonTest)
                    }

                    val linuxX64Main by getting{
                        dependsOn(nativeMain)
                    }

                    val mingwX64Main by getting{
                        dependsOn(nativeMain)
                    }

                    val macosX64Main by getting{
                        dependsOn(nativeMain)
                    }

                    val linuxX64Test by getting{
                        dependsOn(nativeTest)
                    }

                    val mingwX64Test by getting{
                        dependsOn(nativeTest)
                    }

                    val macosX64Test by getting{
                        dependsOn(nativeTest)
                    }
                }
            }
        }
    }
}