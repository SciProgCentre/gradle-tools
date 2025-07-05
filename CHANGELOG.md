# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added

### Changed
- Replace manual JS dependency resource handling with `opensavvy` plugin.
- Replace `useContextRecievers` with `useContextParameters`.
- Submodule readme generated for all projects, not only for root
- Replace `wasm` plugin configuration block with `wasmJs`

### Deprecated
- `useContextRecievers`
- `wasm` configuration block

### Removed
- `fullStackApplication` configuration. Replaced by optional field in `fullStack`
- Jupyter integration

### Fixed

### Security

## 0.17.x

### Added

- kotlinx-io dependency in version catalog

### Changed

- Use the new jvm executable option for full-stack instead of gradle application plugin.

### Removed

- `application` option

### Fixed

- Fix readme generation

## 0.16.0-kotlin-2.1.0 - 2025-01-02

### Changed

- Kotlin 2.1.0
- Publication to central via `com.vanniktech.maven.publish.base`

## 0.15.4-kotlin-2.0.0 - 2024-06-04

### Added

- Pass `compose` extension to the kscience extension so compose dependencies could be called directly from kscience block

### Changed

- Use ES6 modules by default in JS
- Kotlin 2.0

## 0.15.2-kotlin-1.9.22 - 2024-02-09

### Added

- Add development mode for fullstack.

### Changed

- Kotlin 1.9.20
- Remove publishing defaults for Space and GitHub. Now publishing repositories is configured quasi-manually. Property keys for username and tokens are generated automatically.
- All publishing targets are enabled by default. Introduce `publishing.targets` variable to regulate what is added to the module.

### Deprecated

- JVM plugin in favor of Mpp.

### Removed

- JS plugin. It is deprecated in favor of MPP.
- Unnecessary `dependsOn` for native dependencies. 
- Groovy gradle dependency notation.

## 0.14.4-kotlin-1.8.20-RC - 2023-03-12

### Added

- Easier dependency handling in `kscience` block
- Customizable base jdk version

### Changed

- MPP does not use JVM and JS(IR) targets anymore. They could be turned manually via `js()`, `jvm()` or `fullStack()`
- Signing is not applied if signingId is not provided

### Removed

- FX plugins
- Unnecessary library shortcuts (html and datetime)
- deploy/release tasks

### Fixed

- Gradle 8 compatibility
- Dokka publication
- issues with test sourcesets

## 0.13.4-kotlin-1.8.0 - 2022-12-31

### Added

- Public `isInDevelopment` project flag

### Changed

- Require manual pom config for publications
- Kotlin 1.8.0
- Versions update
- Project group changed to `space.kscience`
- Moved `yarn.lock` to `gradle` directory

### Deprecated

- FX configuration

### Removed

- Xjdk-release flag because it is broken until https://youtrack.jetbrains.com/issue/KT-52823
- Use CSS loader in JS by default

## 0.11.6-kotlin-1.7.0

### Changed

- Coroutines tests are applied only when explicit `useCoroutines` is used.

### Removed

- Atomicfu support inside the plugin

### Fixed

- Rollback coroutines to 1.6.1

## 0.11.5-kotlin-1.7.0

### Added

- Coroutine tests as default dependency for tests
- Context receiver flag

### Changed

- Separate release tasks for each target
- Kotlin 1.7.0
- Ktor 2.0.1
- ExplicitAPI does not override existing value

### Removed

- Ktor specific artifacts from version catalog

### Fixed

- Moved signing out of sonatype block

## 0.11.1-kotlin-1.6.10

### Added

- Default templates for README and ARTIFACT

### Changed

- Replaced Groovy templates by FreeMarker

### Fixed

- JS publication sources jar

## 0.10.9-kotlin-1.6.10

### Added

- html builders for readme

### Changed

- Kotlin 1.6.0
- Use indy lambdas by default #32
- Change version scheme to `<version>-kotlin-<kotlin version>`

### Fixed

- remove `nativeMain` dependency from `nativeTest`

## 0.10.4

### Changed

- Kotlin 1.6

### Fixed

- Some issues with opt-ins

## 0.10.2

### Added

- Experimental automatic JS project bundling in MPP

### Changed

- Remove vcs requirement for Space publication

## 0.10.0

### Added

- Lazy readme properties
- BOM for kotlin-wrappers on JS
- Jupyter loader

### Changed

- API validation disabled for dev versions
- Kotlin plugins are propagated downstream

### Removed

- bson support

## 0.9.5

### Added

- Disable API validation for snapshots
- `-Xjvm-default=all` on JVM

### Changed

- `publication.platform` changed to `publishing.platform`
- Dokka version to `1.4.30`
- `useDateTime` in extension
- Kotlin 1.5

### Removed

- Publish plugin. Use MavenPublish instead

### Fixed

- Removed unnecessary `afterEvaluate` for compatibility with gradle 7.0

## 0.9.0

### Added

- Skip sonatype publishing for dev versions

### Changed

- Publishing repositories are explicit and defined in the top level project
- Paths to publishing properties now use dot notation like `publishing.github.user`

### Deprecated

- Publishing plugin

### Removed

- Bintray publishing

## 0.8.4

### Added

- Adaptive support for host OS in native
- CSS support for JS targets

### Changed

- Kotlin 1.4.31
- Coroutines 1.4.3

### Fixed

- Plugin loading order for publishing
- Release task
- Readme generation for multi-module project

## 0.8.1

### Added

- Ktor version to versions
- Add sonatype publishing
- Per-platform release publishing

### Changed

- Kotlin to 1.4.30 stable.
- Added intermediate jsCommon main/test sourcesSet for node plugin.
- Plugin names changed to `ru.mipt.npm` package.
- Common plugin id changed to `common`
- Plugins group changed to `ru.mipt.npm` with `gradle` prefix

### Removed

- kaml

### Fixed

- Fix publishing load order for sonatype
- Fix root project readme

## 0.7.4

### Added

- Changelog plugin automatically applied to `project`.
- Feature matrix and Readme generation task for a `project` plugin.
- Add `binary-compatibility-validator` to the `project` plugin.
- Separate `yamlKt` serialization target
- Moved all logic to a common plugin, leaving only proxies for platform plugins
- Suppress API validation for modules with maturity below DEVELOPMENT

### Changed

- Remove node plugin. Node binaries should be turned on manually.
- Use default webpack distribution path.
- `ru.mipt.npm.base` -> `ru.mipt.npm.project`.
- Move publishing out of general extension and apply it to project plugin instead.
- Platform plugins are now simple references to common plugin
- FX configuration moved to extension
- Moved internals to internals
- Kotlin 1.4.30-RC

### Deprecated

- Support of `kaml` and `snake-yaml` in favor of `yamlKt`
- Publish plugin

### Removed

- `useDokka` method. Documentation jar should be added manually if needed.

## 0.6.0

### Added

- Migrate to kotlin 1.4.0
- Separate Native (current platform) and nodeJs plugins.
- Add `application()` toggle in plugin configuration to produce binaries on JS and applicaion plugin on jvm.
- Add `publish` to expose publishing configuration.

## 0.5.2

### Added

- Copy resources for jvm modules and jvm source sets in mpp.
