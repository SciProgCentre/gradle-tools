package scientifik

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder

internal fun LanguageSettingsBuilder.applySettings(): Unit {
    progressiveMode = true
    enableLanguageFeature("InlineClasses")
    useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
    useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
    useExperimentalAnnotation("kotlin.time.ExperimentalTime")
}

internal fun RepositoryHandler.applyRepos(): Unit{
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/mipt-npm/scientifik")
    maven("https://dl.bintray.com/mipt-npm/dev")
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
    maven("https://dl.bintray.com/mipt-npm/dataforge")
}