package scientifik

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType


open class ScientifikPublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.plugins.apply("maven-publish")

        project.run {
            val githubProject = findProperty("githubProject") as? String
            val vcs = findProperty("vcs") as? String
                ?: githubProject?.let { "https://github.com/mipt-npm/$it" }

            if (vcs == null) {
                project.logger.warn("[${project.name}] Missing deployment configuration. Skipping publish.")
                return@apply
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
                        }
                    }
                }

                val githubUser: String? by project
                val githubToken: String? by project

                if (githubProject != null && githubUser != null && githubToken != null) {
                    project.logger.info("Adding github publishing to project [${project.name}]")
                    repositories {
                        val githubMavenRepository = maven {
                            name = "github"
                            url = uri("https://maven.pkg.github.com/mipt-npm/$githubProject/")
                            credentials {
                                username = githubUser
                                password = githubToken
                            }

                        }

                        val githubPublishTasks = publications.filterIsInstance<MavenPublication>().map { publication ->
                            tasks.register<PublishToMavenRepository>("publish${publication.name.capitalize()}ToGithub") {
                                group = "publishing"
                                this.publication = publication
                                this.repository = githubMavenRepository
                            }
                        }

                        tasks.register<PublishToMavenRepository>("publishToGithub") {
                            group = "publishing"
                            dependsOn(githubPublishTasks)
                        }

                    }
                }


                val bintrayRepo = if (project.version.toString().contains("dev")) {
                    "dev"
                } else {
                    findProperty("bintrayRepo") as? String
                }

                val bintrayOrg = project.findProperty("bintrayOrg") as? String ?: "mipt-npm"
                val bintrayUser = project.findProperty("bintrayUser") as? String
                val bintrayKey = project.findProperty("bintrayApiKey") as? String

                if (bintrayRepo != null && bintrayUser != null && bintrayKey != null) {
                    project.logger.info("Adding bintray publishing to project [${project.name}]")

                    repositories {
                        val bintrayMavenRepository = maven {
                            name = "bintray"
                            uri("https://api.bintray.com/maven/$bintrayOrg/$bintrayRepo/${project.name}/;publish=0;override=1")
                            credentials {
                                this.username = bintrayUser
                                this.password = bintrayKey
                            }
                        }

                        val bintrayPublishTasks = publications.withType<MavenPublication>().map { publication ->
                            tasks.register<PublishToMavenRepository>("publish${publication.name.capitalize()}ToBintray") {
                                group = "publishing"
                                this.publication = publication
                                this.repository = bintrayMavenRepository
                            }
                        }

                        tasks.register<PublishToMavenRepository>("publishToBintray") {
                            group = "publishing"
                            dependsOn(bintrayPublishTasks)
                        }
                    }
                }
            }
        }
    }
}