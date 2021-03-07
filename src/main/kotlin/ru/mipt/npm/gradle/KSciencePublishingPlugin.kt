package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

@Deprecated("To be replaced by maven-publish")
open class KSciencePublishingPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run {
        if (plugins.findPlugin("maven-publish") == null) {
            plugins.apply("maven-publish")
        }
    }

}


