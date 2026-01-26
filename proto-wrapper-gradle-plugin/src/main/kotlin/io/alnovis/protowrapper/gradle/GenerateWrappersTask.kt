package io.alnovis.protowrapper.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer
import io.alnovis.protowrapper.analyzer.ProtocExecutor
import io.alnovis.protowrapper.generator.GenerationOrchestrator
import io.alnovis.protowrapper.generator.GeneratorConfig
import io.alnovis.protowrapper.merger.VersionMerger
import io.alnovis.protowrapper.merger.VersionMerger.MergerConfig
import io.alnovis.protowrapper.model.FieldMapping
import io.alnovis.protowrapper.model.MergedField
import io.alnovis.protowrapper.model.MergedMessage
import io.alnovis.protowrapper.model.MergedSchema
import io.alnovis.protowrapper.model.ProtoSyntax
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.extension

/**
 * Gradle task that generates version-agnostic wrapper classes from protobuf schemas.
 *
 * <p>This task processes multiple versions of protobuf schemas, merges them into a unified
 * model, and generates Java wrapper classes that provide version-agnostic access to
 * protobuf-generated classes.</p>
 *
 * <p>The task performs the following steps:</p>
 * <ol>
 *   <li>Initializes package configuration from extension settings</li>
 *   <li>Validates protoc availability and configuration</li>
 *   <li>Generates protobuf descriptors for each version</li>
 *   <li>Analyzes descriptors and extracts schema information</li>
 *   <li>Merges all version schemas into a unified model</li>
 *   <li>Generates Java wrapper classes using the merged model</li>
 * </ol>
 *
 * @see ProtoWrapperExtension
 * @see VersionConfig
 */
abstract class GenerateWrappersTask : DefaultTask() {

    // ============ Input Properties ============

    /**
     * Root directory containing all proto version directories.
     * Used as the base for imports and for resolving relative protoDir paths.
     */
    @get:InputDirectory
    abstract val protoRoot: DirectoryProperty

    /**
     * Output directory for generated Java source files.
     * The directory structure will mirror the package structure.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /**
     * Directory for temporary files such as protobuf descriptors.
     * Not tracked for up-to-date checking.
     */
    @get:Internal
    abstract val tempDirectory: DirectoryProperty

    /**
     * Base package for all generated classes.
     * If set, derives other packages automatically:
     * - apiPackage = basePackage + ".api"
     * - implPackagePattern = basePackage + ".{version}"
     */
    @get:Input
    @get:Optional
    abstract val basePackage: Property<String>

    /**
     * Package for version-agnostic API interfaces.
     * If not set and basePackage is set, derived as: basePackage + ".api"
     */
    @get:Input
    @get:Optional
    abstract val apiPackage: Property<String>

    /**
     * Pattern for implementation packages. Use {version} as placeholder.
     * If not set and basePackage is set, derived as: basePackage + ".{version}"
     */
    @get:Input
    @get:Optional
    abstract val implPackagePattern: Property<String>

    /**
     * Pattern for proto packages. Use {version} as placeholder.
     * This should match the java_package option in your proto files.
     * Example: "com.example.proto.{version}"
     */
    @get:Input
    abstract val protoPackagePattern: Property<String>

    /**
     * Path to protoc executable.
     * If not set, protoc is resolved automatically:
     * - System PATH (if protoc is installed)
     * - Embedded (downloaded from Maven Central)
     */
    @get:Input
    @get:Optional
    abstract val protocPath: Property<String>

    /**
     * Version of protoc to use for embedded downloads.
     * Only affects embedded protoc. Ignored if custom protocPath is set
     * or system protoc is found.
     * If not specified, uses the protobuf version that the plugin was built with.
     * @since 1.6.5
     */
    @get:Input
    @get:Optional
    abstract val protocVersion: Property<String>

    /**
     * Whether to generate interface files.
     * Default: true
     */
    @get:Input
    abstract val generateInterfaces: Property<Boolean>

    /**
     * Whether to generate abstract class files.
     * Default: true
     */
    @get:Input
    abstract val generateAbstractClasses: Property<Boolean>

    /**
     * Whether to generate version-specific implementation files.
     * Default: true
     */
    @get:Input
    abstract val generateImplClasses: Property<Boolean>

    /**
     * Whether to generate VersionContext utility class.
     * Default: true
     */
    @get:Input
    abstract val generateVersionContext: Property<Boolean>

    /**
     * Whether to generate ProtocolVersions utility class.
     * ProtocolVersions provides compile-time constants for all supported
     * protocol versions and utility methods for version validation.
     * Default: true
     * @since 2.1.0
     */
    @get:Input
    abstract val generateProtocolVersions: Property<Boolean>

    /**
     * Whether to include version suffix in implementation class names.
     * If true: MoneyV1, DateTimeV2
     * If false: Money, DateTime (version is determined by package only)
     * Default: true
     */
    @get:Input
    abstract val includeVersionSuffix: Property<Boolean>

    /**
     * Whether to generate Builder interfaces and implementations.
     * If true: generates toBuilder() method and Builder nested interface
     * If false: generates read-only wrappers only
     * Default: false
     */
    @get:Input
    abstract val generateBuilders: Property<Boolean>

    /**
     * Major version of protobuf library used in the project.
     * Set to 2 for protobuf 2.x (uses valueOf() for enum conversion).
     * Set to 3 for protobuf 3.x (uses forNumber() for enum conversion).
     * Default: 3
     *
     * @since 2.2.0 Deprecated. Use per-version `protoSyntax` configuration instead.
     *              The plugin now auto-detects proto syntax from .proto files and
     *              uses the appropriate enum conversion method per version.
     *              This global setting is only used as a fallback when syntax cannot be detected.
     *              Scheduled for removal in 3.0.0.
     */
    @Deprecated(
        message = "Use per-version protoSyntax configuration instead. Scheduled for removal in 3.0.0.",
        level = DeprecationLevel.WARNING
    )
    @get:Input
    // TODO: Remove in 3.0.0 - see docs/DEPRECATION_POLICY.md
    abstract val protobufMajorVersion: Property<Int>

    /**
     * Whether to convert Google Well-Known Types to idiomatic Java types.
     * If true: google.protobuf.Timestamp -> java.time.Instant, etc.
     * If false: keeps raw proto types
     * Default: true
     * @since 1.3.0
     */
    @get:Input
    abstract val convertWellKnownTypes: Property<Boolean>

    /**
     * Whether to generate raw proto accessor methods alongside converted types.
     * If true: generates getXxxProto() methods returning original proto types
     * Only relevant when convertWellKnownTypes is true.
     * Default: false
     * @since 1.3.0
     */
    @get:Input
    abstract val generateRawProtoAccessors: Property<Boolean>

    /**
     * Messages to include in generation.
     * Empty list means include all messages.
     */
    @get:Input
    @get:Optional
    abstract val includeMessages: ListProperty<String>

    /**
     * Messages to exclude from generation.
     */
    @get:Input
    @get:Optional
    abstract val excludeMessages: ListProperty<String>

    /**
     * List of version configurations.
     * Each version specifies a proto directory and optional settings.
     */
    @get:Nested
    abstract val versions: ListProperty<VersionConfig>

    // ============ Incremental Generation Properties (since 1.6.0) ============

    /**
     * Enable incremental generation.
     * When enabled, only regenerates when proto files change.
     * Default: true
     */
    @get:Input
    abstract val incremental: Property<Boolean>

    /**
     * Directory for incremental generation cache.
     * Stores state between builds to detect changes.
     */
    @get:OutputDirectory
    abstract val cacheDirectory: DirectoryProperty

    /**
     * Force full regeneration, ignoring cache.
     * Default: false
     */
    @get:Input
    abstract val forceRegenerate: Property<Boolean>

    /**
     * Target Java version for generated code.
     * Use 8 for Java 8 compatible code (avoids private interface methods, List.of()).
     * Default: 9 (uses modern Java features like private interface methods).
     * @since 1.6.8
     */
    @get:Input
    abstract val targetJavaVersion: Property<Int>

    /**
     * Enable parallel generation for improved build performance.
     * When enabled, wrapper classes are generated in parallel using multiple threads.
     * Default: false
     * @since 2.1.0
     */
    @get:Input
    abstract val parallelGeneration: Property<Boolean>

    /**
     * Number of threads for parallel generation.
     * Only used when parallelGeneration is enabled.
     * Default: 0 (auto = number of available processors)
     * @since 2.1.0
     */
    @get:Input
    abstract val generationThreads: Property<Int>

    /**
     * The default version ID for VersionContext.DEFAULT_VERSION and ProtocolVersions.DEFAULT.
     * If not set, the last version in the versions list is used as default.
     *
     * This is useful when versions are listed chronologically (oldest to newest)
     * but you need a specific version (e.g., the stable one) as the default.
     * @since 2.1.1
     */
    @get:Input
    @get:Optional
    abstract val defaultVersion: Property<String>

    /**
     * Field mappings for overriding number-based field matching.
     * Use when the same field has different field numbers across proto versions.
     * @since 2.2.0
     */
    @get:Input
    @get:Optional
    abstract val fieldMappings: ListProperty<FieldMapping>

    /**
     * Whether to generate Bean Validation (JSR-380) annotations on interface getters.
     * When enabled, annotations like @NotNull, @Valid, @Min, @Max are added based on
     * proto field metadata.
     * Default: false
     * @since 2.3.0
     */
    @get:Input
    abstract val generateValidationAnnotations: Property<Boolean>

    /**
     * Validation annotation namespace style.
     * Use "jakarta" (default) for Jakarta EE 9+ (jakarta.validation.constraints)
     * or "javax" for Java EE 8 and earlier (javax.validation.constraints).
     * Note: When targetJavaVersion=8, automatically uses "javax" for compatibility.
     * Default: "jakarta"
     * @since 2.3.0
     */
    @get:Input
    abstract val validationAnnotationStyle: Property<String>

    // ============ Internal State ============

    private lateinit var protocExecutor: ProtocExecutor
    private lateinit var pluginLogger: GradleLogger

    private var effectiveApiPackage: String = ""
    private var effectiveImplPackagePattern: String = ""

    /**
     * Cache for computed version data (resolved proto directory, detected package, descriptor file).
     */
    private val versionDataCache = mutableMapOf<String, VersionData>()

    /**
     * Internal holder for computed version-specific data.
     *
     * @property resolvedProtoDir Absolute path to the proto directory
     * @property descriptorFile Path to the generated descriptor file
     * @property detectedProtoPackage Auto-detected or computed proto package name
     * @property resolvedSyntax Resolved proto syntax (never AUTO after resolution)
     */
    private data class VersionData(
        val resolvedProtoDir: File,
        val descriptorFile: File,
        var detectedProtoPackage: String? = null,
        var resolvedSyntax: ProtoSyntax = ProtoSyntax.PROTO2
    )

    // ============ Task Action ============

    /**
     * Main entry point for the task execution.
     *
     * <p>Orchestrates the entire code generation process:</p>
     * <ol>
     *   <li>Initialize logger and package configuration</li>
     *   <li>Initialize and validate protoc</li>
     *   <li>Validate version configurations</li>
     *   <li>Create output directories</li>
     *   <li>Process each version schema</li>
     *   <li>Merge all schemas</li>
     *   <li>Generate wrapper code</li>
     * </ol>
     *
     * @throws GradleException if any step fails
     */
    @TaskAction
    fun generate() {
        pluginLogger = GradleLogger(logger)
        pluginLogger.info("Proto Wrapper Generator starting...")

        initializePackages()
        initializeProtoc()
        resolveAndValidateConfig()
        createDirectories()

        try {
            val schemas = mutableListOf<ProtoAnalyzer.VersionSchema>()

            for (versionConfig in versions.get()) {
                val schema = processVersion(versionConfig)
                schemas.add(schema)
            }

            pluginLogger.info("Merging ${schemas.size} schemas...")
            val mergerConfig = MergerConfig()
            val mappings = fieldMappings.orNull
            if (!mappings.isNullOrEmpty()) {
                mergerConfig.setFieldMappings(mappings)
                pluginLogger.info("Using ${mappings.size} field mapping(s)")
            }
            val merger = VersionMerger(mergerConfig, pluginLogger)
            val mergedSchema = merger.merge(schemas)

            pluginLogger.info("Merged schema: ${mergedSchema.messages.size} messages, ${mergedSchema.enums.size} enums")
            logConflictStatistics(mergedSchema)

            val generatorConfig = buildGeneratorConfig()
            val orchestrator = GenerationOrchestrator(generatorConfig, pluginLogger)

            val versionConfigs = versions.get().map { GradleVersionConfigAdapter(it) }
            val protoFiles = collectAllProtoFiles()
            val protoRootPath = protoRoot.get().asFile.toPath()

            val generatedFiles = orchestrator.generateAllIncremental(
                mergedSchema,
                versionConfigs,
                { message, versionCfg ->
                    getProtoClassName(message, versionCfg as GradleVersionConfigAdapter)
                },
                protoFiles,
                protoRootPath
            )

            pluginLogger.info("Generated $generatedFiles files in ${outputDirectory.get().asFile}")

        } catch (e: Exception) {
            throw GradleException("Failed to generate code: ${e.message}", e)
        }
    }

    // ============ Private Methods ============

    /**
     * Initializes package settings from basePackage if not explicitly set.
     *
     * <p>Derives apiPackage and implPackagePattern from basePackage when not explicitly
     * configured. Falls back to default values if neither basePackage nor individual
     * settings are provided.</p>
     */
    private fun initializePackages() {
        val defaultApiPackage = "com.example.model.api"
        val defaultImplPattern = "com.example.model.{version}"

        val base = basePackage.orNull
        if (!base.isNullOrEmpty()) {
            effectiveApiPackage = apiPackage.orNull ?: "$base.api"
            effectiveImplPackagePattern = implPackagePattern.orNull ?: "$base.{version}"
            pluginLogger.info("Using basePackage: $base")
        } else {
            effectiveApiPackage = apiPackage.orNull ?: defaultApiPackage
            effectiveImplPackagePattern = implPackagePattern.orNull ?: defaultImplPattern
        }

        pluginLogger.info("Package configuration:")
        pluginLogger.info("  apiPackage: $effectiveApiPackage")
        pluginLogger.info("  implPackagePattern: $effectiveImplPackagePattern")
        pluginLogger.info("  protoPackagePattern: ${protoPackagePattern.get()}")
    }

    /**
     * Initializes the protoc executor and validates availability.
     *
     * <p>Sets up the ProtocExecutor with the configured path and version (if any) and
     * verifies that protoc is available (will auto-download embedded if needed).</p>
     *
     * @throws GradleException if protoc is not available
     */
    private fun initializeProtoc() {
        protocExecutor = ProtocExecutor(pluginLogger)
        protocPath.orNull?.let { path ->
            if (path.isNotEmpty()) {
                protocExecutor.setProtocPath(path)
            }
        }
        protocVersion.orNull?.let { version ->
            if (version.isNotEmpty()) {
                protocExecutor.setProtocVersion(version)
            }
        }

        if (!protocExecutor.isProtocAvailable) {
            throw GradleException(
                "protoc not available. Failed to resolve or download protoc."
            )
        }
        pluginLogger.info("Using ${protocExecutor.queryInstalledProtocVersion()}")
    }

    /**
     * Validates and resolves version configurations.
     *
     * <p>For each version configuration:</p>
     * <ul>
     *   <li>Validates that protoDir is set</li>
     *   <li>Resolves relative paths against protoRoot</li>
     *   <li>Verifies that the proto directory exists</li>
     *   <li>Caches resolved paths and descriptor file locations</li>
     * </ul>
     *
     * @throws GradleException if validation fails
     */
    private fun resolveAndValidateConfig() {
        val versionList = versions.get()
        if (versionList.isEmpty()) {
            throw GradleException("At least one version configuration is required")
        }

        val protoRootDir = protoRoot.get().asFile
        if (!protoRootDir.isDirectory) {
            throw GradleException("protoRoot must be an existing directory: $protoRootDir")
        }

        pluginLogger.info("Proto root: ${protoRootDir.absolutePath}")

        val tempDir = tempDirectory.get().asFile

        for (config in versionList) {
            val protoDirPath = config.protoDir.get()
            if (protoDirPath.isEmpty()) {
                throw GradleException("protoDir is required for each version")
            }

            var protoDir = File(protoDirPath)
            if (!protoDir.isAbsolute) {
                protoDir = File(protoRootDir, protoDirPath)
            }

            if (!protoDir.isDirectory) {
                throw GradleException(
                    "protoDir does not exist: ${protoDir.absolutePath} (resolved from: $protoDirPath)"
                )
            }

            val versionId = config.getVersionId()
            val descriptorFile = File(tempDir, "$versionId-descriptor.pb")

            // Validate protoSyntax if specified
            val syntaxStr = config.protoSyntax.orNull
            if (!syntaxStr.isNullOrEmpty()) {
                val normalized = syntaxStr.lowercase().trim()
                if (normalized != "proto2" && normalized != "proto3" && normalized != "auto") {
                    throw GradleException(
                        "Invalid protoSyntax '$syntaxStr' for version ${config.getEffectiveName()}. " +
                        "Valid values: proto2, proto3, auto"
                    )
                }
            }

            versionDataCache[config.name] = VersionData(protoDir, descriptorFile)

            pluginLogger.info("Version ${config.getEffectiveName()}:")
            pluginLogger.info("  protoDir: ${protoDir.absolutePath}")
            if (!syntaxStr.isNullOrEmpty()) {
                pluginLogger.info("  protoSyntax: $syntaxStr")
            }
        }

        // Validate defaultVersion if specified
        val defVersion = defaultVersion.orNull
        if (!defVersion.isNullOrEmpty()) {
            val validVersionIds = versionList.map { it.getVersionId() }.toSet()
            if (defVersion !in validVersionIds) {
                throw GradleException(
                    "defaultVersion '$defVersion' is not in the configured versions list. " +
                    "Valid versions: $validVersionIds"
                )
            }
            pluginLogger.info("Default version: $defVersion")
        }
    }

    /**
     * Creates output and temporary directories if they don't exist.
     *
     * @throws GradleException if directory creation fails
     */
    private fun createDirectories() {
        val outputDir = outputDirectory.get().asFile
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw GradleException("Failed to create output directory: $outputDir")
        }
        val tempDir = tempDirectory.get().asFile
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw GradleException("Failed to create temp directory: $tempDir")
        }
    }

    /**
     * Processes a single version configuration.
     *
     * <p>This method performs the following steps for a version:</p>
     * <ol>
     *   <li>Generates a protobuf descriptor file using protoc</li>
     *   <li>Analyzes the descriptor to extract schema information</li>
     *   <li>Builds proto class name mappings</li>
     * </ol>
     *
     * @param versionConfig The version configuration to process
     * @return The analyzed version schema
     * @throws GradleException if processing fails
     */
    private fun processVersion(versionConfig: VersionConfig): ProtoAnalyzer.VersionSchema {
        val version = versionConfig.getVersionId()
        val versionName = versionConfig.getEffectiveName()
        pluginLogger.info("Processing $versionName...")

        val versionData = versionDataCache[versionConfig.name]
            ?: throw GradleException("Version data not found for ${versionConfig.name}")

        val protoDir = versionData.resolvedProtoDir
        val descriptorFile = versionData.descriptorFile.toPath()

        pluginLogger.info("  Generating descriptor from $protoDir")

        val excludePatterns = versionConfig.excludeProtos.orNull?.toTypedArray() ?: emptyArray()
        if (excludePatterns.isNotEmpty()) {
            pluginLogger.info("  Excluding patterns: ${excludePatterns.joinToString(", ")}")
        }

        protocExecutor.generateDescriptor(
            protoDir.toPath(),
            descriptorFile,
            excludePatterns,
            protoRoot.get().asFile.toPath()
        )

        val analyzer = ProtoAnalyzer()
        val sourcePrefix = "${versionConfig.protoDir.get()}/"
        val schema = analyzer.analyze(descriptorFile, version, sourcePrefix)
        pluginLogger.info("  ${schema.stats}")

        // Resolve proto syntax for this version
        val configuredSyntax = parseConfiguredSyntax(versionConfig.protoSyntax.orNull)
        val resolvedSyntax = if (configuredSyntax.isAuto) {
            schema.detectedSyntax
        } else {
            configuredSyntax
        }
        versionData.resolvedSyntax = resolvedSyntax
        pluginLogger.info("  Proto syntax: ${resolvedSyntax.name} (configured: ${configuredSyntax.name})")

        val protoPackage = protoPackagePattern.get().replace("{version}", version)
        versionData.detectedProtoPackage = protoPackage

        val autoMappings = protocExecutor.buildProtoMappings(
            protoDir.toPath(),
            protoPackage,
            excludePatterns
        )

        pluginLogger.info("  Proto mappings: ${autoMappings.size} auto-generated")

        return schema
    }

    /**
     * Builds the generator configuration from task properties.
     *
     * <p>Creates a GeneratorConfig instance with all the necessary settings
     * for the code generation process.</p>
     *
     * @return Configured GeneratorConfig instance
     */
    private fun buildGeneratorConfig(): GeneratorConfig {
        val builder = GeneratorConfig.builder()
            .outputDirectory(outputDirectory.get().asFile.toPath())
            .apiPackage(effectiveApiPackage)
            .implPackagePattern(effectiveImplPackagePattern)
            .protoPackagePattern(protoPackagePattern.get())
            .generateInterfaces(generateInterfaces.get())
            .generateAbstractClasses(generateAbstractClasses.get())
            .generateImplClasses(generateImplClasses.get())
            .generateVersionContext(generateVersionContext.get())
            .generateProtocolVersions(generateProtocolVersions.get())
            .includeVersionSuffix(includeVersionSuffix.get())
            .generateBuilders(generateBuilders.get())
            .defaultSyntax(if (protobufMajorVersion.get() == 2) ProtoSyntax.PROTO2 else ProtoSyntax.PROTO3)
            .convertWellKnownTypes(convertWellKnownTypes.get())
            .generateRawProtoAccessors(generateRawProtoAccessors.get())
            // Incremental generation settings
            .incremental(incremental.get())
            .cacheDirectory(cacheDirectory.get().asFile.toPath())
            .forceRegenerate(forceRegenerate.get())
            // Java version compatibility
            .targetJavaVersion(targetJavaVersion.get())
            // Parallel generation (since 2.1.0)
            .parallelGeneration(parallelGeneration.get())
            .generationThreads(generationThreads.get())
            // Default version (since 2.1.1)
            .defaultVersion(defaultVersion.orNull)
            // Field mappings (since 2.2.0)
            .fieldMappings(fieldMappings.orNull)
            // Validation annotations (since 2.3.0)
            .generateValidationAnnotations(generateValidationAnnotations.get())
            .validationAnnotationStyle(validationAnnotationStyle.get())

        includeMessages.orNull?.forEach { msg ->
            builder.includeMessage(msg)
        }

        excludeMessages.orNull?.forEach { msg ->
            builder.excludeMessage(msg)
        }

        return builder.build()
    }

    /**
     * Resolves the fully qualified proto class name for a message in a specific version.
     *
     * <p>Takes into account the proto package pattern and handles both nested
     * classes (inside outer class) and top-level classes (java_multiple_files mode).</p>
     *
     * @param message The merged message definition
     * @param adapter The version configuration adapter
     * @return Fully qualified Java class name for the proto message
     */
    private fun getProtoClassName(message: MergedMessage, adapter: GradleVersionConfigAdapter): String {
        val version = adapter.versionId
        val versionData = versionDataCache[adapter.config.name]
        val protoPackage = versionData?.detectedProtoPackage
            ?: protoPackagePattern.get().replace("{version}", version)

        val outerClassName = message.getOuterClassName(version)
        return if (outerClassName != null) {
            pluginLogger.debug("Using outer class for ${message.name}: $outerClassName")
            "$protoPackage.$outerClassName.${message.name}"
        } else {
            "$protoPackage.${message.name}"
        }
    }

    /**
     * Logs statistics about type conflicts in the merged schema.
     *
     * <p>Summarizes conflicts by type and provides examples of affected fields.
     * Helps identify potential issues when merging multiple schema versions.</p>
     *
     * @param schema The merged schema to analyze
     */
    private fun logConflictStatistics(schema: MergedSchema) {
        val conflictCounts = EnumMap<MergedField.ConflictType, Int>(MergedField.ConflictType::class.java)
        val conflictExamples = EnumMap<MergedField.ConflictType, MutableList<String>>(MergedField.ConflictType::class.java)

        for (message in schema.messages) {
            collectConflicts(message, conflictCounts, conflictExamples)
        }

        conflictCounts.remove(MergedField.ConflictType.NONE)
        conflictExamples.remove(MergedField.ConflictType.NONE)

        if (conflictCounts.isEmpty()) {
            pluginLogger.info("No type conflicts detected")
            return
        }

        val summary = StringBuilder("Type conflicts: ")
        val parts = conflictCounts.map { (type, count) ->
            val examples = conflictExamples[type] ?: emptyList()
            val exampleStr = if (examples.size <= 2) {
                examples.joinToString(", ")
            } else {
                "${examples[0]}, ${examples[1]}, ..."
            }
            "$count ${type.name} ($exampleStr)"
        }
        summary.append(parts.joinToString("; "))
        pluginLogger.info(summary.toString())
    }

    /**
     * Recursively collects conflict information from a message and its nested messages.
     *
     * @param message The message to collect conflicts from
     * @param counts Map to accumulate conflict counts by type
     * @param examples Map to accumulate example field names by conflict type
     */
    private fun collectConflicts(
        message: MergedMessage,
        counts: MutableMap<MergedField.ConflictType, Int>,
        examples: MutableMap<MergedField.ConflictType, MutableList<String>>
    ) {
        for (field in message.fieldsSorted) {
            val conflictType = field.conflictType
            if (conflictType != null && conflictType != MergedField.ConflictType.NONE) {
                counts.merge(conflictType, 1) { a, b -> a + b }
                examples.computeIfAbsent(conflictType) { mutableListOf() }
                    .add("${message.name}.${field.name}")
            }
        }

        for (nested in message.nestedMessages) {
            collectConflicts(nested, counts, examples)
        }
    }

    /**
     * Collects all proto files from all version directories.
     *
     * @return Set of paths to all proto files
     */
    private fun collectAllProtoFiles(): Set<Path> {
        val protoFiles = mutableSetOf<Path>()
        for (versionConfig in versions.get()) {
            val versionData = versionDataCache[versionConfig.name] ?: continue
            val protoDir = versionData.resolvedProtoDir.toPath()
            val excludePatterns = versionConfig.excludeProtos.orNull ?: emptyList()

            Files.walk(protoDir).use { stream ->
                stream.filter { path ->
                    Files.isRegularFile(path) &&
                    path.extension == "proto" &&
                    !isExcluded(path, protoDir, excludePatterns)
                }.forEach { protoFiles.add(it) }
            }
        }
        return protoFiles
    }

    /**
     * Checks if a proto file matches any exclude pattern.
     *
     * @param file Path to the file to check
     * @param baseDir Base directory for relative path resolution
     * @param excludePatterns List of patterns to match against
     * @return true if the file should be excluded
     */
    private fun isExcluded(file: Path, baseDir: Path, excludePatterns: List<String>): Boolean {
        if (excludePatterns.isEmpty()) return false
        val relativePath = baseDir.relativize(file).toString().replace('\\', '/')
        return excludePatterns.any { pattern ->
            val regex = pattern
                .replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*")
            relativePath.matches(Regex(regex))
        }
    }

    /**
     * Parses the configured proto syntax string to ProtoSyntax enum.
     *
     * @param syntaxStr Syntax string from configuration ("proto2", "proto3", "auto", or null)
     * @return ProtoSyntax enum value (AUTO if not specified or "auto")
     * @since 2.2.0
     */
    private fun parseConfiguredSyntax(syntaxStr: String?): ProtoSyntax {
        if (syntaxStr.isNullOrEmpty()) {
            return ProtoSyntax.AUTO
        }
        return when (syntaxStr.lowercase().trim()) {
            "proto2" -> ProtoSyntax.PROTO2
            "proto3" -> ProtoSyntax.PROTO3
            else -> ProtoSyntax.AUTO
        }
    }

    /**
     * Adapter class to bridge Gradle's VersionConfig with GenerationOrchestrator.VersionConfig.
     *
     * <p>Implements the interface required by GenerationOrchestrator while delegating
     * to the Gradle-specific VersionConfig class.</p>
     *
     * @property config The underlying Gradle VersionConfig instance
     */
    private inner class GradleVersionConfigAdapter(
        val config: VersionConfig
    ) : GenerationOrchestrator.VersionConfig {

        /**
         * Returns the version identifier in lowercase format.
         *
         * @return Version ID (e.g., "v1", "v2")
         */
        override fun getVersionId(): String = config.getVersionId()
    }
}
