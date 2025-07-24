package space.kscience.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import space.kscience.gradle.internal.applySettings
import space.kscience.gradle.internal.defaultKotlinCommonArgs
import space.kscience.gradle.internal.defaultKotlinJvmOpts

public open class KScienceJVMPlugin : KSciencePlugin {
    override fun apply(project: Project): Unit = project.run {

        logger.warn("KScience JVM plugin is deprecated. Use MPP.")
        if (!plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
            plugins.apply("org.jetbrains.kotlin.jvm")
        } else {
            logger.info("Kotlin JVM plugin is already present")
        }
        val extension = registerKScienceExtension<KScienceExtension>()

        //logger.info("Applying KScience configuration for JVM project")
        configure<KotlinJvmProjectExtension> {
            sourceSets.all {
                languageSettings.applySettings()
                compilerOptions{
                    defaultKotlinJvmOpts()
                    compilerOptions.freeCompilerArgs.addAll(defaultKotlinCommonArgs)
                }
            }

            sourceSets["test"].apply {
                dependencies {
                    implementation(kotlin("test-junit5"))
                    implementation("org.junit.jupiter:junit-jupiter:${KScienceVersions.junit}")
                }
            }

            if (explicitApi == null) explicitApiWarning()
            jvmToolchain {
                languageVersion.set(extension.jdkVersionProperty.map { JavaLanguageVersion.of(it) })
            }
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }

        // apply dokka for all projects
        if (!plugins.hasPlugin("org.jetbrains.dokka")) {
            apply<DokkaPlugin>()
        }
    }

}
