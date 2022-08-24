package space.kscience.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.hasPlugin

public class KScienceNativePlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        //Apply multiplatform plugin is not applied, apply it
        if (!plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            logger.info("Kotlin multiplatform plugin is not resolved. Adding it automatically")
            plugins.apply("org.jetbrains.kotlin.multiplatform")
        }

        registerKScienceExtension(::KScienceMppExtension).apply {
            native()
        }

        if (!plugins.hasPlugin(KScienceCommonPlugin::class)) {
            configureKScience()
        } else {
            project.logger.warn("Use `kscience.native()` configuration block")
        }
    }
}
