package space.kscience.gradle.internal

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
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.targets
import space.kscience.gradle.isInDevelopment

internal fun Project.requestPropertyOrNull(propertyName: String): String? = findProperty(propertyName) as? String
    ?: System.getenv(propertyName)

internal fun Project.requestProperty(propertyName: String): String = requestPropertyOrNull(propertyName)
    ?: error("Property $propertyName not defined")


internal fun Project.setupPublication(mavenPomConfiguration: MavenPom.() -> Unit = {}) = allprojects {
    plugins.withId("maven-publish") {
        configure<PublishingExtension> {

            plugins.withId("org.jetbrains.kotlin.js") {
                val kotlin: KotlinJsProjectExtension = extensions.findByType()!!

                publications.create<MavenPublication>("js") {
                    kotlin.targets.flatMap { it.components }.forEach {
                        from(it)
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

            // Process each publication we have in this project
            publications.withType<MavenPublication> {
                pom {
                    name.set(project.name)
                    description.set(project.description ?: project.name)

                    scm {
                        tag.set(project.version.toString())
                    }

                    mavenPomConfiguration()
                }
            }

            plugins.withId("org.jetbrains.dokka") {
                val dokkaJar by tasks.creating(Jar::class) {
                    group = "documentation"
                    archiveClassifier.set("javadoc")
                    from(tasks.findByName("dokkaHtml"))
                }
                publications.withType<MavenPublication> {
                    artifact(dokkaJar)
                }
            }

            if (requestPropertyOrNull("publishing.signing.id") != null || requestPropertyOrNull("signing.gnupg.keyName") != null) {

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
                    } // else use agent signing
                    sign(publications)
                }
            } else {
                logger.warn("Signing information is not provided. Skipping artefact signing.")
            }
        }
    }
}

internal fun Project.addGithubPublishing(
    githubOrg: String,
    githubProject: String,
) {
    val githubUser: String? = requestPropertyOrNull("publishing.github.user")
    val githubToken: String? = requestPropertyOrNull("publishing.github.token")

    if (githubUser == null || githubToken == null) {
        logger.info("Skipping Github publishing because Github credentials are not defined")
        return
    }

    allprojects {
        plugins.withId("maven-publish") {
            configure<PublishingExtension> {
                logger.info("Adding Github publishing to project [${project.name}]")

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
    val spaceUser: String? = requestPropertyOrNull("publishing.space.user")
    val spaceToken: String? = requestPropertyOrNull("publishing.space.token")

    if (spaceUser == null || spaceToken == null) {
        logger.info("Skipping Space publishing because Space credentials are not defined")
        return
    }

    allprojects {
        plugins.withId("maven-publish") {
            configure<PublishingExtension> {
                project.logger.info("Adding SPC Space publishing to project [${project.name}]")

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

internal fun Project.addSonatypePublishing(sonatypeRoot: String) {
    if (isInDevelopment) {
        logger.info("Sonatype publishing skipped for development version")
        return
    }

    val sonatypeUser: String? = requestPropertyOrNull("publishing.sonatype.user")
    val sonatypePassword: String? = requestPropertyOrNull("publishing.sonatype.password")

    if (sonatypeUser == null || sonatypePassword == null) {
        logger.info("Skipping Sonatype publishing because Sonatype credentials are not defined")
        return
    }

    allprojects {
        plugins.withId("maven-publish") {
            configure<PublishingExtension> {
                repositories.maven {
                    val sonatypeRepo = "$sonatypeRoot/service/local/staging/deploy/maven2"
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
