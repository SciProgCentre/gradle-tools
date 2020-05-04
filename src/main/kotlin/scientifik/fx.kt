package scientifik

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

enum class FXModule(val artifact: String, vararg val dependencies: FXModule) {
    BASE("javafx-base"),
    GRAPHICS("javafx-graphics", BASE),
    CONTROLS("javafx-controls", GRAPHICS, BASE),
    FXML("javafx-fxml", BASE),
    MEDIA("javafx-media", GRAPHICS, BASE),
    SWING("javafx-swing", GRAPHICS, BASE),
    WEB("javafx-web", CONTROLS, GRAPHICS, BASE)
}

enum class FXPlatform(val id: String) {
    WINDOWS("win"),
    LINUX("linux"),
    MAC("mac")
}

val defaultPlatform: FXPlatform = when {
    Os.isFamily(Os.FAMILY_WINDOWS) -> FXPlatform.WINDOWS
    Os.isFamily(Os.FAMILY_MAC) -> FXPlatform.MAC
    Os.isFamily(Os.FAMILY_UNIX) -> FXPlatform.LINUX
    else -> error("Platform not recognized")
}

fun KotlinDependencyHandler.addFXDependencies(
    vararg modules: FXModule,
    configuration: DependencyConfiguration,
    version: String = "14",
    platform: FXPlatform = defaultPlatform
) {
    modules.flatMap { it.dependencies.toList() + it }.distinct().forEach {
        val notation = "org.openjfx:${it.artifact}:$version:${platform.id}"
        when (configuration) {
            DependencyConfiguration.API -> api(notation)
            DependencyConfiguration.IMPLEMENTATION -> implementation(notation)
            DependencyConfiguration.COMPILE_ONLY -> compileOnly(notation)
        }
    }
}

fun Project.useFx(
    vararg modules: FXModule,
    configuration: DependencyConfiguration = DependencyConfiguration.COMPILE_ONLY,
    version: String = "14",
    platform: FXPlatform = defaultPlatform
): Unit = afterEvaluate{
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.findByType<KotlinMultiplatformExtension>()?.apply {
            sourceSets.findByName("jvmMain")?.apply {
                dependencies {
                    addFXDependencies(*modules, configuration = configuration, version = version, platform = platform)
                }
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.findByType<KotlinJvmProjectExtension>()?.apply {
            sourceSets.findByName("main")?.apply {
                dependencies {
                    addFXDependencies(*modules, configuration = configuration, version = version, platform = platform)
                }
            }
        }
    }
}