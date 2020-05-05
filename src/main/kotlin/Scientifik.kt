import org.gradle.api.JavaVersion

/**
 * Build constants
 */
object Scientifik {
    const val coroutinesVersion = "1.3.4"
    const val serializationVersion = "0.20.0"

    val JVM_TARGET = JavaVersion.VERSION_11
//    val JVM_VERSION = JVM_TARGET.toString()

    object Serialization{
        const val xmlVersion = "0.20.0.0"
        const val yamlVersion = "0.16.1"
        const val bsonVersion = "0.2.1"
    }
}
