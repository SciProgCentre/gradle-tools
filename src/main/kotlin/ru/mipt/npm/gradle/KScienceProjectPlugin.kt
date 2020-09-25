package ru.mipt.npm.gradle

import groovy.text.SimpleTemplateEngine
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.changelog.ChangelogPlugin
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.reflect.KFunction

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

enum class Maturity {
    PROTOTYPE,
    EXPERIMENTAL,
    DEVELOPMENT,
    PRODUCTION
}

class KScienceReadmeExtension(val project: Project) {
    var description: String = ""
    var maturity: Maturity = Maturity.EXPERIMENTAL

    var readmeTemplate: File = project.file("docs/README-TEMPLATE.md")//"docs/README-TEMPLATE.md"

    data class Feature(val id: String, val ref: String, val description: String, val name: String = id)

    val features = ArrayList<Feature>()

    fun feature(id: String, ref: String, description: String, name: String = id) {
        features.add(Feature(id, ref, description, name))
    }

    val properties: MutableMap<String, Any?> = mutableMapOf(
        "name" to project.name,
        "group" to project.group,
        "version" to project.version,
        "features" to featuresString()
    )

    private val actualizedProperties get() = properties.mapValues {(_,value)->
        if(value is KFunction<*>){
            value.call()
        } else {
            value
        }
    }

    fun property(key: String, value: Any?) {
        properties[key] = value
    }

    fun propertyByTemplate(key: String, template: String){
        properties[key] = SimpleTemplateEngine().createTemplate(template).make(actualizedProperties).toString()
    }

    fun propertyByTemplate(key: String, template: File){
        properties[key] = SimpleTemplateEngine().createTemplate(template).make(actualizedProperties).toString()
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
        return if (readmeTemplate.exists()) {
            SimpleTemplateEngine().createTemplate(readmeTemplate).make(actualizedProperties).toString()
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
        extensions.add("readme", rootReadmeExtension)

        //Add readme generators to individual subprojects
        subprojects {
            val readmeExtension = KScienceReadmeExtension(this)
            extensions.add("readme", readmeExtension)
            tasks.create("generateReadme") {
                group = "documentation"
                description = "Generate a README file if stub is present"
                doLast {
                    val readmeString = readmeExtension.readmeString()
                    if (readmeString != null) {
                        val readmeFile = this@subprojects.file("README.md")
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

                if (rootReadmeExtension.readmeTemplate.exists()) {

                    val modulesString = buildString {
                        projects.entries.forEach { (name, ext) ->
                            appendln("### [$name]($name)")
                            if (ext != null) {
                                appendln(ext.description)
                                appendln("**Maturity**: ${ext.maturity}")
                                appendln("#### Features:")
                                appendln(ext.featuresString(pathPrefix = "$name/"))
                            }
                        }
                    }

                    val rootReadmeProperties: Map<String, Any?> = mapOf(
                        "name" to project.name,
                        "group" to project.group,
                        "version" to project.version,
                        "modules" to modulesString
                    )

                    val readmeFile = project.file("README.md")
                    readmeFile.writeText(
                        SimpleTemplateEngine().createTemplate(rootReadmeExtension.readmeTemplate).make(rootReadmeProperties).toString()
                    )
                }

            }
        }
    }
}