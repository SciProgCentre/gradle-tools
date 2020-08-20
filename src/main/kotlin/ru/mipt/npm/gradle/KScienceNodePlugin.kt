package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KScienceNodePlugin : Plugin<Project> {
    override fun apply(target: Project) = target.run{
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            configure<KotlinMultiplatformExtension> {
                js {
                    nodejs()
                }
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.js") {
            configure<KotlinJsProjectExtension> {
                js {
                    nodejs()
                }
            }
        }
    }

}