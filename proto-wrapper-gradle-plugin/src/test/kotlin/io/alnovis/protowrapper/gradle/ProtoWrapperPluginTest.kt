package io.alnovis.protowrapper.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

/**
 * Unit tests for ProtoWrapperPlugin.
 *
 * These tests use Gradle's ProjectBuilder for fast, in-process testing
 * without starting a full Gradle daemon.
 */
class ProtoWrapperPluginTest {

    private lateinit var project: Project

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
    }

    @Nested
    inner class PluginApplication {

        @Test
        fun `plugin can be applied by id`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            assertThat(project.plugins.hasPlugin("io.alnovis.proto-wrapper")).isTrue()
        }

        @Test
        fun `plugin can be applied by class`() {
            project.pluginManager.apply(ProtoWrapperPlugin::class.java)

            assertThat(project.plugins.hasPlugin(ProtoWrapperPlugin::class.java)).isTrue()
        }
    }

    @Nested
    inner class ExtensionCreation {

        @Test
        fun `extension is created with correct name`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.findByName("protoWrapper")

            assertThat(extension).isNotNull
            assertThat(extension).isInstanceOf(ProtoWrapperExtension::class.java)
        }

        @Test
        fun `extension can be accessed by type`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)

            assertThat(extension).isNotNull
        }
    }

    @Nested
    inner class TaskRegistration {

        @Test
        fun `generateProtoWrappers task is registered`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val task = project.tasks.findByName("generateProtoWrappers")

            assertThat(task).isNotNull
            assertThat(task).isInstanceOf(GenerateWrappersTask::class.java)
        }

        @Test
        fun `task has correct group`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val task = project.tasks.getByName("generateProtoWrappers")

            assertThat(task.group).isEqualTo("proto-wrapper")
        }

        @Test
        fun `task has description`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val task = project.tasks.getByName("generateProtoWrappers")

            assertThat(task.description).contains("wrapper")
        }
    }

    @Nested
    inner class DefaultValues {

        @Test
        fun `incremental is enabled by default`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)

            assertThat(extension.incremental.get()).isTrue()
        }

        @Test
        fun `forceRegenerate is disabled by default`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)

            assertThat(extension.forceRegenerate.get()).isFalse()
        }

        @Test
        fun `cacheDirectory has default value`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)

            assertThat(extension.cacheDirectory.isPresent).isTrue()
            assertThat(extension.cacheDirectory.get().asFile.path).contains("proto-wrapper-cache")
        }

        @Test
        fun `outputDirectory has default value`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)

            assertThat(extension.outputDirectory.isPresent).isTrue()
            assertThat(extension.outputDirectory.get().asFile.path).contains("proto-wrapper")
        }

        @Test
        fun `generateInterfaces is enabled by default`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)

            assertThat(extension.generateInterfaces.get()).isTrue()
        }

        @Test
        fun `generateBuilders is disabled by default`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)

            assertThat(extension.generateBuilders.get()).isFalse()
        }

        @Test
        fun `convertWellKnownTypes is enabled by default`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)

            assertThat(extension.convertWellKnownTypes.get()).isTrue()
        }

        @Test
        fun `protobufMajorVersion defaults to 3`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)

            assertThat(extension.protobufMajorVersion.get()).isEqualTo(3)
        }
    }

    @Nested
    inner class ExtensionConfiguration {

        @Test
        fun `incremental can be disabled`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)
            extension.incremental.set(false)

            assertThat(extension.incremental.get()).isFalse()
        }

        @Test
        fun `forceRegenerate can be enabled`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)
            extension.forceRegenerate.set(true)

            assertThat(extension.forceRegenerate.get()).isTrue()
        }

        @Test
        fun `cacheDirectory can be customized`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)
            val customDir = project.layout.buildDirectory.dir("custom-cache")
            extension.cacheDirectory.set(customDir)

            assertThat(extension.cacheDirectory.get().asFile.path).contains("custom-cache")
        }

        @Test
        fun `basePackage configures derived packages`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)
            extension.basePackage.set("com.example.proto")

            assertThat(extension.basePackage.get()).isEqualTo("com.example.proto")
        }

        @Test
        fun `versions container is available`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)

            assertThat(extension.versions).isNotNull
            assertThat(extension.versions.size).isEqualTo(0)
        }

        @Test
        fun `versions can be added`() {
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)
            extension.versions.create("v1") { config ->
                config.protoDir.set("proto/v1")
            }

            assertThat(extension.versions.size).isEqualTo(1)
            assertThat(extension.versions.getByName("v1").protoDir.get()).isEqualTo("proto/v1")
        }
    }

    @Nested
    inner class JavaPluginIntegration {

        @Test
        fun `compileJava depends on generateProtoWrappers when Java plugin is applied`() {
            project.pluginManager.apply("java")
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val compileJava = project.tasks.getByName("compileJava")

            // Get all task dependencies as resolved task names
            val dependencyNames = compileJava.taskDependencies
                .getDependencies(compileJava)
                .map { it.name }

            assertThat(dependencyNames).contains("generateProtoWrappers")
        }

        @Test
        fun `generated sources are added to main source set`() {
            project.pluginManager.apply("java")
            project.pluginManager.apply("io.alnovis.proto-wrapper")

            val extension = project.extensions.getByType(ProtoWrapperExtension::class.java)
            val javaExtension = project.extensions.getByType(
                org.gradle.api.plugins.JavaPluginExtension::class.java
            )
            val mainSourceSet = javaExtension.sourceSets.getByName("main")
            val srcDirs = mainSourceSet.java.srcDirs

            assertThat(srcDirs).contains(extension.outputDirectory.get().asFile)
        }
    }
}
