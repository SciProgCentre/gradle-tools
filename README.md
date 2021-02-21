# KScience build tools

A collection of gradle plugins for building and publish in *kscience* and *dataforge* projects.

## ru.mipt.npm.gradle.common
A primary plugin. When used with kotlin-jvm, kotlin-js or kotlin-mulitplatform configures the project for appropriate target.

## ru.mipt.npm.gradle.project
Root project tool including JetBrains changelog plugin an kotlin binary compatibility validator tool.

## ru.mipt.npm.gradle.publish
Enables publishing to maven-central, bintray, Space and github.

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