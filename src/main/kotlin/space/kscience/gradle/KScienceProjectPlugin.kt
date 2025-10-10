package space.kscience.gradle

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.MavenPublishBasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.Sign
import org.jetbrains.changelog.ChangelogPlugin
import org.jetbrains.changelog.ChangelogPluginExtension
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension
import space.kscience.gradle.internal.addPublishing
import space.kscience.gradle.internal.setupPublication
import space.kscience.gradle.internal.withKScience
import javax.inject.Inject

/**
 * Simplifies adding repositories for Maven publishing, responds for releasing tasks for projects.
 */
public abstract class KScienceProjectExtension @Inject constructor(override val project: Project): KSciencePlatformExtension {

    override var maturity: Maturity = Maturity.EXPERIMENTAL

    private var isVcsInitialized = false

    /**
     * Configures Git repository (sources) for the publication.
     *
     * @param vcsUrl URL of the repository's web interface.
     * @param connectionUrl URL of the Git repository.
     * @param developerConnectionUrl URL of the Git repository for developers.
     */
    public fun pom(
        vcsUrl: String,
        connectionUrl: String? = null,
        developerConnectionUrl: String? = connectionUrl,
        connectionPrefix: String = "scm:git:",
        pomConfig: MavenPom.() -> Unit,
    ) {
        if (!isVcsInitialized) {
            project.setupPublication {
                url.set(vcsUrl)

                scm {
                    url.set(vcsUrl)
                    connectionUrl?.let { connection.set("$connectionPrefix$it") }
                    developerConnectionUrl?.let { developerConnection.set("$connectionPrefix$it") }
                }
                pomConfig()
            }

            isVcsInitialized = true
        }
    }

    /**
     * Add a repository with [repositoryName]. Uses "publishing.$repositoryName.user" and "publishing.$repositoryName.token"
     * properties pattern to store user and token
     */
    public fun publishTo(
        repositoryName: String,
        url: String,
    ) {
        require(isVcsInitialized) { "The project vcs is not set up use 'pom' method to do so" }
        project.addPublishing(repositoryName, url)
    }

    /**
     * Add publishing to maven central "new" API
     */
    public fun publishToCentral(): Unit = with(project) {
        require(isVcsInitialized) { "The project vcs is not set up use 'pom' method to do so" }
        if (isInDevelopment) {
            logger.info("Maven central publishing skipped for development version")
        } else {
            allprojects {
                plugins.withType<MavenPublishBasePlugin> {
                    extensions.configure<MavenPublishBaseExtension> {
                        publishToMavenCentral()
                    }
                }
            }
        }
    }
}



/**
 * Applies third-party plugins (Dokka, Changelog); configures Maven publishing, README
 * generation.
 */
public open class KScienceProjectPlugin : Plugin<Project> {


    override fun apply(target: Project): Unit = target.run {
        apply<ChangelogPlugin>()
        apply<DokkaPlugin>()

        val kscienceProjectExtension = extensions.create("kscienceProject", KScienceProjectExtension::class.java)

        //configure readme for root project
        kscienceProjectExtension.configureReadme()

        allprojects {

            //Add repositories
            repositories {
                mavenCentral()
                maven("https://repo.kotlin.link")
                google()
            }

            // Workaround for https://github.com/gradle/gradle/issues/15568
            tasks.withType<AbstractPublishToMaven>().configureEach {
                mustRunAfter(tasks.withType<Sign>())
            }

            //configure readme for all subprojects that have KScience plugins. If the root project also has the kscience plugin, it is skipped.
            withKScience {
                configureReadme()
            }
        }


        tasks.register("version") {
            group = "publishing"
            val versionFileProvider = project.layout.buildDirectory.file("project-version.txt")
            outputs.file(versionFileProvider)
            doLast {
                val versionFile = versionFileProvider.get().asFile
                versionFile.createNewFile()
                versionFile.writeText(project.version.toString())
            }
        }

        afterEvaluate {
            if (!isInDevelopment) {
                configure<ChangelogPluginExtension> {
                    version.set(project.version.toString())
                }
            }
        }

        plugins.withType<YarnPlugin> {
            rootProject.configure<YarnRootExtension> {
                lockFileDirectory = rootDir.resolve("gradle/js")
                yarnLockMismatchReport = YarnLockMismatchReport.WARNING
            }
        }

        plugins.withType<WasmYarnPlugin> {
            rootProject.configure<WasmYarnRootExtension> {
                lockFileDirectory = rootDir.resolve("gradle/wasm")
                yarnLockMismatchReport = YarnLockMismatchReport.WARNING
            }
        }
    }

    public companion object {
        public const val DEPLOY_GROUP: String = "deploy"
    }
}
