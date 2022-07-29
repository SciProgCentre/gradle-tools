package space.kscience.gradle

import org.gradle.api.Project
import space.kscience.gradle.internal.useCommonDependency

public class SerializationTargets(
    public val sourceSet: DependencySourceSet,
    public val configuration: DependencyConfiguration,
) {
    public fun Project.json(
        version: String = KScienceVersions.serializationVersion,
    ): Unit = useCommonDependency(
        "org.jetbrains.kotlinx:kotlinx-serialization-json:$version",
        dependencySourceSet = sourceSet,
        dependencyConfiguration = configuration,
    )

    public fun Project.cbor(
        version: String = KScienceVersions.serializationVersion,
    ): Unit = useCommonDependency(
        "org.jetbrains.kotlinx:kotlinx-serialization-cbor:$version",
        dependencySourceSet = sourceSet,
        dependencyConfiguration = configuration,
    )

    public fun Project.protobuf(
        version: String = KScienceVersions.serializationVersion,
    ): Unit = useCommonDependency(
        "org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$version",
        dependencySourceSet = sourceSet,
        dependencyConfiguration = configuration,
    )

    public fun Project.xml(
        version: String = KScienceVersions.Serialization.xmlVersion,
    ): Unit = useCommonDependency(
        "io.github.pdvrieze.xmlutil:serialization:$version",
        dependencySourceSet = sourceSet,
        dependencyConfiguration = configuration,
    )

    public fun Project.yamlKt(
        version: String = KScienceVersions.Serialization.yamlKtVersion,
    ): Unit = useCommonDependency(
        "net.mamoe.yamlkt:yamlkt:$version",
        dependencySourceSet = sourceSet,
        dependencyConfiguration = configuration,
    )
}
