package space.kscience.gradle.internal

import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import space.kscience.gradle.KScienceVersions


internal val defaultKotlinJvmArgs: List<String> = listOf(
    "-Xjvm-default=all"
)

internal val defaultKotlinCommonArgs: List<String> = listOf(
    "-Xexpect-actual-classes"
)

internal fun resolveKotlinVersion(): KotlinVersion {
    val (major, minor, patch) = KScienceVersions.kotlinVersion.split(".", "-")
    return KotlinVersion(major.toInt(), minor.toInt(), patch.toInt())
}

internal fun LanguageSettingsBuilder.applySettings(
    kotlinVersion: KotlinVersion = resolveKotlinVersion(),
) {
    val versionString = "${kotlinVersion.major}.${kotlinVersion.minor}"
    languageVersion = versionString
    apiVersion = versionString
    progressiveMode = true


    optIn("kotlin.RequiresOptIn")
    optIn("kotlin.ExperimentalStdlibApi")
    optIn("kotlin.time.ExperimentalTime")
    optIn("kotlin.contracts.ExperimentalContracts")
}

internal fun Copy.fromJsDependencies(configurationName: String) = project.run {
    val configuration = configurations[configurationName]
        ?: error("Configuration with name $configurationName could not be resolved.")
    val projectDeps = configuration.allDependencies.filterIsInstance<ProjectDependency>().map {
        it.dependencyProject
    }
    projectDeps.forEach { dep ->
        dep.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            dep.tasks.findByName("jsProcessResources")?.let { task ->
                dependsOn(task)
                from(task)
            }
        }
        dep.pluginManager.withPlugin("org.jetbrains.kotlin.js") {
            dep.tasks.findByName("processResources")?.let { task ->
                dependsOn(task)
                from(task)
            }
        }
    }
}