package ru.mipt.npm.gradle

import org.gradle.api.Project

class SerializationTargets(
    val sourceSet: DependencySourceSet,
    val configuration: DependencyConfiguration
) {

    fun Project.json(
        version: String = KScienceVersions.serializationVersion
    ) {
        useCommonDependency(
            "org.jetbrains.kotlinx:kotlinx-serialization-json:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.cbor(
        version: String = KScienceVersions.serializationVersion
    ) {
        useCommonDependency(
            "org.jetbrains.kotlinx:kotlinx-serialization-cbor:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.protobuf(
        version: String = KScienceVersions.serializationVersion
    ) {
        useCommonDependency(
            "org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.xml(
        version: String = KScienceVersions.Serialization.xmlVersion
    ) {
        useCommonDependency(
            "net.devrieze:xmlutil-serialization:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.yaml(
        version: String = KScienceVersions.Serialization.yamlVersion
    ) {
        useDependency(
            "jvm" to "com.charleskorn.kaml:kaml:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun Project.bson(
        version: String = KScienceVersions.Serialization.bsonVersion
    ) {
        useDependency(
            "jvm" to "com.github.jershell:kbson:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }
}
