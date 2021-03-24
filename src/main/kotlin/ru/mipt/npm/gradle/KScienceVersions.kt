package ru.mipt.npm.gradle

import org.gradle.api.JavaVersion

/**
 * Build constants
 */
object KScienceVersions {
    const val kotlinVersion = "1.4.32"
    const val kotlinxNodeVersion = "0.0.7"
    const val coroutinesVersion = "1.4.3"
    const val serializationVersion = "1.1.0"
    const val atomicVersion = "0.15.1"
    const val ktorVersion = "1.5.2"
    const val htmlVersion = "0.7.2"
    const val dateTimeVersion = "0.1.1"

    val JVM_TARGET = JavaVersion.VERSION_11

    object Serialization{
        const val xmlVersion = "0.81.1"
        const val bsonVersion = "0.4.4"
        const val yamlKtVersion = "0.9.0"
    }
}
