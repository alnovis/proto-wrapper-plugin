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
 * Integration tests for Schema Diff functionality.
 *
 * These tests create real Gradle projects and run actual Gradle builds.
 * The plugin must be published to mavenLocal before running these tests:
 *   ./gradlew publishToMavenLocal
 */
class DiffIntegrationTest {

    @TempDir
    lateinit var testProjectDir: Path

    private val pluginVersion: String = System.getProperty("pluginVersion", "1.5.0")
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
            // Check common locations for gradle
            val candidates = listOf(
                "/usr/bin/gradle",
                "/usr/local/bin/gradle",
                "${System.getProperty("user.home")}/.sdkman/candidates/gradle/current/bin/gradle",
                "gradle" // fallback to PATH
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
        // Create settings.gradle.kts
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

    private fun copyScenarioProtos(scenarioName: String): Pair<File, File> {
        val v1Dir = testProjectDir.resolve("proto/v1").toFile()
        val v2Dir = testProjectDir.resolve("proto/v2").toFile()
        v1Dir.mkdirs()
        v2Dir.mkdirs()

        val scenarioDir = File(testProtosDir, "diff/$scenarioName")
        File(scenarioDir, "v1").listFiles()?.forEach { file ->
            file.copyTo(File(v1Dir, file.name), overwrite = true)
        }
        File(scenarioDir, "v2").listFiles()?.forEach { file ->
            file.copyTo(File(v2Dir, file.name), overwrite = true)
        }

        return v1Dir to v2Dir
    }

    private fun createBuildFile(taskConfig: String) {
        val buildFile = testProjectDir.resolve("build.gradle.kts").toFile()
        buildFile.writeText("""
            plugins {
                id("io.alnovis.proto-wrapper") version "$pluginVersion"
            }

            tasks.register<io.alnovis.protowrapper.gradle.SchemaDiffTask>("diffSchemas") {
                $taskConfig
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
    fun `diff task detects simple additions`() {
        copyScenarioProtos("01-simple-add")

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("text")
            outputFile.set(file("build/diff-report.txt"))
        """.trimIndent())

        val result = runGradle("diffSchemas")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val reportFile = testProjectDir.resolve("build/diff-report.txt").toFile()
        assertThat(reportFile).exists()

        val content = reportFile.readText()
        assertThat(content).contains("Schema Comparison: v1 -> v2")
        assertThat(content).contains("ADDED")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task detects simple removals`() {
        copyScenarioProtos("02-simple-remove")

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("text")
            outputFile.set(file("build/diff-report.txt"))
        """.trimIndent())

        val result = runGradle("diffSchemas")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val reportFile = testProjectDir.resolve("build/diff-report.txt").toFile()
        assertThat(reportFile).exists()

        val content = reportFile.readText()
        assertThat(content).contains("Schema Comparison: v1 -> v2")
        assertThat(content).contains("REMOVED")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task detects field changes`() {
        copyScenarioProtos("03-field-changes")

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("text")
            outputFile.set(file("build/diff-report.txt"))
        """.trimIndent())

        val result = runGradle("diffSchemas")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val reportFile = testProjectDir.resolve("build/diff-report.txt").toFile()
        assertThat(reportFile).exists()

        val content = reportFile.readText()
        assertThat(content).contains("Schema Comparison: v1 -> v2")
        assertThat(content).contains("MODIFIED")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task detects enum changes`() {
        copyScenarioProtos("04-enum-changes")

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("text")
            outputFile.set(file("build/diff-report.txt"))
        """.trimIndent())

        val result = runGradle("diffSchemas")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val reportFile = testProjectDir.resolve("build/diff-report.txt").toFile()
        assertThat(reportFile).exists()
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task detects complex changes`() {
        copyScenarioProtos("05-complex-changes")

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("text")
            outputFile.set(file("build/diff-report.txt"))
        """.trimIndent())

        val result = runGradle("diffSchemas")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val reportFile = testProjectDir.resolve("build/diff-report.txt").toFile()
        assertThat(reportFile).exists()

        val content = reportFile.readText()
        assertThat(content).contains("Schema Comparison: v1 -> v2")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task reports no changes for identical schemas`() {
        copyScenarioProtos("06-no-changes")

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("text")
            outputFile.set(file("build/diff-report.txt"))
        """.trimIndent())

        val result = runGradle("diffSchemas")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val reportFile = testProjectDir.resolve("build/diff-report.txt").toFile()
        assertThat(reportFile).exists()

        val content = reportFile.readText()
        // Check that summary shows zero changes
        assertThat(content).contains("Messages:  +0 added, ~0 modified, -0 removed")
        assertThat(content).contains("Enums:     +0 added, ~0 modified, -0 removed")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task generates JSON output`() {
        copyScenarioProtos("03-field-changes")

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("json")
            outputFile.set(file("build/diff-report.json"))
        """.trimIndent())

        val result = runGradle("diffSchemas")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val reportFile = testProjectDir.resolve("build/diff-report.json").toFile()
        assertThat(reportFile).exists()

        val content = reportFile.readText()
        assertThat(content).startsWith("{")
        assertThat(content).contains("\"v1\": \"v1\"")
        assertThat(content).contains("\"v2\": \"v2\"")
        assertThat(content).contains("\"summary\"")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task generates Markdown output`() {
        copyScenarioProtos("03-field-changes")

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("markdown")
            outputFile.set(file("build/diff-report.md"))
        """.trimIndent())

        val result = runGradle("diffSchemas")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val reportFile = testProjectDir.resolve("build/diff-report.md").toFile()
        assertThat(reportFile).exists()

        val content = reportFile.readText()
        assertThat(content).contains("# Schema Comparison: v1 -> v2")
        assertThat(content).contains("## Summary")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task fails on breaking changes when failOnBreaking is true`() {
        copyScenarioProtos("03-field-changes")

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("text")
            outputFile.set(file("build/diff-report.txt"))
            failOnBreaking.set(true)
        """.trimIndent())

        val result = runGradle("diffSchemas")

        assertThat(result.isFailed).withFailMessage { "Expected build to fail:\n${result.output}" }.isTrue()
        assertThat(result.output).contains("Breaking changes detected")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task with breakingOnly shows only breaking changes`() {
        copyScenarioProtos("03-field-changes")

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("text")
            outputFile.set(file("build/diff-report.txt"))
            breakingOnly.set(true)
        """.trimIndent())

        val result = runGradle("diffSchemas")

        assertThat(result.isSuccess).withFailMessage { "Build failed:\n${result.output}" }.isTrue()

        val reportFile = testProjectDir.resolve("build/diff-report.txt").toFile()
        assertThat(reportFile).exists()

        val content = reportFile.readText()
        assertThat(content).contains("Breaking Changes")
    }
}
