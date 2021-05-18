package ru.mipt.npm.gradle.internal

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

private fun Project.requestPropertyOrNull(propertyName: String): String? = findProperty(propertyName) as? String
    ?: System.getenv(propertyName)

private fun Project.requestProperty(propertyName: String): String = requestPropertyOrNull(propertyName)
    ?: error("Property $propertyName not defined")


internal fun Project.setupPublication(vcs: String) = allprojects {
    plugins.withId("maven-publish") {
        configure<PublishingExtension> {

            plugins.withId("org.jetbrains.kotlin.js") {
                val kotlin = extensions.findByType<KotlinJsProjectExtension>()!!

                val sourcesJar: Jar by tasks.creating(Jar::class) {
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

            plugins.withId("org.jetbrains.kotlin.jvm") {
                val kotlin = extensions.findByType<KotlinJvmProjectExtension>()!!

                val sourcesJar: Jar by tasks.creating(Jar::class) {
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

            val dokkaJar: Jar by tasks.creating(Jar::class) {
                group = "documentation"
                archiveClassifier.set("javadoc")
                from(tasks.findByName("dokkaHtml"))
            }

            // Process each publication we have in this project
            afterEvaluate {
                publications.withType<MavenPublication>().forEach { publication ->
                    publication.artifact(dokkaJar)
                    publication.pom {
                        name.set(project.name)
                        description.set(project.description ?: project.name)
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
                            //developerConnection = "scm:git:[fetch=]/*ВАША ССЫЛКА НА .git файл*/[push=]/*Повторить предыдущую ссылку*/"
                        }

                    }
                }
            }
        }
    }
}

internal fun Project.isSnapshot() = version.toString().contains("dev") || version.toString().endsWith("SNAPSHOT")

internal val Project.publicationTarget: String
    get() {
        val publicationPlatform = project.findProperty("publishing.platform") as? String
        return if (publicationPlatform == null) {
            "AllPublications"
        } else {
            publicationPlatform.capitalize() + "Publication"
        }
    }

internal fun Project.addGithubPublishing(
    githubOrg: String,
    githubProject: String,
) {
    if (requestPropertyOrNull("publishing.enabled") != "true") {
        logger.info("Skipping github publishing because publishing is disabled")
        return
    }
    if (requestPropertyOrNull("publishing.github") == "false") {
        logger.info("Skipping github publishing  because `publishing.github == false`")
        return
    }

    val githubUser: String = requestProperty("publishing.github.user")
    val githubToken: String = requestProperty("publishing.github.token")

    allprojects {
        plugins.withId("maven-publish") {
            configure<PublishingExtension> {
                logger.info("Adding github publishing to project [${project.name}]")
                repositories {
                    maven {
                        name = "github"
                        url = uri("https://maven.pkg.github.com/$githubOrg/$githubProject/")
                        credentials {
                            username = githubUser
                            password = githubToken
                        }
                    }
                }
            }
        }
    }
}

internal fun Project.addSpacePublishing(spaceRepo: String) {
    if (requestPropertyOrNull("publishing.enabled") != "true") {
        logger.info("Skipping github publishing because publishing is disabled")
        return
    }
    if (requestPropertyOrNull("publishing.space") == "false") {
        logger.info("Skipping space publishing because `publishing.space == false`")
        return
    }

    val spaceUser: String = requestProperty("publishing.space.user")
    val spaceToken: String = requestProperty("publishing.space.token")

    allprojects {
        plugins.withId("maven-publish") {
            configure<PublishingExtension> {
                project.logger.info("Adding mipt-npm Space publishing to project [${project.name}]")
                repositories {
                    maven {
                        name = "space"
                        url = uri(spaceRepo)
                        credentials {
                            username = spaceUser
                            password = spaceToken
                        }

                    }
                }
            }
        }
    }
}

internal fun Project.addSonatypePublishing() {
    if(requestPropertyOrNull("publishing.enabled")!="true"){
        logger.info("Skipping github publishing because publishing is disabled")
        return
    }
    if (isSnapshot()) {
        logger.info("Sonatype publishing skipped for dev version")
        return
    }
    if (requestPropertyOrNull("publishing.sonatype") == "false") {
        logger.info("Skipping sonatype publishing because `publishing.sonatype == false`")
        return
    }

    val sonatypeUser: String = requestProperty("publishing.sonatype.user")
    val sonatypePassword: String = requestProperty("publishing.sonatype.password")
    val signingId: String? = requestPropertyOrNull("publishing.signing.id")

    allprojects {
        plugins.withId("maven-publish") {
            configure<PublishingExtension> {
                val sonatypeRepo: String = "https://oss.sonatype.org/service/local/staging/deploy/maven2"

                if (plugins.findPlugin("signing") == null) {
                    plugins.apply("signing")
                }
                extensions.configure<SigningExtension>("signing") {
                    if (!signingId.isNullOrBlank()) {
                        val signingKey: String = requestProperty("publishing.signing.key")
                        val signingPassphrase: String = requestProperty("publishing.signing.passPhrase")

                        //if key is provided, use it
                        @Suppress("UnstableApiUsage")
                        useInMemoryPgpKeys(signingId, signingKey, signingPassphrase)
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

//internal val Project.bintrayPublish: Boolean
//    get() = (findProperty("publishing.bintray.publish") as? String)?.toBoolean() ?: false
//internal val Project.bintrayOrg: String? get() = findProperty("publishing.bintray.org") as? String
//internal val Project.bintrayUser: String? get() = findProperty("publishing.bintray.user") as? String
//internal val Project.bintrayApiKey: String? get() = findProperty("publishing.bintray.apiKey") as? String
