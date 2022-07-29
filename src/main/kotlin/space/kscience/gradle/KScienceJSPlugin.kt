package space.kscience.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

public open class KScienceJSPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        if (!plugins.hasPlugin("org.jetbrains.kotlin.js")) {
            plugins.apply("org.jetbrains.kotlin.js")
        } else {
            logger.info("Kotlin JS plugin is already present")
        }

        apply<KScienceCommonPlugin>()
    }
}
