package io.alnovis.protowrapper.generator;

import io.alnovis.protowrapper.generator.versioncontext.Java8Codegen;
import io.alnovis.protowrapper.generator.versioncontext.Java9PlusCodegen;
import io.alnovis.protowrapper.generator.versioncontext.JavaVersionCodegen;
import io.alnovis.protowrapper.model.FieldMapping;
import io.alnovis.protowrapper.model.ProtoSyntax;

import java.nio.file.Path;
import java.util.*;

/**
 * Configuration for code generation.
 *
 * <p>This class holds all configuration options that control the code generation
 * process. It uses the Builder pattern for construction and provides sensible
 * defaults for most options.</p>
 *
 * <h2>Required Configuration</h2>
 * <ul>
 *   <li>{@code outputDirectory} - Where to write generated files</li>
 * </ul>
 *
 * <h2>Package Configuration</h2>
 * <ul>
 *   <li>{@code apiPackage} - Package for public interfaces and enums</li>
 *   <li>{@code implPackagePattern} - Pattern for implementation packages (uses {version} placeholder)</li>
 *   <li>{@code protoPackagePattern} - Pattern for proto class packages</li>
 *   <li>{@code abstractClassPackage} - Package for abstract classes (defaults to apiPackage + ".impl")</li>
 * </ul>
 *
 * <h2>Generation Flags</h2>
 * <ul>
 *   <li>{@code generateInterfaces} - Generate public API interfaces</li>
 *   <li>{@code generateAbstractClasses} - Generate abstract implementation classes</li>
 *   <li>{@code generateImplClasses} - Generate version-specific implementations</li>
 *   <li>{@code generateVersionContext} - Generate VersionContext factory class</li>
 *   <li>{@code includeVersionSuffix} - Add version suffix to impl class names (e.g., PersonV1)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * GeneratorConfig config = GeneratorConfig.builder()
 *     .outputDirectory(Path.of("target/generated-sources"))
 *     .apiPackage("com.example.api")
 *     .implPackagePattern("com.example.impl.{version}")
 *     .protoPackagePattern("com.example.proto.{version}")
 *     .generateInterfaces(true)
 *     .generateAbstractClasses(true)
 *     .generateImplClasses(true)
 *     .build();
 * }</pre>
 *
 * <h2>Message Filtering</h2>
 * <p>You can include or exclude specific messages from generation:</p>
 * <pre>{@code
 * GeneratorConfig config = GeneratorConfig.builder()
 *     .outputDirectory(outputDir)
 *     .includeMessage("Person")      // Only generate Person
 *     .includeMessage("Address")     // and Address
 *     .excludeMessage("InternalMsg") // Never generate InternalMsg
 *     .build();
 * }</pre>
 *
 * @see GenerationOrchestrator
 * @see BaseGenerator
 */
public class GeneratorConfig {

    private Path outputDirectory;
    private String apiPackage = "com.example.model.api";
    private String implPackagePattern = "com.example.model.{version}";
    private String protoPackagePattern = "com.example.proto.{version}";
    private String abstractClassPackage; // null = apiPackage + ".impl"

    private boolean generateInterfaces = true;
    private boolean generateAbstractClasses = true;
    private boolean generateImplClasses = true;
    private boolean generateVersionContext = true;
    private boolean generateProtocolVersions = false;
    private boolean includeVersionSuffix = true;
    private boolean generateBuilders = false;
    private ProtoSyntax defaultSyntax = ProtoSyntax.AUTO; // AUTO means detect from .proto files
    private boolean convertWellKnownTypes = true;
    private boolean generateRawProtoAccessors = false;

    private final Set<String> includedMessages = new HashSet<>();
    private final Set<String> excludedMessages = new HashSet<>();

    private final Map<String, String> customTypeMappings = new HashMap<>();
    private final Map<String, String> fieldNameOverrides = new HashMap<>();

    // Incremental generation settings
    private boolean incremental = true;
    private Path cacheDirectory;
    private boolean forceRegenerate = false;

    // Target Java version (8 = Java 8 compatible, 9+ = use modern features)
    private int targetJavaVersion = 9;

    // Parallel generation settings (since 2.1.0)
    private boolean parallelGeneration = false;
    private int generationThreads = 0; // 0 = auto (available processors)

    // Default version for VersionContext.DEFAULT_VERSION and ProtocolVersions.DEFAULT (since 2.1.1)
    // If null, the last version in the list is used as default
    private String defaultVersion = null;

    // Field mappings for name-based matching (since 2.2.0)
    private final List<FieldMapping> fieldMappings = new ArrayList<>();

    // Validation annotations settings (since 2.3.0)
    private boolean generateValidationAnnotations = false;
    private String validationAnnotationStyle = "jakarta";

    // Schema metadata generation settings (since 2.3.0)
    private boolean generateSchemaMetadata = false;

    /**
     * Create a new builder for GeneratorConfig.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** @return the output directory */
    public Path getOutputDirectory() { return outputDirectory; }
    /** @return the API package */
    public String getApiPackage() { return apiPackage; }
    /**
     * Get the implementation package for a version.
     *
     * @param version the version string
     * @return the implementation package
     */
    public String getImplPackage(String version) {
        return implPackagePattern.replace("{version}", version);
    }
    /**
     * Get the proto package for a version.
     *
     * @param version the version string
     * @return the proto package
     */
    public String getProtoPackage(String version) {
        return protoPackagePattern.replace("{version}", version);
    }
    /** @return the proto package pattern */
    public String getProtoPackagePattern() { return protoPackagePattern; }
    /** @return the impl package pattern */
    public String getImplPackagePattern() { return implPackagePattern; }
    /** @return the abstract class package */
    public String getAbstractClassPackage() {
        return abstractClassPackage != null ? abstractClassPackage : apiPackage + ".impl";
    }
    /** @return true if interfaces should be generated */
    public boolean isGenerateInterfaces() { return generateInterfaces; }
    /** @return true if abstract classes should be generated */
    public boolean isGenerateAbstractClasses() { return generateAbstractClasses; }
    /** @return true if implementation classes should be generated */
    public boolean isGenerateImplClasses() { return generateImplClasses; }
    /** @return true if VersionContext should be generated */
    public boolean isGenerateVersionContext() { return generateVersionContext; }
    /** @return true if ProtocolVersions class should be generated */
    public boolean isGenerateProtocolVersions() { return generateProtocolVersions; }
    /** @return true if version suffix should be included */
    public boolean isIncludeVersionSuffix() { return includeVersionSuffix; }
    /** @return true if builders should be generated */
    public boolean isGenerateBuilders() { return generateBuilders; }
    /** @return the default proto syntax */
    public ProtoSyntax getDefaultSyntax() { return defaultSyntax; }

    /**
     * Get protobuf major version.
     *
     * @return the protobuf major version (2 or 3)
     * @deprecated Since 2.2.0. Use {@link #getDefaultSyntax()} instead.
     *             Scheduled for removal in 3.0.0.
     */
    @Deprecated(since = "2.2.0", forRemoval = true)
    // TODO: Remove in 3.0.0 - see docs/DEPRECATION_POLICY.md
    public int getProtobufMajorVersion() {
        return defaultSyntax.isProto2() ? 2 : 3;
    }

    /**
     * Check if protobuf 2 syntax.
     *
     * @return true if proto2
     * @deprecated Since 2.2.0. Use {@link #getDefaultSyntax()}.{@link ProtoSyntax#isProto2() isProto2()} instead.
     *             Scheduled for removal in 3.0.0.
     */
    @Deprecated(since = "2.2.0", forRemoval = true)
    // TODO: Remove in 3.0.0 - see docs/DEPRECATION_POLICY.md
    public boolean isProtobuf2() { return defaultSyntax.isProto2(); }

    /**
     * Check if protobuf 3 syntax.
     *
     * @return true if proto3
     * @deprecated Since 2.2.0. Use {@link #getDefaultSyntax()}.{@link ProtoSyntax#isProto3() isProto3()} instead.
     *             Scheduled for removal in 3.0.0.
     */
    @Deprecated(since = "2.2.0", forRemoval = true)
    // TODO: Remove in 3.0.0 - see docs/DEPRECATION_POLICY.md
    public boolean isProtobuf3() { return defaultSyntax.isProto3() || defaultSyntax.isAuto(); }

    /** @return true if well-known types should be converted */
    public boolean isConvertWellKnownTypes() { return convertWellKnownTypes; }
    /** @return true if raw proto accessors should be generated */
    public boolean isGenerateRawProtoAccessors() { return generateRawProtoAccessors; }

    /** @return true if incremental generation is enabled */
    public boolean isIncremental() { return incremental; }
    /** @return the cache directory path */
    public Path getCacheDirectory() { return cacheDirectory; }
    /** @return true if forced regeneration is enabled */
    public boolean isForceRegenerate() { return forceRegenerate; }
    /** @return the target Java version (8, 9, 11, 17, etc.) */
    public int getTargetJavaVersion() { return targetJavaVersion; }
    /** @return true if generating Java 8 compatible code */
    public boolean isJava8Compatible() { return targetJavaVersion <= 8; }
    /** @return true if parallel generation is enabled */
    public boolean isParallelGeneration() { return parallelGeneration; }
    /** @return number of generation threads (0 = auto) */
    public int getGenerationThreads() { return generationThreads; }
    /**
     * Get the effective number of threads for parallel generation.
     * @return number of threads to use (auto-calculated if generationThreads is 0)
     */
    public int getEffectiveGenerationThreads() {
        return generationThreads > 0 ? generationThreads : Runtime.getRuntime().availableProcessors();
    }

    /**
     * Get the explicitly configured default version.
     * If null, the last version in the versions list should be used as default.
     *
     * @return the default version ID, or null if not explicitly set
     * @since 2.1.1
     */
    public String getDefaultVersion() {
        return defaultVersion;
    }

    /**
     * Get the configured field mappings for name-based field matching.
     *
     * @return unmodifiable list of field mappings
     * @since 2.2.0
     */
    public List<FieldMapping> getFieldMappings() {
        return Collections.unmodifiableList(fieldMappings);
    }

    /**
     * Check if validation annotations should be generated on interface getters.
     *
     * @return true if validation annotations should be generated
     * @since 2.3.0
     */
    public boolean isGenerateValidationAnnotations() {
        return generateValidationAnnotations;
    }

    /**
     * Get the validation annotation style (namespace).
     *
     * @return "jakarta" for Jakarta EE (jakarta.validation.constraints) or
     *         "javax" for Java EE (javax.validation.constraints)
     * @since 2.3.0
     */
    public String getValidationAnnotationStyle() {
        return validationAnnotationStyle;
    }

    /**
     * Get the validation constraints package based on annotation style.
     *
     * <p>Returns the appropriate package for validation constraint annotations:</p>
     * <ul>
     *   <li>"jakarta" style: {@code jakarta.validation.constraints}</li>
     *   <li>"javax" style: {@code javax.validation.constraints}</li>
     * </ul>
     *
     * <p>Note: For Java 8 compatibility ({@code targetJavaVersion=8}), the style
     * is automatically switched to "javax" regardless of the configured value.</p>
     *
     * @return the fully qualified package name for constraint annotations
     * @since 2.3.0
     */
    public String getValidationPackage() {
        String effectiveStyle = targetJavaVersion <= 8 ? "javax" : validationAnnotationStyle;
        return "javax".equals(effectiveStyle)
                ? "javax.validation.constraints"
                : "jakarta.validation.constraints";
    }

    /**
     * Get the base validation package (for @Valid annotation).
     *
     * @return "jakarta.validation" or "javax.validation" based on style
     * @since 2.3.0
     */
    public String getValidationBasePackage() {
        String effectiveStyle = targetJavaVersion <= 8 ? "javax" : validationAnnotationStyle;
        return "javax".equals(effectiveStyle) ? "javax.validation" : "jakarta.validation";
    }

    /**
     * Check if schema metadata should be generated.
     *
     * <p>When enabled, generates SchemaInfo and VersionSchemaDiff classes
     * providing runtime access to enum values, field information, and
     * schema differences between versions.</p>
     *
     * @return true if schema metadata should be generated
     * @since 2.3.0
     */
    public boolean isGenerateSchemaMetadata() {
        return generateSchemaMetadata;
    }

    /**
     * Get the metadata package for generated SchemaInfo and SchemaDiff classes.
     *
     * @return metadata package name (basePackage + ".metadata")
     * @since 2.3.0
     */
    public String getMetadataPackage() {
        return apiPackage.replace(".api", "") + ".metadata";
    }

    /**
     * Get the Java version-specific code generation strategy.
     *
     * <p>Returns appropriate strategy based on target Java version:</p>
     * <ul>
     *   <li>Java 8: {@link Java8Codegen} - uses Collections.unmodifiableList(), simple @Deprecated</li>
     *   <li>Java 9+: {@link Java9PlusCodegen} - uses List.of(), @Deprecated(since, forRemoval)</li>
     * </ul>
     *
     * @return the code generation strategy for the target Java version
     * @since 1.6.9
     */
    public JavaVersionCodegen getJavaVersionCodegen() {
        return targetJavaVersion >= 9 ? Java9PlusCodegen.INSTANCE : Java8Codegen.INSTANCE;
    }

    /**
     * Get the implementation class name for a message in a specific version.
     * @param messageName Simple message name (e.g., "Money")
     * @param version Version string (e.g., "v1")
     * @return Class name with or without version suffix based on configuration
     */
    public String getImplClassName(String messageName, String version) {
        if (includeVersionSuffix) {
            return messageName + version.substring(0, 1).toUpperCase() + version.substring(1);
        }
        return messageName;
    }

    /**
     * Check if a message should be generated.
     *
     * @param messageName the message name
     * @return true if the message should be generated
     */
    public boolean shouldGenerate(String messageName) {
        if (!includedMessages.isEmpty() && !includedMessages.contains(messageName)) {
            return false;
        }
        return !excludedMessages.contains(messageName);
    }

    /**
     * Get custom type mapping for a proto type.
     *
     * @param protoType the proto type name
     * @return the Java type name, or null if no mapping
     */
    public String getCustomTypeMapping(String protoType) {
        return customTypeMappings.get(protoType);
    }

    /**
     * Get field name override for a specific field.
     *
     * @param messageName the message name
     * @param fieldName the field name
     * @return the Java name override, or null if no override
     */
    public String getFieldNameOverride(String messageName, String fieldName) {
        return fieldNameOverrides.get(messageName + "." + fieldName);
    }

    /**
     * Get all custom type mappings.
     * Package-private for ConfigHasher access.
     *
     * @return unmodifiable view of custom type mappings
     */
    Map<String, String> getCustomTypeMappings() {
        return Collections.unmodifiableMap(customTypeMappings);
    }

    /**
     * Get all field name overrides.
     * Package-private for ConfigHasher access.
     *
     * @return unmodifiable view of field name overrides
     */
    Map<String, String> getFieldNameOverrides() {
        return Collections.unmodifiableMap(fieldNameOverrides);
    }

    /**
     * Get all included messages.
     * Package-private for ConfigHasher access.
     *
     * @return unmodifiable view of included messages
     */
    Set<String> getIncludedMessages() {
        return Collections.unmodifiableSet(includedMessages);
    }

    /**
     * Get all excluded messages.
     * Package-private for ConfigHasher access.
     *
     * @return unmodifiable view of excluded messages
     */
    Set<String> getExcludedMessages() {
        return Collections.unmodifiableSet(excludedMessages);
    }

    /**
     * Compute hash of configuration for cache invalidation.
     * Changes in these settings invalidate the cache.
     *
     * @return 16-character hex string representing configuration hash
     */
    public String computeConfigHash() {
        return ConfigHasher.computeHash(this);
    }

    /** Builder for GeneratorConfig. */
    public static class Builder {
        private final GeneratorConfig config = new GeneratorConfig();

        /**
         * Set the output directory.
         * @param path the output directory
         * @return this builder
         */
        public Builder outputDirectory(Path path) {
            config.outputDirectory = path;
            return this;
        }

        /**
         * Set the API package.
         * @param pkg the API package
         * @return this builder
         */
        public Builder apiPackage(String pkg) {
            config.apiPackage = pkg;
            return this;
        }

        /**
         * Set the implementation package pattern.
         * @param pattern the implementation package pattern
         * @return this builder
         */
        public Builder implPackagePattern(String pattern) {
            config.implPackagePattern = pattern;
            return this;
        }

        /**
         * Set the proto package pattern.
         * @param pattern the proto package pattern
         * @return this builder
         */
        public Builder protoPackagePattern(String pattern) {
            config.protoPackagePattern = pattern;
            return this;
        }

        /**
         * Set whether to generate interfaces.
         * @param value whether to generate interfaces
         * @return this builder
         */
        public Builder generateInterfaces(boolean value) {
            config.generateInterfaces = value;
            return this;
        }

        /**
         * Set whether to generate abstract classes.
         * @param value whether to generate abstract classes
         * @return this builder
         */
        public Builder generateAbstractClasses(boolean value) {
            config.generateAbstractClasses = value;
            return this;
        }

        /**
         * Set whether to generate implementation classes.
         * @param value whether to generate implementation classes
         * @return this builder
         */
        public Builder generateImplClasses(boolean value) {
            config.generateImplClasses = value;
            return this;
        }

        /**
         * Set whether to generate VersionContext.
         * @param value whether to generate VersionContext
         * @return this builder
         */
        public Builder generateVersionContext(boolean value) {
            config.generateVersionContext = value;
            return this;
        }

        /**
         * Set whether to generate ProtocolVersions utility class.
         * @param value whether to generate ProtocolVersions
         * @return this builder
         */
        public Builder generateProtocolVersions(boolean value) {
            config.generateProtocolVersions = value;
            return this;
        }

        /**
         * Set whether to include version suffix.
         * @param value whether to include version suffix
         * @return this builder
         */
        public Builder includeVersionSuffix(boolean value) {
            config.includeVersionSuffix = value;
            return this;
        }

        /**
         * Set whether to generate builders.
         * @param value whether to generate builders
         * @return this builder
         */
        public Builder generateBuilders(boolean value) {
            config.generateBuilders = value;
            return this;
        }

        /**
         * Set the default proto syntax.
         * @param syntax the default proto syntax
         * @return this builder
         */
        public Builder defaultSyntax(ProtoSyntax syntax) {
            config.defaultSyntax = syntax;
            return this;
        }

        /**
         * Set protobuf major version.
         *
         * @param version the protobuf major version (2 or 3)
         * @return this builder
         * @deprecated Since 2.2.0. Use {@link #defaultSyntax(ProtoSyntax)} instead.
         *             Scheduled for removal in 3.0.0.
         */
        @Deprecated(since = "2.2.0", forRemoval = true)
        // TODO: Remove in 3.0.0 - see docs/DEPRECATION_POLICY.md
        public Builder protobufMajorVersion(int version) {
            ConfigValidator.validateProtobufMajorVersion(version);
            // Inline conversion to avoid calling deprecated ProtoSyntax.fromMajorVersion()
            config.defaultSyntax = (version == 2) ? ProtoSyntax.PROTO2 : ProtoSyntax.PROTO3;
            return this;
        }

        /**
         * Set whether to convert well-known types.
         * @param value whether to convert well-known types
         * @return this builder
         */
        public Builder convertWellKnownTypes(boolean value) {
            config.convertWellKnownTypes = value;
            return this;
        }

        /**
         * Set whether to generate raw proto accessors.
         * @param value whether to generate raw proto accessors
         * @return this builder
         */
        public Builder generateRawProtoAccessors(boolean value) {
            config.generateRawProtoAccessors = value;
            return this;
        }

        /**
         * Include a message in generation.
         * @param messageName message to include
         * @return this builder
         */
        public Builder includeMessage(String messageName) {
            config.includedMessages.add(messageName);
            return this;
        }

        /**
         * Exclude a message from generation.
         * @param messageName message to exclude
         * @return this builder
         */
        public Builder excludeMessage(String messageName) {
            config.excludedMessages.add(messageName);
            return this;
        }

        /**
         * Add custom type mapping.
         *
         * @param protoType the proto type
         * @param javaType the Java type
         * @return this builder
         */
        public Builder customTypeMapping(String protoType, String javaType) {
            config.customTypeMappings.put(protoType, javaType);
            return this;
        }

        /**
         * Add field name override.
         *
         * @param messageName the message name
         * @param fieldName the field name
         * @param javaName the Java name
         * @return this builder
         */
        public Builder fieldNameOverride(String messageName, String fieldName, String javaName) {
            config.fieldNameOverrides.put(messageName + "." + fieldName, javaName);
            return this;
        }

        /**
         * Enable or disable incremental generation.
         * When enabled, only changed proto files and their dependents are regenerated.
         * Default: true
         *
         * @param incremental true to enable incremental generation
         * @return this builder
         */
        public Builder incremental(boolean incremental) {
            config.incremental = incremental;
            return this;
        }

        /**
         * Set the cache directory for incremental generation state.
         * If not set, a default directory will be used (output directory + ".proto-wrapper-cache").
         *
         * @param cacheDirectory path to cache directory
         * @return this builder
         */
        public Builder cacheDirectory(Path cacheDirectory) {
            config.cacheDirectory = cacheDirectory;
            return this;
        }

        /**
         * Force full regeneration even when incremental mode is enabled.
         * This invalidates the cache and regenerates all files.
         * Default: false
         *
         * @param forceRegenerate true to force full regeneration
         * @return this builder
         */
        public Builder forceRegenerate(boolean forceRegenerate) {
            config.forceRegenerate = forceRegenerate;
            return this;
        }

        /**
         * Set the target Java version for generated code.
         * Use 8 for Java 8 compatible code (avoids private interface methods, List.of()).
         * Default: 9 (uses modern Java features).
         *
         * @param version target Java version (8, 9, 11, 17, etc.)
         * @return this builder
         */
        public Builder targetJavaVersion(int version) {
            ConfigValidator.validateTargetJavaVersion(version);
            config.targetJavaVersion = version;
            return this;
        }

        /**
         * Enable or disable parallel generation.
         * When enabled, wrapper classes are generated in parallel for better performance.
         * Default: false
         *
         * @param parallelGeneration true to enable parallel generation
         * @return this builder
         * @since 2.1.0
         */
        public Builder parallelGeneration(boolean parallelGeneration) {
            config.parallelGeneration = parallelGeneration;
            return this;
        }

        /**
         * Set the number of threads for parallel generation.
         * Only used when parallelGeneration is enabled.
         * Default: 0 (auto = available processors)
         *
         * @param threads number of threads (0 for auto)
         * @return this builder
         * @since 2.1.0
         */
        public Builder generationThreads(int threads) {
            ConfigValidator.validateGenerationThreads(threads);
            config.generationThreads = threads;
            return this;
        }

        /**
         * Set the default version for VersionContext.DEFAULT_VERSION and ProtocolVersions.DEFAULT.
         * If not set, the last version in the versions list is used as default.
         *
         * <p>This is useful when the order of versions in configuration is chronological
         * (oldest to newest), but you need a different version as the default
         * (e.g., the stable version rather than the latest).</p>
         *
         * @param version the version ID to use as default (e.g., "v1", "v202")
         * @return this builder
         * @since 2.1.1
         */
        public Builder defaultVersion(String version) {
            config.defaultVersion = version;
            return this;
        }

        /**
         * Add a field mapping for name-based field matching.
         *
         * @param mapping the field mapping
         * @return this builder
         * @since 2.2.0
         */
        public Builder addFieldMapping(FieldMapping mapping) {
            if (mapping != null) {
                config.fieldMappings.add(mapping);
            }
            return this;
        }

        /**
         * Set field mappings (replaces existing).
         *
         * @param mappings the list of field mappings
         * @return this builder
         * @since 2.2.0
         */
        public Builder fieldMappings(List<FieldMapping> mappings) {
            config.fieldMappings.clear();
            if (mappings != null) {
                config.fieldMappings.addAll(mappings);
            }
            return this;
        }

        /**
         * Enable or disable validation annotation generation.
         *
         * <p>When enabled, generates Bean Validation (JSR-380) annotations on
         * interface getters based on proto field metadata:</p>
         * <ul>
         *   <li>{@code @NotNull} - for required fields and collections</li>
         *   <li>{@code @Valid} - for message-type fields (cascading validation)</li>
         *   <li>{@code @Min}, {@code @Max} - for numeric constraints (custom options)</li>
         *   <li>{@code @Size} - for collection/string size constraints (custom options)</li>
         *   <li>{@code @Pattern} - for regex constraints (custom options)</li>
         * </ul>
         *
         * <p>Default: false</p>
         *
         * @param generateValidationAnnotations true to enable validation annotations
         * @return this builder
         * @since 2.3.0
         */
        public Builder generateValidationAnnotations(boolean generateValidationAnnotations) {
            config.generateValidationAnnotations = generateValidationAnnotations;
            return this;
        }

        /**
         * Set the validation annotation namespace style.
         *
         * <p>Determines which package to use for validation annotations:</p>
         * <ul>
         *   <li>"jakarta" (default) - uses {@code jakarta.validation.constraints} (Jakarta EE 9+)</li>
         *   <li>"javax" - uses {@code javax.validation.constraints} (Java EE 8 and earlier)</li>
         * </ul>
         *
         * <p>Note: When {@code targetJavaVersion=8}, the style is automatically
         * switched to "javax" for compatibility.</p>
         *
         * @param style "jakarta" or "javax"
         * @return this builder
         * @throws IllegalArgumentException if style is not "jakarta" or "javax"
         * @since 2.3.0
         */
        public Builder validationAnnotationStyle(String style) {
            ConfigValidator.validateValidationStyle(style);
            config.validationAnnotationStyle = style != null ? style : "jakarta";
            return this;
        }

        /**
         * Enable or disable schema metadata generation.
         *
         * <p>When enabled, generates SchemaInfo classes for each version
         * and VersionSchemaDiff classes for each version pair, providing
         * runtime access to:</p>
         * <ul>
         *   <li>Enum names and values</li>
         *   <li>Message field information</li>
         *   <li>Schema differences between versions</li>
         *   <li>Migration hints for field changes</li>
         * </ul>
         *
         * <p>Default: false</p>
         *
         * @param generateSchemaMetadata true to enable schema metadata generation
         * @return this builder
         * @since 2.3.0
         */
        public Builder generateSchemaMetadata(boolean generateSchemaMetadata) {
            config.generateSchemaMetadata = generateSchemaMetadata;
            return this;
        }

        /**
         * Build the GeneratorConfig.
         *
         * @return the built configuration
         */
        public GeneratorConfig build() {
            ConfigValidator.validate(config);
            return config;
        }
    }
}
