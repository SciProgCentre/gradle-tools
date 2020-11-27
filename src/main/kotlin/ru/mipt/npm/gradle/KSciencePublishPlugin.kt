package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import ru.mipt.npm.gradle.internal.configurePublishing


open class KSciencePublishPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.plugins.withId("ru.mipt.npm.kscience") {
        project.configurePublishing()
    }
}