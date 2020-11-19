plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("org.jetbrains.changelog") version "0.6.2"
}

group = "ru.mipt.npm"
version = "0.6.4-dev-1.4.20"

repositories {
    gradlePluginPortal()
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
}

val kotlinVersion = "1.4.20"

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

// Add plugins used in buildSrc as dependencies, also we should specify version only here
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.14.4")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.4.10.2")
    implementation("org.jetbrains.dokka:dokka-base:1.4.10.2")
    implementation("org.jetbrains.intellij.plugins:gradle-changelog-plugin:0.6.2")
    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:0.2.4")
}

gradlePlugin {
    plugins {
        create("kscience.project") {
            id = "ru.mipt.npm.project"
            description = "The root plugin for multimodule project infrastructure"
            implementationClass = "ru.mipt.npm.gradle.KScienceProjectPlugin"
        }
        create("kscience.publish") {
            id = "ru.mipt.npm.publish"
            description = "The publication plugin for bintray and github"
            implementationClass = "ru.mipt.npm.gradle.KSciencePublishPlugin"
        }

        create("kscience.mpp") {
            id = "ru.mipt.npm.mpp"
            description = "Pre-configured multiplatform project"
            implementationClass = "ru.mipt.npm.gradle.KScienceMPPlugin"
        }

        create("kscience.jvm") {
            id = "ru.mipt.npm.jvm"
            description = "Pre-configured JVM project"
            implementationClass = "ru.mipt.npm.gradle.KScienceJVMPlugin"
        }

        create("kscience.js") {
            id = "ru.mipt.npm.js"
            description = "Pre-configured JS project"
            implementationClass = "ru.mipt.npm.gradle.KScienceJSPlugin"
        }

        create("kscience.native") {
            id = "ru.mipt.npm.native"
            description = "Additional native targets to be use alongside mpp"
            implementationClass = "ru.mipt.npm.gradle.KScienceNativePlugin"
        }

        create("kscience.node") {
            id = "ru.mipt.npm.node"
            description = "Additional nodejs target to be use alongside mpp"
            implementationClass = "ru.mipt.npm.gradle.KScienceNodePlugin"
        }
    }
}

publishing {
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
                tag.set(project.version.toString())
            }
        }
    }

    val bintrayUser: String? by project
    val bintrayApiKey: String? by project
    val projectName = project.name

    if (bintrayUser != null && bintrayApiKey != null) {
        repositories {
            maven {
                name = "bintray"
                url = uri(
                    "https://api.bintray.com/maven/mipt-npm/dev/$projectName/;publish=1;override=1"
                )
                credentials {
                    username = bintrayUser
                    password = bintrayApiKey
                }
            }
        }

    }
}
