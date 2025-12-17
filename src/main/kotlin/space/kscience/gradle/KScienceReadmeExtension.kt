package space.kscience.gradle

import freemarker.cache.StringTemplateLoader
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateNotFoundException
import kotlinx.html.TagConsumer
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import space.kscience.gradle.internal.withKScience
import space.kscience.gradle.internal.withKotlin
import java.io.File
import java.io.Serializable
import java.io.StringWriter


private fun Template.processToString(args: Map<String, Any?>): String {
    val writer = StringWriter()
    process(args, writer)
    return writer.toString()
}

public class KScienceReadmeExtension(private val kscience: KSciencePlatformExtension) {
    public val project: Project get() = kscience.project

    public var description: String? = null
        get() = field ?: project.description

    public var maturity: Maturity by kscience::maturity

    /**
     * If true, use default templates provided by plugin if override is not defined
     */
    public var useDefaultReadmeTemplate: Boolean = true

    /**
     * Use this template file if it is provided, otherwise use default template
     */
    public var readmeTemplate: File = project.file("docs/README-TEMPLATE.md")
        set(value) {
            field = value
            if (value.exists()) {
                fmLoader.putTemplate("readme", value.readText())
            }
        }

    private val fmLoader = StringTemplateLoader().apply {
        putTemplate(
            "artifact",
            KScienceReadmeExtension::class.java.getResource("/templates/ARTIFACT-TEMPLATE.md")!!.readText()
        )
        if (readmeTemplate.exists()) {
            putTemplate("readme", readmeTemplate.readText())
        } else if (useDefaultReadmeTemplate) {
            putTemplate(
                "readme",
                KScienceReadmeExtension::class.java.getResource("/templates/README-TEMPLATE.md")!!.readText()
            )
        }
    }

    private val fmCfg = Configuration(Configuration.VERSION_2_3_31).apply {
        defaultEncoding = "UTF-8"
        templateLoader = fmLoader
    }

    public data class Feature(val id: String, val description: String, val ref: String?, val name: String = id) :
        Serializable

    public val features: MutableList<Feature> = mutableListOf()

    /**
     * A plain readme feature with description
     */
    public fun feature(
        id: String,
        @Language("File") ref: String? = null,
        name: String = id,
        @Language("markdown") description: () -> String,
    ) {
        features += Feature(id, description(), ref, name)
    }

    /**
     * A readme feature with HTML description
     */
    public fun featureWithHtml(
        id: String,
        ref: String? = null,
        name: String = id,
        htmlBuilder: TagConsumer<String>.() -> Unit,
    ) {
        val text = createHTML().apply {
            div("readme-feature") {
                htmlBuilder()
            }
        }.finalize()
        features += Feature(id, text, ref, name)
    }

    private val properties: MutableMap<String, Project.() -> Any?> = mutableMapOf(
        "name" to { name },
        "group" to { group },
        "version" to { version },
        "description" to { description ?: "" },
        "features" to { featuresString() },
        "published" to { plugins.findPlugin("maven-publish") != null },
        "artifact" to {
            val projectProperties = mapOf(
                "name" to name,
                "group" to group,
                "version" to version
            )
            fmCfg.getTemplate("artifact").processToString(projectProperties)
        },
        "modules" to {
            buildString {
                subprojects.forEach { subproject ->
                    subproject.extensions.findByType<KScienceReadmeExtension>()?.let { ext ->
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
        }
    )

    public fun getPropertyValues(): Map<String, Any?> = properties.mapValues { (_, value) -> project.value() }

    public fun property(key: String, value: Any?) {
        properties[key] = { value }
    }

    public fun property(key: String, value: Project.() -> Any?) {
        properties[key] = value
    }

    public fun propertyByTemplate(key: String, templateString: String) {
        //need to freeze it, otherwise values could change
        val actual = getPropertyValues()
        fmLoader.putTemplate(key, templateString)
        val template = fmCfg.getTemplate(key)

        properties[key] = { template.processToString(actual) }
    }

    /**
     * Files that are use in readme generation
     */
    internal val inputFiles = ArrayList<File>()

    public fun propertyByTemplate(key: String, templateFile: File) {
        //need to freeze it, otherwise values could change
        val actual = getPropertyValues()
        fmLoader.putTemplate(key, templateFile.readText())
        val template: Template = fmCfg.getTemplate(key)

        properties[key] = { template.processToString(actual) }
        inputFiles += templateFile
    }

    /**
     * Generate a markdown string listing features
     */
    internal fun featuresString(itemPrefix: String = " - ", pathPrefix: String = ""): String = buildString {
        features.forEach {
            appendLine(
                "$itemPrefix[${it.name}]($pathPrefix${it.ref ?: "#"}) : ${
                    it.description.lines().firstOrNull() ?: ""
                }"
            )
        }
    }

    /**
     * Generate a readme string from the template
     */
    public fun readmeString(): String? = try {
        fmCfg.getTemplate("readme").processToString(getPropertyValues())
    } catch (ex: TemplateNotFoundException) {
        project.logger.warn("Template with name ${ex.templateName} not found in ${project.name}")
        null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KScienceReadmeExtension

        if (project != other.project) return false
        if (maturity != other.maturity) return false
        if (useDefaultReadmeTemplate != other.useDefaultReadmeTemplate) return false
        if (readmeTemplate != other.readmeTemplate) return false
        if (fmLoader != other.fmLoader) return false
        if (fmCfg != other.fmCfg) return false
        if (features != other.features) return false
        if (properties != other.properties) return false
        if (inputFiles != other.inputFiles) return false

        return true
    }

    override fun hashCode(): Int {
        var result = project.hashCode()
        result = 31 * result + maturity.hashCode()
        result = 31 * result + useDefaultReadmeTemplate.hashCode()
        result = 31 * result + readmeTemplate.hashCode()
        result = 31 * result + fmLoader.hashCode()
        result = 31 * result + fmCfg.hashCode()
        result = 31 * result + features.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + inputFiles.hashCode()
        return result
    }


}


internal fun KSciencePlatformExtension.configureReadme() = with(project) {

    //early return is readme is already configured
    if (extensions.findByType<KScienceReadmeExtension>() != null) return@with

    //Add readme generators to individual subprojects and root project
    val readmeExtension = KScienceReadmeExtension(this@configureReadme)
    this.extensions.add("readme", readmeExtension)

    this@configureReadme.extensions.add("readme", readmeExtension)


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

        // add dependency for this task on subprojects readme tasks
        subprojects {
            withKScience {
                extensions.findByType<KScienceReadmeExtension>()?.let { subProjectReadmeExtension ->
                    tasks.findByName("generateReadme")?.let { readmeTask ->
                        dependsOn(readmeTask)
                    }
                    inputs.property("features-${name}", subProjectReadmeExtension.features)
                }
            }
        }

        val readmeFile = file("README.md")
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


    // Enable API validation for production releases
    if (!isInDevelopment && isMature()) {
        withKotlin {
            extensions.configure<AbiValidationExtension> {
                @OptIn(ExperimentalAbiValidation::class)
                enabled.set(true)
            }
        }
    }
}