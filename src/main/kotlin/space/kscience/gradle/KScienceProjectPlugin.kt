package space.kscience.gradle

import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.BinaryCompatibilityValidatorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.*
import org.jetbrains.changelog.ChangelogPlugin
import org.jetbrains.changelog.ChangelogPluginExtension
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaPlugin
import space.kscience.gradle.internal.*

/**
 * Simplifies adding repositories for Maven publishing, responds for releasing tasks for projects.
 */
public class KSciencePublishingExtension(public val project: Project) {
    private var isVcsInitialized = false
    internal val repositoryNames = mutableSetOf<String>()

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
     * @param addToRelease publish packages in the `release` task to the GitHub repository.
     */
    public fun github(
        githubOrg: String,
        githubProject: String,
        addToRelease: Boolean = project.requestPropertyOrNull("publishing.github") == "true",
    ) {

        if (addToRelease) {
            try {
                project.addGithubPublishing(githubOrg, githubProject)
                repositoryNames += "github"
            } catch (t: Throwable) {
                project.logger.error("Failed to set up github publication", t)
            }
        }
    }

    /**
     * Adds Space Packages Maven repository to publishing.
     *
     * @param spaceRepo the repository URL.
     * @param addToRelease publish packages in the `release` task to the Space repository.
     */
    public fun space(
        spaceRepo: String,
        addToRelease: Boolean = project.requestPropertyOrNull("publishing.space") != "false",
    ) {
        project.addSpacePublishing(spaceRepo)

        if (addToRelease) repositoryNames += "space"
    }

    /**
     * Adds Sonatype Maven repository to publishing.
     *
     * @param addToRelease  publish packages in the `release` task to the Sonatype repository.
     */
    public fun sonatype(
        addToRelease: Boolean = (project.requestPropertyOrNull("publishing.sonatype") != "false"),
    ) {
        require(isVcsInitialized) { "The project vcs is not set up use 'pom' method to do so" }
        project.addSonatypePublishing()

        if (addToRelease) repositoryNames += "sonatype"
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

            @Suppress("UNUSED_VARIABLE")
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
                            val name = subproject.name
                            val path = subproject.path.replaceFirst(":", "").replace(":", "/")
                            val ext = subproject.extensions.findByType<KScienceReadmeExtension>()
                            appendLine("\n### [$name]($path)")
                            if (ext != null) {
                                appendLine("> ${ext.description}")
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


        val releaseAll by tasks.creating {
            group = RELEASE_GROUP
            description = "Publish development or production release based on version suffix"
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

        allprojects {
            afterEvaluate {
                ksciencePublish.repositoryNames.forEach { repositoryName ->
                    val repositoryNameCapitalized = repositoryName.capitalize()

                    val pattern = "publish(?<publication>.*)PublicationTo${repositoryNameCapitalized}Repository"
                        .toRegex()

                    tasks.withType<PublishToMavenRepository>().toList().forEach forEachPublication@{
                        val matchResult = pattern.matchEntire(it.name) ?: return@forEachPublication
                        val publicationName = matchResult.groups["publication"]!!.value.capitalize()
                        val releaseTaskName = "release$publicationName"

                        val targetReleaseTask =
                            rootProject.tasks.findByName(releaseTaskName) ?: rootProject.tasks.create(releaseTaskName) {
                                group = RELEASE_GROUP
                                description =
                                    "Publish platform release artifact for $publicationName to all repositories"
                            }

                        releaseAll.dependsOn(targetReleaseTask)

                        targetReleaseTask.dependsOn(it)
                    }
                }
            }
        }

        // Disable API validation for snapshots
        if (isInDevelopment) {
            extensions.findByType<ApiValidationExtension>()?.apply {
                validationDisabled = true
                logger.warn("API validation is disabled for snapshot or dev version")
            }
        }
    }

    public companion object {
        public const val RELEASE_GROUP: String = "release"
    }
}
