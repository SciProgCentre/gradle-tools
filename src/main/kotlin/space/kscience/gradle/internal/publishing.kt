package space.kscience.gradle.internal

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.MavenPublishBasePlugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal fun Project.requestPropertyOrNull(propertyName: String): String? = findProperty(propertyName) as? String
    ?: System.getenv(propertyName)

internal fun Project.requestProperty(propertyName: String): String = requestPropertyOrNull(propertyName)
    ?: error("Property $propertyName not defined")


internal fun Project.setupPublication(mavenPomConfiguration: MavenPom.() -> Unit = {}) = allprojects {
    plugins.withId("maven-publish") {
        apply<MavenPublishBasePlugin>()

        configure<PublishingExtension> {

            plugins.withId("org.jetbrains.kotlin.jvm") {
                val kotlin = extensions.findByType<KotlinJvmProjectExtension>()!!

                val sourcesJar by tasks.registering(Jar::class) {
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
                val dokkaJar by tasks.registering(Jar::class) {
                    group = "documentation"
                    archiveClassifier.set("javadoc")
                    from(tasks.findByName("dokkaGenerate"))
                }
                publications.withType<MavenPublication> {
                    artifact(dokkaJar)
                }
            }

            //apply signing if signing configuration is available
            if (requestPropertyOrNull("signing.password") != null || requestPropertyOrNull("signing.secretKeyRingFile") != null) {
                plugins.withType<MavenPublishBasePlugin> {
                    extensions.configure<MavenPublishBaseExtension> {
                        signAllPublications()
                    }
                }
            } else {
                logger.warn("Signing information is not provided. Skipping artefact signing.")
            }
        }
    }
}

internal fun Project.addPublishing(
    repositoryName: String,
    urlString: String
) {
    require(repositoryName.matches("\\w*".toRegex())) { "Repository name must contain only letters or numbers" }
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
