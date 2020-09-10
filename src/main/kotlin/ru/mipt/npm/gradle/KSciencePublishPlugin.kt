package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType


open class KSciencePublishPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run{
        plugins.apply("maven-publish")
        registerKScienceExtension()

        afterEvaluate {
            val githubOrg: String = project.findProperty("githubOrg") as? String ?: "mipt-npm"
            val githubProject: String? by project
            val vcs = findProperty("vcs") as? String
                ?: githubProject?.let { "https://github.com/$githubOrg/$it" }

            if (vcs == null) {
                project.logger.warn("[${project.name}] Missing deployment configuration. Skipping publish.")
                return@afterEvaluate
            }

            project.configure<PublishingExtension> {
                // Process each publication we have in this project
                publications.withType<MavenPublication>().forEach { publication ->
                    @Suppress("UnstableApiUsage")
                    publication.pom {
                        name.set(project.name)
                        description.set(project.description)
                        url.set(vcs)

                        licenses {
                            license {
                                name.set("The Apache Software License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("MIPT-NPM")
                                name.set("MIPT nuclear physics methods laboratory")
                                organization.set("MIPT")
                                organizationUrl.set("http://npm.mipt.ru")
                            }

                        }
                        scm {
                            url.set(vcs)
                            tag.set(project.version.toString())
                        }
                    }
                }

                val githubUser: String? by project
                val githubToken: String? by project

                if (githubProject != null && githubUser != null && githubToken != null) {
                    project.logger.info("Adding github publishing to project [${project.name}]")
                    repositories {
                        maven {
                            name = "github"
                            url = uri("https://maven.pkg.github.com/mipt-npm/$githubProject/")
                            credentials {
                                username = githubUser
                                password = githubToken
                            }

                        }
                    }
                }

                val spaceRepo: String? by project
                val spaceUser: String? by project
                val spaceToken: String? by project

                if (spaceRepo != null && spaceUser != null && spaceToken != null) {
                    project.logger.info("Adding mipt-npm Space publishing to project [${project.name}]")
                    repositories {
                        maven {
                            name = "space"
                            url = uri(spaceRepo!!)
                            credentials {
                                username = spaceUser
                                password = spaceToken
                            }

                        }
                    }
                }

                val bintrayOrg = project.findProperty("bintrayOrg") as? String ?: "mipt-npm"
                val bintrayUser: String? by project
                val bintrayApiKey: String? by project


                val bintrayRepo = if (project.version.toString().contains("dev")) {
                    "dev"
                } else {
                    findProperty("bintrayRepo") as? String
                }

                val projectName = project.name

                if (bintrayRepo != null && bintrayUser != null && bintrayApiKey != null) {
                    project.logger.info("Adding bintray publishing to project [$projectName]")

                    repositories {
                        maven {
                            name = "bintray"
                            url = uri(
                                "https://api.bintray.com/maven/$bintrayOrg/$bintrayRepo/$projectName/;publish=1;override=1"
                            )
                            credentials {
                                username = bintrayUser
                                password = bintrayApiKey
                            }
                        }
                    }

                }
            }
        }
    }
}