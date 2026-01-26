package io.alnovis.protowrapper.it

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Integration tests for Schema Metadata functionality in Gradle plugin.
 *
 * These tests create real Gradle projects and run actual Gradle builds
 * to verify that generateSchemaMetadata=true produces expected code.
 *
 * The plugin must be published to mavenLocal before running these tests:
 *   ./gradlew publishToMavenLocal
 */
class SchemaMetadataIntegrationTest {

    @TempDir
    lateinit var testProjectDir: Path

    private val pluginVersion: String = System.getProperty("pluginVersion", "2.3.1")
    private val projectRoot: String = System.getProperty("projectRoot", "..")
    private val testProtosDir: File = File(System.getProperty("testProtosDir", "../test-protos/scenarios"))
    private val gradleExecutable: String = System.getProperty("gradleExecutable", findGradleExecutable())

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
        val settingsFile = testProjectDir.resolve("settings.gradle.kts").toFile()
        settingsFile.writeText("""
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

    private fun copyGenerationProtos(): Pair<File, File> {
        val v1Dir = testProjectDir.resolve("proto/v1").toFile()
        val v2Dir = testProjectDir.resolve("proto/v2").toFile()
        v1Dir.mkdirs()
        v2Dir.mkdirs()

        val generationDir = File(testProtosDir, "../generation")

        // Copy v1 protos
        File(generationDir, "v1").listFiles()?.forEach { file ->
            if (file.extension == "proto") {
                file.copyTo(File(v1Dir, file.name), overwrite = true)
            }
        }

        // Copy v2 protos
        File(generationDir, "v2").listFiles()?.forEach { file ->
            if (file.extension == "proto") {
                file.copyTo(File(v2Dir, file.name), overwrite = true)
            }
        }

        return v1Dir to v2Dir
    }

    private fun createBuildFile(extraConfig: String = "") {
        val buildFile = testProjectDir.resolve("build.gradle.kts").toFile()
        buildFile.writeText("""
            plugins {
                id("java")
                id("io.alnovis.proto-wrapper") version "$pluginVersion"
            }

            dependencies {
                implementation("com.google.protobuf:protobuf-java:3.25.1")
                implementation("io.alnovis:proto-wrapper-core:$pluginVersion")
            }

            protoWrapper {
                basePackage.set("io.alnovis.test.model")
                protoPackagePattern.set("io.alnovis.test.proto.{version}")
                protoRoot.set(file("proto"))
                generateBuilders.set(true)
                generateSchemaMetadata.set(true)
                $extraConfig
                versions {
                    register("v1") {
                        protoDir.set("v1")
                    }
                    register("v2") {
                        protoDir.set("v2")
                    }
                }
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
        val completed = process.waitFor(5, TimeUnit.MINUTES)

        return if (completed) {
            GradleResult(process.exitValue(), output)
        } else {
            process.destroyForcibly()
            GradleResult(-1, "$output\n[TIMEOUT]")
        }
    }

    data class GradleResult(val exitCode: Int, val output: String) {
        val isSuccess: Boolean get() = exitCode == 0
        val isFailed: Boolean get() = exitCode != 0
    }

    // ==================== Tests ====================

    @Test
    @EnabledIf("isProtocAvailable")
    fun `generateSchemaMetadata generates SchemaInfo classes`() {
        copyGenerationProtos()
        createBuildFile()

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        // Check that SchemaInfo classes are generated
        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()

        val schemaInfoV1 = File(outputDir, "io/alnovis/test/metadata/SchemaInfoV1.java")
        assertThat(schemaInfoV1).exists()

        val schemaInfoV2 = File(outputDir, "io/alnovis/test/metadata/SchemaInfoV2.java")
        assertThat(schemaInfoV2).exists()
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `generateSchemaMetadata generates SchemaDiff classes`() {
        copyGenerationProtos()
        createBuildFile()

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()

        // Check that SchemaDiff class is generated for v1->v2 transition
        val schemaDiffV1ToV2 = File(outputDir, "io/alnovis/test/metadata/SchemaDiffV1ToV2.java")
        assertThat(schemaDiffV1ToV2).exists()
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `SchemaInfo class implements SchemaInfo interface`() {
        copyGenerationProtos()
        createBuildFile()

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val schemaInfoV1 = File(outputDir, "io/alnovis/test/metadata/SchemaInfoV1.java")

        val content = schemaInfoV1.readText()
        assertThat(content).contains("implements SchemaInfo")
        assertThat(content).contains("getVersionId()")
        assertThat(content).contains("getEnums()")
        assertThat(content).contains("getMessages()")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `SchemaDiff class implements VersionSchemaDiff interface`() {
        copyGenerationProtos()
        createBuildFile()

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val schemaDiffFile = File(outputDir, "io/alnovis/test/metadata/SchemaDiffV1ToV2.java")

        val content = schemaDiffFile.readText()
        assertThat(content).contains("implements VersionSchemaDiff")
        assertThat(content).contains("getFromVersion()")
        assertThat(content).contains("getToVersion()")
        assertThat(content).contains("getFieldChanges()")
        assertThat(content).contains("getEnumChanges()")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `VersionContext has getSchemaInfo method when metadata enabled`() {
        copyGenerationProtos()
        createBuildFile()

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()

        val versionContextV1 = File(outputDir, "io/alnovis/test/model/v1/VersionContextV1.java")
        assertThat(versionContextV1).exists()

        val content = versionContextV1.readText()
        assertThat(content).contains("getSchemaInfo()")
        assertThat(content).contains("SchemaInfo")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `VersionContext has getDiffFrom method when metadata enabled`() {
        copyGenerationProtos()
        createBuildFile()

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()

        val versionContextV2 = File(outputDir, "io/alnovis/test/model/v2/VersionContextV2.java")
        assertThat(versionContextV2).exists()

        val content = versionContextV2.readText()
        assertThat(content).contains("getDiffFrom")
        assertThat(content).contains("VersionSchemaDiff")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `SchemaInfo uses singleton pattern`() {
        copyGenerationProtos()
        createBuildFile()

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val schemaInfoV1 = File(outputDir, "io/alnovis/test/metadata/SchemaInfoV1.java")

        val content = schemaInfoV1.readText()
        assertThat(content).contains("public static final SchemaInfoV1 INSTANCE")
        assertThat(content).contains("private SchemaInfoV1()")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `SchemaDiff uses singleton pattern`() {
        copyGenerationProtos()
        createBuildFile()

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val schemaDiffFile = File(outputDir, "io/alnovis/test/metadata/SchemaDiffV1ToV2.java")

        val content = schemaDiffFile.readText()
        assertThat(content).contains("public static final SchemaDiffV1ToV2 INSTANCE")
        assertThat(content).contains("private SchemaDiffV1ToV2()")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `generated code compiles successfully`() {
        copyGenerationProtos()
        createBuildFile()

        // Run both generateProtoWrappers and compileJava
        val result = runGradle("compileJava")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `SchemaInfo contains enum metadata`() {
        copyGenerationProtos()
        createBuildFile()

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val schemaInfoV1 = File(outputDir, "io/alnovis/test/metadata/SchemaInfoV1.java")

        val content = schemaInfoV1.readText()
        // Should contain ENUMS map initialization
        assertThat(content).contains("Map<String, EnumInfo> ENUMS")
        assertThat(content).contains("EnumInfoImpl")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `SchemaInfo contains message metadata`() {
        copyGenerationProtos()
        createBuildFile()

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val schemaInfoV1 = File(outputDir, "io/alnovis/test/metadata/SchemaInfoV1.java")

        val content = schemaInfoV1.readText()
        // Should contain MESSAGES map initialization
        assertThat(content).contains("Map<String, MessageInfo> MESSAGES")
        assertThat(content).contains("MessageInfoImpl")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `VersionContext interface has metadata methods`() {
        copyGenerationProtos()
        createBuildFile()

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val versionContextInterface = File(outputDir, "io/alnovis/test/model/api/VersionContext.java")

        assertThat(versionContextInterface).exists()

        val content = versionContextInterface.readText()
        assertThat(content).contains("getSchemaInfo()")
        assertThat(content).contains("getDiffFrom(String fromVersion)")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `Java 8 compatibility uses Collections API`() {
        copyGenerationProtos()
        createBuildFile("targetJavaVersion.set(8)")

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val schemaInfoV1 = File(outputDir, "io/alnovis/test/metadata/SchemaInfoV1.java")

        val content = schemaInfoV1.readText()
        // Java 8 should use Collections.unmodifiableMap instead of Map.ofEntries
        assertThat(content).contains("Collections.unmodifiableMap")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `Java 9+ uses modern API`() {
        copyGenerationProtos()
        createBuildFile("targetJavaVersion.set(9)")

        val result = runGradle("generateProtoWrappers")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val schemaInfoV1 = File(outputDir, "io/alnovis/test/metadata/SchemaInfoV1.java")

        val content = schemaInfoV1.readText()
        // Java 9+ should use Map.ofEntries
        assertThat(content).containsAnyOf("Map.ofEntries", "Map.of()")
    }
}
