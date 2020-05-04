package scientifik

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import java.io.File

internal fun LanguageSettingsBuilder.applySettings(): Unit {
    progressiveMode = true
    enableLanguageFeature("InlineClasses")
    useExperimentalAnnotation("kotlin.Experimental")
    useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
    useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
    useExperimentalAnnotation("kotlin.time.ExperimentalTime")
}

internal fun RepositoryHandler.applyRepos(): Unit {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/mipt-npm/scientifik")
    maven("https://dl.bintray.com/mipt-npm/dev")
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
    maven("https://dl.bintray.com/mipt-npm/dataforge")
}


internal fun Copy.copyJSResources(configuration: Configuration): Unit = project.afterEvaluate {
    val projectDeps = configuration
        .allDependencies
        .filterIsInstance<ProjectDependency>()
        .map { it.dependencyProject }

    val destination = destinationDir

    projectDeps.forEach { dep ->
        dep.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            dep.tasks.findByName("jsProcessResources")?.let { task ->
                val sourceDir = (task as Copy).destinationDir
                inputs.files(sourceDir)
                dependsOn(task)
                from(sourceDir)
            }
        }
        dep.pluginManager.withPlugin("org.jetbrains.kotlin.js") {
            dep.tasks.findByName("processResources")?.let { task ->
                val sourceDir = (task as Copy).destinationDir
                inputs.files(sourceDir)
                dependsOn(task)
                from(sourceDir)
            }
        }
    }
}


val Project.jsDistDirectory: File
    get() {
        val distributionName = listOf(
            name,
            "js",
            version.toString()
        ).joinToString("-")

        return buildDir.resolve(
            "distributions/$distributionName"
        )
    }