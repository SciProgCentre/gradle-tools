package ru.mipt.npm.gradle.internal

import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder

internal fun LanguageSettingsBuilder.applySettings(): Unit {
    languageVersion = "1.5"
    apiVersion = "1.5"
    progressiveMode = true
    useExperimentalAnnotation("kotlin.Experimental")
    useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
    useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
    useExperimentalAnnotation("kotlin.time.ExperimentalTime")
    useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
    useExperimentalAnnotation("kotlin.js.ExperimentalJsExport")
}

internal fun RepositoryHandler.applyRepos(): Unit {
    mavenCentral()
    maven("https://repo.kotlin.link")
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
}

internal fun Copy.fromJsDependencies(configurationName: String) = project.afterEvaluate {
    val configuration = configurations[configurationName]
        ?: error("Configuration with name $configurationName could not be resolved.")
    val projectDeps = configuration.allDependencies.filterIsInstance<ProjectDependency>().map {
        it.dependencyProject
    }
    projectDeps.forEach { dep ->
        dep.afterEvaluate {
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
}


//
//internal fun Copy.copyJVMResources(configuration: Configuration): Unit = project.afterEvaluate {
//    val projectDeps = configuration.allDependencies
//        .filterIsInstance<ProjectDependency>()
//        .map { it.dependencyProject }
//
//    projectDeps.forEach { dep ->
//        dep.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
//            dep.tasks.findByName("jvmProcessResources")?.let { task ->
//                dependsOn(task)
//                from(task)
//            }
//        }
//        dep.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
//            dep.tasks.findByName("processResources")?.let { task ->
//                dependsOn(task)
//                from(task)
//            }
//        }
//    }
//}