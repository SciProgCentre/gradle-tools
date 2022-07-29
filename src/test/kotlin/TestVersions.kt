import org.junit.jupiter.api.Test
import space.kscience.gradle.KScienceVersions

class TestPlugins {
    @Test
    fun testVersions() {
        println(KScienceVersions.coroutinesVersion)
    }
}