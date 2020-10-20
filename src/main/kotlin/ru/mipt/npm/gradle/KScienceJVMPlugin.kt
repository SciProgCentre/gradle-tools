package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class KScienceJVMPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        plugins.apply("org.jetbrains.kotlin.jvm")
        registerKScienceExtension()

        repositories.applyRepos()

        extensions.findByType<JavaPluginExtension>()?.apply {
            targetCompatibility = KScienceVersions.JVM_TARGET
            withSourcesJar()
            //withJavadocJar()
        }

        tasks.withType<KotlinCompile> {
            kotlinOptions {
//                useIR = true
                jvmTarget = KScienceVersions.JVM_TARGET.toString()
            }
        }

        configure<KotlinJvmProjectExtension> {
            explicitApiWarning()
            val sourceSet = sourceSets["main"].apply {
                languageSettings.applySettings()
            }

            sourceSets["test"].apply {
                languageSettings.applySettings()
                dependencies {
                    implementation(kotlin("test-junit5"))
                    implementation("org.junit.jupiter:junit-jupiter:5.6.1")
                }
            }
        }

        tasks.apply {
            withType<Test>() {
                useJUnitPlatform()
            }
//
//            val processResources by getting(Copy::class)
//            processResources.copyJVMResources(configurations["runtimeClasspath"])
        }
    }

}
