package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import ru.mipt.npm.gradle.internal.applyRepos
import ru.mipt.npm.gradle.internal.applySettings
import ru.mipt.npm.gradle.internal.fromJsDependencies

@Suppress("UNUSED_VARIABLE")
public open class KScienceCommonPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        //Common configuration
        registerKScienceExtension()
        repositories.applyRepos()

        //Configuration for K-JVM plugin
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            //logger.info("Applying KScience configuration for JVM project")
            configure<KotlinJvmProjectExtension> {
                explicitApiWarning()

                sourceSets.all {
                    languageSettings.applySettings()
                }

                sourceSets["test"].apply {
                    dependencies {
                        implementation(kotlin("test-junit5"))
                        implementation("org.junit.jupiter:junit-jupiter:5.6.1")
                    }
                }
            }
            tasks.withType<KotlinJvmCompile> {
                kotlinOptions {
                    jvmTarget = KScienceVersions.JVM_TARGET.toString()
                    freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
                }
            }

            extensions.findByType<JavaPluginExtension>()?.apply {
                targetCompatibility = KScienceVersions.JVM_TARGET
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.js") {
            //logger.info("Applying KScience configuration for JS project")
            configure<KotlinJsProjectExtension> {
                explicitApiWarning()

                js(IR) {
                    browser {
                        commonWebpackConfig {
                            cssSupport.enabled = true
                        }
                    }
                }

                sourceSets.all {
                    languageSettings.applySettings()
                }

                sourceSets["main"].apply {
                    dependencies {
                        api(project.dependencies.platform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:${KScienceVersions.jsBom}"))
                    }
                }

                sourceSets["test"].apply {
                    dependencies {
                        implementation(kotlin("test-js"))
                    }
                }
            }

            (tasks.findByName("processResources") as? Copy)?.apply {
                fromJsDependencies("runtimeClasspath")
            }

        }

        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            configure<KotlinMultiplatformExtension> {
                explicitApiWarning()

                jvm {
                    compilations.all {
                        kotlinOptions {
                            jvmTarget = KScienceVersions.JVM_TARGET.toString()
                            freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
                        }
                    }
                }

                js(IR) {
                    browser {
                        commonWebpackConfig {
                            cssSupport.enabled = true
                        }
                    }
                }

                sourceSets {
                    val commonMain by getting {
                        dependencies {
                            api(project.dependencies.platform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:${KScienceVersions.jsBom}"))
                        }
                    }
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
                            implementation("org.junit.jupiter:junit-jupiter:5.7.2")
                        }
                    }
                    val jsMain by getting
                    val jsTest by getting {
                        dependencies {
                            implementation(kotlin("test-js"))
                        }
                    }
                }

                sourceSets.all {
                    languageSettings.applySettings()
                }

                (tasks.findByName("jsProcessResources") as? Copy)?.apply {
                    fromJsDependencies("jsRuntimeClasspath")
                }

                extensions.findByType<JavaPluginExtension>()?.apply {
                    targetCompatibility = KScienceVersions.JVM_TARGET
                }

                tasks.withType<Test> {
                    useJUnitPlatform()
                }
            }
        }

        // apply dokka for all projects
        if (!plugins.hasPlugin("org.jetbrains.dokka")) {
            apply<DokkaPlugin>()
        }
    }
}
