package ru.mipt.npm.gradle

import groovy.text.SimpleTemplateEngine
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.changelog.ChangelogPlugin
import kotlin.collections.component1
import kotlin.collections.component2

class KSciencePublishingExtension(val project: Project) {
    var githubOrg: String? by project.extra
    var githubProject: String? by project.extra
    var spaceRepo: String? by project.extra
    var spaceUser: String? by project.extra
    var spaceToken: String? by project.extra
    var bintrayOrg: String? by project.extra
    var bintrayUser: String? by project.extra
    var bintrayApiKey: String? by project.extra
    var bintrayRepo: String? by project.extra
}

class KScienceReadmeExtension(val project: Project) {
    val properties = HashMap<String, String>()
    var readmeStubPath: String = "docs/README-STUB.md"
    val features = ArrayList<Feature>()

    data class Feature(val id: String, val ref: String, val description: String, val name: String = id)


    fun feature(id: String, ref: String, description: String, name: String = id) {
        features.add(Feature(id, ref, description, name))
    }

    /**
     * Generate a markdown string listing features
     */
    fun featuresString(itemPrefix: String = " - ", pathPrefix: String = "") = buildString {
        features.forEach {
            appendln("$itemPrefix[${it.id}]($pathPrefix${it.ref}) : ${it.description}")
        }
    }

    /**
     * Generate a readme string from the stub
     */
    fun readmeString(): String? {
        val readmeStubFile = project.file(readmeStubPath)
        return if (readmeStubFile.exists()) {
            buildString {

                val readmeProperties: Map<String, Any?> = (properties + mapOf(
                    "name" to project.name,
                    "group" to project.group,
                    "version" to project.version,
                    "features" to featuresString()
                )).withDefault { null }
                SimpleTemplateEngine().createTemplate(readmeStubFile).make(properties).toString()
            }
        } else {
            null
        }
    }
}

/**
 * Apply extension and repositories
 */
open class KScienceProjectPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = target.run {
        apply<ChangelogPlugin>()
        val rootReadmeExtension = KScienceReadmeExtension(this)
        extensions.add("ksciencePublish", KSciencePublishingExtension(this))
        extensions.add("kscienceReadme", rootReadmeExtension)

        //Add readme generators to individual subprojects
        subprojects {
            val readmeExtension = KScienceReadmeExtension(this)
            extensions.add("kscienceReadme", readmeExtension)
            tasks.create("generateReadme") {
                group = "documentation"
                description = "Generate a README file if stub is present"
                doLast {
                    val readmeString = readmeExtension.readmeString()
                    if (readmeString != null) {
                        val readmeFile = file("README.md")
                        readmeFile.writeText(readmeString)
                    }
                }
            }
        }

        tasks.create("generateReadme") {
            group = "documentation"
            description = "Generate a README file and a feature matrix if stub is present"

            doLast {
                val reader = groovy.json.JsonSlurper()
                val projects = subprojects.associate {
                    it.name to it.extensions.findByType<KScienceReadmeExtension>()
                }


                val rootReadmeStub = project.file(rootReadmeExtension.readmeStubPath)
                if (rootReadmeStub.exists()) {

                    val modulesString = buildString {
                        projects.entries.filter { !it.value?.features.isNullOrEmpty() }.forEach { (name, ext) ->
                            appendln("### [$name]($name)")
                            appendln(ext!!.featuresString(pathPrefix = "$name/"))
                        }
                    }

                    val rootReadmeProperties: Map<String, Any> = mapOf(
                        "name" to project.name,
                        "group" to project.group,
                        "version" to project.version,
                        "modulesString" to modulesString
                    )

                    val readmeFile = project.file("README.md")
                    readmeFile.writeText(
                        SimpleTemplateEngine().createTemplate(rootReadmeStub).make(rootReadmeProperties).toString()
                    )
                }

            }
        }
    }
}