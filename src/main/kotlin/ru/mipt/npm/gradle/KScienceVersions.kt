package ru.mipt.npm.gradle

import org.gradle.api.JavaVersion
import org.gradle.internal.impldep.org.tomlj.Toml

/**
 * Build constants
 */
public object KScienceVersions {

    private val toml =
        Toml.parse(KScienceVersions.javaClass.getResource("/libs.versions.toml")!!.readText())


    public val kotlinVersion: String = toml.getString("versions.kotlin")!!
    public val kotlinxNodeVersion: String = toml.getString("versions.kotlinx-nodejs")!!
    public val coroutinesVersion: String = toml.getString("versions.kotlinx-coroutines")!!
    public val serializationVersion: String = toml.getString("versions.kotlinx-serialization")!!
    public val atomicVersion: String = toml.getString("versions.atomicfu")!!
    public val ktorVersion: String = toml.getString("versions.ktor")!!
    public val htmlVersion: String = toml.getString("versions.kotlinx-html")!!
    public val dateTimeVersion: String = toml.getString("versions.kotlinx-datetime")!!
    public val jsBom: String = toml.getString("versions.jsBom")!!

    public val JVM_TARGET: JavaVersion = JavaVersion.VERSION_11

    public object Serialization {
        public val xmlVersion: String = toml.getString("versions.xmlutil")!!
        public val yamlKtVersion: String = toml.getString("versions.yamlkt")!!
    }
}
