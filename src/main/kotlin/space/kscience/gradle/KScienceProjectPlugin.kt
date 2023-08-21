package space.kscience.gradle

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
import space.kscience.gradle.internal.*

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
     * Adds GitHub as VCS and adds GitHub Packages Maven repository to publishing.
     *
     * @param githubProject the GitHub project.
     * @param githubOrg the GitHub user or organization.
     * @param deploy publish packages in the `deploy` task to the GitHub repository.
     */
    public fun github(
        githubOrg: String,
        githubProject: String,
        deploy: Boolean = project.requestPropertyOrNull("publishing.github") == "true",
    ) {
        if (deploy) {
            try {
                project.addGithubPublishing(githubOrg, githubProject)
            } catch (t: Throwable) {
                project.logger.error("Failed to set up github publication", t)
            }
        }
    }

    /**
     * Adds Space Packages Maven repository to publishing.
     *
     * @param spaceRepo the repository URL.
     * @param deploy publish packages in the `deploy` task to the Space repository.
     */
    public fun space(
        spaceRepo: String,
    ) {
        project.addSpacePublishing(spaceRepo)
    }

    /**
     * Adds Sonatype Maven repository to publishing.
     *
     * @param addToRelease  publish packages in the `release` task to the Sonatype repository.
     */
    public fun sonatype(sonatypeRoot: String = "https://s01.oss.sonatype.org") {
        require(isVcsInitialized) { "The project vcs is not set up use 'pom' method to do so" }
        project.addSonatypePublishing(sonatypeRoot)
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

        allprojects {
            repositories {
                mavenCentral()
                maven("https://repo.kotlin.link")
                maven("https://maven.pkg.jetbrains.space/spc/p/sci/dev")
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

        val rootReadmeExtension = KScienceReadmeExtension(this)
        val ksciencePublish = KSciencePublishingExtension(this)
        extensions.add("ksciencePublish", ksciencePublish)
        extensions.add("readme", rootReadmeExtension)

        //Add readme generators to individual subprojects
        subprojects {
            val readmeExtension = KScienceReadmeExtension(this)
            extensions.add("readme", readmeExtension)

            val generateReadme by tasks.creating {
                group = "documentation"
                description = "Generate a README file if stub is present"

                if (readmeExtension.readmeTemplate.exists()) {
                    inputs.file(readmeExtension.readmeTemplate)
                }
                readmeExtension.inputFiles.forEach {
                    if (it.exists()) {
                        inputs.file(it)
                    }
                }

                val readmeFile = this@subprojects.file("README.md")
                outputs.file(readmeFile)

                doLast {
                    val readmeString = readmeExtension.readmeString()
                    if (readmeString != null) {
                        readmeFile.writeText(readmeString)
                    }
                }
            }

//            tasks.withType<AbstractDokkaTask> {
//                dependsOn(generateReadme)
//            }
        }

        val generateReadme by tasks.creating {
            group = "documentation"
            description = "Generate a README file and a feature matrix if stub is present"

            subprojects {
                tasks.findByName("generateReadme")?.let {
                    dependsOn(it)
                }
            }

            if (rootReadmeExtension.readmeTemplate.exists()) {
                inputs.file(rootReadmeExtension.readmeTemplate)
            }

            rootReadmeExtension.inputFiles.forEach {
                if (it.exists()) {
                    inputs.file(it)
                }
            }

            val readmeFile = project.file("README.md")
            outputs.file(readmeFile)

            doLast {
//                val projects = subprojects.associate {
//                    val normalizedPath = it.path.replaceFirst(":","").replace(":","/")
//                    it.path.replace(":","/") to it.extensions.findByType<KScienceReadmeExtension>()
//                }

                if (rootReadmeExtension.readmeTemplate.exists()) {

                    val modulesString = buildString {
                        subprojects.forEach { subproject ->
//                            val name = subproject.name
                            subproject.extensions.findByType<KScienceReadmeExtension>()
                                ?.let { ext: KScienceReadmeExtension ->
                                    val path = subproject.path.replaceFirst(":", "").replace(":", "/")
                                    appendLine("\n### [$path]($path)")
                                    ext.description?.let { appendLine("> ${ext.description}") }
                                    appendLine(">\n> **Maturity**: ${ext.maturity}")
                                    val featureString = ext.featuresString(itemPrefix = "> - ", pathPrefix = "$path/")
                                    if (featureString.isNotBlank()) {
                                        appendLine(">\n> **Features:**")
                                        appendLine(featureString)
                                    }
                                }
                        }
                    }

                    rootReadmeExtension.property("modules", modulesString)

                    rootReadmeExtension.readmeString()?.let {
                        readmeFile.writeText(it)
                    }
                }

            }
        }

        tasks.withType<AbstractDokkaTask> {
            dependsOn(generateReadme)
        }

        tasks.create("version") {
            group = "publishing"
            val versionFile = project.buildDir.resolve("project-version.txt")
            outputs.file(versionFile)
            doLast {
                versionFile.createNewFile()
                versionFile.writeText(project.version.toString())
                println(project.version)
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
