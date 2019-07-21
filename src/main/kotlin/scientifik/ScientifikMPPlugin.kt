package scientifik

import Scientifik
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

open class ScientifikMPPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.scientifik

        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        project.plugins.apply("kotlinx-serialization")
        project.plugins.apply("kotlinx-atomicfu")

        project.repositories {
            mavenCentral()
            jcenter()
            maven("https://dl.bintray.com/kotlin/kotlin-eap")
            maven("https://kotlin.bintray.com/kotlinx")
            maven("https://dl.bintray.com/mipt-npm/dev")
        }

        project.configure<KotlinMultiplatformExtension> {
            jvm {
                compilations.all {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                }
            }

            js {
                browser {}
            }

            if (extension.native) {
                linuxX64()
                mingwX64()
            }

            sourceSets.invoke {
                val commonMain by getting {
                    dependencies {
                        api(kotlin("stdlib"))
                        project.afterEvaluate {
                            if (extension.serialization) {
                                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Scientifik.serializationVersion}")
                            }
                            if(extension.atomicfu){
                                implementation("org.jetbrains.kotlinx:atomicfu-common:${Scientifik.atomicfuVersion}")
                            }
                            if(extension.io){
                                api("org.jetbrains.kotlinx:kotlinx-io:${Scientifik.ioVersion}")
                            }
                        }
                    }
                }
                val commonTest by getting {
                    dependencies {
                        implementation(kotlin("test-common"))
                        implementation(kotlin("test-annotations-common"))
                    }
                }
                val jvmMain by getting {
                    dependencies {
                        api(kotlin("stdlib-jdk8"))
                        if(extension.atomicfu){
                            implementation("org.jetbrains.kotlinx:atomicfu:${Scientifik.atomicfuVersion}")
                        }
                        if(extension.io){
                            api("org.jetbrains.kotlinx:kotlinx-io-jvm:${Scientifik.ioVersion}")
                        }
                    }
                }
                val jvmTest by getting {
                    dependencies {
                        implementation(kotlin("test"))
                        implementation(kotlin("test-junit"))
                    }
                }
                val jsMain by getting {
                    dependencies {
                        api(kotlin("stdlib-js"))
                        if(extension.atomicfu){
                            implementation("org.jetbrains.kotlinx:atomicfu-common-js:${Scientifik.atomicfuVersion}")
                        }
                        if(extension.io){
                            api("org.jetbrains.kotlinx:kotlinx-io-js:${Scientifik.ioVersion}")
                        }
                    }
                }
                val jsTest by getting {
                    dependencies {
                        implementation(kotlin("test-js"))
                    }
                }

                project.afterEvaluate {
                    if (extension.native) {
                        val native by creating {
                            dependsOn(commonMain)
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

            targets.all {
                sourceSets.all {
                    languageSettings.apply {
                        progressiveMode = true
                        enableLanguageFeature("InlineClasses")
                        useExperimentalAnnotation("ExperimentalUnsignedType")
                    }
                }
            }
        }

    }
}