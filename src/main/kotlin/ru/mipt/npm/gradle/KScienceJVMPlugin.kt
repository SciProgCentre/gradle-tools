package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

public open class KScienceJVMPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        if (!plugins.hasPlugin("org.jetbrains.kotlin.jvm"))
            plugins.apply("org.jetbrains.kotlin.jvm")
        else
            logger.info("Kotlin JVM plugin is already present")

        apply<KScienceCommonPlugin>()
    }
}
