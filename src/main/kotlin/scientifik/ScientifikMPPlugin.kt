package scientifik

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import java.io.File

open class ScientifikMPPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.scientifik

        project.run {

            plugins.apply("org.jetbrains.kotlin.multiplatform")

            repositories.applyRepos()

            configure<KotlinMultiplatformExtension> {
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

                pluginManager.withPlugin("org.jetbrains.dokka") {
                    val dokka by tasks.getting(DokkaTask::class) {
                        outputFormat = "html"
                        outputDirectory = "$buildDir/javadoc"
                        jdkVersion = 8

                        kotlinTasks {
                            // dokka fails to retrieve sources from MPP-tasks so we only define the jvm task
                            listOf(tasks.getByPath("compileKotlinJvm"))
                        }
                        sourceRoot {
                            // assuming only single source dir
                            path = sourceSets["commonMain"].kotlin.srcDirs.first().toString()
                            platforms = listOf("Common")
                        }
                        // although the JVM sources are now taken from the task,
                        // we still define the jvm source root to get the JVM marker in the generated html
                        sourceRoot {
                            // assuming only single source dir
                            path = sourceSets["jvmMain"].kotlin.srcDirs.first().toString()
                            platforms = listOf("JVM")
                        }

                    }

                    val kdocJar by tasks.registering(Jar::class) {
                        group = JavaBasePlugin.DOCUMENTATION_GROUP
                        dependsOn(dokka)
                        archiveClassifier.set("javadoc")
                        from("$buildDir/javadoc")
                    }

                    configure<PublishingExtension> {

                        targets.all {
                            val publication = publications.findByName(name) as MavenPublication

                            // Patch publications with fake javadoc
                            publication.artifact(kdocJar.get())
                        }
                    }
                }
            }



            tasks.apply {
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

                afterEvaluate {
                    val installJsDist by creating(Copy::class) {
                        group = "distribution"
                        dependsOn(jsBrowserWebpack)
                        from(project.fileTree("src/jsMain/web"))
                        into(jsBrowserWebpack.destinationDirectory!!)
                        doLast {
                            val indexFile = File(jsBrowserWebpack.destinationDirectory!!, "index.html")
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