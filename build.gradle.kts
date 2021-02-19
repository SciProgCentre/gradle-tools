plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    signing
    id("de.marcphilipp.nexus-publish") version "0.4.0"
    id("org.jetbrains.changelog") version "1.0.0"
    id("org.jetbrains.dokka") version "1.4.20"
}

group = "ru.mipt.npm"
version = "0.7.7"

description = "Build tools for DataForge and kscience projects"

repositories {
    gradlePluginPortal()
    jcenter()
    maven("https://repo.kotlin.link")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://dl.bintray.com/kotlin/kotlin-dev")

}

val kotlinVersion = "1.4.30"

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

// Add plugins used in buildSrc as dependencies, also we should specify version only here
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.15.1")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.4.20")
//    implementation("org.jetbrains.dokka:dokka-base:1.4.20")
    implementation("org.jetbrains.intellij.plugins:gradle-changelog-plugin:1.0.0")
    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:0.4.0")
}

gradlePlugin {
    plugins {
        create("kscience.common") {
            id = "ru.mipt.npm.kscience"
            description = "The generalized kscience plugin that works in conjunction with any kotlin plugin"
            implementationClass = "ru.mipt.npm.gradle.KScienceCommonPlugin"
        }

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
    val vcs = "https://github.com/mipt-npm/gradle-tools"

    val sourcesJar: Jar by tasks.creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.named("main").get().allSource)
    }

    val javadocsJar: Jar by tasks.creating(Jar::class) {
        group = "documentation"
        archiveClassifier.set("javadoc")
        from(tasks.dokkaHtml)
    }

    // Process each publication we have in this project
    publications.filterIsInstance<MavenPublication>().forEach { publication ->

        publication.apply {
            artifact(sourcesJar)
            artifact(javadocsJar)

            pom {
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
    }

    val spaceRepo: String = "https://maven.pkg.jetbrains.space/mipt-npm/p/mipt-npm/maven"
    val spaceUser: String? by project
    val spaceToken: String? by project

    if (spaceUser != null && spaceToken != null) {
        project.logger.info("Adding mipt-npm Space publishing to project [${project.name}]")
        repositories {
            maven {
                name = "space"
                url = uri(spaceRepo)
                credentials {
                    username = spaceUser
                    password = spaceToken
                }

            }
        }
    }

    val sonatypeUser: String? by project
    val sonatypePassword: String? by project

//    if (sonatypeUser != null && sonatypePassword != null) {
//        nexusPublishing {
//            repositories {
//                sonatype {
//                    username.set(sonatypeUser)
//                    password.set(sonatypePassword)
//                }
//            }
//        }
//    }

    if (sonatypeUser != null && sonatypePassword != null) {
        val sonatypeRepo: String = if (project.version.toString().contains("dev")) {
            "https://oss.sonatype.org/content/repositories/snapshots"
        } else {
            "https://oss.sonatype.org/service/local/staging/deploy/maven2"
        }

        if (plugins.findPlugin("signing") == null) {
            plugins.apply("signing")
        }

        repositories {
            maven {
                name = "sonatype"
                url = uri(sonatypeRepo)
                credentials {
                    username = sonatypeUser
                    password = sonatypePassword
                }
            }
        }
        signing {
            //useGpgCmd()
            sign(publications)
        }
    }
}


