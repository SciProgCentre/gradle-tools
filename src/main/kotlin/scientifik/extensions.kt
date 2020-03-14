package scientifik

import Scientifik
import kotlinx.atomicfu.plugin.gradle.sourceSets
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

enum class DependencyConfiguration {
    API,
    IMPLEMENTATION
}

enum class DependencySourceSet(val setName: String, val suffix: String) {
    MAIN("main", "Main"),
    TEST("test", "Test")
}

internal fun Project.useDependency(
    vararg pairs: Pair<String, String>,
    dependencySourceSet: DependencySourceSet = DependencySourceSet.MAIN,
    dependencyConfiguration: DependencyConfiguration = DependencyConfiguration.API
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
                    }
                    add(configurationName, dep.second)
                }
            }
        }
    }
}

class SerializationTargets(
    val sourceSet: DependencySourceSet,
    val configuration: DependencyConfiguration
) {
    fun Project.cbor(
        version: String = Scientifik.serializationVersion
    ) {
        useDependency(
            "common" to "org.jetbrains.kotlinx:kotlinx-serialization-cbor-common:$version",
            "jvm" to "org.jetbrains.kotlinx:kotlinx-serialization-cbor:$version",
            "js" to "org.jetbrains.kotlinx:kotlinx-serialization-cbor-js:$version",
            "native" to "org.jetbrains.kotlinx:kotlinx-serialization-cbor-native:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.protobuf(
        version: String = Scientifik.serializationVersion
    ) {
        useDependency(
            "common" to "org.jetbrains.kotlinx:kotlinx-serialization-protobuf-common:$version",
            "jvm" to "org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$version",
            "js" to "org.jetbrains.kotlinx:kotlinx-serialization-protobuf-js:$version",
            "native" to "org.jetbrains.kotlinx:kotlinx-serialization-protobuf-native:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.xml(
        version: String = Scientifik.Serialization.xmlVersion
    ) {
        useDependency(
            "common" to "net.devrieze:xmlutil:$version",
            "jvm" to "net.devrieze:xmlutil:$version",
            "js" to "net.devrieze:xmlutil:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.yaml(
        version: String = Scientifik.Serialization.yamlVersion
    ) {
        useDependency(
            "jvm" to "com.charleskorn.kaml:kaml:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.bson(
        version: String = Scientifik.Serialization.bsonVersion
    ) {
        useDependency(
            "jvm" to "com.github.jershell:kbson:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }
}

fun Project.serialization(
    version: String = Scientifik.serializationVersion,
    sourceSet: DependencySourceSet = DependencySourceSet.MAIN,
    configuration: DependencyConfiguration = DependencyConfiguration.API,
    block: SerializationTargets.() -> Unit = {}
) {
    plugins.apply("org.jetbrains.kotlin.plugin.serialization")
    useDependency(
        "common" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$version",
        "jvm" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime:$version",
        "js" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$version",
        "native" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$version",
        dependencySourceSet = sourceSet
    )
    SerializationTargets(sourceSet, configuration).apply(block)
}

fun Project.coroutines(
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

//fun Project.atomic(version: String = Scientifik.atomicfuVersion) {
//    plugins.apply("kotlinx-atomicfu")
//    useDependency(
//        "commonMain" to "org.jetbrains.kotlinx:atomicfu-common:$version",
//        "jvmMain" to "org.jetbrains.kotlinx:atomicfu:$version",
//        "jsMain" to "org.jetbrains.kotlinx:atomicfu-js:$version",
//        "nativeMain" to "org.jetbrains.kotlinx:atomicfu-native:$version"
//    )
//}
