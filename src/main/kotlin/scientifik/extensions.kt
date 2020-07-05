package scientifik

import Scientifik
import kotlinx.atomicfu.plugin.gradle.sourceSets
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

enum class DependencyConfiguration {
    API,
    IMPLEMENTATION,
    COMPILE_ONLY
}

enum class DependencySourceSet(val setName: String, val suffix: String) {
    MAIN("main", "Main"),
    TEST("test", "Test")
}

internal fun Project.useDependency(
    vararg pairs: Pair<String, String>,
    dependencySourceSet: DependencySourceSet = DependencySourceSet.MAIN,
    dependencyConfiguration: DependencyConfiguration = DependencyConfiguration.IMPLEMENTATION
) {
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.findByType<KotlinMultiplatformExtension>()?.apply {
            sourceSets {
                pairs.forEach { (target, dep) ->
                    val name = target + dependencySourceSet.suffix
                    findByName(name)?.apply {
                        dependencies {
                            when (dependencyConfiguration) {
                                DependencyConfiguration.API -> api(dep)
                                DependencyConfiguration.IMPLEMENTATION -> implementation(dep)
                                DependencyConfiguration.COMPILE_ONLY-> compileOnly(dep)
                            }
                        }
                    }
                }
            }
        }
    }

    pairs.find { it.first == "jvm" }?.let { dep ->
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            sourceSets.findByName(dependencySourceSet.setName)?.apply {
                dependencies.apply {
                    val configurationName = when (dependencyConfiguration) {
                        DependencyConfiguration.API -> apiConfigurationName
                        DependencyConfiguration.IMPLEMENTATION -> implementationConfigurationName
                        DependencyConfiguration.COMPILE_ONLY-> compileOnlyConfigurationName
                    }
                    add(configurationName, dep.second)
                }
            }
        }
    }

    pairs.find { it.first == "js" }?.let { dep ->
        pluginManager.withPlugin("org.jetbrains.kotlin.js") {
            sourceSets.findByName(dependencySourceSet.setName)?.apply {
                dependencies.apply {
                    val configurationName = when (dependencyConfiguration) {
                        DependencyConfiguration.API -> apiConfigurationName
                        DependencyConfiguration.IMPLEMENTATION -> implementationConfigurationName
                        DependencyConfiguration.COMPILE_ONLY-> compileOnlyConfigurationName
                    }
                    add(configurationName, dep.second)
                }
            }
        }
    }
}

fun Project.useCoroutines(
    version: String = Scientifik.coroutinesVersion,
    sourceSet: DependencySourceSet = DependencySourceSet.MAIN,
    configuration: DependencyConfiguration = DependencyConfiguration.API
) = useDependency(
    "common" to "org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$version",
    "jvm" to "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version",
    "js" to "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$version",
    "native" to "org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$version",
    dependencySourceSet = sourceSet,
    dependencyConfiguration = configuration
)

fun Project.useAtomic(version: String = Scientifik.atomicVersion) {
    plugins.apply("kotlinx-atomicfu")
    useDependency(
        "commonMain" to "org.jetbrains.kotlinx:atomicfu-common:$version",
        "jvmMain" to "org.jetbrains.kotlinx:atomicfu:$version",
        "jsMain" to "org.jetbrains.kotlinx:atomicfu-js:$version",
        "nativeMain" to "org.jetbrains.kotlinx:atomicfu-native:$version"
    )
}
