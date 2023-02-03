package space.kscience.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import space.kscience.gradle.internal.applySettings
import space.kscience.gradle.internal.defaultKotlinJvmArgs

public open class KScienceJVMPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        if (!plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
            plugins.apply("org.jetbrains.kotlin.jvm")
        } else {
            logger.info("Kotlin JVM plugin is already present")
        }
        registerKScienceExtension(::KScienceExtension)

        //logger.info("Applying KScience configuration for JVM project")
        configure<KotlinJvmProjectExtension> {
            sourceSets.all {
                languageSettings.applySettings()
            }

            sourceSets["test"].apply {
                dependencies {
                    implementation(kotlin("test-junit5"))
                    implementation("org.junit.jupiter:junit-jupiter:${KScienceVersions.junit}")
                }
            }

            if (explicitApi == null) explicitApiWarning()
            jvmToolchain {
                languageVersion.set(KScienceVersions.JVM_TARGET)
            }
        }

        tasks.withType<KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + defaultKotlinJvmArgs
            }
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }

        // apply dokka for all projects
        if (!plugins.hasPlugin("org.jetbrains.dokka")) {
            apply<DokkaPlugin>()
        }
    }

}
