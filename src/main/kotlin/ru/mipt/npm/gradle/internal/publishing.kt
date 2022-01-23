package ru.mipt.npm.gradle.internal

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal fun Project.requestPropertyOrNull(propertyName: String): String? = findProperty(propertyName) as? String
    ?: System.getenv(propertyName)

internal fun Project.requestProperty(propertyName: String): String = requestPropertyOrNull(propertyName)
    ?: error("Property $propertyName not defined")


internal fun Project.setupPublication(mavenPomConfiguration: MavenPom.() -> Unit = {}) = allprojects {
    plugins.withId("maven-publish") {
        configure<PublishingExtension> {

            plugins.withId("org.jetbrains.kotlin.js") {
                val kotlin: KotlinJsProjectExtension = extensions.findByType()!!

                val sourcesJar by tasks.creating(Jar::class) {
                    archiveClassifier.set("sources")
                    kotlin.sourceSets.forEach {
                        from(it.kotlin)
                    }
                }
                afterEvaluate {
                    publications.create<MavenPublication>("js") {
                        kotlin.js().components.forEach {
                            from(it)
                        }

                        artifact(sourcesJar)
                    }
                }
            }

            plugins.withId("org.jetbrains.kotlin.jvm") {
                val kotlin = extensions.findByType<KotlinJvmProjectExtension>()!!

                val sourcesJar by tasks.creating(Jar::class) {
                    archiveClassifier.set("sources")
                    kotlin.sourceSets.forEach {
                        from(it.kotlin)
                    }
                }

                publications.create<MavenPublication>("jvm") {
                    kotlin.target.components.forEach {
                        from(it)
                    }

                    artifact(sourcesJar)
                }
            }

            val dokkaJar by tasks.creating(Jar::class) {
                group = "documentation"
                archiveClassifier.set("javadoc")
                from(tasks.findByName("dokkaHtml"))
            }

            // Process each publication we have in this project
            afterEvaluate {
                publications.withType<MavenPublication> {
                    artifact(dokkaJar)

                    pom {
                        name.set(project.name)
                        description.set(project.description ?: project.name)

                        licenses {
                            license {
                                name.set("The Apache Software License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }

                        developers {
                            developer {
                                id.set("MIPT-NPM")
                                name.set("MIPT nuclear physics methods laboratory")
                                organization.set("MIPT")
                                organizationUrl.set("https://npm.mipt.ru")
                            }
                        }

                        scm {
                            tag.set(project.version.toString())
                        }

                        mavenPomConfiguration()
                    }
                }
            }
        }
    }
}

internal fun Project.isSnapshot() = "dev" in version.toString() || version.toString().endsWith("SNAPSHOT")

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
    if (requestPropertyOrNull("publishing.github") != "false") {
        logger.info("Skipping github publishing  because `publishing.github != true`")
        return
    }

    val githubUser: String = requestProperty("publishing.github.user")
    val githubToken: String = requestProperty("publishing.github.token")

    allprojects {
        plugins.withId("maven-publish") {
            configure<PublishingExtension> {
                logger.info("Adding github publishing to project [${project.name}]")

                repositories.maven {
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

internal fun Project.addSpacePublishing(spaceRepo: String) {
    if (requestPropertyOrNull("publishing.enabled") != "true") {
        logger.info("Skipping space publishing because publishing is disabled")
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

                repositories.maven {
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

internal fun Project.addSonatypePublishing() {
    if (requestPropertyOrNull("publishing.enabled") != "true") {
        logger.info("Skipping sonatype publishing because publishing is disabled")
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

    allprojects {
        plugins.withId("maven-publish") {
            configure<PublishingExtension> {
                if (!plugins.hasPlugin("signing")) {
                    apply<SigningPlugin>()
                }

                extensions.configure<SigningExtension>("signing") {
                    val signingId: String? = requestPropertyOrNull("publishing.signing.id")
                    if (!signingId.isNullOrBlank()) {
                        val signingKey: String = requestProperty("publishing.signing.key")
                        val signingPassphrase: String = requestProperty("publishing.signing.passPhrase")

                        // if key is provided, use it
                        useInMemoryPgpKeys(signingId, signingKey, signingPassphrase)
                    } // else use file signing
                    sign(publications)
                }

                repositories.maven {
                    val sonatypeRepo = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
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
