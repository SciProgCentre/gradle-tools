package space.kscience.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import space.kscience.gradle.internal.applySettings
import space.kscience.gradle.internal.fromJsDependencies

public open class KScienceJSPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        if (!plugins.hasPlugin("org.jetbrains.kotlin.js")) {
            plugins.apply("org.jetbrains.kotlin.js")
        } else {
            logger.info("Kotlin JS plugin is already present")
        }
        registerKScienceExtension(::KScienceExtension)

        //logger.info("Applying KScience configuration for JS project")
        configure<KotlinJsProjectExtension> {
            js(IR) { browser { } }

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

            if (explicitApi == null) explicitApiWarning()
        }

        (tasks.findByName("processResources") as? Copy)?.apply {
            fromJsDependencies("runtimeClasspath")
        }


        // apply dokka for all projects
        if (!plugins.hasPlugin("org.jetbrains.dokka")) {
            apply<DokkaPlugin>()
        }
    }
}
