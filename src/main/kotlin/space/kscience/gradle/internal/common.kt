package space.kscience.gradle.internal

import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import space.kscience.gradle.KScienceExtension
import space.kscience.gradle.KSciencePlugin
import space.kscience.gradle.KScienceVersions


internal fun KotlinJvmCompilerOptions.defaultKotlinJvmOpts() {
    jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
}

internal fun resolveKotlinVersion(): KotlinVersion {
    val (major, minor, patch) = KScienceVersions.kotlinVersion.split(".", "-")
    return KotlinVersion(major.toInt(), minor.toInt(), patch.toInt())
}

internal fun LanguageSettingsBuilder.applySettings(
    kotlinVersion: KotlinVersion = resolveKotlinVersion(),
) {
    val versionString = "${kotlinVersion.major}.${kotlinVersion.minor}"
    languageVersion = versionString
    apiVersion = versionString
    progressiveMode = true

    optIn("kotlin.ExperimentalStdlibApi")
    optIn("kotlin.contracts.ExperimentalContracts")
    optIn("kotlin.js.ExperimentalJsExport")
}

/**
 * Configures the project using the `KScienceExtension` provided by the `KSciencePlugin`.
 *
 * This function locates the `KSciencePlugin` in the project, and if found, applies the given
 * configuration block to the `KScienceExtension` associated with the plugin.
 *
 * @param block a configuration block that is applied to the `KScienceExtension` instance, allowing
 *              users to customize the project according to the extension's capabilities.
 */
internal fun Project.withKScience(block: KScienceExtension.() -> Unit) {
    plugins.withType<KSciencePlugin>().configureEach {
        extensions.findByType<KScienceExtension>()?.apply(block)
    }
}