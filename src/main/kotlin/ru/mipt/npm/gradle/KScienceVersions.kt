package ru.mipt.npm.gradle

import org.gradle.api.JavaVersion

/**
 * Build constants
 */
object KScienceVersions {
    const val kotlinVersion = "1.4.21"
    const val kotlinxNodeVersion = "0.0.7"
    const val coroutinesVersion = "1.4.2"
    const val serializationVersion = "1.0.1"
    const val atomicVersion = "0.14.4"

    val JVM_TARGET = JavaVersion.VERSION_11

    object Serialization{
        const val xmlVersion = "0.80.1"
        @Deprecated("Use yamlKt instead")
        const val yamlVersion = "0.21.0"
        const val bsonVersion = "0.4.4"
        const val yamlKtVersion = "0.7.5"
    }
}
