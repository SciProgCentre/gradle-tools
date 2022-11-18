package space.kscience.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
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


    public fun dependencies(sourceSet: String? = null, dependencyBlock: KotlinDependencyHandler.() -> Unit) {
        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            project.configure<KotlinJvmProjectExtension> {
                sourceSets.getByName(sourceSet ?: "main") {
                    dependencies(dependencyBlock)
                }
            }
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.js") {
            project.configure<KotlinJsProjectExtension> {
                sourceSets.getByName(sourceSet ?: "main") {
                    dependencies(dependencyBlock)
                }
            }
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            project.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName(sourceSet ?: "commonMain") {
                    dependencies(dependencyBlock)
                }
            }
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
            js(IR) {
                binaries.executable()
            }
        }

        project.extensions.findByType<KotlinMultiplatformExtension>()?.apply {
            js(IR) {
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
    macosArm64,
    iosX64,
    iosArm64,
    iosSimulatorArm64,
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
        public val macosArm64: KScienceNativeTarget = KScienceNativeTarget(KotlinNativePreset.macosX64)
        public val iosX64: KScienceNativeTarget = KScienceNativeTarget(KotlinNativePreset.iosX64)
        public val iosArm64: KScienceNativeTarget = KScienceNativeTarget(KotlinNativePreset.iosArm64)
        public val iosSimulatorArm64: KScienceNativeTarget = KScienceNativeTarget(KotlinNativePreset.iosSimulatorArm64)
    }
}

public class KScienceNativeConfiguration {
    internal var targets: MutableMap<KotlinNativePreset, KScienceNativeTarget> = listOf(
        KScienceNativeTarget.linuxX64,
        KScienceNativeTarget.mingwX64,
        KScienceNativeTarget.macosX64,
        KScienceNativeTarget.macosArm64,
        KScienceNativeTarget.iosX64,
        KScienceNativeTarget.iosArm64,
        KScienceNativeTarget.iosSimulatorArm64,
    ).associateByTo(mutableMapOf()) { it.preset }

    /**
     * Replace all targets
     */
    public fun setTargets(vararg target: KScienceNativeTarget) {
        targets = target.associateByTo(mutableMapOf()) { it.preset }
    }

    /**
     * Add a native target
     */
    public fun target(target: KScienceNativeTarget) {
        targets[target.preset] = target
    }

    public fun target(
        preset: KotlinNativePreset,
        targetName: String = preset.name,
        targetConfiguration: KotlinNativeTarget.() -> Unit = { },
    ): Unit = target(KScienceNativeTarget(preset, targetName, targetConfiguration))
}

public open class KScienceMppExtension(project: Project) : KScienceExtension(project) {
    /**
     * Custom configuration for JVM target. If null - disable JVM target
     */
    public fun jvm(block: KotlinJvmTarget.() -> Unit) {
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            project.configure<KotlinMultiplatformExtension> {
                jvm(block)
            }
        }
    }

    /**
     * Remove Jvm target
     */
    public fun noJvm() {
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            project.configure<KotlinMultiplatformExtension> {
                targets.removeIf { it is KotlinJvmTarget }
            }
        }
    }

    /**
     * Custom configuration for JS target. If null - disable JS target
     */
    public fun js(block: KotlinJsTargetDsl.() -> Unit) {
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            project.configure<KotlinMultiplatformExtension> {
                js(block)
            }
        }
    }

    public fun noJs() {
        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            project.configure<KotlinMultiplatformExtension> {
                targets.removeIf { it is KotlinJsTargetDsl }
            }
        }
    }

    public fun native(block: KScienceNativeConfiguration.() -> Unit = {}): Unit = with(project) {
        val nativeConfiguration = KScienceNativeConfiguration().apply(block)
        pluginManager.withPlugin("space.kscience.gradle.mpp") {
            configure<KotlinMultiplatformExtension> {
                sourceSets {
                    val nativeTargets: List<KotlinNativeTarget> =
                        nativeConfiguration.targets.values.mapNotNull { nativeTarget ->
                            when (nativeTarget.preset) {
                                KotlinNativePreset.linuxX64 -> linuxX64(
                                    nativeTarget.targetName,
                                    nativeTarget.targetConfiguration
                                )

                                KotlinNativePreset.mingwX64 -> mingwX64(
                                    nativeTarget.targetName,
                                    nativeTarget.targetConfiguration
                                )

                                KotlinNativePreset.macosX64 -> macosX64(
                                    nativeTarget.targetName,
                                    nativeTarget.targetConfiguration
                                )

                                KotlinNativePreset.macosArm64 -> macosX64(
                                    nativeTarget.targetName,
                                    nativeTarget.targetConfiguration
                                )

                                KotlinNativePreset.iosX64 -> iosX64(
                                    nativeTarget.targetName,
                                    nativeTarget.targetConfiguration
                                )

                                KotlinNativePreset.iosArm64 -> iosArm64(
                                    nativeTarget.targetName,
                                    nativeTarget.targetConfiguration
                                )

                                KotlinNativePreset.iosSimulatorArm64 -> iosArm64(
                                    nativeTarget.targetName,
                                    nativeTarget.targetConfiguration
                                )

//                                else -> {
//                                    logger.error("Native preset ${nativeTarget.preset} not recognised.")
//                                    null
//                                }
                            }
                        }
                    val commonMain by getting
                    val commonTest by getting

                    val nativeMain by creating {
                        dependsOn(commonMain)
                    }

                    val nativeTest by creating {
                        //should NOT depend on nativeMain because automatic dependency by plugin
                        dependsOn(commonTest)
                    }

                    configure(nativeTargets) {
                        compilations["main"]?.apply {
                            configure(kotlinSourceSets) {
                                dependsOn(nativeMain)
                            }
                        }

                        compilations["test"]?.apply {
                            configure(kotlinSourceSets) {
                                dependsOn(nativeTest)
                            }
                        }
                    }
                }
            }
        }
    }
}


internal inline fun <reified T : KScienceExtension> Project.registerKScienceExtension(constructor: (Project) -> T): T {
    extensions.findByType<T>()?.let { return it }
    return constructor(this).also {
        extensions.add("kscience", it)
    }
}
