package space.kscience.gradle


import org.gradle.api.JavaVersion
import org.tomlj.Toml

/**
 * Build constants
 */
public object KScienceVersions {

    private val toml by lazy {
        Toml.parse(javaClass.getResource("/libs.versions.toml")!!.readText())
    }

    public val kotlinVersion: String get() = toml.getString("versions.kotlin")!!
    public val kotlinxNodeVersion: String get() = toml.getString("versions.kotlinx-nodejs")!!
    public val coroutinesVersion: String get() = toml.getString("versions.kotlinx-coroutines")!!
    public val serializationVersion: String get() = toml.getString("versions.kotlinx-serialization")!!
    public val atomicVersion: String get() = toml.getString("versions.atomicfu")!!
    public val ktorVersion: String get() = toml.getString("versions.ktor")!!
    public val htmlVersion: String get() = toml.getString("versions.kotlinx-html")!!
    public val dateTimeVersion: String get() = toml.getString("versions.kotlinx-datetime")!!
    public val jsBom: String get() = toml.getString("versions.jsBom")!!
    internal val junit: String get() = toml.getString("versions.junit")!!

    public val JVM_TARGET: JavaVersion = JavaVersion.VERSION_11

    public object Serialization {
        public val xmlVersion: String get() = toml.getString("versions.xmlutil")!!
        public val yamlKtVersion: String get() = toml.getString("versions.yamlkt")!!
    }
}
