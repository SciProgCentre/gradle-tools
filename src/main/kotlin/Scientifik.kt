import org.gradle.api.JavaVersion

/**
 * Build constants
 */
object Scientifik {
    const val coroutinesVersion = "1.3.7"
    const val serializationVersion = "0.20.0"
    const val atomicVersion = "0.14.3"

    val JVM_TARGET = JavaVersion.VERSION_1_8

    object Serialization{
        const val xmlVersion = "0.20.0.1"
        const val yamlVersion = "0.16.1"
        const val bsonVersion = "0.2.1"
    }
}
