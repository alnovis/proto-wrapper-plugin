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
 * Functional tests for SchemaDiffTask using Gradle TestKit.
 *
 * These tests require protoc to be available on the system PATH.
 * Tagged as "slow" because each test starts a full Gradle process (~7 sec/test).
 * Run with: ./gradlew :proto-wrapper-gradle-plugin:slowTest
 */
@Tag("slow")
class SchemaDiffTaskTest {

    @TempDir
    lateinit var testProjectDir: Path

    private lateinit var buildFile: File
    private lateinit var settingsFile: File
    private lateinit var v1Dir: File
    private lateinit var v2Dir: File

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

        // Create proto directories
        v1Dir = testProjectDir.resolve("proto/v1").toFile()
        v2Dir = testProjectDir.resolve("proto/v2").toFile()
        v1Dir.mkdirs()
        v2Dir.mkdirs()

        // Copy test protos
        copyTestProto("/proto/v1/test.proto", v1Dir)
        copyTestProto("/proto/v2/test.proto", v2Dir)
    }

    private fun copyTestProto(resourcePath: String, targetDir: File) {
        val content = javaClass.getResourceAsStream(resourcePath)?.bufferedReader()?.readText()
            ?: throw IllegalStateException("Resource not found: $resourcePath")
        File(targetDir, "test.proto").writeText(content)
    }

    private fun createBuildFile(taskConfig: String) {
        buildFile.writeText("""
            plugins {
                id("io.github.alnovis.proto-wrapper")
            }

            tasks.register<space.alnovis.protowrapper.gradle.SchemaDiffTask>("diffSchemas") {
                $taskConfig
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

    private fun runTaskAndFail(vararg args: String): org.gradle.testkit.runner.BuildResult {
        return GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments(*args, "--stacktrace")
            .withPluginClasspath()
            .buildAndFail()
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task compares schemas and generates text output`() {
        val outputFile = testProjectDir.resolve("build/diff-report.txt").toFile()

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("text")
            outputFile.set(file("build/diff-report.txt"))
        """.trimIndent())

        val result = runTask("diffSchemas")

        assertThat(result.task(":diffSchemas")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(outputFile).exists()

        val content = outputFile.readText()
        assertThat(content).contains("Schema Comparison: v1 -> v2")
        assertThat(content).contains("Profile")  // Added message
        assertThat(content).contains("DeprecatedMessage")  // Removed message
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task generates JSON output`() {
        val outputFile = testProjectDir.resolve("build/diff-report.json").toFile()

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("json")
            outputFile.set(file("build/diff-report.json"))
        """.trimIndent())

        val result = runTask("diffSchemas")

        assertThat(result.task(":diffSchemas")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(outputFile).exists()

        val content = outputFile.readText()
        assertThat(content).startsWith("{")
        assertThat(content).contains("\"v1\": \"v1\"")
        assertThat(content).contains("\"v2\": \"v2\"")
        assertThat(content).contains("\"summary\"")
        assertThat(content).contains("\"breakingChanges\"")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task generates Markdown output`() {
        val outputFile = testProjectDir.resolve("build/diff-report.md").toFile()

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("markdown")
            outputFile.set(file("build/diff-report.md"))
        """.trimIndent())

        val result = runTask("diffSchemas")

        assertThat(result.task(":diffSchemas")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(outputFile).exists()

        val content = outputFile.readText()
        assertThat(content).contains("# Schema Comparison: v1 -> v2")
        assertThat(content).contains("## Summary")
        assertThat(content).contains("| Category | Added | Modified | Removed |")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task fails on breaking changes when configured`() {
        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("text")
            failOnBreaking.set(true)
        """.trimIndent())

        val result = runTaskAndFail("diffSchemas")

        assertThat(result.task(":diffSchemas")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("Breaking changes detected")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task generates breaking-only output`() {
        val outputFile = testProjectDir.resolve("build/breaking-only.txt").toFile()

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("text")
            outputFile.set(file("build/breaking-only.txt"))
            breakingOnly.set(true)
        """.trimIndent())

        val result = runTask("diffSchemas")

        assertThat(result.task(":diffSchemas")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(outputFile).exists()

        val content = outputFile.readText()
        assertThat(content).contains("Breaking Changes")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task succeeds when no breaking changes and failOnBreaking is true`() {
        // Create identical proto files (no breaking changes)
        val v2Proto = File(v2Dir, "test.proto")
        val v1Proto = File(v1Dir, "test.proto")
        v2Proto.writeText(v1Proto.readText())

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("v1")
            v2Name.set("v2")
            outputFormat.set("text")
            failOnBreaking.set(true)
        """.trimIndent())

        val result = runTask("diffSchemas")

        assertThat(result.task(":diffSchemas")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("No breaking changes detected")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task uses custom version names`() {
        val outputFile = testProjectDir.resolve("build/diff-report.txt").toFile()

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            v1Name.set("production")
            v2Name.set("development")
            outputFormat.set("text")
            outputFile.set(file("build/diff-report.txt"))
        """.trimIndent())

        val result = runTask("diffSchemas")

        assertThat(result.task(":diffSchemas")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(outputFile).exists()

        val content = outputFile.readText()
        assertThat(content).contains("Schema Comparison: production -> development")
    }

    @Test
    @EnabledIf("isProtocAvailable")
    fun `diff task detects all expected changes`() {
        val outputFile = testProjectDir.resolve("build/diff-report.txt").toFile()

        createBuildFile("""
            v1Directory.set(file("proto/v1"))
            v2Directory.set(file("proto/v2"))
            outputFormat.set("text")
            outputFile.set(file("build/diff-report.txt"))
        """.trimIndent())

        val result = runTask("diffSchemas")

        assertThat(result.task(":diffSchemas")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val content = outputFile.readText()

        // Check for expected changes
        assertThat(content).contains("ADDED: Profile")  // New message
        assertThat(content).contains("REMOVED: DeprecatedMessage")  // Removed message

        // Check for field changes
        assertThat(content).contains("email")  // Removed field
        assertThat(content).contains("phone")  // Added field

        // Check for enum changes
        assertThat(content).contains("USER_STATUS_DELETED")  // Removed enum value
        assertThat(content).contains("USER_STATUS_SUSPENDED")  // Added enum value
    }
}
