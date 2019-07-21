package scientifik

import Scientifik
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension

open class ScientifikJSPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.scientifik

        with(project) {
            plugins.apply("org.jetbrains.kotlin.js")
            plugins.apply("kotlinx-serialization")
            plugins.apply("kotlinx-atomicfu")


            repositories {
                mavenCentral()
                jcenter()
                maven("https://dl.bintray.com/kotlin/kotlin-eap")
                maven("https://kotlin.bintray.com/kotlinx")
                maven("https://dl.bintray.com/mipt-npm/dev")
            }

            configure<KotlinJsProjectExtension> {
                target {
                    browser()
                }
                sourceSets["main"].apply {
                    languageSettings.apply {
                        progressiveMode = true
                        enableLanguageFeature("InlineClasses")
                        useExperimentalAnnotation("ExperimentalUnsignedType")
                    }

                    dependencies {
                        api(kotlin("stdlib-jdk8"))
                        if (extension.serialization) {
                            implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Scientifik.serializationVersion}")
                        }
                        if (extension.atomicfu) {
                            implementation("org.jetbrains.kotlinx:atomicfu-js:${Scientifik.atomicfuVersion}")
                        }
                        if (extension.io) {
                            api("org.jetbrains.kotlinx:kotlinx-io-js:${Scientifik.ioVersion}")
                        }
                    }
                }
            }
        }

    }
}