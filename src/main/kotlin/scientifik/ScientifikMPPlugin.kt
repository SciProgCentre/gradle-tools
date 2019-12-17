package scientifik

import Scientifik
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
        project.run {

            plugins.apply("org.jetbrains.kotlin.multiplatform")

            repositories.applyRepos()

            configure<KotlinMultiplatformExtension> {
                jvm {
                    compilations.all {
                        kotlinOptions {
                            jvmTarget = Scientifik.JVM_VERSION
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
//                            implementation(kotlin("test-junit5"))
//                            implementation("org.junit.jupiter:junit-jupiter:5.5.2")
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
                    logger.info("Adding dokka functionality to project ${this@run.name}")
                    val dokka by tasks.getting(DokkaTask::class) {
                        outputFormat = "html"
                        outputDirectory = "$buildDir/javadoc"
                        multiplatform {

                        }
                    }

                    val kdocJar by tasks.registering(Jar::class) {
                        group = JavaBasePlugin.DOCUMENTATION_GROUP
                        dependsOn(dokka)
                        archiveClassifier.set("javadoc")
                        from("$buildDir/javadoc")
                    }

                    pluginManager.withPlugin("maven-publish") {
                        configure<PublishingExtension> {

                            targets.all {
                                val publication = publications.findByName(name) as MavenPublication

                                // Patch publications with fake javadoc
                                publication.artifact(kdocJar.get())
                            }
                        }
                    }
                }
            }



            tasks.apply {
                val jsBrowserWebpack by getting(KotlinWebpack::class) {
                    afterEvaluate {
                        val destination = listOf(name, version.toString()).joinToString("-")
                        destinationDirectory = destinationDirectory?.resolve(destination)
                    }
                    outputFileName = "main.bundle.js"
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

//                withType<Test>(){
//                    useJUnitPlatform()
//                }
            }
        }

    }
}