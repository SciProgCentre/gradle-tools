job("Build") {
    gradlew("openjdk:11", "build")
}

job("Publish"){
    startOn {
        gitPush { enabled = false }
    }
    container("openjdk:11") {
        env["SPACE_USER"] = Secrets("space_user")
        env["SPACE_TOKEN"] = Secrets("space_token")
        kotlinScript { api ->
            val spaceUser = System.getenv("SPACE_USER")
            val spaceToken = System.getenv("SPACE_TOKEN")

            api.space().projects.automation.deployments.start(
                project = api.projectIdentifier(),
                targetIdentifier = TargetIdentifier.Key("gradle-tools"),
                version = api.gitRevision(),
                // automatically update deployment status based on a status of a job
                syncWithAutomationJob = true
            )
            try {
                api.gradlew(
                    "publishAllPublicationsToSpaceRepository",
                    "-Ppublishing.space.user=\"$spaceUser\"",
                    "-Ppublishing.space.token=\"$spaceToken\"",
                )
            } catch (ex: Exception) {
                println("Publish failed")
            }
        }
    }
}
