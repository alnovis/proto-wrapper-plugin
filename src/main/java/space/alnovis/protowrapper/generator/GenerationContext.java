package space.alnovis.protowrapper.generator;

import space.alnovis.protowrapper.model.MergedSchema;

/**
 * Immutable context for code generation.
 *
 * <p>Encapsulates all dependencies needed during generation, eliminating
 * mutable state and the need for setter methods like setSchema().</p>
 *
 * <p>Usage:</p>
 * <pre>
 * GenerationContext ctx = GenerationContext.forVersion(schema, config, "v1");
 * implGenerator.generate(message, ctx);
 * </pre>
 *
 * <p>For version-agnostic generation (interfaces, abstract classes):</p>
 * <pre>
 * GenerationContext ctx = GenerationContext.create(schema, config);
 * interfaceGenerator.generate(message, ctx);
 * </pre>
 */
public final class GenerationContext {

    private final MergedSchema schema;
    private final GeneratorConfig config;
    private final TypeResolver typeResolver;
    private final String currentVersion; // null for version-agnostic generation

    private GenerationContext(MergedSchema schema, GeneratorConfig config, String currentVersion) {
        this.schema = schema;
        this.config = config;
        this.typeResolver = new TypeResolver(config, schema);
        this.currentVersion = currentVersion;
    }

    /**
     * Create a version-agnostic context for generating interfaces and abstract classes.
     *
     * @param schema Merged schema
     * @param config Generator config
     * @return GenerationContext without version
     */
    public static GenerationContext create(MergedSchema schema, GeneratorConfig config) {
        return new GenerationContext(schema, config, null);
    }

    /**
     * Create a version-specific context for generating implementation classes.
     *
     * @param schema Merged schema
     * @param config Generator config
     * @param version Version string (e.g., "v1")
     * @return GenerationContext with version
     */
    public static GenerationContext forVersion(MergedSchema schema, GeneratorConfig config, String version) {
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
        return new GenerationContext(schema, config, version);
    }

    /**
     * Create a new context with a different version.
     * Useful when iterating over versions.
     *
     * @param version New version string
     * @return New context with the specified version
     */
    public GenerationContext withVersion(String version) {
        return new GenerationContext(this.schema, this.config, version);
    }

    /**
     * Get the merged schema.
     * @return MergedSchema
     */
    public MergedSchema getSchema() {
        return schema;
    }

    /**
     * Get the generator config.
     * @return GeneratorConfig
     */
    public GeneratorConfig getConfig() {
        return config;
    }

    /**
     * Get the type resolver.
     * @return TypeResolver configured for this context
     */
    public TypeResolver getTypeResolver() {
        return typeResolver;
    }

    /**
     * Get the current version for version-specific generation.
     *
     * @return Version string or null if version-agnostic
     */
    public String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Check if this is a version-specific context.
     *
     * @return true if a version is set
     */
    public boolean hasVersion() {
        return currentVersion != null;
    }

    /**
     * Get the current version, throwing if not set.
     * Use this in version-specific generators.
     *
     * @return Version string
     * @throws IllegalStateException if version is not set
     */
    public String requireVersion() {
        if (currentVersion == null) {
            throw new IllegalStateException("Version is required but not set in this context");
        }
        return currentVersion;
    }

    /**
     * Get the API package.
     * Convenience method delegating to config.
     *
     * @return API package name
     */
    public String getApiPackage() {
        return config.getApiPackage();
    }

    /**
     * Get the implementation package for the current version.
     * Requires version to be set.
     *
     * @return Implementation package name
     * @throws IllegalStateException if version is not set
     */
    public String getImplPackage() {
        return config.getImplPackage(requireVersion());
    }

    /**
     * Get the implementation class name for a message.
     *
     * @param messageName Message name
     * @return Implementation class name
     * @throws IllegalStateException if version is not set
     */
    public String getImplClassName(String messageName) {
        return config.getImplClassName(messageName, requireVersion());
    }

    /**
     * Extract version number from version string.
     * E.g., "v1" -> 1
     *
     * @return Version number
     * @throws IllegalStateException if version is not set
     */
    public int getVersionNumber() {
        return typeResolver.extractVersionNumber(requireVersion());
    }
}
