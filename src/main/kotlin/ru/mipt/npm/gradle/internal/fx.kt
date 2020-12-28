package ru.mipt.npm.gradle.internal

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import ru.mipt.npm.gradle.DependencyConfiguration
import ru.mipt.npm.gradle.FXModule
import ru.mipt.npm.gradle.FXPlatform

val defaultPlatform: FXPlatform = when {
    Os.isFamily(Os.FAMILY_WINDOWS) -> FXPlatform.WINDOWS
    Os.isFamily(Os.FAMILY_MAC) -> FXPlatform.MAC
    Os.isFamily(Os.FAMILY_UNIX) -> FXPlatform.LINUX
    else -> error("Platform not recognized")
}

private fun KotlinDependencyHandler.addFXDependencies(
    modules: List<FXModule>,
    configuration: DependencyConfiguration,
    version: String = "14",
    platform: FXPlatform = defaultPlatform
) {
    modules.flatMap { it.dependencies.toList() + it }.distinct().forEach {
        val notation = "org.openjfx:${it.artifact}:$version:${platform.id}"
        when (configuration) {
            DependencyConfiguration.API -> api(notation)
            DependencyConfiguration.IMPLEMENTATION -> implementation(notation)
            DependencyConfiguration.COMPILE_ONLY -> compileOnly(notation)
        }
    }
}

internal fun Project.useFx(
    modules: List<FXModule>,
    configuration: DependencyConfiguration = DependencyConfiguration.COMPILE_ONLY,
    version: String = "14",
    platform: FXPlatform = defaultPlatform
): Unit = afterEvaluate {
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.findByType<KotlinMultiplatformExtension>()?.apply {
            sourceSets.findByName("jvmMain")?.apply {
                dependencies {
                    addFXDependencies(modules, configuration = configuration, version = version, platform = platform)
                }
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.findByType<KotlinJvmProjectExtension>()?.apply {
            sourceSets.findByName("main")?.apply {
                dependencies {
                    addFXDependencies(modules, configuration = configuration, version = version, platform = platform)
                }
            }
        }
    }
}