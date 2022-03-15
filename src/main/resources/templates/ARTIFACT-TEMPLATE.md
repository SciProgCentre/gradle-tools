## Artifact:

The Maven coordinates of this project are `${group}:${name}:${version}`.

**Gradle Groovy:**
```groovy
repositories {
    maven { url 'https://repo.kotlin.link' }
    mavenCentral()
}

dependencies {
    implementation '${group}:${name}:${version}'
}
```
**Gradle Kotlin DSL:**
```kotlin
repositories {
    maven("https://repo.kotlin.link")
    mavenCentral()
}

dependencies {
    implementation("${group}:${name}:${version}")
}
```