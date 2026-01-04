package space.alnovis.protowrapper.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class ProtoWrapperPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Create extension for DSL configuration
        val extension = project.extensions.create(
            "protoWrapper",
            ProtoWrapperExtension::class.java,
            project
        )

        // Register schema diff task type for ad-hoc usage
        // Users can create their own instances with custom configuration:
        // tasks.register<SchemaDiffTask>("diffSchemas") { ... }

        // Create task for generation
        val generateTask = project.tasks.register(
            "generateProtoWrappers",
            GenerateWrappersTask::class.java
        ) { task ->
            task.group = "proto-wrapper"
            task.description = "Generates version-agnostic wrapper classes from protobuf schemas"

            // Bind task parameters to extension
            task.protoRoot.set(extension.protoRoot)
            task.outputDirectory.set(extension.outputDirectory)
            task.tempDirectory.set(extension.tempDirectory)
            task.basePackage.set(extension.basePackage)
            task.apiPackage.set(extension.apiPackage)
            task.implPackagePattern.set(extension.implPackagePattern)
            task.protoPackagePattern.set(extension.protoPackagePattern)
            task.protocPath.set(extension.protocPath)
            task.generateInterfaces.set(extension.generateInterfaces)
            task.generateAbstractClasses.set(extension.generateAbstractClasses)
            task.generateImplClasses.set(extension.generateImplClasses)
            task.generateVersionContext.set(extension.generateVersionContext)
            task.includeVersionSuffix.set(extension.includeVersionSuffix)
            task.generateBuilders.set(extension.generateBuilders)
            task.protobufMajorVersion.set(extension.protobufMajorVersion)
            task.convertWellKnownTypes.set(extension.convertWellKnownTypes)
            task.generateRawProtoAccessors.set(extension.generateRawProtoAccessors)
            task.includeMessages.set(extension.includeMessages)
            task.excludeMessages.set(extension.excludeMessages)

            // Pass versions via provider
            task.versions.set(project.provider { extension.versions.toList() })
        }

        // Integration with Java plugin
        project.plugins.withType(JavaPlugin::class.java) {
            // Add generated sources to main source set
            project.extensions.getByType(
                org.gradle.api.plugins.JavaPluginExtension::class.java
            ).sourceSets.getByName("main").java.srcDir(extension.outputDirectory)

            // compileJava depends on generation
            project.tasks.named("compileJava") {
                it.dependsOn(generateTask)
            }
        }
    }
}
