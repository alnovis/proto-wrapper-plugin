package space.alnovis.protowrapper.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class ProtoWrapperPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Создать extension для конфигурации DSL
        val extension = project.extensions.create(
            "protoWrapper",
            ProtoWrapperExtension::class.java,
            project
        )

        // Создать task для генерации
        val generateTask = project.tasks.register(
            "generateProtoWrappers",
            GenerateWrappersTask::class.java
        ) { task ->
            task.group = "proto-wrapper"
            task.description = "Generates version-agnostic wrapper classes from protobuf schemas"

            // Связать параметры task с extension
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
            task.includeMessages.set(extension.includeMessages)
            task.excludeMessages.set(extension.excludeMessages)

            // Передать versions через provider
            task.versions.set(project.provider { extension.versions.toList() })
        }

        // Интеграция с Java plugin
        project.plugins.withType(JavaPlugin::class.java) {
            // Добавить generated sources в main source set
            project.extensions.getByType(
                org.gradle.api.plugins.JavaPluginExtension::class.java
            ).sourceSets.getByName("main").java.srcDir(extension.outputDirectory)

            // compileJava зависит от генерации
            project.tasks.named("compileJava") {
                it.dependsOn(generateTask)
            }
        }
    }
}
