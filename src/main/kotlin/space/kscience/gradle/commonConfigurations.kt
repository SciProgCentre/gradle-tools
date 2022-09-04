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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import space.kscience.gradle.internal.applyRepos
import space.kscience.gradle.internal.applySettings
import space.kscience.gradle.internal.fromJsDependencies


private val defaultKotlinJvmArgs: List<String> =
    listOf("-Xjvm-default=all", "-Xlambdas=indy"/* "-Xjdk-release=${KScienceVersions.JVM_TARGET}"*/)

private fun resolveKotlinVersion(): KotlinVersion {
    val (major, minor, patch) = KScienceVersions.kotlinVersion.split(".", "-")
    return KotlinVersion(major.toInt(), minor.toInt(), patch.toInt())
}


/**
 * Check if this project version has a development tag (`development` property to true, "dev" in the middle or "SNAPSHOT" in the end).
 */
public val Project.isInDevelopment: Boolean
    get() = findProperty("development") == true
            || "dev" in version.toString()
            || version.toString().endsWith("SNAPSHOT")

/**
 * Configure KScience extensions
 */
@Suppress("UNUSED_VARIABLE")
public fun Project.configureKScience(
    kotlinVersion: KotlinVersion = resolveKotlinVersion(),
) {
    repositories.applyRepos()

    //Configuration for K-JVM plugin
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        val extension = registerKScienceExtension(::KScienceExtension)

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
        val extension = registerKScienceExtension(::KScienceExtension)

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
        val extension = registerKScienceExtension(::KScienceMppExtension)

        configure<KotlinMultiplatformExtension> {
            jvm {
                compilations.all {
                    kotlinOptions {
                        jvmTarget = KScienceVersions.JVM_TARGET.toString()
                        freeCompilerArgs = freeCompilerArgs + defaultKotlinJvmArgs
                    }
                }
            }

            js(IR) {
                browser {
                    commonWebpackConfig {
                        cssSupport {
                            enabled = true
                        }
                    }
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
                val jvmTest by getting {
                    dependencies {
                        implementation(kotlin("test-junit5"))
                        implementation("org.junit.jupiter:junit-jupiter:${KScienceVersions.junit}")
                    }
                }
                val jsTest by getting {
                    dependencies {
                        implementation(kotlin("test-js"))
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