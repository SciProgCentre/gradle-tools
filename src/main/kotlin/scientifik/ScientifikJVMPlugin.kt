package scientifik

import Scientifik
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class ScientifikJVMPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.scientifik

        with(project) {
            plugins.apply("org.jetbrains.kotlin.jvm")
            plugins.apply("kotlinx-serialization")

            repositories.applyRepos()

            tasks.withType<KotlinCompile> {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }

            configure<KotlinJvmProjectExtension> {
                sourceSets["main"].apply {
                    languageSettings.applySettings()

                    dependencies {
                        api(kotlin("stdlib-jdk8"))
                        afterEvaluate {
                            if (extension.serialization) {
                                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Scientifik.serializationVersion}")
                            }
                            if (extension.io) {
                                api("org.jetbrains.kotlinx:kotlinx-io-jvm:${Scientifik.ioVersion}")
                            }
                        }
                    }
                }
            }
        }

    }
}