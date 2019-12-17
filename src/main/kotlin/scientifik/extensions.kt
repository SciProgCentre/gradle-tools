package scientifik

import Scientifik
import kotlinx.atomicfu.plugin.gradle.sourceSets
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

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
               dependencies.apply{
                   add(implementationConfigurationName, dep.second)
               }
            }
        }
    }

    pairs.find { it.first == "jsMain" }?.let { dep ->
        pluginManager.withPlugin("org.jetbrains.kotlin.js") {
            sourceSets.findByName("main")?.apply {
                sourceSets.findByName("main")?.apply {
                    dependencies.apply{
                        add(implementationConfigurationName, dep.second)
                    }
                }
            }
        }
    }
}


fun Project.useSerialization(version: String = Scientifik.serializationVersion) {
    plugins.apply("org.jetbrains.kotlin.plugin.serialization")
    useDependency(
        "commonMain" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$version",
        "jvmMain" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime:$version",
        "jsMain" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$version",
        "nativeMain" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$version"
    )
}

fun Project.useCoroutines(version: String = Scientifik.coroutinesVersion) = useDependency(
    "commonMain" to "org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$version",
    "jvmMain" to "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version",
    "jsMain" to "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$version",
    "nativeMain" to "org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$version"
)

//fun Project.useAtomic(version: String = Scientifik.atomicfuVersion) {
//    plugins.apply("kotlinx-atomicfu")
//    useDependency(
//        "commonMain" to "org.jetbrains.kotlinx:atomicfu-common:$version",
//        "jvmMain" to "org.jetbrains.kotlinx:atomicfu:$version",
//        "jsMain" to "org.jetbrains.kotlinx:atomicfu-js:$version",
//        "nativeMain" to "org.jetbrains.kotlinx:atomicfu-native:$version"
//    )
//}
