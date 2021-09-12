package ru.mipt.npm.gradle

import groovy.text.SimpleTemplateEngine
import kotlinx.validation.ApiValidationExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import java.io.File

public enum class Maturity {
    PROTOTYPE,
    EXPERIMENTAL,
    DEVELOPMENT,
    STABLE
}


public class KScienceReadmeExtension(public val project: Project) {
    public var description: String = project.description ?: ""

    public var maturity: Maturity = Maturity.EXPERIMENTAL
        set(value) {
            field = value
            val projectName = project.name
            if (value == Maturity.EXPERIMENTAL || value == Maturity.PROTOTYPE) {
                project.rootProject.run {
                    plugins.withId("org.jetbrains.kotlinx.binary-compatibility-validator") {
                        extensions.findByType<ApiValidationExtension>()?.apply {
                            project.logger.warn("$value project $projectName is excluded from API validation")
                            ignoredProjects.add(projectName)
                        }
                    }
                }
            }
        }

    public var readmeTemplate: File = project.file("docs/README-TEMPLATE.md")

    public data class Feature(val id: String, val description: String, val ref: String?, val name: String = id)

    public val features: MutableList<Feature> = ArrayList()

    @Deprecated("Use lambda builder instead.")
    public fun feature(id: String, description: String, ref: String? = null, name: String = id) {
        features += Feature(id, description, ref, name)
    }

    public fun feature(id: String, ref: String? = null, name: String = id, description: () -> String) {
        features += Feature(id, description(), ref, name)
    }

    private val properties: MutableMap<String, () -> Any?> = mutableMapOf(
        "name" to { project.name },
        "group" to { project.group },
        "version" to { project.version },
        "features" to { featuresString() }
    )

    public val actualizedProperties: Map<String, Any?>
        get() = properties.mapValues { (_, value) -> value() }

    public fun property(key: String, value: Any?) {
        properties[key] = { value }
    }

    public fun property(key: String, value: () -> Any?) {
        properties[key] = value
    }

    public fun propertyByTemplate(key: String, template: String) {
        val actual = actualizedProperties
        properties[key] = { SimpleTemplateEngine().createTemplate(template).make(actual).toString() }
    }

    internal val additionalFiles = ArrayList<File>()

    public fun propertyByTemplate(key: String, template: File) {
        val actual = actualizedProperties
        properties[key] = { SimpleTemplateEngine().createTemplate(template).make(actual).toString() }
        additionalFiles += template
    }

    /**
     * Generate a markdown string listing features
     */
    public fun featuresString(itemPrefix: String = " - ", pathPrefix: String = ""): String = buildString {
        features.forEach {
            appendLine("$itemPrefix[${it.name}]($pathPrefix${it.ref ?: "#"}) : ${it.description}")
        }
    }

    /**
     * Generate a readme string from the stub
     */
    public fun readmeString(): String? = if (readmeTemplate.exists()) {
        val actual = actualizedProperties
        SimpleTemplateEngine().createTemplate(readmeTemplate).make(actual).toString()
    } else {
        null
    }
}
