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
        val extension = project.scientifik

        with(project) {
            plugins.apply("org.jetbrains.kotlin.js")
            plugins.apply("kotlinx-serialization")
            plugins.apply("kotlinx-atomicfu")


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
            }

            tasks.apply {
                val browserWebpack by getting(KotlinWebpack::class) {
                    afterEvaluate {
                        val destination = listOf(archiveBaseName, archiveAppendix, archiveVersion, archiveClassifier)
                            .filter { it != null && it.isNotBlank() }
                            .joinToString("-")
                        destinationDirectory = destinationDirectory?.resolve(destination)
                    }
                    archiveFileName = "main.bundle.js"
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