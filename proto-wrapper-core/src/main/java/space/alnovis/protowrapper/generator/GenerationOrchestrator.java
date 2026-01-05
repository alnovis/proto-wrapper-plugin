package space.alnovis.protowrapper.generator;

import com.squareup.javapoet.JavaFile;
import space.alnovis.protowrapper.PluginLogger;
import space.alnovis.protowrapper.generator.wellknown.StructConverterGenerator;
import space.alnovis.protowrapper.generator.wellknown.WellKnownTypeInfo;
import space.alnovis.protowrapper.incremental.ChangeDetector;
import space.alnovis.protowrapper.incremental.IncrementalStateManager;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /**
     * Plugin version used for cache invalidation.
     * Update this when making changes that affect generated output.
     */
    private static final String PLUGIN_VERSION = "1.6.0";

    private final GeneratorConfig config;
    private final PluginLogger logger;
    private IncrementalStateManager stateManager;

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

        // Generate conflict enums for INT_ENUM type conflicts
        // Always generate if there are conflicts - interfaces reference them via getXxxEnum()
        if (!schema.getConflictEnums().isEmpty()) {
            generatedFiles += generateConflictEnums(schema);
        }

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

        // Generate utility classes (StructConverter) if needed
        if (config.isConvertWellKnownTypes()) {
            generatedFiles += generateUtilityClasses(schema);
        }

        logger.info("Generated " + generatedFiles + " files total");
        return generatedFiles;
    }

    /**
     * Generate all code with incremental support.
     *
     * <p>This method checks if proto files have changed since the last generation.
     * If no changes are detected, generation is skipped. If changes are detected
     * or cache is invalid, full generation is performed.</p>
     *
     * @param schema Merged schema
     * @param versionConfigs Version configurations
     * @param protoClassNameResolver Function to resolve proto class name
     * @param protoFiles Set of all proto file paths (absolute)
     * @param protoRoot Root directory for proto files
     * @return Number of generated files (0 if skipped due to no changes)
     * @throws IOException if generation fails
     */
    public int generateAllIncremental(MergedSchema schema,
                                       List<VersionConfig> versionConfigs,
                                       ProtoClassNameResolver protoClassNameResolver,
                                       Set<Path> protoFiles,
                                       Path protoRoot) throws IOException {

        // If incremental is disabled or force regenerate, use full generation
        if (!config.isIncremental() || config.isForceRegenerate()) {
            if (config.isForceRegenerate()) {
                logger.info("Force regenerate requested, performing full generation");
            }
            int count = generateAll(schema, versionConfigs, protoClassNameResolver);
            saveIncrementalState(protoFiles, protoRoot);
            return count;
        }

        // Determine cache directory
        Path cacheDirectory = config.getCacheDirectory();
        if (cacheDirectory == null) {
            cacheDirectory = config.getOutputDirectory().resolve(".proto-wrapper-cache");
        }

        // Initialize state manager
        stateManager = new IncrementalStateManager(
            cacheDirectory,
            protoRoot,
            PLUGIN_VERSION,
            config.computeConfigHash(),
            logger
        );

        // Load previous state
        stateManager.loadPreviousState();

        // Check for full cache invalidation
        if (stateManager.shouldInvalidateCache()) {
            String reason = stateManager.getInvalidationReason();
            logger.info("Cache invalidated: " + reason);
            stateManager.invalidateCache();
            int count = generateAll(schema, versionConfigs, protoClassNameResolver);
            saveStateAfterGeneration(protoFiles);
            return count;
        }

        // Analyze changes
        ChangeDetector.ChangeResult changes = stateManager.analyzeChanges(protoFiles);

        if (!changes.hasChanges()) {
            logger.info("No proto file changes detected, skipping generation");
            return 0;
        }

        // Log change details
        logChangeDetails(changes);

        // For now, perform full generation when any changes detected
        // Future enhancement: selective regeneration of affected messages only
        int count = generateAll(schema, versionConfigs, protoClassNameResolver);

        // Save new state
        saveStateAfterGeneration(protoFiles);

        return count;
    }

    /**
     * Log details about detected changes.
     */
    private void logChangeDetails(ChangeDetector.ChangeResult changes) {
        int total = changes.totalChanges();
        logger.info("Detected " + total + " proto file change(s)");

        if (!changes.added().isEmpty()) {
            logger.debug("  Added: " + changes.added());
        }
        if (!changes.modified().isEmpty()) {
            logger.debug("  Modified: " + changes.modified());
        }
        if (!changes.deleted().isEmpty()) {
            logger.debug("  Deleted: " + changes.deleted());
        }
    }

    /**
     * Save incremental state after generation (when incremental is disabled).
     */
    private void saveIncrementalState(Set<Path> protoFiles, Path protoRoot) {
        if (protoFiles == null || protoFiles.isEmpty()) {
            return;
        }

        try {
            Path cacheDirectory = config.getCacheDirectory();
            if (cacheDirectory == null) {
                cacheDirectory = config.getOutputDirectory().resolve(".proto-wrapper-cache");
            }

            IncrementalStateManager manager = new IncrementalStateManager(
                cacheDirectory,
                protoRoot,
                PLUGIN_VERSION,
                config.computeConfigHash(),
                logger
            );
            manager.loadPreviousState();
            manager.analyzeChanges(protoFiles);
            manager.saveCurrentState();
            logger.debug("Saved incremental state for " + protoFiles.size() + " proto files");
        } catch (IOException e) {
            logger.warn("Failed to save incremental state: " + e.getMessage());
        }
    }

    /**
     * Save state after generation when using incremental mode.
     */
    private void saveStateAfterGeneration(Set<Path> protoFiles) {
        if (stateManager == null) {
            return;
        }

        try {
            // Re-analyze to capture current fingerprints if not already done
            if (stateManager.getChangeResult() == null) {
                stateManager.analyzeChanges(protoFiles);
            }
            stateManager.saveCurrentState();
            logger.debug("Saved incremental state");
        } catch (IOException e) {
            logger.warn("Failed to save incremental state: " + e.getMessage());
        }
    }

    /**
     * Get the plugin version used for cache invalidation.
     *
     * @return plugin version string
     */
    public static String getPluginVersion() {
        return PLUGIN_VERSION;
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
            // Note: Using forEach instead of map().count() because Java 9+ optimizes
            // count() to skip intermediate operations for sized streams
            int[] count = {0};
            schema.getEnums().forEach(enumInfo -> {
                generateWithLogging(
                        () -> generator.generateAndWrite(enumInfo),
                        "Generated enum: ");
                count[0]++;
            });

            logger.info("Generated " + count[0] + " enums");
            return count[0];
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Generate conflict enum classes for INT_ENUM type conflicts.
     *
     * @param schema Merged schema
     * @return Number of generated files
     * @throws IOException if generation fails
     */
    public int generateConflictEnums(MergedSchema schema) throws IOException {
        ConflictEnumGenerator generator = new ConflictEnumGenerator(config);

        try {
            int[] count = {0};
            schema.getConflictEnums().forEach(enumInfo -> {
                generateWithLogging(
                        () -> generator.generateAndWrite(enumInfo),
                        "Generated conflict enum: ");
                count[0]++;
            });

            if (count[0] > 0) {
                logger.info("Generated " + count[0] + " conflict enums for INT_ENUM type conflicts");
            }
            return count[0];
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
            // Note: forEach used instead of count() to ensure map() is executed
            // (count() may optimize away intermediate operations in Java 9+)
            int[] implCount = {0};
            versionConfigs.forEach(versionConfig -> {
                Map<String, String> protoMappings = buildProtoMappings(schema, versionConfig, protoClassNameResolver);
                generateWithLogging(
                        () -> generator.generateAndWriteImpl(
                                schema,
                                versionConfig.getVersionId(),
                                protoMappings),
                        "Generated VersionContext impl: ");
                implCount[0]++;
            });

            int count = 1 + implCount[0];
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
     * Generate utility classes like StructConverter if needed.
     *
     * @param schema Merged schema
     * @return Number of generated files
     * @throws IOException if generation fails
     */
    public int generateUtilityClasses(MergedSchema schema) throws IOException {
        // Check if any field requires StructConverter
        boolean needsStructConverter = requiresStructConverter(schema);

        if (!needsStructConverter) {
            logger.debug("No Struct/Value/ListValue fields found, skipping StructConverter generation");
            return 0;
        }

        // Generate StructConverter
        JavaFile structConverterFile = StructConverterGenerator.generate(config.getApiPackage());
        Path outputPath = structConverterFile.writeToPath(config.getOutputDirectory());
        logger.info("Generated utility class: " + outputPath);

        return 1;
    }

    /**
     * Check if any field in the schema requires StructConverter.
     */
    private boolean requiresStructConverter(MergedSchema schema) {
        return schema.getMessages().stream()
                .anyMatch(this::messageRequiresStructConverter);
    }

    /**
     * Check if a message or its nested messages require StructConverter.
     */
    private boolean messageRequiresStructConverter(MergedMessage message) {
        // Check fields of this message
        for (MergedField field : message.getFieldsSorted()) {
            if (field.isWellKnownType()) {
                WellKnownTypeInfo wkt = field.getWellKnownType();
                if (wkt != null && wkt.requiresUtilityClass()) {
                    return true;
                }
            }
        }

        // Check nested messages recursively
        for (MergedMessage nested : message.getNestedMessages()) {
            if (messageRequiresStructConverter(nested)) {
                return true;
            }
        }

        return false;
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
