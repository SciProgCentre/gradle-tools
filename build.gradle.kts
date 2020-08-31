import java.util.*

plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"
    id("org.jetbrains.changelog") version "0.4.0"
}

group = "ru.mipt.npm"
version = "0.6.0"

repositories {
    gradlePluginPortal()
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

val kotlinVersion = "1.4.0"

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

// Add plugins used in buildSrc as dependencies, also we should specify version only here
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.14.4")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.4.0-rc")
    implementation("org.jetbrains.dokka:dokka-core:1.4.0-rc")
}

gradlePlugin {
    plugins {
        create("kscience-publish") {
            id = "kscience.publish"
            description = "The publication plugin for bintray and github"
            implementationClass = "ru.mipt.npm.gradle.KSciencePublishPlugin"
        }

        create("kscience.mpp") {
            id = "kscience.mpp"
            description = "Pre-configured multiplatform project"
            implementationClass = "ru.mipt.npm.gradle.KScienceMPPlugin"
        }

        create("kscience.jvm") {
            id = "kscience.jvm"
            description = "Pre-configured JVM project"
            implementationClass = "ru.mipt.npm.gradle.KScienceJVMPlugin"
        }

        create("kscience.js") {
            id = "kscience.js"
            description = "Pre-configured JS project"
            implementationClass = "ru.mipt.npm.gradle.KScienceJSPlugin"
        }

        create("kscience.native") {
            id = "kscience.native"
            description = "Additional native targets to be use alongside mpp"
            implementationClass = "ru.mipt.npm.gradle.KScienceNativePlugin"
        }

        create("kscience.node") {
            id = "kscience.node"
            description = "NodeJS target for kotlin-mpp and kotlin-js"
            implementationClass = "ru.mipt.npm.gradle.KScienceNodePlugin"
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
            repo = if (project.version.toString().contains("dev")) "dev" else "kscience"
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


