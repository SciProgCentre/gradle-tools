plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    `version-catalog`
    alias(libs.plugins.jetbrains.changelog)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.versions.update)
    alias(libs.plugins.maven.publish.base)
}

group = "space.kscience"
version = libs.versions.tools.get()

description = "Build tools for kotlin for science projects"

changelog.version.set(project.version.toString())

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.kotlin.link")
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.asProvider().get()}")
    api("org.gradle.toolchains:foojay-resolver:1.0.0")
    api("com.vanniktech:gradle-maven-publish-plugin:0.33.0")
    api("org.jetbrains.kotlinx:binary-compatibility-validator:0.18.0")
    api("org.jetbrains.intellij.plugins:gradle-changelog-plugin:${libs.versions.changelog.get()}")
    api("org.jetbrains.dokka:dokka-gradle-plugin:${libs.versions.dokka.get()}")

    implementation("dev.opensavvy.resources.producer:dev.opensavvy.resources.producer.gradle.plugin:${libs.versions.opensavvy.resources.get()}")
    implementation("dev.opensavvy.resources.consumer:dev.opensavvy.resources.consumer.gradle.plugin:${libs.versions.opensavvy.resources.get()}")

    implementation(libs.kotlin.serialization)
    implementation(libs.kotlinx.html)
    implementation(libs.tomlj)
//    // nexus publishing plugin
//    implementation("io.github.gradle-nexus:publish-plugin:_")

    implementation(libs.freemarker)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

//declaring exported plugins

gradlePlugin {
    plugins {
        create("project") {
            id = "space.kscience.gradle.project"
            description = "The root plugin for multi-module project infrastructure"
            implementationClass = "space.kscience.gradle.KScienceProjectPlugin"
        }

        create("mpp") {
            id = "space.kscience.gradle.mpp"
            description = "Pre-configured multiplatform project"
            implementationClass = "space.kscience.gradle.KScienceMPPlugin"
        }

        create("jvm") {
            id = "space.kscience.gradle.jvm"
            description = "Pre-configured JVM project"
            implementationClass = "space.kscience.gradle.KScienceJVMPlugin"
        }
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

//publishing version catalog

catalog.versionCatalog {
    from(files("gradle/libs.versions.toml"))
}

//publishing the artifact
mavenPublishing {
    configure(
        com.vanniktech.maven.publish.GradlePlugin(
            javadocJar = com.vanniktech.maven.publish.JavadocJar.Dokka("dokkaGenerate"),
            sourcesJar = true,
        )
    )


    publishing.publications.create<MavenPublication>("version-catalog") {
        from(components["versionCatalog"])
        artifactId = "version-catalog"

        pom {
            name.set("version-catalog")
        }
    }

    val vcs = "https://git.sciprog.center/kscience/gradle-tools"

    pom {
        name.set(project.name)
        description.set(project.description)
        url.set(vcs)

        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("SPC")
                name.set("Scientific Programming Centre")
                organization.set("SPC")
                organizationUrl.set("https://sciprog.center/")
            }
        }

        scm {
            url.set(vcs)
            tag.set(project.version.toString())
        }
    }

    val spaceRepo = "https://maven.sciprog.center/kscience"
    val spcUser: String? = findProperty("publishing.spc.user") as? String
    val spcToken: String? = findProperty("publishing.spc.token") as? String

    if (spcUser != null && spcToken != null) {
        publishing.repositories.maven {
            name = "spc"
            url = uri(spaceRepo)

            credentials {
                username = spcUser
                password = spcToken
            }
        }
    }

    val centralUser: String? = project.findProperty("mavenCentralUsername") as? String
    val centralPassword: String? = project.findProperty("mavenCentralPassword") as? String

    if (centralUser != null && centralPassword != null) {
        publishToMavenCentral()
        signAllPublications()
    }
}

kotlin {
    explicitApiWarning()
    jvmToolchain(17)
}

tasks.processResources.configure {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from("gradle/libs.versions.toml")
}

// Workaround for https://github.com/gradle/gradle/issues/15568
tasks.withType<AbstractPublishToMaven>().configureEach {
    mustRunAfter(tasks.withType<Sign>())
}

versionCatalogUpdate {
    sortByKey.set(false)

    keep.keepUnusedVersions = true
}
 