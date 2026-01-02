package space.alnovis.protowrapper.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class ProtoWrapperExtension(private val project: Project) {

    // Directories
    abstract val protoRoot: DirectoryProperty
    abstract val outputDirectory: DirectoryProperty
    abstract val tempDirectory: DirectoryProperty

    // Packages
    abstract val basePackage: Property<String>
    abstract val apiPackage: Property<String>
    abstract val implPackagePattern: Property<String>
    abstract val protoPackagePattern: Property<String>

    // Protoc
    abstract val protocPath: Property<String>

    // Generation flags
    abstract val generateInterfaces: Property<Boolean>
    abstract val generateAbstractClasses: Property<Boolean>
    abstract val generateImplClasses: Property<Boolean>
    abstract val generateVersionContext: Property<Boolean>
    abstract val includeVersionSuffix: Property<Boolean>
    abstract val generateBuilders: Property<Boolean>
    abstract val protobufMajorVersion: Property<Int>

    // Well-Known Types support (since 1.3.0)
    abstract val convertWellKnownTypes: Property<Boolean>
    abstract val generateRawProtoAccessors: Property<Boolean>

    // Filtering
    abstract val includeMessages: ListProperty<String>
    abstract val excludeMessages: ListProperty<String>

    // Versions container
    val versions: NamedDomainObjectContainer<VersionConfig> =
        project.container(VersionConfig::class.java) { name ->
            project.objects.newInstance(VersionConfig::class.java, name)
        }

    fun versions(action: Action<NamedDomainObjectContainer<VersionConfig>>) {
        action.execute(versions)
    }

    init {
        // Default values
        outputDirectory.convention(
            project.layout.buildDirectory.dir("generated/sources/proto-wrapper/main/java")
        )
        tempDirectory.convention(
            project.layout.buildDirectory.dir("proto-wrapper-tmp")
        )
        generateInterfaces.convention(true)
        generateAbstractClasses.convention(true)
        generateImplClasses.convention(true)
        generateVersionContext.convention(true)
        includeVersionSuffix.convention(true)
        generateBuilders.convention(false)
        protobufMajorVersion.convention(3)
        convertWellKnownTypes.convention(true)
        generateRawProtoAccessors.convention(false)
    }
}
