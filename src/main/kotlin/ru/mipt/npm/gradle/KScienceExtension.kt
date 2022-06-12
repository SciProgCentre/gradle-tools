package ru.mipt.npm.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlinx.jupyter.api.plugin.tasks.JupyterApiResourcesTask
import ru.mipt.npm.gradle.internal.defaultPlatform
import ru.mipt.npm.gradle.internal.useCommonDependency
import ru.mipt.npm.gradle.internal.useFx

public enum class FXModule(public val artifact: String, public vararg val dependencies: FXModule) {
    BASE("javafx-base"),
    GRAPHICS("javafx-graphics", BASE),
    CONTROLS("javafx-controls", GRAPHICS, BASE),
    FXML("javafx-fxml", BASE),
    MEDIA("javafx-media", GRAPHICS, BASE),
    SWING("javafx-swing", GRAPHICS, BASE),
    WEB("javafx-web", CONTROLS, GRAPHICS, BASE)
}

public enum class FXPlatform(public val id: String) {
    WINDOWS("win"),
    LINUX("linux"),
    MAC("mac")
}

public enum class DependencyConfiguration {
    API,
    IMPLEMENTATION,
    COMPILE_ONLY,
}

public enum class DependencySourceSet(public val setName: String, public val suffix: String) {
    MAIN("main", "Main"),
    TEST("test", "Test")
}


public class KScienceExtension(public val project: Project) {
    /**
     * Use coroutines-core with default version or [version]
     */
    public fun useCoroutines(
        version: String = KScienceVersions.coroutinesVersion,
        sourceSet: DependencySourceSet = DependencySourceSet.MAIN,
        configuration: DependencyConfiguration = DependencyConfiguration.API,
    ): Unit {
        project.useCommonDependency(
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
        project.useCommonDependency(
            "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version",
            dependencySourceSet = DependencySourceSet.TEST,
            dependencyConfiguration = DependencyConfiguration.IMPLEMENTATION
        )
    }

    /**
     * Use core serialization library and configure targets
     */
    public fun useSerialization(
        version: String = KScienceVersions.serializationVersion,
        sourceSet: DependencySourceSet = DependencySourceSet.MAIN,
        configuration: DependencyConfiguration = DependencyConfiguration.API,
        block: SerializationTargets.() -> Unit = {},
    ): Unit = project.run {
        plugins.apply("org.jetbrains.kotlin.plugin.serialization")
        val artifactName = if (version.startsWith("0")) {
            "kotlinx-serialization-runtime"
        } else {
            "kotlinx-serialization-core"
        }
        useCommonDependency(
            "org.jetbrains.kotlinx:$artifactName:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
        SerializationTargets(sourceSet, configuration).block()
    }

    /**
     * Add platform-specific JavaFX dependencies with given list of [FXModule]s
     */
    public fun useFx(
        vararg modules: FXModule,
        configuration: DependencyConfiguration = DependencyConfiguration.COMPILE_ONLY,
        version: String = "11",
        platform: FXPlatform = defaultPlatform,
    ): Unit = project.useFx(modules.toList(), configuration, version, platform)

    /**
     * Add dependency on kotlinx-html library
     */
    public fun useHtml(
        version: String = KScienceVersions.htmlVersion,
        sourceSet: DependencySourceSet = DependencySourceSet.MAIN,
        configuration: DependencyConfiguration = DependencyConfiguration.API,
    ): Unit = project.useCommonDependency(
        "org.jetbrains.kotlinx:kotlinx-html:$version",
        dependencySourceSet = sourceSet,
        dependencyConfiguration = configuration
    )

    /**
     * Use kotlinx-datetime library with default version or [version]
     */
    public fun useDateTime(
        version: String = KScienceVersions.dateTimeVersion,
        sourceSet: DependencySourceSet = DependencySourceSet.MAIN,
        configuration: DependencyConfiguration = DependencyConfiguration.API,
    ): Unit = project.useCommonDependency(
        "org.jetbrains.kotlinx:kotlinx-datetime:$version",
        dependencySourceSet = sourceSet,
        dependencyConfiguration = configuration
    )

    /**
     * Apply jupyter plugin
     */
    @Deprecated("Use jupyterLibrary")
    public fun useJupyter() {
        project.plugins.apply("org.jetbrains.kotlin.jupyter.api")
    }

    /**
     * Apply jupyter plugin and add entry point for the jupyter library.
     * If left empty applies a plugin without declaring library producers
     */
    public fun jupyterLibrary(vararg pluginClasses: String) {
        project.plugins.apply("org.jetbrains.kotlin.jupyter.api")
        project.tasks.named("processJupyterApiResources", JupyterApiResourcesTask::class.java) {
            libraryProducers = pluginClasses.toList()
        }
    }

    /**
     * Mark this module as an application module. JVM application should be enabled separately
     */
    public fun application() {
        project.extensions.findByType<KotlinProjectExtension>()?.apply {
            explicitApi = null
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            project.apply<ApplicationPlugin>()
        }

        project.extensions.findByType<KotlinJsProjectExtension>()?.apply {
            js {
                binaries.executable()
            }
        }

        project.extensions.findByType<KotlinMultiplatformExtension>()?.apply {
            js {
                binaries.executable()
            }

            targets.withType<KotlinNativeTarget> {
                binaries.executable()
            }
        }
    }
}

internal fun Project.registerKScienceExtension() {
    if (extensions.findByType<KScienceExtension>() == null) {
        extensions.add("kscience", KScienceExtension(this))
    }
}
