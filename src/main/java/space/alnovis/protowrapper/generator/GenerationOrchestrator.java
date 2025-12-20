package space.alnovis.protowrapper.generator;

import space.alnovis.protowrapper.PluginLogger;
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates the code generation process.
 *
 * <p>This class coordinates the generation of all wrapper code types:
 * enums, interfaces, abstract classes, implementations, and version context.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, logger);
 * GenerationResult result = orchestrator.generate(mergedSchema, versionConfigs);
 * </pre>
 */
public class GenerationOrchestrator {

    private final GeneratorConfig config;
    private final PluginLogger logger;

    public GenerationOrchestrator(GeneratorConfig config, PluginLogger logger) {
        this.config = config;
        this.logger = logger != null ? logger : PluginLogger.console();
    }

    public GenerationOrchestrator(GeneratorConfig config) {
        this(config, PluginLogger.console());
    }

    /**
     * Generate all code for the merged schema.
     *
     * @param schema Merged schema
     * @param versionConfigs Version configurations for impl and version context generation
     * @param protoClassNameResolver Function to resolve proto class name for a message
     * @return Number of generated files
     * @throws IOException if generation fails
     */
    public int generateAll(MergedSchema schema,
                           List<VersionConfig> versionConfigs,
                           ProtoClassNameResolver protoClassNameResolver) throws IOException {
        int generatedFiles = 0;

        // Generate enums first (interfaces depend on them)
        generatedFiles += generateEnums(schema);

        if (config.isGenerateInterfaces()) {
            generatedFiles += generateInterfaces(schema);
        }

        if (config.isGenerateAbstractClasses()) {
            generatedFiles += generateAbstractClasses(schema);
        }

        if (config.isGenerateImplClasses()) {
            generatedFiles += generateImplClasses(schema, versionConfigs, protoClassNameResolver);
        }

        if (config.isGenerateVersionContext()) {
            generatedFiles += generateVersionContext(schema, versionConfigs, protoClassNameResolver);
        }

        logger.info("Generated " + generatedFiles + " files total");
        return generatedFiles;
    }

    /**
     * Generate enum classes.
     *
     * @param schema Merged schema
     * @return Number of generated files
     * @throws IOException if generation fails
     */
    public int generateEnums(MergedSchema schema) throws IOException {
        EnumGenerator generator = new EnumGenerator(config);

        try {
            long count = schema.getEnums().stream()
                    .map(enumInfo -> generateWithLogging(
                            () -> generator.generateAndWrite(enumInfo),
                            "Generated enum: "))
                    .count();

            logger.info("Generated " + count + " enums");
            return (int) count;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Generate interface classes.
     *
     * @param schema Merged schema
     * @return Number of generated files
     * @throws IOException if generation fails
     */
    public int generateInterfaces(MergedSchema schema) throws IOException {
        InterfaceGenerator generator = new InterfaceGenerator(config);
        GenerationContext ctx = GenerationContext.create(schema, config);

        try {
            long count = schema.getMessages().stream()
                    .filter(message -> config.shouldGenerate(message.getName()))
                    .map(message -> generateWithLogging(
                            () -> generator.generateAndWrite(message, ctx),
                            "Generated interface: "))
                    .count();

            logger.info("Generated " + count + " interfaces");
            return (int) count;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Generate abstract classes.
     *
     * @param schema Merged schema
     * @return Number of generated files
     * @throws IOException if generation fails
     */
    public int generateAbstractClasses(MergedSchema schema) throws IOException {
        AbstractClassGenerator generator = new AbstractClassGenerator(config);
        GenerationContext ctx = GenerationContext.create(schema, config);

        try {
            long count = schema.getMessages().stream()
                    .filter(message -> config.shouldGenerate(message.getName()))
                    .map(message -> generateWithLogging(
                            () -> generator.generateAndWrite(message, ctx),
                            "Generated abstract class: "))
                    .count();

            logger.info("Generated " + count + " abstract classes");
            return (int) count;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Generate implementation classes.
     *
     * @param schema Merged schema
     * @param versionConfigs Version configurations
     * @param protoClassNameResolver Resolver for proto class names
     * @return Number of generated files
     * @throws IOException if generation fails
     */
    public int generateImplClasses(MergedSchema schema,
                                    List<VersionConfig> versionConfigs,
                                    ProtoClassNameResolver protoClassNameResolver) throws IOException {
        ImplClassGenerator generator = new ImplClassGenerator(config);
        GenerationContext baseCtx = GenerationContext.create(schema, config);

        try {
            long count = versionConfigs.stream()
                    .flatMap(versionConfig -> {
                        String version = versionConfig.getVersionId();
                        GenerationContext ctx = baseCtx.withVersion(version);

                        return schema.getMessages().stream()
                                .filter(message -> config.shouldGenerate(message.getName()))
                                .filter(message -> {
                                    if (!message.getPresentInVersions().contains(version)) {
                                        logger.debug("Skipping " + message.getName() + " for " + version + " - not present");
                                        return false;
                                    }
                                    return true;
                                })
                                .map(message -> {
                                    String protoClassName = protoClassNameResolver.resolve(message, versionConfig);
                                    return generateWithLogging(
                                            () -> generator.generateAndWrite(message, protoClassName, ctx),
                                            "Generated impl class: ");
                                });
                    })
                    .count();

            logger.info("Generated " + count + " implementation classes");
            return (int) count;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Generate VersionContext interface and implementations.
     *
     * @param schema Merged schema
     * @param versionConfigs Version configurations
     * @param protoClassNameResolver Resolver for proto class names
     * @return Number of generated files
     * @throws IOException if generation fails
     */
    public int generateVersionContext(MergedSchema schema,
                                       List<VersionConfig> versionConfigs,
                                       ProtoClassNameResolver protoClassNameResolver) throws IOException {
        VersionContextGenerator generator = new VersionContextGenerator(config);

        try {
            // Generate interface
            generateWithLogging(
                    () -> generator.generateAndWriteInterface(schema),
                    "Generated VersionContext interface: ");

            // Generate implementations for each version
            long implCount = versionConfigs.stream()
                    .map(versionConfig -> {
                        Map<String, String> protoMappings = buildProtoMappings(schema, versionConfig, protoClassNameResolver);
                        return generateWithLogging(
                                () -> generator.generateAndWriteImpl(
                                        schema,
                                        versionConfig.getVersionId(),
                                        protoMappings),
                                "Generated VersionContext impl: ");
                    })
                    .count();

            int count = 1 + (int) implCount;
            logger.info("Generated " + count + " VersionContext files");
            return count;
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Build proto mappings for a specific version.
     */
    private Map<String, String> buildProtoMappings(MergedSchema schema,
                                                    VersionConfig versionConfig,
                                                    ProtoClassNameResolver protoClassNameResolver) {
        String version = versionConfig.getVersionId();

        return schema.getMessages().stream()
                .filter(message -> message.getPresentInVersions().contains(version))
                .collect(Collectors.toMap(
                        MergedMessage::getName,
                        message -> protoClassNameResolver.resolve(message, versionConfig),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    /**
     * Helper method to wrap IOException-throwing operations in streams.
     */
    private Path generateWithLogging(ThrowingSupplier<Path> generator, String logPrefix) {
        try {
            Path path = generator.get();
            logger.debug(logPrefix + path);
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Functional interface for suppliers that throw IOException.
     */
    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws IOException;
    }

    /**
     * Interface for version configuration needed by the orchestrator.
     */
    public interface VersionConfig {
        /**
         * Get the version identifier (e.g., "v1", "v2").
         */
        String getVersionId();
    }

    /**
     * Functional interface to resolve proto class names.
     */
    @FunctionalInterface
    public interface ProtoClassNameResolver {
        /**
         * Resolve the fully qualified proto class name for a message.
         *
         * @param message The message
         * @param versionConfig The version configuration
         * @return Fully qualified proto class name
         */
        String resolve(MergedMessage message, VersionConfig versionConfig);
    }
}
