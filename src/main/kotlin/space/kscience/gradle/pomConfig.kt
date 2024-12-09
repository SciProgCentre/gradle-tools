package space.kscience.gradle

import org.gradle.api.publish.maven.MavenPom

public fun MavenPom.useApache2Licence(){
    licenses {
        license {
            name.set("The Apache Software License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            distribution.set("repo")
        }
    }
}

public fun MavenPom.useSPCTeam(){
    developers {
        developer {
            id.set("SPC")
            name.set("Scientific programming centre")
            organization.set("SPC")
            organizationUrl.set("https://sciprog.center/")
        }
    }
}