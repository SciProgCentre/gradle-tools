package ru.mipt.npm.gradle

import org.gradle.api.JavaVersion

/**
 * Build constants
 */
object KScienceVersions {
    const val kotlinVersion = "1.5.10"
    const val kotlinxNodeVersion = "0.0.7"
    const val coroutinesVersion = "1.5.0"
    const val serializationVersion = "1.2.1"
    const val atomicVersion = "0.16.1"
    const val ktorVersion = "1.6.0"
    const val htmlVersion = "0.7.3"
    const val dateTimeVersion = "0.2.1"
    const val jsBom = "0.0.1-pre.213-kotlin-1.5.10"

    val JVM_TARGET = JavaVersion.VERSION_11

    object Serialization{
        const val xmlVersion = "0.82.0"
        const val yamlKtVersion = "0.9.0"
    }
}
