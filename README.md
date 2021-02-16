# KScience build tools

A collection of gradle plugins for building and publishin *kscience* and *dataforge* projects.

## ru.mipt.npm.kscience
A primary plugin. When used with kotlin-jvm, kotlin-js or kotlin-mulitplatform configures the project for appropriate target.

## ru.mipt.npm.project
Root project tool including JetBrains changelog plugin an kotlin binary compatibility validator tool.

## ru.mipt.npm.publish
Enables publishing to maven-central, bintray, Space and github.

## ru.mipt.npm.mpp
`= kotlin("multiplatform") + ru.mipt.npm.kscience`

Includes JVM-IR and JS-IR-Browser targets.

## ru.mipt.npm.jvm
`= kotlin("jvm") + ru.mipt.npm.kscience`

## ru.mipt.npm.js
`= kotlin("js + ru.mipt.npm.kscience`

## ru.mipt.npm.native
add default native targets to `ru.mipt.npm.mpp`

## ru.mipt.npm.node
add node target to `ru.mipt.npm.mpp`