package ru.mipt.npm.gradle

import groovy.text.SimpleTemplateEngine
import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.BinaryCompatibilityValidatorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.changelog.ChangelogPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import ru.mipt.npm.gradle.internal.*

@Suppress("unused")
class KSciencePublishingExtension(val project: Project) {
    private var initializedFlag = false

    fun configurePublications(vcsUrl: String) {
        if (!initializedFlag) {
            project.setupPublication(vcsUrl)
            initializedFlag = true
        }
    }

    /**
     * github publishing
     */
    fun github(githubProject: String, githubOrg: String = "mipt-npm") {
        //automatically initialize vcs using github
        if (!initializedFlag) {
            configurePublications("https://github.com/$githubOrg/$githubProject")
        }
        project.addGithubPublishing(githubOrg, githubProject)
    }

    /**
     *  Space publishing
     */
    fun space(spaceRepo: String = "https://maven.pkg.jetbrains.space/mipt-npm/p/sci/maven") {
        require(initializedFlag) { "The publishing is not set up use 'configurePublications' method to do so" }
        project.addSpacePublishing(spaceRepo)
    }

//    // Bintray publishing
//    var bintrayOrg: String? by project.extra
//    var bintrayUser: String? by project.extra
//    var bintrayApiKey: String? by project.extra
//    var bintrayRepo: String? by project.extra

    /**
     *  Sonatype publishing
     */
    fun sonatype() {
        require(initializedFlag) { "The publishing is not set up use 'configurePublications' method to do so" }
        project.addSonatypePublishing()
    }
}


/**
 * Apply extension and repositories
 */
open class KScienceProjectPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = target.run {
        apply<ChangelogPlugin>()
        apply<DokkaPlugin>()
        apply<BinaryCompatibilityValidatorPlugin>()

        val rootReadmeExtension = KScienceReadmeExtension(this)
        extensions.add("ksciencePublish", KSciencePublishingExtension(this))
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
                readmeExtension.additionalFiles.forEach {
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
            tasks.withType<DokkaTask> {
                dependsOn(generateReadme)
            }
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
            rootReadmeExtension.additionalFiles.forEach {
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
                            appendln("<hr/>")
                            appendln("\n* ### [$name]($path)")
                            if (ext != null) {
                                appendln("> ${ext.description}")
                                appendln(">\n> **Maturity**: ${ext.maturity}")
                                val featureString = ext.featuresString(itemPrefix = "> - ", pathPrefix = "$path/")
                                if (featureString.isNotBlank()) {
                                    appendln(">\n> **Features:**")
                                    appendln(featureString)
                                }
                            }
                        }
                        appendln("<hr/>")
                    }

                    val rootReadmeProperties: Map<String, Any?> =
                        rootReadmeExtension.actualizedProperties + ("modules" to modulesString)

                    readmeFile.writeText(
                        SimpleTemplateEngine().createTemplate(rootReadmeExtension.readmeTemplate)
                            .make(rootReadmeProperties).toString()
                    )
                }

            }
        }

        tasks.withType<DokkaTask> {
            dependsOn(generateReadme)
        }

        val patchChangelog by tasks.getting

        val release by tasks.creating {
            group = RELEASE_GROUP
            description = "Publish development or production release based on version suffix"
            dependsOn(generateReadme)
        }

        // Disable API validation for snapshots
        if (isSnapshot()) {
            extensions.findByType<ApiValidationExtension>()?.apply {
                validationDisabled = true
                logger.warn("API validation is disabled for snapshot or dev version")
            }
        }
    }

    companion object {
        const val RELEASE_GROUP = "release"
    }
}