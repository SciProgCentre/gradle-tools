package space.kscience.gradle

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.MavenPublishBasePlugin
import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.BinaryCompatibilityValidatorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.Sign
import org.jetbrains.changelog.ChangelogPlugin
import org.jetbrains.changelog.ChangelogPluginExtension
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import space.kscience.gradle.internal.addPublishing
import space.kscience.gradle.internal.setupPublication
import space.kscience.gradle.internal.withKScience

/**
 * Simplifies adding repositories for Maven publishing, responds for releasing tasks for projects.
 */
public class KSciencePublishingExtension(public val project: Project) {
    private var isVcsInitialized = false

    /**
     * Configures Git repository (sources) for the publication.
     *
     * @param vcsUrl URL of the repository's web interface.
     * @param connectionUrl URL of the Git repository.
     * @param developerConnectionUrl URL of the Git repository for developers.
     */
    public fun pom(
        vcsUrl: String,
        connectionUrl: String? = null,
        developerConnectionUrl: String? = connectionUrl,
        connectionPrefix: String = "scm:git:",
        pomConfig: MavenPom.() -> Unit,
    ) {
        if (!isVcsInitialized) {
            project.setupPublication {
                url.set(vcsUrl)

                scm {
                    url.set(vcsUrl)
                    connectionUrl?.let { connection.set("$connectionPrefix$it") }
                    developerConnectionUrl?.let { developerConnection.set("$connectionPrefix$it") }
                }
                pomConfig()
            }

            isVcsInitialized = true
        }
    }

    /**
     * Add a repository with [repositoryName]. Uses "publishing.$repositoryName.user" and "publishing.$repositoryName.token"
     * properties pattern to store user and token
     */
    public fun repository(
        repositoryName: String,
        url: String,
    ) {
        require(isVcsInitialized) { "The project vcs is not set up use 'pom' method to do so" }
        project.addPublishing(repositoryName, url)
    }

    /**
     * Add publishing to maven central "new" API
     */
    public fun central(): Unit = with(project) {
        require(isVcsInitialized) { "The project vcs is not set up use 'pom' method to do so" }
        if (isInDevelopment) {
            logger.info("Maven central publishing skipped for development version")
        } else {
            allprojects {
                plugins.withType<MavenPublishBasePlugin> {
                    extensions.configure<MavenPublishBaseExtension> {
                        publishToMavenCentral()
                    }
                }
            }
        }
    }
}

/**
 * Applies third-party plugins (Dokka, Changelog, binary compatibility validator); configures Maven publishing, README
 * generation.
 */
public open class KScienceProjectPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = target.run {
        apply<ChangelogPlugin>()
        apply<DokkaPlugin>()
        apply<BinaryCompatibilityValidatorPlugin>()

        val ksciencePublish = KSciencePublishingExtension(this)
        extensions.add("ksciencePublish", ksciencePublish)

        withKScience {
            extensions.add("publish", ksciencePublish)
        }

        allprojects {
            repositories {
                mavenCentral()
                maven("https://repo.kotlin.link")
                google()
            }

            // Workaround for https://github.com/gradle/gradle/issues/15568
            tasks.withType<AbstractPublishToMaven>().configureEach {
                mustRunAfter(tasks.withType<Sign>())
            }
        }

        afterEvaluate {
            if (isInDevelopment) {
                configure<ApiValidationExtension> {
                    validationDisabled = true
                }
            } else {
                configure<ChangelogPluginExtension> {
                    version.set(project.version.toString())
                }
            }
        }

        //Add readme generators to individual subprojects and root project
        allprojects {
            val readmeExtension = KScienceReadmeExtension(this)
            extensions.add("readme", readmeExtension)

            withKScience {
                extensions.add("readme", readmeExtension)
            }

            val generateReadme by tasks.registering {
                group = "documentation"
                description = "Generate a README file if stub is present"

                inputs.property("features", readmeExtension.features)

                if (readmeExtension.readmeTemplate.exists()) {
                    inputs.file(readmeExtension.readmeTemplate)
                }

                readmeExtension.inputFiles.forEach {
                    if (it.exists()) {
                        inputs.file(it)
                    }
                }

                subprojects {
                    extensions.findByType<KScienceReadmeExtension>()?.let { subProjectReadmeExtension ->
                        tasks.findByName("generateReadme")?.let { readmeTask ->
                            dependsOn(readmeTask)
                        }
                        inputs.property("features-${name}", subProjectReadmeExtension.features)
                    }
                }

                val readmeFile = this@allprojects.file("README.md")
                outputs.file(readmeFile)

                doLast {
                    val readmeString = readmeExtension.readmeString()
                    if (readmeString != null) {
                        readmeFile.writeText(readmeString)
                    }
                }
            }

            tasks.withType<AbstractDokkaTask> {
                dependsOn(generateReadme)
            }
        }


        tasks.register("version") {
            group = "publishing"
            val versionFileProvider = project.layout.buildDirectory.file("project-version.txt")
            outputs.file(versionFileProvider)
            doLast {
                val versionFile = versionFileProvider.get().asFile
                versionFile.createNewFile()
                versionFile.writeText(project.version.toString())
            }
        }

        // Disable API validation for snapshots
        if (isInDevelopment) {
            extensions.findByType<ApiValidationExtension>()?.apply {
                validationDisabled = true
                logger.warn("API validation is disabled for snapshot or dev version")
            }
        }

        plugins.withType<YarnPlugin>() {
            rootProject.configure<YarnRootExtension> {
                lockFileDirectory = rootDir.resolve("gradle")
                yarnLockMismatchReport = YarnLockMismatchReport.WARNING
            }
        }
    }

    public companion object {
        public const val DEPLOY_GROUP: String = "deploy"
    }
}
