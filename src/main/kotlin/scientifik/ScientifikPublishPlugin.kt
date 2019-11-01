package scientifik

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.artifact.FileBasedMavenArtifact
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.*


open class ScientifikPublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.plugins.apply("maven-publish")

        project.run {
            val bintrayRepo = if (project.version.toString().contains("dev")) {
                "dev"
            } else {
                findProperty("bintrayRepo") as? String
            }

            val githubProject = findProperty("githubProject") as? String
            val vcs = findProperty("vcs") as? String
                ?: githubProject?.let { "https://github.com/mipt-npm/$it" }

            if (vcs == null) {
                project.logger.warn("[${project.name}] Missing deployment configuration. Skipping publish.")
                return@apply
            }

            project.plugins.apply("com.jfrog.bintray")

            project.configure<PublishingExtension> {
                // Process each publication we have in this project
                publications.filterIsInstance<MavenPublication>().forEach { publication ->

                    @Suppress("UnstableApiUsage")
                    publication.pom {
                        name.set(project.name)
                        description.set(project.description)
                        url.set(vcs)

                        licenses {
                            license {
                                name.set("The Apache Software License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("MIPT-NPM")
                                name.set("MIPT nuclear physics methods laboratory")
                                organization.set("MIPT")
                                organizationUrl.set("http://npm.mipt.ru")
                            }

                        }
                        scm {
                            url.set(vcs)
                        }
                    }
                }

                val githubUser: String? by project
                val githubToken: String? by project

                if (githubProject != null && githubUser != null && githubToken != null) {
                    repositories {
                        val repository = maven {
                            name = "github"
                            url = uri("https://maven.pkg.github.com/mipt-npm/$githubProject/")
                            credentials {
                                username = githubUser
                                password = githubToken
                            }

                        }

                        val githubPublishTasks = publications.filterIsInstance<MavenPublication>().map { publication ->
                            tasks.register<PublishToMavenRepository>("publish${publication.name.capitalize()}ToGithub") {
                                group = "publishing"
                                this.publication = publication
                                this.repository = repository
                            }
                        }

                        tasks.register<PublishToMavenRepository>("publishToGithub") {
                            group = "publishing"
                            dependsOn(githubPublishTasks)
                        }

                    }
                }

                pluginManager.withPlugin("scientifik.mpp"){
                    tasks.filterIsInstance<BintrayUploadTask>().forEach {
                        it.doFirst {
                            publications.filterIsInstance<MavenPublication>()
                                .forEach { publication ->
                                    val moduleFile =
                                        buildDir.resolve("publications/${publication.name}/module.json")
                                    if (moduleFile.exists()) {
                                        publication.artifact(object : FileBasedMavenArtifact(moduleFile) {
                                            override fun getDefaultExtension() = "module"
                                        })
                                    }
                                }
                        }
                    }
                }

            }

            if (bintrayRepo == null) {
                project.logger.warn("[${project.name}] Bintray repository not defined")
            } else {
                project.logger.info("Adding bintray publishing to project [${project.name}]")
                project.configure<PublishingExtension> {
                    repositories {
                        maven("https://bintray.com/mipt-npm/$bintrayRepo")
                    }
                }

                project.configure<BintrayExtension> {
                    user = project.findProperty("bintrayUser") as? String?
                    key = project.findProperty("bintrayApiKey") as? String?
                    publish = true
                    override = true

                    // We have to use delegateClosureOf because bintray supports only dynamic groovy syntax
                    // this is a problem of this plugin
                    pkg.apply {
                        userOrg = "mipt-npm"
                        repo = bintrayRepo
                        name = project.name
                        issueTrackerUrl = "$vcs/issues"
                        setLicenses("Apache-2.0")
                        vcsUrl = vcs
                        version.apply {
                            this.name = project.version.toString()
                            this.vcsTag = project.version.toString()
                            this.released = java.util.Date().toString()
                        }
                    }

                    //workaround bintray bug
                    afterEvaluate {
                        setPublications(*project.extensions.findByType<PublishingExtension>()!!.publications.names.toTypedArray())
                    }
                }
            }
        }
    }
}