job("Build") {
    gradlew("openjdk:11", "build")
}

job("Publish"){
    startOn {
        gitPush { enabled = false }
    }
    gradlew("openjdk:11", "publishAllPublicationsToSpaceRepository")
}
