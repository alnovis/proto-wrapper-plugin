package space.alnovis.protowrapper.generator;

import space.alnovis.protowrapper.PluginLogger;
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        int count = 0;

        for (MergedEnum enumInfo : schema.getEnums()) {
            Path path = generator.generateAndWrite(enumInfo);
            logger.debug("Generated enum: " + path);
            count++;
        }

        logger.info("Generated " + count + " enums");
        return count;
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
        int count = 0;

        for (MergedMessage message : schema.getMessages()) {
            if (!config.shouldGenerate(message.getName())) {
                continue;
            }

            Path path = generator.generateAndWrite(message, ctx);
            logger.debug("Generated interface: " + path);
            count++;
        }

        logger.info("Generated " + count + " interfaces");
        return count;
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
        int count = 0;

        for (MergedMessage message : schema.getMessages()) {
            if (!config.shouldGenerate(message.getName())) {
                continue;
            }

            Path path = generator.generateAndWrite(message, ctx);
            logger.debug("Generated abstract class: " + path);
            count++;
        }

        logger.info("Generated " + count + " abstract classes");
        return count;
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
        int count = 0;

        for (VersionConfig versionConfig : versionConfigs) {
            String version = versionConfig.getVersionId();
            GenerationContext ctx = baseCtx.withVersion(version);

            for (MergedMessage message : schema.getMessages()) {
                if (!config.shouldGenerate(message.getName())) {
                    continue;
                }

                // Skip if message is not present in this version
                if (!message.getPresentInVersions().contains(version)) {
                    logger.debug("Skipping " + message.getName() + " for " + version + " - not present");
                    continue;
                }

                String protoClassName = protoClassNameResolver.resolve(message, versionConfig);
                Path path = generator.generateAndWrite(message, protoClassName, ctx);
                logger.debug("Generated impl class: " + path);
                count++;
            }
        }

        logger.info("Generated " + count + " implementation classes");
        return count;
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
        int count = 0;

        // Generate interface
        Path interfacePath = generator.generateAndWriteInterface(schema);
        logger.debug("Generated VersionContext interface: " + interfacePath);
        count++;

        // Generate implementations for each version
        for (VersionConfig versionConfig : versionConfigs) {
            Map<String, String> protoMappings = buildProtoMappings(schema, versionConfig, protoClassNameResolver);

            Path implPath = generator.generateAndWriteImpl(
                    schema,
                    versionConfig.getVersionId(),
                    protoMappings
            );
            logger.debug("Generated VersionContext impl: " + implPath);
            count++;
        }

        logger.info("Generated " + count + " VersionContext files");
        return count;
    }

    /**
     * Build proto mappings for a specific version.
     */
    private Map<String, String> buildProtoMappings(MergedSchema schema,
                                                    VersionConfig versionConfig,
                                                    ProtoClassNameResolver protoClassNameResolver) {
        Map<String, String> mappings = new LinkedHashMap<>();
        String version = versionConfig.getVersionId();

        for (MergedMessage message : schema.getMessages()) {
            if (message.getPresentInVersions().contains(version)) {
                String protoClassName = protoClassNameResolver.resolve(message, versionConfig);
                mappings.put(message.getName(), protoClassName);
            }
        }

        return mappings;
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
