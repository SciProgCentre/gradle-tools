package ru.mipt.npm.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import ru.mipt.npm.gradle.internal.configurePublishing
import ru.mipt.npm.gradle.internal.defaultPlatform
import ru.mipt.npm.gradle.internal.useCommonDependency
import ru.mipt.npm.gradle.internal.useFx

class KScienceExtension(val project: Project) {

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

    enum class DependencyConfiguration {
        API,
        IMPLEMENTATION,
        COMPILE_ONLY
    }

    enum class DependencySourceSet(val setName: String, val suffix: String) {
        MAIN("main", "Main"),
        TEST("test", "Test")
    }

    fun useCoroutines(
        version: String = KScienceVersions.coroutinesVersion,
        sourceSet: DependencySourceSet = DependencySourceSet.MAIN,
        configuration: DependencyConfiguration = DependencyConfiguration.API
    ): Unit = project.useCommonDependency(
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version",
        dependencySourceSet = sourceSet,
        dependencyConfiguration = configuration
    )

    fun useAtomic(version: String = KScienceVersions.atomicVersion): Unit = project.run {
        plugins.apply("kotlinx-atomicfu")
        useCommonDependency(
            "org.jetbrains.kotlinx:atomicfu:$version"
        )
    }

    fun useSerialization(
        version: String = KScienceVersions.serializationVersion,
        sourceSet: DependencySourceSet = DependencySourceSet.MAIN,
        configuration: DependencyConfiguration = DependencyConfiguration.API,
        block: SerializationTargets.() -> Unit = {}
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
        SerializationTargets(sourceSet, configuration).apply(block)
    }

    fun useAtomic(
        version: String = KScienceVersions.atomicVersion,
        sourceSet: DependencySourceSet = DependencySourceSet.MAIN,
        configuration: DependencyConfiguration = DependencyConfiguration.IMPLEMENTATION
    ): Unit = project.run {
        plugins.apply("kotlinx-atomicfu")
        useCommonDependency(
            "org.jetbrains.kotlinx:atomicfu-common:$version",
            dependencySourceSet = sourceSet,
            dependencyConfiguration = configuration
        )
    }

    fun useDokka(): Unit = project.run {
        plugins.apply("org.jetbrains.dokka")
    }

    fun useFx(
        vararg modules: FXModule,
        configuration: DependencyConfiguration = DependencyConfiguration.COMPILE_ONLY,
        version: String = "14",
        platform: FXPlatform = defaultPlatform
    ) = project.useFx(modules.toList(), configuration, version, platform)

    /**
     * Mark this module as an application module. JVM application should be enabled separately
     */
    fun application() {
        project.extensions.findByType<KotlinProjectExtension>()?.apply {
            explicitApi = null
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            project.plugins.apply(ApplicationPlugin::class.java)
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
            targets.filterIsInstance<KotlinNativeTarget>().forEach {
                it.binaries.executable()
            }
        }
    }

    fun publish() {
        project.configurePublishing()
    }
}

internal fun Project.registerKScienceExtension() {
    if (extensions.findByType<KScienceExtension>() == null) {
        extensions.add("kscience", KScienceExtension(this))
    }
}