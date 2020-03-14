package scientifik

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.provideDelegate
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


                val bintrayOrg = project.findProperty("bintrayOrg") as? String ?: "mipt-npm"
                val bintrayUser = project.findProperty("bintrayUser") as? String
                val bintrayKey = project.findProperty("bintrayApiKey") as? String


                val bintrayRepo = if (project.version.toString().contains("dev")) {
                    "dev"
                } else {
                    findProperty("bintrayRepo") as? String
                }

                val projectName = project.name

                if (bintrayRepo != null && bintrayUser != null && bintrayKey != null) {
                    project.logger.info("Adding bintray publishing to project [$projectName]")

                    repositories {
                        maven {
                            name = "bintray"
                            url = uri(
                                "https://api.bintray.com/maven/$bintrayOrg/$bintrayRepo/$projectName/;publish=0;override=1"
                            )
                            credentials {
                                username = bintrayUser
                                password = bintrayKey
                            }
                        }
                    }

                }
            }
        }
    }
}