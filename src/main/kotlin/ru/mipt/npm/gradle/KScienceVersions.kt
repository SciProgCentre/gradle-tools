package ru.mipt.npm.gradle

import org.gradle.api.JavaVersion

/**
 * Build constants
 */
object KScienceVersions {
    const val coroutinesVersion = "1.3.9"
    const val serializationVersion = "1.0.0-RC"
    const val atomicVersion = "0.14.4"

    val JVM_TARGET = JavaVersion.VERSION_11

    object Serialization{
        const val xmlVersion = "0.20.0.1"
        const val yamlVersion = "0.16.1"
        const val bsonVersion = "0.2.1"
    }
}
