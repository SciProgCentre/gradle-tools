import java.util.*

plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"
    id("org.jetbrains.changelog") version "0.3.2"
}

group = "scientifik"
version = "0.5.1"

repositories {
    gradlePluginPortal()
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

val kotlinVersion = "1.3.72"

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

// Add plugins used in buildSrc as dependencies, also we should specify version only here
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.14.3")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:0.10.1")
}

gradlePlugin {
    plugins {
        create("scientifik-publish") {
            id = "scientifik.publish"
            description = "The publication plugin for bintray and github"
            implementationClass = "scientifik.ScientifikPublishPlugin"
        }

        create("scientifik-mpp") {
            id = "scientifik.mpp"
            description = "Pre-configured multiplatform project"
            implementationClass = "scientifik.ScientifikMPPlugin"
        }

        create("scientifik-jvm") {
            id = "scientifik.jvm"
            description = "Pre-configured JVM project"
            implementationClass = "scientifik.ScientifikJVMPlugin"
        }

        create("scientifik-js") {
            id = "scientifik.js"
            description = "Pre-configured JS project"
            implementationClass = "scientifik.ScientifikJSPlugin"
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


