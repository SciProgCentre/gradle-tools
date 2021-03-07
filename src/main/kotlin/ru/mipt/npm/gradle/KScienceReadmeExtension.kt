package ru.mipt.npm.gradle

import groovy.text.SimpleTemplateEngine
import kotlinx.validation.ApiValidationExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import java.io.File

enum class Maturity {
    PROTOTYPE,
    EXPERIMENTAL,
    DEVELOPMENT,
    STABLE
}


class KScienceReadmeExtension(val project: Project) {
    var description: String = project.description ?: ""
    var maturity: Maturity = Maturity.EXPERIMENTAL
        set(value) {
            field = value
            val projectName = project.name
            if (value == Maturity.EXPERIMENTAL || value == Maturity.PROTOTYPE) {
                project.rootProject.run {
                    plugins.withId("org.jetbrains.kotlinx.binary-compatibility-validator") {
                        extensions.getByType<ApiValidationExtension>().apply {
                            project.logger.warn("$value project $projectName is excluded from API validation")
                            ignoredProjects.add(projectName)
                        }
                    }
                }
            }
        }

    var readmeTemplate: File = project.file("docs/README-TEMPLATE.md")

    data class Feature(val id: String, val description: String, val ref: String?, val name: String = id)

    val features = ArrayList<Feature>()

    fun feature(id: String, description: String, ref: String? = null, name: String = id) {
        features.add(Feature(id, description, ref, name))
    }

    fun feature(id: String, ref: String? = null, name: String = id, description: () -> String) {
        features.add(Feature(id, description(), ref, name))
    }

    private val properties: MutableMap<String, () -> Any?> = mutableMapOf(
        "name" to { project.name },
        "group" to { project.group },
        "version" to { project.version },
        "features" to { featuresString() }
    )

    val actualizedProperties
        get() = properties.mapValues { (_, value) ->
            value.invoke()
        }

    fun property(key: String, value: Any?) {
        properties[key] = { value }
    }

    fun propertyByTemplate(key: String, template: String) {
        val actual = actualizedProperties
        properties[key] = { SimpleTemplateEngine().createTemplate(template).make(actual).toString() }
    }

    internal val additionalFiles = ArrayList<File>()

    fun propertyByTemplate(key: String, template: File) {
        val actual = actualizedProperties
        properties[key] = { SimpleTemplateEngine().createTemplate(template).make(actual).toString() }
        additionalFiles.add(template)
    }

    /**
     * Generate a markdown string listing features
     */
    fun featuresString(itemPrefix: String = " - ", pathPrefix: String = "") = buildString {
        features.forEach {
            appendln("$itemPrefix[${it.name}]($pathPrefix${it.ref ?: "#"}) : ${it.description}")
        }
    }

    /**
     * Generate a readme string from the stub
     */
    fun readmeString(): String? {
        return if (readmeTemplate.exists()) {
            val actual = actualizedProperties
            SimpleTemplateEngine().createTemplate(readmeTemplate).make(actual).toString()
        } else {
            null
        }
    }
}