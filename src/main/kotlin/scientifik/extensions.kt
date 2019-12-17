package scientifik

import Scientifik
import kotlinx.atomicfu.plugin.gradle.sourceSets
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal fun Project.useDependency(vararg pairs: Pair<String, String>) {
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.findByType<KotlinMultiplatformExtension>()?.apply {
            sourceSets {
                pairs.forEach { (name, dep)->
                    findByName(name)?.apply {
                        dependencies {
                            implementation(dep)
                        }
                    }
                }
            }
        }
    }
    pairs.find { it.first == "jvmMain" }?.let { dep ->
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            sourceSets.findByName("main")?.apply {
                this as KotlinSourceSet
                dependencies {
                    implementation(dep)
                }
            }
        }
    }

    pairs.find { it.first == "jsMain" }?.let { dep ->
        pluginManager.withPlugin("org.jetbrains.kotlin.js") {
            sourceSets.findByName("main")?.apply {
                this as KotlinSourceSet
                dependencies {
                    implementation(dep)
                }
            }
        }
    }
}


fun Project.useSerialization(version: String = Scientifik.serializationVersion) = useDependency(
    "commonMain" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$version",
    "jvmMain" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime:$version",
    "jsMain" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$version",
    "nativeMain" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$version"
)

fun Project.useCoroutines(version: String = Scientifik.coroutinesVersion) = useDependency(
    "commonMain" to "org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$version",
    "jvmMain" to "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version",
    "jsMain" to "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$version",
    "nativeMain" to "org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$version"
)