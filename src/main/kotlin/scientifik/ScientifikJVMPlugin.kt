package scientifik

import Scientifik
import kotlinx.atomicfu.plugin.gradle.sourceSets
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class ScientifikJVMPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.scientifik

        with(project) {
            plugins.apply("org.jetbrains.kotlin.jvm")
            plugins.apply("kotlinx-serialization")
            plugins.apply("kotlinx-atomicfu")

            repositories {
                mavenCentral()
                jcenter()
                maven("https://dl.bintray.com/kotlin/kotlin-eap")
                maven("https://kotlin.bintray.com/kotlinx")
                maven("https://dl.bintray.com/mipt-npm/dev")
            }

            tasks.withType<KotlinCompile> {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }

            sourceSets["main"].apply {
                this as KotlinSourceSet
                languageSettings.apply {
                    progressiveMode = true
                    enableLanguageFeature("InlineClasses")
                    useExperimentalAnnotation("ExperimentalUnsignedType")
                }
            }

            dependencies {
                this as KotlinDependencyHandler
                api(kotlin("stdlib-jdk8"))
                if (extension.serialization) {
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Scientifik.serializationVersion}")
                }
                if (extension.atomicfu) {
                    implementation("org.jetbrains.kotlinx:atomicfu:${Scientifik.atomicfuVersion}")
                }
                if (extension.io) {
                    api("org.jetbrains.kotlinx:kotlinx-io-jvm:${Scientifik.ioVersion}")
                }
            }

        }

    }
}