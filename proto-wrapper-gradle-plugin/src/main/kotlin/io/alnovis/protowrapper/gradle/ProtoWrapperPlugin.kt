package io.alnovis.protowrapper.gradle

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
            task.protocVersion.set(extension.protocVersion)
            task.generateInterfaces.set(extension.generateInterfaces)
            task.generateAbstractClasses.set(extension.generateAbstractClasses)
            task.generateImplClasses.set(extension.generateImplClasses)
            task.generateVersionContext.set(extension.generateVersionContext)
            task.generateProtocolVersions.set(extension.generateProtocolVersions)
            task.includeVersionSuffix.set(extension.includeVersionSuffix)
            task.generateBuilders.set(extension.generateBuilders)
            task.protobufMajorVersion.set(extension.protobufMajorVersion)
            task.convertWellKnownTypes.set(extension.convertWellKnownTypes)
            task.generateRawProtoAccessors.set(extension.generateRawProtoAccessors)
            task.includeMessages.set(extension.includeMessages)
            task.excludeMessages.set(extension.excludeMessages)

            // Incremental generation settings (since 1.6.0)
            task.incremental.set(extension.incremental)
            task.cacheDirectory.set(extension.cacheDirectory)
            task.forceRegenerate.set(extension.forceRegenerate)

            // Java version compatibility (since 1.6.8)
            task.targetJavaVersion.set(extension.targetJavaVersion)

            // Parallel generation (since 2.1.0)
            task.parallelGeneration.set(extension.parallelGeneration)
            task.generationThreads.set(extension.generationThreads)

            // Default version (since 2.1.1)
            task.defaultVersion.set(extension.defaultVersion)

            // Field mappings (since 2.2.0)
            task.fieldMappings.set(project.provider { extension.fieldMappings.toList() })

            // Validation annotations (since 2.3.0)
            task.generateValidationAnnotations.set(extension.generateValidationAnnotations)
            task.validationAnnotationStyle.set(extension.validationAnnotationStyle)

            // Schema metadata (since 2.3.1)
            task.generateSchemaMetadata.set(extension.generateSchemaMetadata)

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
