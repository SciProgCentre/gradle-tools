package ru.mipt.npm.gradle.internal

import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder

internal fun LanguageSettingsBuilder.applySettings(): Unit {
    progressiveMode = true
    enableLanguageFeature("InlineClasses")
    useExperimentalAnnotation("kotlin.Experimental")
    useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
    useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
    useExperimentalAnnotation("kotlin.time.ExperimentalTime")
    useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
    useExperimentalAnnotation("kotlin.js.ExperimentalJsExport")
}

internal fun RepositoryHandler.applyRepos(): Unit {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
    maven("https://dl.bintray.com/mipt-npm/kscience")
    maven("https://dl.bintray.com/mipt-npm/dev")
    maven("https://dl.bintray.com/mipt-npm/dataforge")
}

internal fun Copy.fromDependencies(configurationName: String) = project.afterEvaluate {
    val configuration = configurations[configurationName]
        ?: error("Configuration with name $configurationName could not be resolved.")
    val projectDeps = configuration.allDependencies.filterIsInstance<ProjectDependency>().map {
        it.dependencyProject
    }
    into(buildDir.resolve("processedResources/js"))
    projectDeps.forEach { dep ->
        dep.afterEvaluate {
            dep.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                dep.tasks.findByName("jsProcessResources")?.let { task ->
                    dependsOn(task)
                    from(task)
                    //from(dep.buildDir.resolve("processedResources/js"))
                }
                //from(dep.buildDir.resolve("processedResources/js"))
            }
            dep.pluginManager.withPlugin("org.jetbrains.kotlin.js") {
                dep.tasks.findByName("processResources")?.let { task ->
                    dependsOn(task)
                    from(task)
                    //from(dep.buildDir.resolve("processedResources/js"))
                }
                // from(dep.buildDir.resolve("processedResources/js"))
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