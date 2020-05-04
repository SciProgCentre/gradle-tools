package scientifik

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension

open class ScientifikJSPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        with(project) {
            plugins.apply("org.jetbrains.kotlin.js")

            repositories.applyRepos()

            configure<KotlinJsProjectExtension> {
                target {
                    browser {
                        webpackTask {
                            outputFileName = "main.bundle.js"
                        }
                        distribution {
                            directory = project.jsDistDirectory
                        }
                    }
                }
                sourceSets["main"].apply {
                    languageSettings.applySettings()

                    dependencies {
                        api(kotlin("stdlib-js"))
                    }
                }

                sourceSets["test"].apply {
                    languageSettings.applySettings()
                    dependencies {
                        implementation(kotlin("test-js"))
                    }
                }
            }

            tasks.apply {

                val processResources by getting(Copy::class)
                processResources.copyJSResources(configurations["runtimeClasspath"])

                val browserDistribution by getting {
                    doLast {
                        val indexFile = project.jsDistDirectory.resolve("index.html")
                        if (indexFile.exists()) {
                            println("Run JS distribution at: ${indexFile.canonicalPath}")
                        }
                    }
                    group = "distribution"
                }

//                findByName("assemble")?.dependsOn(installJsDist)

            }
        }

    }
}