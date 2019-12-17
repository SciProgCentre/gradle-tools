package scientifik

import Scientifik
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class ScientifikJVMPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            plugins.apply("org.jetbrains.kotlin.jvm")

            repositories.applyRepos()

            extensions.findByType<JavaPluginExtension>()?.apply {
                targetCompatibility = JavaVersion.VERSION_11
            }

            tasks.withType<KotlinCompile> {
                kotlinOptions {
                    jvmTarget = Scientifik.JVM_VERSION
                }
            }

            configure<KotlinJvmProjectExtension> {

                val sourceSet = sourceSets["main"].apply {
                    languageSettings.applySettings()
                    dependencies {
                        api(kotlin("stdlib-jdk8"))
                    }
                }

                sourceSets["test"].apply {
                    languageSettings.applySettings()
                    dependencies {
                        implementation(kotlin("test"))
                        implementation(kotlin("test-junit"))
//                        implementation(kotlin("test-junit5"))
//                        implementation("org.junit.jupiter:junit-jupiter:5.5.2")
                    }
                }

                val sourcesJar by tasks.registering(Jar::class) {
                    archiveClassifier.set("sources")
                    from(sourceSet.kotlin.srcDirs.first())
                }

                pluginManager.withPlugin("maven-publish") {

                    configure<PublishingExtension> {
                        publications {
                            register("jvm", MavenPublication::class) {
                                from(components["java"])
                                artifact(sourcesJar.get())
                            }
                        }
                    }

                    pluginManager.withPlugin("org.jetbrains.dokka") {
                        logger.info("Adding dokka functionality to project ${this@with.name}")

                        val dokka by tasks.getting(DokkaTask::class) {
                            outputFormat = "html"
                            outputDirectory = "$buildDir/javadoc"
                        }

                        val kdocJar by tasks.registering(Jar::class) {
                            group = JavaBasePlugin.DOCUMENTATION_GROUP
                            dependsOn(dokka)
                            archiveClassifier.set("javadoc")
                            from("$buildDir/javadoc")
                        }

                        configure<PublishingExtension> {
                            publications {
                                getByName("jvm") {
                                    this as MavenPublication
                                    artifact(kdocJar.get())
                                }
                            }
                        }
                    }
                }
            }
//            tasks.withType<Test>(){
//                useJUnitPlatform()
//            }
        }

    }
}