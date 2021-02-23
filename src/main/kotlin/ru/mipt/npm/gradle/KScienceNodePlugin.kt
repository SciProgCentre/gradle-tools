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

/**
 * Create a separate target for node
 */
class KScienceNodePlugin : Plugin<Project> {
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
            js(name = "node", compiler = IR) {
                nodejs()
            }

            sourceSets {
                val commonMain = findByName("commonMain")!!
                val commonTest = findByName("commonTest")!!

                val jsCommonMain = create("jsCommonMain").apply {
                    dependsOn(commonMain)
                }

                val jsCommonTest = create("jsCommonTest").apply {
                    dependsOn(commonTest)
                }

                findByName("jsMain")?.dependsOn(jsCommonMain)
                findByName("jsTest")?.dependsOn(jsCommonTest)

                findByName("nodeMain")?.apply {
                    dependsOn(jsCommonMain)
                    dependencies {
                        api("org.jetbrains.kotlinx:kotlinx-nodejs:${KScienceVersions.kotlinxNodeVersion}")
                    }
                }
                findByName("nodeTest")?.dependsOn(jsCommonMain)
            }
        }
    }
}