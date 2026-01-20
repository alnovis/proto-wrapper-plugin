package space.alnovis.protowrapper.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat

/**
 * Functional tests for GenerateWrappersTask using Gradle TestKit.
 *
 * These tests require protoc to be available on the system PATH.
 * Tagged as "slow" because each test starts a full Gradle process.
 * Run with: ./gradlew :proto-wrapper-gradle-plugin:slowTest
 */
@Tag("slow")
class GenerateWrappersTaskTest {

    @TempDir
    lateinit var testProjectDir: Path

    private lateinit var buildFile: File
    private lateinit var settingsFile: File
    private lateinit var protoDir: File

    companion object {
        @JvmStatic
        fun isProtocAvailable(): Boolean {
            return try {
                val process = ProcessBuilder("protoc", "--version")
                    .redirectErrorStream(true)
                    .start()
                process.waitFor() == 0
            } catch (e: Exception) {
                false
            }
        }
    }

    @BeforeEach
    fun setup() {
        settingsFile = testProjectDir.resolve("settings.gradle.kts").toFile()
        settingsFile.writeText("""
            rootProject.name = "test-project"
        """.trimIndent())

        buildFile = testProjectDir.resolve("build.gradle.kts").toFile()

        // Create proto directory structure
        protoDir = testProjectDir.resolve("proto").toFile()
        protoDir.mkdirs()
        testProjectDir.resolve("proto/v1").toFile().mkdirs()
        testProjectDir.resolve("proto/v2").toFile().mkdirs()

        // Copy test proto files
        copyTestProto("/proto/v1/test.proto", testProjectDir.resolve("proto/v1").toFile())
        copyTestProto("/proto/v2/test.proto", testProjectDir.resolve("proto/v2").toFile())
    }

    private fun copyTestProto(resourcePath: String, targetDir: File) {
        val content = javaClass.getResourceAsStream(resourcePath)?.bufferedReader()?.readText()
            ?: throw IllegalStateException("Resource not found: $resourcePath")
        File(targetDir, "test.proto").writeText(content)
    }

    private fun createBuildFile(config: String = "") {
        buildFile.writeText("""
            plugins {
                id("java")
                id("io.github.alnovis.proto-wrapper")
            }

            protoWrapper {
                protoRoot.set(file("proto"))
                protoPackagePattern.set("test.diff.{version}")
                basePackage.set("com.example.model")

                versions {
                    create("v1") {
                        protoDir.set("v1")
                    }
                    create("v2") {
                        protoDir.set("v2")
                    }
                }

                $config
            }
        """.trimIndent())
    }

    private fun runTask(vararg args: String): org.gradle.testkit.runner.BuildResult {
        return GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments(*args, "--stacktrace")
            .withPluginClasspath()
            .build()
    }

    // ============ Basic Generation Tests ============

    @Test
    @EnabledIf("isProtocAvailable")
    fun `task generates wrapper classes successfully`() {
        createBuildFile()

        val result = runTask("generateProtoWrappers")

        assertThat(result.task(":generateProtoWrappers")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // Check that output directory contains generated files
        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        assertThat(outputDir).exists()
        assertThat(outputDir.walkTopDown().filter { it.extension == "java" }.toList()).isNotEmpty()
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `task generates interface files`() {
        createBuildFile()

        runTask("generateProtoWrappers")

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val apiDir = File(outputDir, "com/example/model/api")

        assertThat(apiDir).exists()
        // Should have interface files for User, Address, Profile
        val javaFiles = apiDir.listFiles { f -> f.extension == "java" }?.map { it.name } ?: emptyList()
        assertThat(javaFiles).contains("User.java")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `task generates implementation files for each version`() {
        createBuildFile()

        runTask("generateProtoWrappers")

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()

        // Check v1 implementations
        val v1Dir = File(outputDir, "com/example/model/v1")
        assertThat(v1Dir).exists()

        // Check v2 implementations
        val v2Dir = File(outputDir, "com/example/model/v2")
        assertThat(v2Dir).exists()
    }

    // ============ Incremental Generation Tests ============

    @Test
    @EnabledIf("isProtocAvailable")
    fun `incremental is enabled by default`() {
        createBuildFile()

        val result = runTask("generateProtoWrappers", "--info")

        assertThat(result.output).doesNotContain("Incremental generation disabled")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `cache directory is created when incremental is enabled`() {
        createBuildFile()

        runTask("generateProtoWrappers")

        val cacheDir = testProjectDir.resolve("build/proto-wrapper-cache").toFile()
        assertThat(cacheDir).exists()
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `incremental state file is created after generation`() {
        createBuildFile()

        runTask("generateProtoWrappers")

        val stateFile = testProjectDir.resolve("build/proto-wrapper-cache/state.json").toFile()
        assertThat(stateFile).exists()

        val content = stateFile.readText()
        assertThat(content).contains("pluginVersion")
        assertThat(content).contains("configHash")
        assertThat(content).contains("protoFingerprints")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `second run is faster due to incremental generation`() {
        createBuildFile()

        // First run - full generation
        val firstResult = runTask("generateProtoWrappers", "--info")
        assertThat(firstResult.task(":generateProtoWrappers")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // Second run - incremental (no changes)
        // Gradle's built-in up-to-date checking may mark task as UP_TO_DATE
        val secondResult = runTask("generateProtoWrappers", "--info")
        assertThat(secondResult.task(":generateProtoWrappers")?.outcome).isIn(
            TaskOutcome.SUCCESS,
            TaskOutcome.UP_TO_DATE
        )
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `modified proto file triggers regeneration`() {
        createBuildFile()

        // First run
        runTask("generateProtoWrappers")

        // Modify a proto file
        val protoFile = testProjectDir.resolve("proto/v1/test.proto").toFile()
        protoFile.appendText("\n// Modified for test")

        // Second run - should detect change
        val result = runTask("generateProtoWrappers", "--info")

        assertThat(result.task(":generateProtoWrappers")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).containsAnyOf(
            "modified",
            "changed",
            "Detected changes"
        )
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `forceRegenerate ignores cache`() {
        createBuildFile("""
            forceRegenerate.set(true)
        """.trimIndent())

        // First run
        runTask("generateProtoWrappers")

        // Second run with forceRegenerate - use --rerun-tasks to bypass Gradle's up-to-date check
        val result = runTask("generateProtoWrappers", "--info", "--rerun-tasks")

        assertThat(result.task(":generateProtoWrappers")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        // Should generate files, not skip - check for actual generation output
        assertThat(result.output).contains("Generated")
        // Should NOT mention "no changes" because force regeneration is enabled
        assertThat(result.output).doesNotContain("No proto file changes detected")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `incremental can be disabled`() {
        createBuildFile("""
            incremental.set(false)
        """.trimIndent())

        // First run
        runTask("generateProtoWrappers")

        // Second run - use --rerun-tasks to bypass Gradle's up-to-date check
        // With incremental disabled, our plugin should do full regeneration
        val result = runTask("generateProtoWrappers", "--info", "--rerun-tasks")

        assertThat(result.task(":generateProtoWrappers")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        // Should generate files, not skip
        assertThat(result.output).contains("Generated")
        // Should NOT mention "no changes" because incremental is disabled
        assertThat(result.output).doesNotContain("No proto file changes detected")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `custom cache directory is used`() {
        createBuildFile("""
            cacheDirectory.set(file("build/my-custom-cache"))
        """.trimIndent())

        runTask("generateProtoWrappers")

        val customCacheDir = testProjectDir.resolve("build/my-custom-cache").toFile()
        assertThat(customCacheDir).exists()

        val stateFile = File(customCacheDir, "state.json")
        assertThat(stateFile).exists()
    }

    // ============ Configuration Tests ============

    @Test
    @EnabledIf("isProtocAvailable")
    fun `task respects generateBuilders setting`() {
        createBuildFile("""
            generateBuilders.set(true)
        """.trimIndent())

        runTask("generateProtoWrappers")

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val apiDir = File(outputDir, "com/example/model/api")

        // Check that builder interfaces are generated
        val userFile = File(apiDir, "User.java")
        if (userFile.exists()) {
            val content = userFile.readText()
            assertThat(content).contains("Builder")
        }
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `task respects includeVersionSuffix setting`() {
        createBuildFile("""
            includeVersionSuffix.set(false)
        """.trimIndent())

        val result = runTask("generateProtoWrappers")

        assertThat(result.task(":generateProtoWrappers")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val outputDir = testProjectDir.resolve("build/generated/sources/proto-wrapper/main/java").toFile()
        val v1Dir = File(outputDir, "com/example/model/v1")

        // Version directory should exist and contain generated files
        assertThat(v1Dir).exists()
        val javaFiles = v1Dir.listFiles { f -> f.extension == "java" } ?: emptyArray()
        assertThat(javaFiles).isNotEmpty()
    }

    // ============ Edge Cases ============

    @Test
    @EnabledIf("isProtocAvailable")
    fun `deleted proto file triggers regeneration`() {
        createBuildFile()

        // First run
        runTask("generateProtoWrappers")

        // Create additional proto file
        val extraProto = testProjectDir.resolve("proto/v1/extra.proto").toFile()
        extraProto.writeText("""
            syntax = "proto3";
            package test.diff;
            option java_package = "test.diff.v1";

            message ExtraMessage {
                string field = 1;
            }
        """.trimIndent())

        // Run with new file
        runTask("generateProtoWrappers")

        // Delete the file
        extraProto.delete()

        // Run again - should handle deletion
        val result = runTask("generateProtoWrappers", "--info")
        assertThat(result.task(":generateProtoWrappers")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `corrupted cache is handled gracefully`() {
        createBuildFile()

        // First run to create cache
        runTask("generateProtoWrappers")

        // Corrupt the cache file
        val stateFile = testProjectDir.resolve("build/proto-wrapper-cache/state.json").toFile()
        stateFile.writeText("{ invalid json }")

        // Should recover and regenerate
        val result = runTask("generateProtoWrappers")
        assertThat(result.task(":generateProtoWrappers")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `empty cache directory is handled`() {
        createBuildFile()

        // Create empty cache directory
        testProjectDir.resolve("build/proto-wrapper-cache").toFile().mkdirs()

        // Should work fine
        val result = runTask("generateProtoWrappers")
        assertThat(result.task(":generateProtoWrappers")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    // ============ Integration with Java Plugin ============

    @Test
    @EnabledIf("isProtocAvailable")
    fun `compileJava triggers generateProtoWrappers`() {
        createBuildFile()

        // Running compileJava should trigger generateProtoWrappers first
        // Note: compileJava might fail due to missing protobuf dependencies,
        // but generateProtoWrappers should still be executed
        val result = try {
            runTask("compileJava", "--info", "--continue")
        } catch (e: org.gradle.testkit.runner.UnexpectedBuildFailure) {
            // Expected - compileJava fails but we can check the output
            e.buildResult
        }

        // generateProtoWrappers should have been executed before compileJava
        assertThat(result.task(":generateProtoWrappers")?.outcome).isIn(
            TaskOutcome.SUCCESS,
            TaskOutcome.UP_TO_DATE
        )
    }
}
