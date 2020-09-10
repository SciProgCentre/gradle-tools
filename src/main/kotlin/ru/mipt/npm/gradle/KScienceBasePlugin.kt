package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Apply extension and repositories
 */
open class KScienceBasePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = target.run{
        registerKScienceExtension()
    }
}