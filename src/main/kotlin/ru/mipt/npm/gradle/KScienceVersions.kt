package ru.mipt.npm.gradle

import org.gradle.api.JavaVersion

/**
 * Build constants
 */
object KScienceVersions {
    const val kotlinVersion = "1.5.0"
    const val kotlinxNodeVersion = "0.0.7"
    const val coroutinesVersion = "1.5.0"
    const val serializationVersion = "1.2.1"
    const val atomicVersion = "0.16.1"
    const val ktorVersion = "1.5.3"
    const val htmlVersion = "0.7.3"
    const val dateTimeVersion = "0.2.0"

    val JVM_TARGET = JavaVersion.VERSION_11

    object Serialization{
        const val xmlVersion = "0.82.0"
        const val bsonVersion = "0.4.4"
        const val yamlKtVersion = "0.9.0"
    }
}
