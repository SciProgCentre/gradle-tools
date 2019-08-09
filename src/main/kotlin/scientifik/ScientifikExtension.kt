package scientifik

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType

open class ScientifikExtension {
    fun Project.withDokka() {
        apply(plugin = "org.jetbrains.dokka")
        subprojects {
            this.scientifik.apply{
                withDokka()
            }
        }
    }

    fun Project.withSerialization() {
        apply(plugin = "kotlinx-serialization")
        _serialization = true
        //recursively apply to all subprojecs
        subprojects{
            this.scientifik.apply{
                withSerialization()
            }
        }
    }

    private var _serialization = false
    var serialization = false


    var io = false
}

internal val Project.scientifik: ScientifikExtension
    get() = extensions.findByType() ?: extensions.create("scientifik")