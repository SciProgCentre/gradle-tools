package scientifik

import Scientifik
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

open class ScientifikMPPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.scientifik

        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        project.plugins.apply("kotlinx-serialization")
        project.plugins.apply("kotlinx-atomicfu")

        project.repositories.applyRepos()

        project.configure<KotlinMultiplatformExtension> {
            jvm {
                compilations.all {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                }
            }

            js {
                browser {}
            }


            sourceSets.invoke {
                val commonMain by getting {
                    dependencies {
                        api(kotlin("stdlib"))
                        project.afterEvaluate {
                            if (extension.serialization) {
                                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Scientifik.serializationVersion}")
                            }
                            if(extension.atomicfu){
                                implementation("org.jetbrains.kotlinx:atomicfu-common:${Scientifik.atomicfuVersion}")
                            }
                            if(extension.io){
                                api("org.jetbrains.kotlinx:kotlinx-io:${Scientifik.ioVersion}")
                            }
                        }
                    }
                }
                val commonTest by getting {
                    dependencies {
                        implementation(kotlin("test-common"))
                        implementation(kotlin("test-annotations-common"))
                    }
                }
                val jvmMain by getting {
                    dependencies {
                        api(kotlin("stdlib-jdk8"))
                        project.afterEvaluate {
                            if (extension.atomicfu) {
                                implementation("org.jetbrains.kotlinx:atomicfu:${Scientifik.atomicfuVersion}")
                            }
                            if (extension.io) {
                                api("org.jetbrains.kotlinx:kotlinx-io-jvm:${Scientifik.ioVersion}")
                            }
                        }
                    }
                }
                val jvmTest by getting {
                    dependencies {
                        implementation(kotlin("test"))
                        implementation(kotlin("test-junit"))
                    }
                }
                val jsMain by getting {
                    dependencies {
                        api(kotlin("stdlib-js"))
                        project.afterEvaluate {
                            if (extension.atomicfu) {
                                implementation("org.jetbrains.kotlinx:atomicfu-common-js:${Scientifik.atomicfuVersion}")
                            }
                            if (extension.io) {
                                api("org.jetbrains.kotlinx:kotlinx-io-js:${Scientifik.ioVersion}")
                            }
                        }
                    }
                }
                val jsTest by getting {
                    dependencies {
                        implementation(kotlin("test-js"))
                    }
                }
            }

            targets.all {
                sourceSets.all {
                    languageSettings.applySettings()
                }
            }
        }

        project.tasks.apply {
            val jsBrowserWebpack by getting(KotlinWebpack::class) {
                archiveClassifier = "js"
                project.afterEvaluate {
                    val destination = listOf(archiveBaseName, archiveAppendix, archiveVersion, archiveClassifier)
                        .filter { it != null && it.isNotBlank() }
                        .joinToString("-")
                    destinationDirectory = destinationDirectory?.resolve(destination)
                }
                archiveFileName = "main.bundle.js"
            }

            project.afterEvaluate {
                val installJsDist by creating(Copy::class) {
                    group = "distribution"
                    dependsOn(jsBrowserWebpack)
                    from(project.fileTree("src/jsMain/web"))
                    into(jsBrowserWebpack.destinationDirectory!!)
                }

                findByName("assemble")?.dependsOn(installJsDist)
            }
        }

    }
}