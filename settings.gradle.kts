rootProject.name = "gradle-tools"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.kotlin.link")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.9.0")
}

