package io.alnovis.protowrapper.gradle

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import io.alnovis.protowrapper.model.FieldMapping

abstract class ProtoWrapperExtension(private val project: Project) {

    // Directories
    abstract val protoRoot: DirectoryProperty
    abstract val outputDirectory: DirectoryProperty
    abstract val tempDirectory: DirectoryProperty

    // Packages
    abstract val basePackage: Property<String>
    abstract val apiPackage: Property<String>
    abstract val implPackagePattern: Property<String>
    abstract val protoPackagePattern: Property<String>

    // Protoc
    abstract val protocPath: Property<String>

    /**
     * Version of protoc to use for embedded downloads.
     * Only affects embedded protoc. Ignored if custom protocPath is set
     * or system protoc is found.
     * If not specified, uses the protobuf version that the plugin was built with.
     * @since 1.6.5
     */
    abstract val protocVersion: Property<String>

    // Generation flags
    abstract val generateInterfaces: Property<Boolean>
    abstract val generateAbstractClasses: Property<Boolean>
    abstract val generateImplClasses: Property<Boolean>
    abstract val generateVersionContext: Property<Boolean>
    /**
     * Whether to generate ProtocolVersions utility class.
     * ProtocolVersions provides compile-time constants for all supported
     * protocol versions and utility methods for version validation.
     * Default: true
     * @since 2.1.0
     */
    abstract val generateProtocolVersions: Property<Boolean>
    abstract val includeVersionSuffix: Property<Boolean>
    abstract val generateBuilders: Property<Boolean>

    /**
     * @deprecated Since 2.2.0. Use per-version protoSyntax configuration instead.
     *             Scheduled for removal in 3.0.0.
     */
    @Deprecated(
        message = "Use per-version protoSyntax configuration instead. Scheduled for removal in 3.0.0.",
        level = DeprecationLevel.WARNING
    )
    // TODO: Remove in 3.0.0 - see docs/DEPRECATION_POLICY.md
    abstract val protobufMajorVersion: Property<Int>

    // Well-Known Types support (since 1.3.0)
    abstract val convertWellKnownTypes: Property<Boolean>
    abstract val generateRawProtoAccessors: Property<Boolean>

    // Filtering
    abstract val includeMessages: ListProperty<String>
    abstract val excludeMessages: ListProperty<String>

    // Incremental generation (since 1.6.0)
    /**
     * Enable incremental generation.
     * When enabled, only regenerates when proto files change.
     * Default: true
     */
    abstract val incremental: Property<Boolean>

    /**
     * Directory for incremental generation cache.
     * Stores state between builds to detect changes.
     */
    abstract val cacheDirectory: DirectoryProperty

    /**
     * Force full regeneration, ignoring cache.
     * Useful when you want to regenerate all files regardless of changes.
     * Default: false
     */
    abstract val forceRegenerate: Property<Boolean>

    /**
     * Target Java version for generated code.
     * Use 8 for Java 8 compatible code (avoids private interface methods, List.of()).
     * Default: 9 (uses modern Java features like private interface methods).
     * @since 1.6.8
     */
    abstract val targetJavaVersion: Property<Int>

    // Parallel generation (since 2.1.0)
    /**
     * Enable parallel generation for improved build performance.
     * When enabled, wrapper classes are generated in parallel using multiple threads.
     * Default: false
     * @since 2.1.0
     */
    abstract val parallelGeneration: Property<Boolean>

    /**
     * Number of threads for parallel generation.
     * Only used when parallelGeneration is enabled.
     * Default: 0 (auto = number of available processors)
     * @since 2.1.0
     */
    abstract val generationThreads: Property<Int>

    /**
     * The default version ID for VersionContext.DEFAULT_VERSION and ProtocolVersions.DEFAULT.
     * If not set, the last version in the versions list is used as default.
     *
     * This is useful when versions are listed chronologically (oldest to newest)
     * but you need a specific version (e.g., the stable one) as the default.
     *
     * Example: If you have versions [v1, v2, v3] but want v2 as default:
     * ```
     * defaultVersion.set("v2")
     * ```
     * @since 2.1.1
     */
    abstract val defaultVersion: Property<String>

    /**
     * Field mappings for overriding number-based field matching.
     * Use when the same field has different field numbers across proto versions.
     *
     * Example:
     * ```kotlin
     * fieldMappings {
     *     fieldMapping("Order", "parent_order")
     *     fieldMapping("Payment", "amount", mapOf("v1" to 17, "v2" to 15))
     * }
     * ```
     * @since 2.2.0
     */
    val fieldMappings: MutableList<FieldMapping> = mutableListOf()

    /**
     * Whether to generate Bean Validation (JSR-380) annotations on interface getters.
     * When enabled, annotations like @NotNull, @Valid, @Min, @Max are added based on
     * proto field metadata.
     * Default: false
     * @since 2.3.0
     */
    abstract val generateValidationAnnotations: Property<Boolean>

    /**
     * Validation annotation namespace style.
     * Use "jakarta" (default) for Jakarta EE 9+ (jakarta.validation.constraints)
     * or "javax" for Java EE 8 and earlier (javax.validation.constraints).
     * Note: When targetJavaVersion=8, automatically uses "javax" for compatibility.
     * Default: "jakarta"
     * @since 2.3.0
     */
    abstract val validationAnnotationStyle: Property<String>

    /**
     * Whether to generate runtime schema metadata classes.
     * When enabled, generates SchemaInfo classes for each version and
     * VersionSchemaDiff classes for version-to-version changes.
     *
     * Adds VersionContext methods:
     * - getSchemaInfo() - returns SchemaInfo for the version
     * - getDiffFrom(fromVersion) - returns Optional<VersionSchemaDiff> for version transition
     *
     * Default: false
     * @since 2.3.1
     */
    abstract val generateSchemaMetadata: Property<Boolean>

    /**
     * Target language for code generation.
     * Selects the generator factory to use. Built-in: "java" (default).
     * Additional languages can be registered via SPI (ServiceLoader).
     *
     * Example for Kotlin (requires proto-wrapper-kotlin dependency):
     * ```kotlin
     * language.set("kotlin")
     * ```
     *
     * Default: "java"
     * @since 2.4.0
     */
    abstract val language: Property<String>

    /**
     * Add a name-based field mapping.
     *
     * @param message the message name
     * @param fieldName the proto field name
     */
    fun fieldMapping(message: String, fieldName: String) {
        fieldMappings.add(FieldMapping(message, fieldName))
    }

    /**
     * Add an explicit field mapping with version-specific numbers.
     *
     * @param message the message name
     * @param fieldName the proto field name
     * @param versionNumbers map of version to field number
     */
    fun fieldMapping(message: String, fieldName: String, versionNumbers: Map<String, Int>) {
        fieldMappings.add(FieldMapping(message, fieldName, versionNumbers))
    }

    // Versions container
    val versions: NamedDomainObjectContainer<VersionConfig> =
        project.container(VersionConfig::class.java) { name ->
            project.objects.newInstance(VersionConfig::class.java, name)
        }

    fun versions(action: Action<NamedDomainObjectContainer<VersionConfig>>) {
        action.execute(versions)
    }

    init {
        // Default values
        outputDirectory.convention(
            project.layout.buildDirectory.dir("generated/sources/proto-wrapper/main/java")
        )
        tempDirectory.convention(
            project.layout.buildDirectory.dir("proto-wrapper-tmp")
        )
        generateInterfaces.convention(true)
        generateAbstractClasses.convention(true)
        generateImplClasses.convention(true)
        generateVersionContext.convention(true)
        generateProtocolVersions.convention(true)
        includeVersionSuffix.convention(true)
        generateBuilders.convention(false)
        protobufMajorVersion.convention(3)
        convertWellKnownTypes.convention(true)
        generateRawProtoAccessors.convention(false)
        // Incremental generation defaults
        incremental.convention(true)
        cacheDirectory.convention(
            project.layout.buildDirectory.dir("proto-wrapper-cache")
        )
        forceRegenerate.convention(false)
        // Java version compatibility (default: modern Java 9+)
        targetJavaVersion.convention(9)
        // Parallel generation (since 2.1.0)
        parallelGeneration.convention(false)
        generationThreads.convention(0)
        // Validation annotations (since 2.3.0)
        generateValidationAnnotations.convention(false)
        validationAnnotationStyle.convention("jakarta")
        // Schema metadata (since 2.3.1)
        generateSchemaMetadata.convention(false)
        // Target language (since 2.4.0)
        language.convention("java")
    }
}
