package ru.mipt.npm.gradle

import org.gradle.api.JavaVersion

/**
 * Build constants
 */
object KScienceVersions {
    const val kotlinVersion = "1.5.21"
    const val kotlinxNodeVersion = "0.0.7"
    const val coroutinesVersion = "1.5.1"
    const val serializationVersion = "1.2.2"
    const val atomicVersion = "0.16.2"
    const val ktorVersion = "1.6.1"
    const val htmlVersion = "0.7.3"
    const val dateTimeVersion = "0.2.1"
    const val jsBom = "0.0.1-pre.216-kotlin-1.5.20"

    val JVM_TARGET = JavaVersion.VERSION_11

    object Serialization{
        const val xmlVersion = "0.82.0"
        const val yamlKtVersion = "0.10.0"
    }
}
