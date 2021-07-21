[![Maven Central](https://img.shields.io/maven-central/v/ru.mipt.npm.gradle.project/ru.mipt.npm.gradle.project.gradle.plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22ru.mipt.npm.gradle.project%22%20AND%20a:%22ru.mipt.npm.gradle.project.gradle.plugin%22)

# KScience build tools

A collection of gradle plugins for building and publish in *kscience* and *dataforge* projects.

## ru.mipt.npm.gradle.common
A primary plugin. When used with kotlin-jvm, kotlin-js or kotlin-mulitplatform configures the project for appropriate target.

## ru.mipt.npm.gradle.project
Root project tool including JetBrains changelog plugin an kotlin binary compatibility validator tool.

## ru.mipt.npm.gradle.mpp
`= kotlin("multiplatform") + ru.mipt.npm.gradle.common`

Includes JVM-IR and JS-IR-Browser targets.

## ru.mipt.npm.gradle.jvm
`= kotlin("jvm") + ru.mipt.npm.gradle.common`

## ru.mipt.npm.gradle.js
`= kotlin("js") + ru.mipt.npm.gradle.common`

## ru.mipt.npm.gradle.native
add default native targets to `ru.mipt.npm.gradle.mpp`

## ru.mipt.npm.gradle.node
add node target to `ru.mipt.npm.gradle.mpp`
