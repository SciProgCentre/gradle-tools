package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KScienceNativePlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run {
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
            //deploy mode
            linuxX64()
            mingwX64()
            macosX64()

            sourceSets {
                val commonMain by getting
                val commonTest by getting

                val nativeMain by creating {
                    dependsOn(commonMain)
                }

                val nativeTest by creating {
                    dependsOn(commonTest)
                }

                val linuxX64Main by getting {
                    dependsOn(nativeMain)
                }

                val mingwX64Main by getting {
                    dependsOn(nativeMain)
                }

                val macosX64Main by getting {
                    dependsOn(nativeMain)
                }

                val linuxX64Test by getting {
                    dependsOn(nativeTest)
                }

                val mingwX64Test by getting {
                    dependsOn(nativeTest)
                }

                val macosX64Test by getting {
                    dependsOn(nativeTest)
                }
            }
        }
    }
}