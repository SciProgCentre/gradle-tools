[![Maven Central](https://img.shields.io/maven-central/v/space.kscience.gradle.project/space.kscience.gradle.project.gradle.plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22space.kscience.gradle.project%22%20AND%20a:%22space.kscience.gradle.project.gradle.plugin%22)

# KScience build tools

A collection of gradle plugins for building and publish in *kscience* and *dataforge* projects.

## space.kscience.gradle.common
A primary plugin. When used with kotlin-jvm, kotlin-js or kotlin-mulitplatform configures the project for appropriate target.

## space.kscience.gradle.project
Root project tool including JetBrains changelog plugin an kotlin binary compatibility validator tool.

## space.kscience.gradle.mpp
`= kotlin("multiplatform") + space.kscience.gradle.common`

Includes JVM-IR and JS-IR-Browser targets.

## space.kscience.gradle.jvm
`= kotlin("jvm") + space.kscience.gradle.common`
