package ru.mipt.npm.gradle

import groovy.text.SimpleTemplateEngine
import kotlinx.validation.BinaryCompatibilityValidatorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.changelog.ChangelogPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import kotlin.collections.component1
import kotlin.collections.component2

class KSciencePublishingExtension(val project: Project) {
    var vcs: String? by project.extra

    // github publishing
    var githubOrg: String? by project.extra
    var githubProject: String? by project.extra

    // Space publishing
    var spaceRepo: String? by project.extra
    var spaceUser: String? by project.extra
    var spaceToken: String? by project.extra

    // Bintray publishing
    var bintrayOrg: String? by project.extra
    var bintrayUser: String? by project.extra
    var bintrayApiKey: String? by project.extra
    var bintrayRepo: String? by project.extra

    // Sonatype publising
    var sonatypeUser: String? by project.extra
    var sonatypePassword: String? by project.extra
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

                if(readmeExtension.readmeTemplate.exists()) {
                    inputs.file(readmeExtension.readmeTemplate)
                }
                readmeExtension.additionalFiles.forEach {
                    if(it.exists()){
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
            tasks.withType<DokkaTask>{
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

            if(rootReadmeExtension.readmeTemplate.exists()) {
                inputs.file(rootReadmeExtension.readmeTemplate)
            }
            rootReadmeExtension.additionalFiles.forEach {
                if(it.exists()){
                    inputs.file(it)
                }
            }

            val readmeFile = project.file("README.md")
            outputs.file(readmeFile)

            doLast {
                val projects = subprojects.associate {
                    it.name to it.extensions.findByType<KScienceReadmeExtension>()
                }

                if (rootReadmeExtension.readmeTemplate.exists()) {

                    val modulesString = buildString {
                        projects.entries.forEach { (name, ext) ->
                            appendln("<hr/>")
                            appendln("\n* ### [$name]($name)")
                            if (ext != null) {
                                appendln("> ${ext.description}")
                                appendln(">\n> **Maturity**: ${ext.maturity}")
                                val featureString = ext.featuresString(itemPrefix = "> - ", pathPrefix = "$name/")
                                if(featureString.isNotBlank()) {
                                    appendln(">\n> **Features:**")
                                    appendln(featureString)
                                }
                            }
                        }
                        appendln("<hr/>")
                    }

                    val rootReadmeProperties: Map<String, Any?> = mapOf(
                        "name" to project.name,
                        "group" to project.group,
                        "version" to project.version,
                        "modules" to modulesString
                    )

                    readmeFile.writeText(
                        SimpleTemplateEngine().createTemplate(rootReadmeExtension.readmeTemplate)
                            .make(rootReadmeProperties).toString()
                    )
                }

            }
        }

        tasks.withType<DokkaTask>{
            dependsOn(generateReadme)
        }

        val patchChangelog by tasks.getting

        val release by tasks.creating{
            afterEvaluate {
                group = RELEASE_GROUP
                description = "Publish development or production release based on version suffix"
                dependsOn(generateReadme)
                tasks.findByName("publishAllPublicationsToBintrayRepository")?.let {
                    dependsOn(it)
                }
                tasks.findByName("publishAllPublicationsToSpaceRepository")?.let {
                    dependsOn(it)
                }
            }
        }
    }
    companion object{
        const val RELEASE_GROUP = "release"
    }
}