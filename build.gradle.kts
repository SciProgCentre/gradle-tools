plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    signing
    `version-catalog`
    alias(libs.plugins.changelog)
    alias(libs.plugins.dokka)
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

kotlin.explicitApiWarning()

dependencies {
    api(libs.kotlin.gradle)
    implementation(libs.binary.compatibility.validator)
    implementation(libs.changelog.gradle)
    implementation(libs.dokka.gradle)
    implementation(libs.kotlin.jupyter.gradle)
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlinx.html)
    implementation("org.tomlj:tomlj:1.1.0")
//    // nexus publishing plugin
//    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")

    implementation("org.freemarker:freemarker:2.3.31")

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

tasks.create("version") {
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

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.named("main").get().allSource)
}

val javadocsJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

val emptyJavadocJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveBaseName.set("empty")
    archiveClassifier.set("javadoc")
}


val emptySourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    archiveBaseName.set("empty")
}

publishing {
    val vcs = "https://github.com/SciProgCentre/gradle-tools"

    // Process each publication we have in this project
    publications {
        create<MavenPublication>("catalog") {
            from(components["versionCatalog"])
            artifactId = "version-catalog"

            pom {
                name.set("version-catalog")
            }
        }

        withType<MavenPublication> {
            // thanks @vladimirsitnikv for the fix
            artifact(if (name == "catalog") emptySourcesJar else sourcesJar)
            artifact(if (name == "catalog") emptyJavadocJar else javadocsJar)


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
        }
    }

    val spaceRepo = "https://maven.sciprog.center/kscience"
    val spcUser: String? = findProperty("publishing.spc.user") as? String
    val spcToken: String? = findProperty("publishing.spc.token") as? String

    if (spcUser != null && spcToken != null) {
        repositories.maven {
            name = "spc"
            url = uri(spaceRepo)

            credentials {
                username = spcUser
                password = spcToken
            }
        }
    }

    val sonatypeUser: String? = project.findProperty("publishing.sonatype.user") as? String
    val sonatypePassword: String? = project.findProperty("publishing.sonatype.password") as? String

    if (sonatypeUser != null && sonatypePassword != null) {
        val sonatypeRepo: String = if (project.version.toString().contains("dev")) {
            "https://oss.sonatype.org/content/repositories/snapshots"
        } else {
            "https://oss.sonatype.org/service/local/staging/deploy/maven2"
        }

        repositories.maven {
            name = "sonatype"
            url = uri(sonatypeRepo)

            credentials {
                username = sonatypeUser
                password = sonatypePassword
            }
        }

        signing {
            //useGpgCmd()
            sign(publications)
        }
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.processResources.configure {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from("gradle/libs.versions.toml")
}

// Workaround for https://github.com/gradle/gradle/issues/15568
tasks.withType<AbstractPublishToMaven>().configureEach {
    mustRunAfter(tasks.withType<Sign>())
}