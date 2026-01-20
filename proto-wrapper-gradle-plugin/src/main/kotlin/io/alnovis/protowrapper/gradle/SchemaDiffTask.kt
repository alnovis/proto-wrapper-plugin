package io.alnovis.protowrapper.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer
import io.alnovis.protowrapper.analyzer.ProtocExecutor
import io.alnovis.protowrapper.diff.SchemaDiff
import io.alnovis.protowrapper.diff.formatter.DiffFormatter
import io.alnovis.protowrapper.diff.formatter.JsonDiffFormatter
import io.alnovis.protowrapper.diff.formatter.MarkdownDiffFormatter
import io.alnovis.protowrapper.diff.formatter.TextDiffFormatter
import java.io.File

/**
 * Gradle task that compares two protobuf schema versions and generates a diff report.
 *
 * This task can be used to:
 * - Detect breaking changes between schema versions
 * - Generate migration reports
 * - Validate backward compatibility in CI/CD pipelines
 *
 * Usage:
 * ```kotlin
 * tasks.register<SchemaDiffTask>("diffSchemas") {
 *     v1Directory.set(file("proto/v1"))
 *     v2Directory.set(file("proto/v2"))
 *     outputFormat.set("markdown")
 *     outputFile.set(file("build/diff-report.md"))
 * }
 * ```
 */
abstract class SchemaDiffTask : DefaultTask() {

    /**
     * Directory containing the source (older) version proto files.
     */
    @get:InputDirectory
    abstract val v1Directory: DirectoryProperty

    /**
     * Directory containing the target (newer) version proto files.
     */
    @get:InputDirectory
    abstract val v2Directory: DirectoryProperty

    /**
     * Name/label for the source version (used in reports).
     * Defaults to "v1".
     */
    @get:Input
    @get:Optional
    abstract val v1Name: Property<String>

    /**
     * Name/label for the target version (used in reports).
     * Defaults to "v2".
     */
    @get:Input
    @get:Optional
    abstract val v2Name: Property<String>

    /**
     * Output format: text, json, or markdown.
     * Defaults to "text".
     */
    @get:Input
    @get:Optional
    abstract val outputFormat: Property<String>

    /**
     * Output file path. If not specified, output is written to console.
     */
    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    /**
     * If true, only show breaking changes in the output.
     * Defaults to false.
     */
    @get:Input
    @get:Optional
    abstract val breakingOnly: Property<Boolean>

    /**
     * If true, fail the build when breaking changes are detected.
     * Useful for CI/CD pipelines.
     * Defaults to false.
     */
    @get:Input
    @get:Optional
    abstract val failOnBreaking: Property<Boolean>

    /**
     * If true, treat warnings as errors when using failOnBreaking.
     * Defaults to false.
     */
    @get:Input
    @get:Optional
    abstract val failOnWarning: Property<Boolean>

    /**
     * Path to protoc executable. If not set, uses 'protoc' from PATH.
     */
    @get:Input
    @get:Optional
    abstract val protocPath: Property<String>

    /**
     * Directory for temporary files (descriptors).
     */
    @get:Internal
    abstract val tempDirectory: DirectoryProperty

    /**
     * Include path for proto imports. If not specified, uses v1Directory and v2Directory.
     */
    @get:InputDirectory
    @get:Optional
    abstract val includePath: DirectoryProperty

    private lateinit var protocExecutor: ProtocExecutor
    private lateinit var pluginLogger: GradleLogger

    init {
        group = "proto-wrapper"
        description = "Compares two protobuf schema versions and generates a diff report"
    }

    @TaskAction
    fun compare() {
        pluginLogger = GradleLogger(logger)
        pluginLogger.info("Proto Schema Diff Tool")

        val v1Dir = v1Directory.get().asFile
        val v2Dir = v2Directory.get().asFile

        pluginLogger.info("Comparing: $v1Dir -> $v2Dir")

        initializeProtoc()
        createTempDirectory()

        try {
            val v1Schema = analyzeVersion(v1Dir, getV1Name())
            val v2Schema = analyzeVersion(v2Dir, getV2Name())

            pluginLogger.info("Comparing schemas...")
            val diff = SchemaDiff.compare(v1Schema, v2Schema)

            val output = formatOutput(diff)

            if (outputFile.isPresent) {
                writeToFile(output)
            } else {
                pluginLogger.info("\n$output")
            }

            logSummary(diff)
            checkBreakingChanges(diff)

        } catch (e: Exception) {
            throw GradleException("Failed to compare schemas: ${e.message}", e)
        }
    }

    private fun getV1Name(): String = v1Name.getOrElse("v1")
    private fun getV2Name(): String = v2Name.getOrElse("v2")
    private fun getOutputFormat(): String = outputFormat.getOrElse("text")
    private fun isBreakingOnly(): Boolean = breakingOnly.getOrElse(false)
    private fun isFailOnBreaking(): Boolean = failOnBreaking.getOrElse(false)
    private fun isFailOnWarning(): Boolean = failOnWarning.getOrElse(false)

    private fun initializeProtoc() {
        protocExecutor = ProtocExecutor(pluginLogger)
        protocPath.orNull?.let { path ->
            if (path.isNotEmpty()) {
                protocExecutor.setProtocPath(path)
            }
        }

        if (!protocExecutor.isProtocAvailable) {
            throw GradleException(
                "protoc not found. Please install protobuf compiler or set protocPath parameter."
            )
        }
        pluginLogger.info("Using ${protocExecutor.protocVersion}")
    }

    private fun createTempDirectory() {
        val tempDir = if (tempDirectory.isPresent) {
            tempDirectory.get().asFile
        } else {
            File(project.layout.buildDirectory.get().asFile, "proto-wrapper-diff-tmp")
        }

        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw GradleException("Failed to create temp directory: $tempDir")
        }
    }

    private fun analyzeVersion(protoDir: File, versionName: String): ProtoAnalyzer.VersionSchema {
        pluginLogger.info("Analyzing $versionName from ${protoDir.name}...")

        val tempDir = if (tempDirectory.isPresent) {
            tempDirectory.get().asFile
        } else {
            File(project.layout.buildDirectory.get().asFile, "proto-wrapper-diff-tmp")
        }

        val descriptorFile = File(tempDir, "$versionName-descriptor.pb").toPath()
        val includeDir = if (includePath.isPresent) {
            includePath.get().asFile.toPath()
        } else {
            protoDir.toPath()
        }

        protocExecutor.generateDescriptor(
            protoDir.toPath(),
            descriptorFile,
            emptyArray(),
            includeDir
        )

        val analyzer = ProtoAnalyzer()
        val schema = analyzer.analyze(descriptorFile, versionName)

        pluginLogger.info("  Found ${schema.messages.size} messages, ${schema.enums.size} enums")

        return schema
    }

    private fun formatOutput(diff: SchemaDiff): String {
        val formatter = createFormatter()
        return if (isBreakingOnly()) {
            formatter.formatBreakingOnly(diff)
        } else {
            formatter.format(diff)
        }
    }

    private fun createFormatter(): DiffFormatter {
        return when (getOutputFormat().lowercase()) {
            "json" -> JsonDiffFormatter()
            "markdown", "md" -> MarkdownDiffFormatter()
            else -> TextDiffFormatter()
        }
    }

    private fun writeToFile(output: String) {
        val file = outputFile.get().asFile
        file.parentFile?.let { parent ->
            if (!parent.exists() && !parent.mkdirs()) {
                throw GradleException("Failed to create output directory: $parent")
            }
        }
        file.writeText(output)
        pluginLogger.info("Report written to: ${file.absolutePath}")
    }

    private fun logSummary(diff: SchemaDiff) {
        val summary = diff.summary

        pluginLogger.info("")
        pluginLogger.info("=== Summary ===")
        pluginLogger.info("Messages: +${summary.addedMessages()} / -${summary.removedMessages()} / ~${summary.modifiedMessages()}")
        pluginLogger.info("Enums: +${summary.addedEnums()} / -${summary.removedEnums()} / ~${summary.modifiedEnums()}")

        if (diff.hasBreakingChanges()) {
            pluginLogger.warn("Breaking changes: ${summary.errorCount()} errors, ${summary.warningCount()} warnings")
        } else {
            pluginLogger.info("No breaking changes detected")
        }
    }

    private fun checkBreakingChanges(diff: SchemaDiff) {
        if (!isFailOnBreaking()) {
            return
        }

        val errors = diff.errors
        val warnings = diff.warnings

        var failureCount = errors.size
        if (isFailOnWarning()) {
            failureCount += warnings.size
        }

        if (failureCount > 0) {
            val message = buildString {
                append("Breaking changes detected!\n\n")

                if (errors.isNotEmpty()) {
                    append("ERRORS (${errors.size}):\n")
                    for (bc in errors) {
                        append("  - ${bc.entityPath()}: ${bc.description()}\n")
                    }
                }

                if (isFailOnWarning() && warnings.isNotEmpty()) {
                    append("\nWARNINGS (${warnings.size}):\n")
                    for (bc in warnings) {
                        append("  - ${bc.entityPath()}: ${bc.description()}\n")
                    }
                }
            }

            throw GradleException(message)
        }
    }
}
