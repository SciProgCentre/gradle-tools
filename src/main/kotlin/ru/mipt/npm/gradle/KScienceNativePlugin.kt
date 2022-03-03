package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

public class KScienceNativePlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        //Apply multiplatform plugin is not applied, apply it
        if (!plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            logger.info("Kotlin multiplatform plugin is not resolved. Adding it automatically")
            plugins.apply("org.jetbrains.kotlin.multiplatform")
        }

        if (!plugins.hasPlugin(KScienceCommonPlugin::class)) {
            logger.info("KScience plugin is not resolved. Adding it automatically")
            apply<KScienceCommonPlugin>()
        }

        configure<KotlinMultiplatformExtension> {
            val nativeTargets = setOf(
                linuxX64(),
                mingwX64(),
                macosX64(),
                iosX64(),
                iosArm64()
            )

            sourceSets {
                val commonMain = findByName("commonMain")!!
                val commonTest = findByName("commonTest")!!

                val nativeMain by creating {
                    dependsOn(commonMain)
                }

                val nativeTest by creating {
                    //should NOT depend on nativeMain because automatic dependency by plugin
                    dependsOn(commonTest)
                }

                configure(nativeTargets) {
                    compilations["main"]?.apply {
                        configure(kotlinSourceSets) {
                            dependsOn(nativeMain)
                        }
                    }

                    compilations["test"]?.apply {
                        configure(kotlinSourceSets) {
                            dependsOn(nativeTest)
                        }
                    }
                }
            }
        }
    }
}
