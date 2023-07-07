## Artifact:

The Maven coordinates of this project are `${group}:${name}:${version}`.

**Gradle Kotlin DSL:**
```kotlin
repositories {
    maven("https://repo.kotlin.link")
    //uncomment to access development builds
    //maven("https://maven.pkg.jetbrains.space/spc/p/sci/dev")
    mavenCentral()
}

dependencies {
    implementation("${group}:${name}:${version}")
}
```