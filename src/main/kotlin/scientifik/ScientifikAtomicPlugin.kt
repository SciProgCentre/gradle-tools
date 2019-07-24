package scientifik

import Scientifik
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ScientifikAtomicPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins.apply("kotlinx-atomicfu")

            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                project.configure<KotlinMultiplatformExtension> {
                    sourceSets.invoke {
                        val commonMain by getting {
                            dependencies {
                                implementation("org.jetbrains.kotlinx:atomicfu-common:${Scientifik.atomicfuVersion}")
                            }
                        }

                        val jvmMain by getting {
                            dependencies {
                                implementation("org.jetbrains.kotlinx:atomicfu:${Scientifik.atomicfuVersion}")
                            }
                        }
                        val jsMain by getting {
                            dependencies {
                                implementation("org.jetbrains.kotlinx:atomicfu-common-js:${Scientifik.atomicfuVersion}")
                            }
                        }
                        val jsTest by getting {
                            dependencies {
                                implementation(kotlin("test-js"))
                            }
                        }
                    }
                }
                //TODO add native clause
            }

            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                configure<KotlinJvmProjectExtension> {
                    sourceSets["main"].apply {
                        dependencies {
                            implementation("org.jetbrains.kotlinx:atomicfu:${Scientifik.atomicfuVersion}")
                        }
                    }
                }
            }

            pluginManager.withPlugin("org.jetbrains.kotlin.js") {
                configure<KotlinJsProjectExtension> {
                    sourceSets["main"].apply {
                        dependencies {
                            implementation("org.jetbrains.kotlinx:atomicfu-js:${Scientifik.atomicfuVersion}")
                        }
                    }
                }
            }
        }
    }
}


