package space.kscience.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import space.kscience.gradle.internal.applySettings
import space.kscience.gradle.internal.defaultKotlinCommonArgs

public interface KSciencePlugin : Plugin<Project>

public open class KScienceMPPlugin : KSciencePlugin {

    override fun apply(project: Project): Unit = project.run {
        if (!plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            //apply<KotlinMultiplatformPlugin>() for some reason it does not work
            plugins.apply("org.jetbrains.kotlin.multiplatform")
        } else {
            logger.info("Kotlin MPP plugin is already present")
        }

        val kscience = registerKScienceExtension<KScienceMppExtension>()

        configure<KotlinMultiplatformExtension> {
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
            sourceSets {
                getByName("commonTest") {
                    dependencies {
                        implementation(kotlin("test"))
                        implementation(kotlin("test-annotations-common"))
                    }
                }
                all {
                    languageSettings.applySettings()
                    compilerOptions.freeCompilerArgs.addAll(defaultKotlinCommonArgs)
                }
            }

            if (explicitApi == null) explicitApiWarning()

            //pass compose extension inside kscience extensions to make it available inside kscience block
            plugins.withId("org.jetbrains.compose") {
                kscience.extensions.add(
                    "compose",
                    (this@configure as org.gradle.api.plugins.ExtensionAware).extensions.getByName("compose")
                )
            }

        }


        // apply dokka for all projects
        if (!plugins.hasPlugin("org.jetbrains.dokka")) {
            apply<DokkaPlugin>()
        }
    }
}
