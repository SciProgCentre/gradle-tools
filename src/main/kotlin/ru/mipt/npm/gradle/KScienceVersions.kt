package ru.mipt.npm.gradle

import org.gradle.api.JavaVersion

/**
 * Build constants
 */
object KScienceVersions {
    const val kotlinVersion = "1.4.20-M1"
    const val kotlinxNodeVersion = "0.0.7"
    const val coroutinesVersion = "1.3.9"
    const val serializationVersion = "1.0.0-RC2"
    const val atomicVersion = "0.14.4"

    val JVM_TARGET = JavaVersion.VERSION_11

    object Serialization{
        const val xmlVersion = "0.80.0-RC"
        const val yamlVersion = "0.21.0"
        const val bsonVersion = "0.4.1-rc"
    }
}
