package ru.mipt.npm.gradle.internal

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import ru.mipt.npm.gradle.KScienceExtension.DependencyConfiguration
import ru.mipt.npm.gradle.KScienceExtension.DependencySourceSet

internal fun Project.useDependency(
    vararg pairs: Pair<String, String>,
    dependencySourceSet: DependencySourceSet = DependencySourceSet.MAIN,
    dependencyConfiguration: DependencyConfiguration = DependencyConfiguration.IMPLEMENTATION
) {
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        configure<KotlinMultiplatformExtension> {
            sourceSets {
                pairs.forEach { (target, dep) ->
                    val name = target + dependencySourceSet.suffix
                    findByName(name)?.apply {
                        dependencies {
                            when (dependencyConfiguration) {
                                DependencyConfiguration.API -> api(dep)
                                DependencyConfiguration.IMPLEMENTATION -> implementation(dep)
                                DependencyConfiguration.COMPILE_ONLY -> compileOnly(dep)
                            }
                        }
                    }
                }
            }
        }
    }

    pairs.find { it.first == "jvm" }?.let { dep ->
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            configure<KotlinJvmProjectExtension> {
                sourceSets.findByName(dependencySourceSet.setName)?.apply {
                    dependencies.apply {
                        val configurationName = when (dependencyConfiguration) {
                            DependencyConfiguration.API -> apiConfigurationName
                            DependencyConfiguration.IMPLEMENTATION -> implementationConfigurationName
                            DependencyConfiguration.COMPILE_ONLY -> compileOnlyConfigurationName
                        }
                        add(configurationName, dep.second)
                    }
                }
            }
        }
    }

    pairs.find { it.first == "js" }?.let { dep ->
        pluginManager.withPlugin("org.jetbrains.kotlin.js") {
            configure<KotlinJsProjectExtension> {
                sourceSets.findByName(dependencySourceSet.setName)?.apply {
                    dependencies.apply {
                        val configurationName = when (dependencyConfiguration) {
                            DependencyConfiguration.API -> apiConfigurationName
                            DependencyConfiguration.IMPLEMENTATION -> implementationConfigurationName
                            DependencyConfiguration.COMPILE_ONLY -> compileOnlyConfigurationName
                        }
                        add(configurationName, dep.second)
                    }
                }
            }
        }
    }
}

internal fun Project.useCommonDependency(
    dep: String,
    dependencySourceSet: DependencySourceSet = DependencySourceSet.MAIN,
    dependencyConfiguration: DependencyConfiguration = DependencyConfiguration.IMPLEMENTATION
): Unit = pluginManager.run {
    withPlugin("org.jetbrains.kotlin.multiplatform") {
        configure<KotlinMultiplatformExtension> {
            sourceSets.findByName("common${dependencySourceSet.suffix}")?.apply {
                dependencies {
                    when (dependencyConfiguration) {
                        DependencyConfiguration.API -> api(dep)
                        DependencyConfiguration.IMPLEMENTATION -> implementation(dep)
                        DependencyConfiguration.COMPILE_ONLY -> compileOnly(dep)
                    }
                }
            }
        }
    }


    withPlugin("org.jetbrains.kotlin.jvm") {
        configure<KotlinJvmProjectExtension> {
            sourceSets.findByName(dependencySourceSet.setName)?.apply {
                dependencies.apply {
                    val configurationName = when (dependencyConfiguration) {
                        DependencyConfiguration.API -> apiConfigurationName
                        DependencyConfiguration.IMPLEMENTATION -> implementationConfigurationName
                        DependencyConfiguration.COMPILE_ONLY -> compileOnlyConfigurationName
                    }
                    add(configurationName, dep)
                }
            }
        }
    }
    withPlugin("org.jetbrains.kotlin.js") {
        configure<KotlinJsProjectExtension> {
            sourceSets.findByName(dependencySourceSet.setName)?.apply {
                dependencies.apply {
                    val configurationName = when (dependencyConfiguration) {
                        DependencyConfiguration.API -> apiConfigurationName
                        DependencyConfiguration.IMPLEMENTATION -> implementationConfigurationName
                        DependencyConfiguration.COMPILE_ONLY -> compileOnlyConfigurationName
                    }
                    add(configurationName, dep)
                }
            }
        }
    }
}