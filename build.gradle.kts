import java.util.*

plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
}

group = "scientifik"
version = "0.1.4-dev"

repositories {
    gradlePluginPortal()
    jcenter()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

val kotlinVersion = "1.3.50-eap-5"

// Add plugins used in buildSrc as dependencies, also we should specify version only here
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jfrog.buildinfo:build-info-extractor-gradle:4.9.7")
    implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")
}

gradlePlugin {
    plugins {
        create("scientifik-publish") {
            id = "scientifik.publish"
            description = "The publication plugin for bintray and artifactory"
            implementationClass = "scientifik.ScientifikPublishPlugin"
        }
        create("scientifik-mpp") {
            id = "scientifik.mpp"
            description = "Pre-configured multiplatform project"
            implementationClass = "scientifik.ScientifikMPPlugin"
        }
    }
}

publishing {
    repositories {
        maven("https://bintray.com/mipt-npm/scientifik")
    }

    val vcs = "https://github.com/mipt-npm/scientifik-gradle-tools"

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

    bintray {
        user = project.findProperty("bintrayUser") as? String ?: System.getenv("BINTRAY_USER")
        key = project.findProperty("bintrayApiKey") as? String? ?: System.getenv("BINTRAY_API_KEY")
        publish = true
        override = true // for multi-platform Kotlin/Native publishing

        // We have to use delegateClosureOf because bintray supports only dynamic groovy syntax
        // this is a problem of this plugin
        pkg.apply {
            userOrg = "mipt-npm"
            repo = if (project.version.toString().contains("dev")) "dev" else "scientifik"
            name = project.name
            issueTrackerUrl = "$vcs/issues"
            setLicenses("Apache-2.0")
            vcsUrl = vcs
            version.apply {
                name = project.version.toString()
                vcsTag = project.version.toString()
                released = Date().toString()
            }
        }

        //workaround bintray bug
        project.afterEvaluate {
            setPublications(*project.extensions.findByType<PublishingExtension>()!!.publications.names.toTypedArray())
        }

    }
}


