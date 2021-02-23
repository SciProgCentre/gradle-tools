package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension


private fun Project.isSnapshot() = version.toString().contains("dev") || version.toString().endsWith("SNAPSHOT")

open class KSciencePublishingPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit {

        //Add publishing plugin and new publications
        project.run {
            if (plugins.findPlugin("maven-publish") == null) {
                plugins.apply("maven-publish")
            }

            configure<PublishingExtension> {
                plugins.withId("ru.mipt.npm.gradle.js") {
                    val kotlin = extensions.findByType<KotlinJsProjectExtension>()!!

                    val sourcesJar: Jar by project.tasks.creating(Jar::class) {
                        archiveClassifier.set("sources")
                        from(kotlin.sourceSets["main"].kotlin)
                    }

                    publications {
                        create("js", MavenPublication::class) {
                            from(components["kotlin"])
                            artifact(sourcesJar)
                        }
                    }
                }

                plugins.withId("ru.mipt.npm.gradle.jvm") {
                    val kotlin = extensions.findByType<KotlinJvmProjectExtension>()!!

                    val sourcesJar: Jar by project.tasks.creating(Jar::class) {
                        archiveClassifier.set("sources")
                        from(kotlin.sourceSets["main"].kotlin)
                    }

                    publications {
                        create("jvm", MavenPublication::class) {
                            from(components["kotlin"])
                            artifact(sourcesJar)
                        }
                    }
                }
            }
        }

        //configure publications after everything is set in the root project
        project.rootProject.afterEvaluate {
            project.run {
                val githubOrg: String = project.findProperty("githubOrg") as? String ?: "mipt-npm"
                val githubProject: String? by project
                val vcs = findProperty("vcs") as? String
                    ?: githubProject?.let { "https://github.com/$githubOrg/$it" }

                configure<PublishingExtension> {
                    val dokkaJar: Jar by tasks.creating(Jar::class) {
                        group = "documentation"
                        archiveClassifier.set("javadoc")
                        from(tasks.findByName("dokkaHtml"))
                    }

                    // Process each publication we have in this project
                    publications.withType<MavenPublication>().forEach { publication ->
                        publication.artifact(dokkaJar)
                        publication.pom {
                            name.set(project.name)
                            description.set(project.description ?: project.name)
                            vcs?.let { url.set(vcs) }

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
                            vcs?.let {
                                scm {
                                    url.set(vcs)
                                    tag.set(project.version.toString())
                                    //developerConnection = "scm:git:[fetch=]/*ВАША ССЫЛКА НА .git файл*/[push=]/*Повторить предыдущую ссылку*/"
                                }
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
                    val bintrayPublish: String? by project

                    val bintrayRepo = if (isSnapshot()) {
                        "dev"
                    } else {
                        findProperty("bintrayRepo") as? String
                    }

                    val projectName = project.name

                    if (bintrayPublish == "true" && bintrayRepo != null && bintrayUser != null && bintrayApiKey != null) {
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

                    val sonatypePublish: String? by project
                    val sonatypeUser: String? by project
                    val sonatypePassword: String? by project

                    val keyId: String? by project
                    val signingKey: String? =
                        project.findProperty("signingKey") as? String ?: System.getenv("signingKey")
                    val signingKeyPassphrase: String? by project

                    if (sonatypePublish == "true" && sonatypeUser != null && sonatypePassword != null) {
                        val sonatypeRepo: String = if (isSnapshot()) {
                            "https://oss.sonatype.org/content/repositories/snapshots"
                        } else {
                            "https://oss.sonatype.org/service/local/staging/deploy/maven2"
                        }

                        if (plugins.findPlugin("signing") == null) {
                            plugins.apply("signing")
                        }

                        extensions.configure<SigningExtension>("signing") {
                            if (!signingKey.isNullOrBlank()) {
                                //if key is provided, use it
                                @Suppress("UnstableApiUsage")
                                useInMemoryPgpKeys(keyId, signingKey, signingKeyPassphrase)
                            } // else use file signing
                            sign(publications)
                        }

                        repositories {
                            maven {
                                name = "sonatype"
                                url = uri(sonatypeRepo)
                                credentials {
                                    username = sonatypeUser
                                    password = sonatypePassword
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


