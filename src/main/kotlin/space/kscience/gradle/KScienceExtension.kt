package space.kscience.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlinx.jupyter.api.plugin.tasks.JupyterApiResourcesTask
import space.kscience.gradle.internal.defaultPlatform
import space.kscience.gradle.internal.useCommonDependency
import space.kscience.gradle.internal.useFx

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


public open class KScienceExtension(public val project: Project) {

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
    @Deprecated("Use manual FX configuration")
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

    /**
     * Add context receivers to this project and all subprojects
     */
    public fun withContextReceivers() {
        project.allprojects {
            tasks.withType<KotlinCompile> {
                kotlinOptions {
                    freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
                }
            }
        }
    }
}

public enum class KotlinNativePreset {
    linuxX64,
    mingwX64,
    macosX64,
    iosX64,
    iosArm64
}

public data class KScienceNativeTarget(
    val preset: KotlinNativePreset,
    val targetName: String = preset.name,
    val targetConfiguration: KotlinNativeTarget.() -> Unit = { },
) {
    public companion object {
        public val linuxX64: KScienceNativeTarget = KScienceNativeTarget(KotlinNativePreset.linuxX64)
        public val mingwX64: KScienceNativeTarget = KScienceNativeTarget(KotlinNativePreset.mingwX64)
        public val macosX64: KScienceNativeTarget = KScienceNativeTarget(KotlinNativePreset.macosX64)
        public val iosX64: KScienceNativeTarget = KScienceNativeTarget(KotlinNativePreset.iosX64)
        public val iosArm64: KScienceNativeTarget = KScienceNativeTarget(KotlinNativePreset.iosArm64)
    }
}

public class KScienceNativeConfiguration {
    internal var targets: MutableMap<KotlinNativePreset, KScienceNativeTarget> = listOf(
        KScienceNativeTarget.linuxX64,
        KScienceNativeTarget.mingwX64,
        KScienceNativeTarget.macosX64,
        KScienceNativeTarget.iosX64,
        KScienceNativeTarget.iosArm64,
    ).associateBy { it.preset }.toMutableMap()

    public fun targets(vararg target: KScienceNativeTarget) {
        targets = target.associateBy { it.preset }.toMutableMap()
    }

    public fun target(target: KScienceNativeTarget) {
        targets[target.preset] = target
    }
}

public open class KScienceMppExtension(project: Project) : KScienceExtension(project) {
    internal var jvmConfiguration: ((KotlinJvmTarget) -> Unit)? = { }

    /**
     * Custom configuration for JVM target. If null - disable JVM target
     */
    public fun jvm(block: KotlinJvmTarget.() -> Unit) {
        jvmConfiguration = block
    }

    internal var jsConfiguration: ((KotlinJsTargetDsl) -> Unit)? = { }

    /**
     * Custom configuration for JS target. If null - disable JS target
     */
    public fun js(block: KotlinJsTargetDsl.() -> Unit) {
        jsConfiguration = block
    }

    internal var nativeConfiguration: KScienceNativeConfiguration? = null

    public fun native(block: KScienceNativeConfiguration.() -> Unit = {}) {
        nativeConfiguration = KScienceNativeConfiguration().apply(block)
    }
}


internal inline fun <reified T : KScienceExtension> Project.registerKScienceExtension(constructor: (Project) -> T): T {
    extensions.findByType<T>()?.let { return it }
    return constructor(this).also {
        extensions.add("kscience", it)
    }
}
