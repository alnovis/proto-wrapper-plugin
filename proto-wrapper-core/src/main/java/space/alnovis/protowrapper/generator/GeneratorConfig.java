package space.alnovis.protowrapper.generator;

import space.alnovis.protowrapper.model.ProtoSyntax;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private boolean includeVersionSuffix = true;
    private boolean generateBuilders = false;
    private ProtoSyntax defaultSyntax = ProtoSyntax.AUTO; // AUTO means detect from .proto files
    private boolean convertWellKnownTypes = true;
    private boolean generateRawProtoAccessors = false;

    private Set<String> includedMessages = new HashSet<>();
    private Set<String> excludedMessages = new HashSet<>();

    private Map<String, String> customTypeMappings = new HashMap<>();
    private Map<String, String> fieldNameOverrides = new HashMap<>();

    // Incremental generation settings
    private boolean incremental = true;
    private Path cacheDirectory;
    private boolean forceRegenerate = false;

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Path getOutputDirectory() { return outputDirectory; }
    public String getApiPackage() { return apiPackage; }
    public String getImplPackage(String version) {
        return implPackagePattern.replace("{version}", version);
    }
    public String getProtoPackage(String version) {
        return protoPackagePattern.replace("{version}", version);
    }
    public String getProtoPackagePattern() { return protoPackagePattern; }
    public String getAbstractClassPackage() {
        return abstractClassPackage != null ? abstractClassPackage : apiPackage + ".impl";
    }
    public boolean isGenerateInterfaces() { return generateInterfaces; }
    public boolean isGenerateAbstractClasses() { return generateAbstractClasses; }
    public boolean isGenerateImplClasses() { return generateImplClasses; }
    public boolean isGenerateVersionContext() { return generateVersionContext; }
    public boolean isIncludeVersionSuffix() { return includeVersionSuffix; }
    public boolean isGenerateBuilders() { return generateBuilders; }
    public ProtoSyntax getDefaultSyntax() { return defaultSyntax; }

    /**
     * @deprecated Use {@link #getDefaultSyntax()} instead
     */
    @Deprecated
    public int getProtobufMajorVersion() {
        return defaultSyntax.isProto2() ? 2 : 3;
    }

    /**
     * @deprecated Use {@link #getDefaultSyntax()}.{@link ProtoSyntax#isProto2() isProto2()} instead
     */
    @Deprecated
    public boolean isProtobuf2() { return defaultSyntax.isProto2(); }

    /**
     * @deprecated Use {@link #getDefaultSyntax()}.{@link ProtoSyntax#isProto3() isProto3()} instead
     */
    @Deprecated
    public boolean isProtobuf3() { return defaultSyntax.isProto3() || defaultSyntax.isAuto(); }

    public boolean isConvertWellKnownTypes() { return convertWellKnownTypes; }
    public boolean isGenerateRawProtoAccessors() { return generateRawProtoAccessors; }

    // Incremental generation getters
    public boolean isIncremental() { return incremental; }
    public Path getCacheDirectory() { return cacheDirectory; }
    public boolean isForceRegenerate() { return forceRegenerate; }

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

    public boolean shouldGenerate(String messageName) {
        if (!includedMessages.isEmpty() && !includedMessages.contains(messageName)) {
            return false;
        }
        return !excludedMessages.contains(messageName);
    }

    public String getCustomTypeMapping(String protoType) {
        return customTypeMappings.get(protoType);
    }

    public String getFieldNameOverride(String messageName, String fieldName) {
        return fieldNameOverrides.get(messageName + "." + fieldName);
    }

    /**
     * Compute hash of configuration for cache invalidation.
     * Changes in these settings invalidate the cache.
     *
     * @return 16-character hex string representing configuration hash
     */
    public String computeConfigHash() {
        StringBuilder sb = new StringBuilder();
        sb.append(apiPackage).append("|");
        sb.append(implPackagePattern).append("|");
        sb.append(protoPackagePattern).append("|");
        sb.append(abstractClassPackage).append("|");
        sb.append(generateInterfaces).append("|");
        sb.append(generateAbstractClasses).append("|");
        sb.append(generateImplClasses).append("|");
        sb.append(generateVersionContext).append("|");
        sb.append(generateBuilders).append("|");
        sb.append(includeVersionSuffix).append("|");
        sb.append(convertWellKnownTypes).append("|");
        sb.append(generateRawProtoAccessors).append("|");
        sb.append(defaultSyntax).append("|");
        // Include custom mappings
        sb.append(customTypeMappings.toString()).append("|");
        sb.append(fieldNameOverrides.toString()).append("|");
        // Include message filters
        sb.append(includedMessages.toString()).append("|");
        sb.append(excludedMessages.toString());

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hashCode (should never happen as SHA-256 is always available)
            return String.format("%016x", sb.toString().hashCode());
        }
    }

    public static class Builder {
        private final GeneratorConfig config = new GeneratorConfig();

        public Builder outputDirectory(Path path) {
            config.outputDirectory = path;
            return this;
        }

        public Builder apiPackage(String pkg) {
            config.apiPackage = pkg;
            return this;
        }

        public Builder implPackagePattern(String pattern) {
            config.implPackagePattern = pattern;
            return this;
        }

        public Builder protoPackagePattern(String pattern) {
            config.protoPackagePattern = pattern;
            return this;
        }

        public Builder generateInterfaces(boolean value) {
            config.generateInterfaces = value;
            return this;
        }

        public Builder generateAbstractClasses(boolean value) {
            config.generateAbstractClasses = value;
            return this;
        }

        public Builder generateImplClasses(boolean value) {
            config.generateImplClasses = value;
            return this;
        }

        public Builder generateVersionContext(boolean value) {
            config.generateVersionContext = value;
            return this;
        }

        public Builder includeVersionSuffix(boolean value) {
            config.includeVersionSuffix = value;
            return this;
        }

        public Builder generateBuilders(boolean value) {
            config.generateBuilders = value;
            return this;
        }

        public Builder defaultSyntax(ProtoSyntax syntax) {
            config.defaultSyntax = syntax;
            return this;
        }

        /**
         * @deprecated Use {@link #defaultSyntax(ProtoSyntax)} instead
         */
        @Deprecated
        public Builder protobufMajorVersion(int version) {
            if (version < 2 || version > 3) {
                throw new IllegalArgumentException("protobufMajorVersion must be 2 or 3, got: " + version);
            }
            config.defaultSyntax = ProtoSyntax.fromMajorVersion(version);
            return this;
        }

        public Builder convertWellKnownTypes(boolean value) {
            config.convertWellKnownTypes = value;
            return this;
        }

        public Builder generateRawProtoAccessors(boolean value) {
            config.generateRawProtoAccessors = value;
            return this;
        }

        public Builder includeMessage(String messageName) {
            config.includedMessages.add(messageName);
            return this;
        }

        public Builder excludeMessage(String messageName) {
            config.excludedMessages.add(messageName);
            return this;
        }

        public Builder customTypeMapping(String protoType, String javaType) {
            config.customTypeMappings.put(protoType, javaType);
            return this;
        }

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

        public GeneratorConfig build() {
            Objects.requireNonNull(config.outputDirectory, "Output directory is required");
            return config;
        }
    }
}
