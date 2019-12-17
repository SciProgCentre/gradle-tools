package scientifik

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import java.io.File

open class ScientifikJSPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        with(project) {
            plugins.apply("org.jetbrains.kotlin.js")

            repositories.applyRepos()

            configure<KotlinJsProjectExtension> {
                target {
                    browser()
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
                val browserWebpack by getting(KotlinWebpack::class) {
                    afterEvaluate {
                        val destination = listOf(name, version.toString()).joinToString("-")
                        destinationDirectory = destinationDirectory?.resolve(destination)
                    }
                    outputFileName = "main.bundle.js"
                }

                afterEvaluate {
                    val installJsDist by creating(Copy::class) {
                        group = "distribution"
                        dependsOn(browserWebpack)
                        from(fileTree("src/main/web"))
                        into(browserWebpack.destinationDirectory!!)
                        doLast {
                            val indexFile = File(browserWebpack.destinationDirectory!!, "index.html")
                            if (indexFile.exists()) {
                                println("Run JS distribution at: ${indexFile.canonicalPath}")
                            }
                        }
                    }

                    findByName("assemble")?.dependsOn(installJsDist)

                }
            }
        }

    }
}