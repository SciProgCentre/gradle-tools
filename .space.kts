job("Build") {
    gradlew("openjdk:11", "build")
}

job("Publish"){
    startOn {
        gitPush { enabled = false }
    }
    container("openjdk:11") {
        kotlinScript { api ->
            api.space().projects.automation.deployments.start(
                project = api.projectIdentifier(),
                targetIdentifier = TargetIdentifier.Key("gradle-tools"),
                version = "current",
                // automatically update deployment status based on a status of a job
                syncWithAutomationJob = true
            )
            api.gradlew("publishAllPublicationsToSpaceRepository")
        }
    }
}
