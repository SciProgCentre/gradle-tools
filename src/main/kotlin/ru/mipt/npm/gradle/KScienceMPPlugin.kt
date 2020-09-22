package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

open class KScienceMPPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        plugins.apply("org.jetbrains.kotlin.multiplatform")
        registerKScienceExtension()
        repositories.applyRepos()

        configure<KotlinMultiplatformExtension> {
            explicitApiWarning()

            jvm {
                compilations.all {
                    kotlinOptions {
//                        useIR = true
                        jvmTarget = KScienceVersions.JVM_TARGET.toString()
                    }
                }
            }

            js(IR) {
                browser()
                nodejs()
            }

            sourceSets.invoke {
                val commonMain by getting
                val commonTest by getting {
                    dependencies {
                        implementation(kotlin("test-common"))
                        implementation(kotlin("test-annotations-common"))
                    }
                }
                val jvmMain by getting
                val jvmTest by getting {
                    dependencies {
                        implementation(kotlin("test-junit5"))
                        implementation("org.junit.jupiter:junit-jupiter:5.6.1")
                    }
                }
                val jsMain by getting
                val jsTest by getting {
                    dependencies {
                        implementation(kotlin("test-js"))
                    }
                }
            }

            afterEvaluate {
                targets.all {
                    sourceSets.all {
                        languageSettings.applySettings()
                    }
                }
            }

//            pluginManager.withPlugin("org.jetbrains.dokka") {
//                logger.info("Adding dokka functionality to project ${this@run.name}")
//
//                val dokkaHtml by tasks.getting(DokkaTask::class) {
//                    dokkaSourceSets {
//                        register("commonMain") {
//                            displayName = "common"
//                            platform = "common"
//                        }
//                        register("jvmMain") {
//                            displayName = "jvm"
//                            platform = "jvm"
//                        }
//                        register("jsMain") {
//                            displayName = "js"
//                            platform = "js"
//                        }
//                        configureEach {
//                            jdkVersion = 11
//                        }
//                    }
//                }
//            }

            tasks.apply {
                withType<Test> {
                    useJUnitPlatform()
                }

                val jsProcessResources by getting(Copy::class)
                jsProcessResources.copyJSResources(configurations["jsRuntimeClasspath"])

                val jvmProcessResources by getting(Copy::class)
                jvmProcessResources.copyJVMResources(configurations["jvmRuntimeClasspath"])

                findByName("jsBrowserDistribution")?.apply {
                    doLast {
                        val indexFile = project.jsDistDirectory.resolve("index.html")
                        if (indexFile.exists()) {
                            println("Run JS distribution at: ${indexFile.canonicalPath}")
                        }
                    }
                }
            }

        }
    }
}
