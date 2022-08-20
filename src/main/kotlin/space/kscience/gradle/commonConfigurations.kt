package space.kscience.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import space.kscience.gradle.internal.applyRepos
import space.kscience.gradle.internal.applySettings
import space.kscience.gradle.internal.fromJsDependencies


private val defaultKotlinJvmArgs: List<String> =
    listOf("-Xjvm-default=all", "-Xlambdas=indy", "-Xjdk-release=${KScienceVersions.JVM_TARGET}")

private fun resolveKotlinVersion():KotlinVersion {
    val (major, minor, patch) = KScienceVersions.kotlinVersion.split(".", "-")
    return KotlinVersion(major.toInt(),minor.toInt(),patch.toInt())
}

public fun Project.configureKScience(
    kotlinVersion: KotlinVersion = resolveKotlinVersion(),
) {
    repositories.applyRepos()

    //Configuration for K-JVM plugin
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        registerKScienceExtension(::KScienceExtension)

        //logger.info("Applying KScience configuration for JVM project")
        configure<KotlinJvmProjectExtension> {
            sourceSets.all {
                languageSettings.applySettings(kotlinVersion)
            }

            sourceSets["test"].apply {
                dependencies {
                    implementation(kotlin("test-junit5"))
                    implementation("org.junit.jupiter:junit-jupiter:${KScienceVersions.junit}")
                }
            }

            if (explicitApi == null) explicitApiWarning()
        }
        tasks.withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = KScienceVersions.JVM_TARGET.toString()
                freeCompilerArgs = freeCompilerArgs + defaultKotlinJvmArgs
            }
        }

        extensions.findByType<JavaPluginExtension>()?.apply {
            targetCompatibility = KScienceVersions.JVM_TARGET
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.js") {
        registerKScienceExtension(::KScienceExtension)

        //logger.info("Applying KScience configuration for JS project")
        configure<KotlinJsProjectExtension> {
            js(IR) {
                browser {
                    commonWebpackConfig {
                        cssSupport {
                            enabled = true
                        }
                    }
                }
            }

            sourceSets.all {
                languageSettings.applySettings(kotlinVersion)
            }

            sourceSets["main"].apply {
                dependencies {
                    api(project.dependencies.platform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:${KScienceVersions.jsBom}"))
                }
            }

            sourceSets["test"].apply {
                dependencies {
                    implementation(kotlin("test-js"))
                }
            }

            if (explicitApi == null) explicitApiWarning()
        }

        (tasks.findByName("processResources") as? Copy)?.apply {
            fromJsDependencies("runtimeClasspath")
        }

    }

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        val configuration = registerKScienceExtension(::KScienceMppExtension)

        configure<KotlinMultiplatformExtension> {
            configuration.jvmConfiguration?.let { jvmConfiguration ->
                jvm {
                    compilations.all {
                        kotlinOptions {
                            jvmTarget = KScienceVersions.JVM_TARGET.toString()
                            freeCompilerArgs = freeCompilerArgs + defaultKotlinJvmArgs
                        }
                    }
                    jvmConfiguration(this)
                }
            }

            configuration.jsConfiguration?.let { jsConfiguration ->
                js(IR) {
                    browser {
                        commonWebpackConfig {
                            cssSupport {
                                enabled = true
                            }
                        }
                    }
                    jsConfiguration(this)
                }
            }

            sourceSets {
                val commonMain by getting {
                    dependencies {
                        api(project.dependencies.platform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:${KScienceVersions.jsBom}"))
                    }
                }
                val commonTest by getting {
                    dependencies {
                        implementation(kotlin("test-common"))
                        implementation(kotlin("test-annotations-common"))
                    }
                }
                val jvmMain by getting
                val jvmTest by getting {
                    dependencies {
                        implementation(kotlin("test-junit5"))
                        implementation("org.junit.jupiter:junit-jupiter:${KScienceVersions.junit}")
                    }
                }
                val jsMain by getting
                val jsTest by getting {
                    dependencies {
                        implementation(kotlin("test-js"))
                    }
                }

                configuration.nativeConfiguration?.let { nativeConfiguration ->
                    val nativeTargets: List<KotlinNativeTargetWithHostTests> = nativeConfiguration.targets.values.mapNotNull { nativeTarget ->
                        when (nativeTarget.preset) {
                            KotlinNativePreset.linuxX64 -> linuxX64(nativeTarget.targetName, nativeTarget.targetConfiguration)
                            KotlinNativePreset.mingwX64 -> linuxX64(nativeTarget.targetName, nativeTarget.targetConfiguration)
                            KotlinNativePreset.macosX64 -> linuxX64(nativeTarget.targetName, nativeTarget.targetConfiguration)
                            KotlinNativePreset.iosX64 -> linuxX64(nativeTarget.targetName, nativeTarget.targetConfiguration)
                            KotlinNativePreset.iosArm64 -> linuxX64(nativeTarget.targetName, nativeTarget.targetConfiguration)
                            else -> {
                                logger.error("Native preset ${nativeTarget.preset} not recognised.")
                                null
                            }
                        }
                    }

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

            sourceSets.all {
                languageSettings.applySettings(kotlinVersion)
            }

            (tasks.findByName("jsProcessResources") as? Copy)?.apply {
                fromJsDependencies("jsRuntimeClasspath")
            }

            extensions.findByType<JavaPluginExtension>()?.apply {
                targetCompatibility = KScienceVersions.JVM_TARGET
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }

            if (explicitApi == null) explicitApiWarning()
        }
    }

    // apply dokka for all projects
    if (!plugins.hasPlugin("org.jetbrains.dokka")) {
        apply<DokkaPlugin>()
    }
}