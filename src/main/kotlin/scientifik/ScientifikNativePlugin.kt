package scientifik

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ScientifikNativePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            target.configure<KotlinMultiplatformExtension> {
                linuxX64()
                mingwX64()

                sourceSets.apply {
                    val commonMain by getting {}

                    val native by creating {
                        dependsOn(commonMain)

                        dependencies {
                            //TODO add stdlib here
                            target.afterEvaluate {
                                //TODO add optional libraries here
                            }
                        }
                    }

                    mingwX64().compilations["main"].defaultSourceSet {
                        dependsOn(native)
                    }

                    linuxX64().compilations["main"].defaultSourceSet {
                        dependsOn(native)
                    }
                }
            }
        }
    }
}