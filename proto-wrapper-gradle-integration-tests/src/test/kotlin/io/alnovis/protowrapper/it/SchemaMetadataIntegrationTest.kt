package io.alnovis.protowrapper.it

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Integration tests for Schema Metadata generation in Gradle plugin.
 *
 * Verifies that when generateSchemaMetadata=true:
 * 1. SchemaInfo classes are generated for each version
 * 2. SchemaDiff classes are generated for version transitions
 * 3. Generated code compiles successfully
 */
@DisplayName("Schema Metadata Integration Tests")
class SchemaMetadataIntegrationTest {

    @TempDir
    lateinit var testProjectDir: Path

    private val pluginVersion: String = System.getProperty("pluginVersion", "2.3.1")
    private val gradleExecutable: String = findGradleExecutable()

    companion object {
        @JvmStatic
        fun isProtocAvailable(): Boolean {
            return try {
                val process = ProcessBuilder("protoc", "--version")
                    .redirectErrorStream(true)
                    .start()
                process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0
            } catch (e: Exception) {
                false
            }
        }

        private fun findGradleExecutable(): String {
            val candidates = listOf(
                "/usr/bin/gradle",
                "/usr/local/bin/gradle",
                "${System.getProperty("user.home")}/.sdkman/candidates/gradle/current/bin/gradle",
                "gradle"
            )
            for (candidate in candidates) {
                val file = File(candidate)
                if (file.exists() && file.canExecute()) {
                    return candidate
                }
            }
            return "gradle"
        }
    }

    @BeforeEach
    fun setUp() {
        testProjectDir.resolve("settings.gradle.kts").toFile().writeText("""
            pluginManagement {
                repositories {
                    mavenLocal()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            dependencyResolutionManagement {
                repositories {
                    mavenLocal()
                    mavenCentral()
                }
            }

            rootProject.name = "test-project"
        """.trimIndent())
    }

    private fun createProtoFiles() {
        val v1Dir = testProjectDir.resolve("proto/v1").toFile()
        val v2Dir = testProjectDir.resolve("proto/v2").toFile()
        v1Dir.mkdirs()
        v2Dir.mkdirs()

        // V1 proto
        File(v1Dir, "order.proto").writeText("""
            syntax = "proto2";
            package com.example.proto.v1;
            option java_package = "com.example.proto.v1";

            enum Status {
                UNKNOWN = 0;
                PENDING = 1;
                COMPLETED = 2;
            }

            message Order {
                required string id = 1;
                optional Status status = 2;
            }
        """.trimIndent())

        // V2 proto (with new field)
        File(v2Dir, "order.proto").writeText("""
            syntax = "proto2";
            package com.example.proto.v2;
            option java_package = "com.example.proto.v2";

            enum Status {
                UNKNOWN = 0;
                PENDING = 1;
                COMPLETED = 2;
                CANCELLED = 3;
            }

            message Order {
                required string id = 1;
                optional Status status = 2;
                optional string description = 3;
            }
        """.trimIndent())
    }

    private fun createBuildFile(generateMetadata: Boolean = true) {
        testProjectDir.resolve("build.gradle.kts").toFile().writeText("""
            plugins {
                java
                id("com.google.protobuf") version "0.9.4"
                id("io.alnovis.proto-wrapper") version "$pluginVersion"
            }

            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            }

            dependencies {
                implementation("com.google.protobuf:protobuf-java:3.25.1")
                implementation("io.alnovis:proto-wrapper-core:$pluginVersion")
            }

            sourceSets {
                main {
                    proto {
                        srcDir("proto")
                    }
                }
            }

            protobuf {
                protoc {
                    artifact = "com.google.protobuf:protoc:3.25.1"
                }
            }

            protoWrapper {
                basePackage.set("com.example.model")
                protoRoot.set(file("proto"))
                protoPackagePattern.set("com.example.proto.{version}")
                generateSchemaMetadata.set($generateMetadata)
                versions {
                    create("v1") {
                        protoDir.set("v1")
                    }
                    create("v2") {
                        protoDir.set("v2")
                    }
                }
            }

            tasks.named("compileJava") {
                dependsOn("generateProtoWrappers")
            }
        """.trimIndent())
    }

    private fun runGradle(vararg args: String): GradleResult {
        val command = listOf(gradleExecutable, "--no-daemon", "--stacktrace") + args
        val process = ProcessBuilder(command)
            .directory(testProjectDir.toFile())
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        return GradleResult(exitCode == 0, output)
    }

    data class GradleResult(val isSuccess: Boolean, val output: String)

    // ========== Tests ==========

    @Test
    @EnabledIf("isProtocAvailable")
    fun `generates SchemaInfo classes when metadata enabled`() {
        createProtoFiles()
        createBuildFile(generateMetadata = true)

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val metadataDir = File(outputDir, "com/example/model/metadata")

        assertThat(metadataDir).exists()
        assertThat(File(metadataDir, "SchemaInfoV1.java")).exists()
        assertThat(File(metadataDir, "SchemaInfoV2.java")).exists()
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `generates SchemaDiff class for version transition`() {
        createProtoFiles()
        createBuildFile(generateMetadata = true)

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val metadataDir = File(outputDir, "com/example/model/metadata")

        assertThat(File(metadataDir, "SchemaDiffV1ToV2.java")).exists()
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `does not generate metadata when disabled`() {
        createProtoFiles()
        createBuildFile(generateMetadata = false)

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val metadataDir = File(outputDir, "com/example/model/metadata")

        // Metadata directory should not exist when disabled
        assertThat(metadataDir.exists()).isFalse()
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `generated metadata code compiles successfully`() {
        createProtoFiles()
        createBuildFile(generateMetadata = true)

        val result = runGradle("compileJava")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `SchemaInfo implements runtime interface`() {
        createProtoFiles()
        createBuildFile(generateMetadata = true)

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val schemaInfoV1 = File(outputDir, "com/example/model/metadata/SchemaInfoV1.java")

        val content = schemaInfoV1.readText()
        assertThat(content).contains("implements SchemaInfo")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `SchemaDiff implements runtime interface`() {
        createProtoFiles()
        createBuildFile(generateMetadata = true)

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val schemaDiff = File(outputDir, "com/example/model/metadata/SchemaDiffV1ToV2.java")

        val content = schemaDiff.readText()
        assertThat(content).contains("implements VersionSchemaDiff")
    }
}
