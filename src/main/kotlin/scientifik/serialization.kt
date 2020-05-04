package scientifik

import Scientifik
import org.gradle.api.Project
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

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
        repositories {
            maven("https://dl.bintray.com/pdvrieze/maven")
        }
        useDependency(
            "common" to "net.devrieze:xmlutil-serialization:$version",
            "jvm" to "net.devrieze:xmlutil-serialization:$version",
            "js" to "net.devrieze:xmlutil-serialization:$version",
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

fun Project.useSerialization(
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