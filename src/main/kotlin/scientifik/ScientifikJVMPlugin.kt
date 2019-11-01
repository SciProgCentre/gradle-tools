package scientifik

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class ScientifikJVMPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.scientifik

        with(project) {
            plugins.apply("org.jetbrains.kotlin.jvm")
            plugins.apply("maven-publish")

            repositories.applyRepos()

            tasks.withType<KotlinCompile> {
                kotlinOptions {
                    jvmTarget = "1.8"
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
                    }
                }

                val sourcesJar by tasks.registering(Jar::class) {
                    archiveClassifier.set("sources")
                    from(sourceSet.kotlin.srcDirs.first())
                }

                configure<PublishingExtension> {
                    publications {
                        register("jvm", MavenPublication::class) {
                            from(components["java"])
                            artifact(sourcesJar.get())
                        }
                    }
                }
            }

            pluginManager.withPlugin("org.jetbrains.dokka") {
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
}