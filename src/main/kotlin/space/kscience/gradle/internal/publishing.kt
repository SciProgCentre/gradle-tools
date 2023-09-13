package space.kscience.gradle.internal

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import space.kscience.gradle.isInDevelopment

internal fun Project.requestPropertyOrNull(propertyName: String): String? = findProperty(propertyName) as? String
    ?: System.getenv(propertyName)

internal fun Project.requestProperty(propertyName: String): String = requestPropertyOrNull(propertyName)
    ?: error("Property $propertyName not defined")


internal fun Project.setupPublication(mavenPomConfiguration: MavenPom.() -> Unit = {}) = allprojects {
    plugins.withId("maven-publish") {
        configure<PublishingExtension> {

            plugins.withId("org.jetbrains.kotlin.jvm") {
                val kotlin = extensions.findByType<KotlinJvmProjectExtension>()!!

                val sourcesJar by tasks.creating(Jar::class) {
                    archiveClassifier.set("sources")
                    kotlin.sourceSets.forEach {
                        from(it.kotlin)
                    }
                }

                publications.create<MavenPublication>("jvm") {
                    from(project.components["java"])

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

                        // if a key is provided, use it
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

internal fun Project.addPublishing(
    repositoryName: String,
    urlString:String
){
    require(repositoryName.matches("\\w*".toRegex())){"Repository name must contain only letters or numbers"}
    val user: String? = requestPropertyOrNull("publishing.$repositoryName.user")
    val token: String? = requestPropertyOrNull("publishing.$repositoryName.token")

    if (user == null || token == null) {
        logger.info("Skipping $repositoryName publishing because $repositoryName credentials are not defined")
        return
    }

    allprojects {
        plugins.withId("maven-publish") {
            configure<PublishingExtension> {
                logger.info("Adding $repositoryName publishing to project [${project.name}]")

                repositories.maven {
                    name = repositoryName
                    url = uri(urlString)

                    credentials {
                        username = user
                        password = token
                    }
                }
            }
        }
    }
}


internal fun Project.addSonatypePublishing(sonatypeRoot: String) {
    if (isInDevelopment) {
        logger.info("Sonatype publishing skipped for development version")
    } else {
        addPublishing("sonatype", "$sonatypeRoot/service/local/staging/deploy/maven2")
    }
}
