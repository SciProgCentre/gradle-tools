package scientifik

import Scientifik
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private fun Project.applyMPPDependency(vararg pairs: Pair<String, String>) {
    val map = mapOf(*pairs)
    extensions.findByType<KotlinMultiplatformExtension>()?.apply {
        pairs.forEach{(target,depString)->
            sourceSets[target].apply {
                dependencies {
                    api(depString)
                }
            }

        }
    }
    extensions.findByType<KotlinJvmProjectExtension>()?.apply {
        sourceSets["main"].apply {
            dependencies {
                api(map["jvmMain"] ?: error("jvmMain dependency not found"))
            }
        }
    }

    extensions.findByType<KotlinJsProjectExtension>()?.apply {
        sourceSets["main"].apply {
            dependencies {
                api(map["jsMain"] ?: error("jsMain dependency not found"))
            }
        }
    }
}

open class ScientifikExtension {
    fun Project.withDokka() {
        apply(plugin = "org.jetbrains.dokka")
        subprojects {
            scientifik.apply {
                withDokka()
            }
        }
    }

    fun Project.withSerialization() {
        apply(plugin = "kotlinx-serialization")
        applyMPPDependency(
            "commonMain" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:${Scientifik.serializationVersion}",
            "jvmMain" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Scientifik.serializationVersion}",
            "jsMain" to "org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:${Scientifik.serializationVersion}"
        )
        //recursively apply to all subprojecs
        subprojects {
            scientifik.apply {
                withSerialization()
            }
        }
    }

    fun Project.withIO() {
        applyMPPDependency(
            "commonMain" to "org.jetbrains.kotlinx:kotlinx-io:${Scientifik.ioVersion}",
            "jvmMain" to "org.jetbrains.kotlinx:kotlinx-io-jvm:${Scientifik.ioVersion}",
            "jsMain" to "org.jetbrains.kotlinx:kotlinx-io-js:${Scientifik.ioVersion}"
        )
        subprojects {
            scientifik.apply {
                withIO()
            }
        }
    }
}

internal val Project.scientifik: ScientifikExtension
    get() = extensions.findByType() ?: extensions.create("scientifik")