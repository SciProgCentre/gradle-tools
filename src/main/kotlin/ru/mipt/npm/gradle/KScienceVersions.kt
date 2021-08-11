package ru.mipt.npm.gradle

import org.gradle.api.JavaVersion

/**
 * Build constants
 */
public object KScienceVersions {
    public const val kotlinVersion: String = "1.5.21"
    public const val kotlinxNodeVersion: String = "0.0.7"
    public const val coroutinesVersion: String = "1.5.1"
    public const val serializationVersion: String = "1.2.2"
    public const val atomicVersion: String = "0.16.2"
    public const val ktorVersion: String = "1.6.1"
    public const val htmlVersion: String = "0.7.3"
    public const val dateTimeVersion: String = "0.2.1"
    public const val jsBom: String = "0.0.1-pre.216-kotlin-1.5.20"

    public val JVM_TARGET: JavaVersion = JavaVersion.VERSION_11

    public object Serialization {
        public const val xmlVersion: String = "0.82.0"
        public const val yamlKtVersion: String = "0.10.0"
    }
}
