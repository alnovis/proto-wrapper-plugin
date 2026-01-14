package space.alnovis.protowrapper.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import space.alnovis.protowrapper.analyzer.ProtoAnalyzer;
import space.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import space.alnovis.protowrapper.analyzer.ProtocExecutor;
import space.alnovis.protowrapper.generator.*;
import space.alnovis.protowrapper.merger.VersionMerger;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;
import space.alnovis.protowrapper.model.ProtoSyntax;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maven goal that generates version-agnostic wrapper classes from protobuf schemas.
 *
 * <p>Simplified usage (recommended):</p>
 * <pre>
 * &lt;plugin&gt;
 *     &lt;groupId&gt;space.alnovis&lt;/groupId&gt;
 *     &lt;artifactId&gt;proto-wrapper-maven-plugin&lt;/artifactId&gt;
 *     &lt;configuration&gt;
 *         &lt;basePackage&gt;com.example.model&lt;/basePackage&gt;
 *         &lt;protoPackagePattern&gt;com.example.proto.{version}&lt;/protoPackagePattern&gt;
 *         &lt;protoRoot&gt;${basedir}/../proto&lt;/protoRoot&gt;
 *         &lt;versions&gt;
 *             &lt;version&gt;
 *                 &lt;protoDir&gt;v1&lt;/protoDir&gt;
 *             &lt;/version&gt;
 *             &lt;version&gt;
 *                 &lt;protoDir&gt;v2&lt;/protoDir&gt;
 *             &lt;/version&gt;
 *         &lt;/versions&gt;
 *     &lt;/configuration&gt;
 * &lt;/plugin&gt;
 * </pre>
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMojo extends AbstractMojo {

    /**
     * Root directory containing all proto version directories.
     * This is used as the base for imports and for resolving relative protoDir paths.
     * Example: ${basedir}/../proto/jabba
     */
    @Parameter(required = true)
    private File protoRoot;

    /**
     * List of proto version configurations.
     * Each version needs only a protoDir (relative to protoRoot).
     */
    @Parameter(required = true)
    private List<ProtoWrapperConfig> versions;

    /**
     * Output directory for generated sources.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/proto-wrapper")
    private File outputDirectory;

    /**
     * Directory for temporary files (descriptors).
     */
    @Parameter(defaultValue = "${project.build.directory}/proto-wrapper-tmp")
    private File tempDirectory;

    /**
     * Base package for all generated classes.
     * If set, derives other packages automatically:
     * - apiPackage = basePackage + ".api"
     * - implPackagePattern = basePackage + ".{version}"
     *
     * Individual package settings override derived values.
     */
    @Parameter
    private String basePackage;

    /**
     * Package for version-agnostic API interfaces.
     * If not set and basePackage is set, derived as: basePackage + ".api"
     */
    @Parameter
    private String apiPackage;

    /**
     * Pattern for implementation packages. Use {version} as placeholder.
     * If not set and basePackage is set, derived as: basePackage + ".{version}"
     */
    @Parameter
    private String implPackagePattern;

    /**
     * Pattern for proto packages. Use {version} as placeholder.
     * This should match the java_package option in your proto files.
     * Example: com.example.proto.{version}
     */
    @Parameter(required = true)
    private String protoPackagePattern;

    /**
     * Path to protoc executable. If not set, protoc is resolved automatically:
     * <ol>
     *   <li>System PATH (if protoc is installed)</li>
     *   <li>Embedded (downloaded from Maven Central)</li>
     * </ol>
     */
    @Parameter(property = "protoc.path")
    private String protocPath;

    /**
     * Version of protoc to use for embedded downloads.
     * Only affects embedded protoc. Ignored if custom protocPath is set
     * or system protoc is found.
     *
     * <p>If not specified, uses the protobuf version that the plugin was built with.</p>
     *
     * @since 1.6.5
     */
    @Parameter(property = "protoc.version")
    private String protocVersion;

    /**
     * Whether to generate interface files.
     */
    @Parameter(defaultValue = "true")
    private boolean generateInterfaces;

    /**
     * Whether to generate abstract class files.
     */
    @Parameter(defaultValue = "true")
    private boolean generateAbstractClasses;

    /**
     * Whether to generate version-specific implementation files.
     */
    @Parameter(defaultValue = "true")
    private boolean generateImplClasses;

    /**
     * Whether to generate VersionContext.
     */
    @Parameter(defaultValue = "true")
    private boolean generateVersionContext;

    /**
     * Whether to include version suffix in implementation class names.
     * If true (default for backward compatibility): MoneyV1, DateTimeV2
     * If false: Money, DateTime (version is determined by package only)
     */
    @Parameter(defaultValue = "true")
    private boolean includeVersionSuffix;

    /**
     * Whether to generate Builder interfaces and implementations.
     * If true: generates toBuilder() method and Builder nested interface
     * If false (default): generates read-only wrappers only
     */
    @Parameter(defaultValue = "false")
    private boolean generateBuilders;

    /**
     * Major version of protobuf library used in the project.
     * Set to 2 for protobuf 2.x projects (uses valueOf() for enum conversion).
     * Set to 3 for protobuf 3.x projects (uses forNumber() for enum conversion).
     * Default is 3.
     */
    @Parameter(defaultValue = "3")
    private int protobufMajorVersion;

    /**
     * Whether to convert Google Well-Known Types to Java types.
     * If true (default): google.protobuf.Timestamp -> java.time.Instant, etc.
     * If false: keep proto types as-is.
     *
     * @since 1.3.0
     */
    @Parameter(defaultValue = "true")
    private boolean convertWellKnownTypes;

    /**
     * Whether to generate additional raw proto accessors for well-known types.
     * If true: generates getXxxProto() methods returning the original proto type.
     * If false (default): only Java type accessors are generated.
     *
     * @since 1.3.0
     */
    @Parameter(defaultValue = "false")
    private boolean generateRawProtoAccessors;

    /**
     * Messages to include (empty = all).
     */
    @Parameter
    private List<String> includeMessages;

    /**
     * Messages to exclude.
     */
    @Parameter
    private List<String> excludeMessages;

    /**
     * Skip plugin execution.
     */
    @Parameter(property = "proto-wrapper.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Enable incremental generation.
     * When enabled, only regenerates when proto files change.
     * Default: true
     *
     * @since 1.6.0
     */
    @Parameter(property = "proto-wrapper.incremental", defaultValue = "true")
    private boolean incremental;

    /**
     * Directory for incremental generation cache.
     * Stores state between builds to detect changes.
     *
     * @since 1.6.0
     */
    @Parameter(property = "proto-wrapper.cacheDirectory",
               defaultValue = "${project.build.directory}/proto-wrapper-cache")
    private File cacheDirectory;

    /**
     * Force full regeneration, ignoring cache.
     * Useful when you want to regenerate all files regardless of changes.
     * Can be set via command line: -Dproto-wrapper.force=true
     *
     * @since 1.6.0
     */
    @Parameter(property = "proto-wrapper.force", defaultValue = "false")
    private boolean forceRegenerate;

    /**
     * Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    private ProtocExecutor protocExecutor;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Proto Wrapper generation skipped");
            return;
        }

        getLog().info("Proto Wrapper Generator starting...");

        // Initialize packages from basePackage if needed
        initializePackages();

        // Initialize protoc executor
        protocExecutor = new ProtocExecutor(MavenLogger.from(getLog()));
        if (protocPath != null && !protocPath.isEmpty()) {
            protocExecutor.setProtocPath(protocPath);
        }
        if (protocVersion != null && !protocVersion.isEmpty()) {
            protocExecutor.setProtocVersion(protocVersion);
        }

        // Check protoc availability (will auto-download embedded if needed)
        if (!protocExecutor.isProtocAvailable()) {
            throw new MojoExecutionException(
                "protoc not available. Failed to resolve or download protoc.");
        }
        getLog().info("Using " + protocExecutor.queryInstalledProtocVersion());

        // Validate and resolve configuration
        resolveAndValidateConfig();

        // Create directories
        createDirectories();

        try {
            // Process each version
            List<VersionSchema> schemas = new ArrayList<>();

            for (ProtoWrapperConfig versionConfig : versions) {
                VersionSchema schema = processVersion(versionConfig);
                schemas.add(schema);
            }

            // Merge schemas
            getLog().info("Merging " + schemas.size() + " schemas...");
            VersionMerger merger = new VersionMerger(MavenLogger.from(getLog()));
            MergedSchema mergedSchema = merger.merge(schemas);

            getLog().info("Merged schema: " + mergedSchema.getMessages().size() + " messages, " +
                    mergedSchema.getEnums().size() + " enums");

            // Log conflict statistics
            logConflictStatistics(mergedSchema);

            // Configure generators
            GeneratorConfig generatorConfig = buildGeneratorConfig();

            // Collect all proto files for incremental generation
            Set<Path> allProtoFiles = collectAllProtoFiles();

            // Use orchestrator for generation
            GenerationOrchestrator orchestrator = new GenerationOrchestrator(
                    generatorConfig, MavenLogger.from(getLog()));

            int generatedFiles = orchestrator.generateAllIncremental(
                    mergedSchema,
                    new ArrayList<>(versions),
                    this::getProtoClassName,
                    allProtoFiles,
                    protoRoot.toPath()
            );

            if (generatedFiles == 0) {
                getLog().info("No changes detected, generation skipped");
            } else {
                getLog().info("Generated " + generatedFiles + " files in " + outputDirectory);
            }

            // Add generated sources to compile path
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate code", e);
        }
    }

    /**
     * Resolve version configurations and validate.
     * This resolves relative protoDir paths to absolute paths and validates existence.
     */
    private void resolveAndValidateConfig() throws MojoExecutionException {
        if (versions == null || versions.isEmpty()) {
            throw new MojoExecutionException("At least one version configuration is required");
        }

        if (protoRoot == null || !protoRoot.isDirectory()) {
            throw new MojoExecutionException("protoRoot is required and must be an existing directory: " + protoRoot);
        }

        getLog().info("Proto root: " + protoRoot.getAbsolutePath());

        for (ProtoWrapperConfig config : versions) {
            if (config.getProtoDir() == null || config.getProtoDir().isEmpty()) {
                throw new MojoExecutionException("protoDir is required for each version");
            }

            // Resolve protoDir (can be relative to protoRoot or absolute)
            File protoDir = new File(config.getProtoDir());
            if (!protoDir.isAbsolute()) {
                protoDir = new File(protoRoot, config.getProtoDir());
            }

            if (!protoDir.isDirectory()) {
                throw new MojoExecutionException(
                    "protoDir does not exist: " + protoDir.getAbsolutePath() +
                    " (resolved from: " + config.getProtoDir() + ")");
            }

            config.setResolvedProtoDir(protoDir);

            // Set up descriptor file path
            String versionId = config.getVersionId();
            File descriptorFile = new File(tempDirectory, versionId + "-descriptor.pb");
            config.setGeneratedDescriptorFile(descriptorFile);

            getLog().info("Version " + config.getEffectiveName() + ":");
            getLog().info("  protoDir: " + protoDir.getAbsolutePath());
        }
    }

    private VersionSchema processVersion(ProtoWrapperConfig versionConfig) throws IOException, MojoExecutionException {
        String version = versionConfig.getVersionId();
        String versionName = versionConfig.getEffectiveName();
        getLog().info("Processing " + versionName + "...");

        File protoDir = versionConfig.getResolvedProtoDir();
        Path descriptorFile = versionConfig.getGeneratedDescriptorFile().toPath();

        // Generate descriptor from proto files in this directory only
        getLog().info("  Generating descriptor from " + protoDir);

        String[] excludePatterns = versionConfig.getExcludeProtos();
        if (excludePatterns != null && excludePatterns.length > 0) {
            getLog().info("  Excluding patterns: " + String.join(", ", excludePatterns));
        }

        protocExecutor.generateDescriptor(
            protoDir.toPath(),
            descriptorFile,
            excludePatterns,
            protoRoot.toPath()  // Include path for imports
        );

        // Analyze descriptor - filter by source directory to exclude imported files from other directories
        ProtoAnalyzer analyzer = new ProtoAnalyzer();
        String sourcePrefix = versionConfig.getProtoDir() + "/";  // e.g., "v1/"
        VersionSchema schema = analyzer.analyze(descriptorFile, version, sourcePrefix);
        getLog().info("  " + schema.getStats());

        // Auto-generate proto mappings
        String protoPackage = protoPackagePattern.replace("{version}", version);
        versionConfig.setDetectedProtoPackage(protoPackage);

        Map<String, String> autoMappings = protocExecutor.buildProtoMappings(
            protoDir.toPath(),
            protoPackage,
            excludePatterns
        );

        getLog().info("  Proto mappings: " + autoMappings.size() + " auto-generated");

        return schema;
    }

    /**
     * Initialize package settings from basePackage if not explicitly set.
     */
    private void initializePackages() {
        // Default values if nothing is set
        String defaultApiPackage = "com.example.model.api";
        String defaultImplPattern = "com.example.model.{version}";

        if (basePackage != null && !basePackage.isEmpty()) {
            // Derive packages from basePackage
            if (apiPackage == null || apiPackage.isEmpty()) {
                apiPackage = basePackage + ".api";
            }
            if (implPackagePattern == null || implPackagePattern.isEmpty()) {
                implPackagePattern = basePackage + ".{version}";
            }
            getLog().info("Using basePackage: " + basePackage);
        } else {
            // Use defaults if not set
            if (apiPackage == null || apiPackage.isEmpty()) {
                apiPackage = defaultApiPackage;
            }
            if (implPackagePattern == null || implPackagePattern.isEmpty()) {
                implPackagePattern = defaultImplPattern;
            }
        }

        getLog().info("Package configuration:");
        getLog().info("  apiPackage: " + apiPackage);
        getLog().info("  implPackagePattern: " + implPackagePattern);
        getLog().info("  protoPackagePattern: " + protoPackagePattern);
    }

    private void createDirectories() throws MojoExecutionException {
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new MojoExecutionException("Failed to create output directory: " + outputDirectory);
        }
        if (!tempDirectory.exists() && !tempDirectory.mkdirs()) {
            throw new MojoExecutionException("Failed to create temp directory: " + tempDirectory);
        }
    }

    private GeneratorConfig buildGeneratorConfig() {
        GeneratorConfig.Builder builder = GeneratorConfig.builder()
                .outputDirectory(outputDirectory.toPath())
                .apiPackage(apiPackage)
                .implPackagePattern(implPackagePattern)
                .protoPackagePattern(protoPackagePattern)
                .generateInterfaces(generateInterfaces)
                .generateAbstractClasses(generateAbstractClasses)
                .generateImplClasses(generateImplClasses)
                .generateVersionContext(generateVersionContext)
                .includeVersionSuffix(includeVersionSuffix)
                .generateBuilders(generateBuilders)
                .defaultSyntax(protobufMajorVersion == 2 ? ProtoSyntax.PROTO2 : ProtoSyntax.PROTO3)
                .convertWellKnownTypes(convertWellKnownTypes)
                .generateRawProtoAccessors(generateRawProtoAccessors)
                // Incremental generation settings
                .incremental(incremental)
                .cacheDirectory(cacheDirectory != null ? cacheDirectory.toPath() : null)
                .forceRegenerate(forceRegenerate);

        if (includeMessages != null) {
            for (String msg : includeMessages) {
                builder.includeMessage(msg);
            }
        }

        if (excludeMessages != null) {
            for (String msg : excludeMessages) {
                builder.excludeMessage(msg);
            }
        }

        return builder.build();
    }

    /**
     * Get proto class name for a message.
     * Used by GenerationOrchestrator via method reference.
     */
    private String getProtoClassName(MergedMessage message, GenerationOrchestrator.VersionConfig versionConfig) {
        ProtoWrapperConfig config = (ProtoWrapperConfig) versionConfig;
        String version = config.getVersionId();
        String protoPackage = config.getDetectedProtoPackage();
        if (protoPackage == null) {
            protoPackage = protoPackagePattern.replace("{version}", version);
        }

        // Get outer class name from source file (protobuf generates nested classes
        // inside outer class when java_multiple_files is not set)
        String outerClassName = message.getOuterClassName(version);
        if (outerClassName != null) {
            String protoClassName = protoPackage + "." + outerClassName + "." + message.getName();
            getLog().debug("Using outer class for " + message.getName() + ": " + outerClassName);
            return protoClassName;
        } else {
            // Fallback for messages without source file info (multiple files mode)
            return protoPackage + "." + message.getName();
        }
    }

    /**
     * Log statistics about type conflicts in the merged schema.
     */
    private void logConflictStatistics(MergedSchema schema) {
        Map<MergedField.ConflictType, Integer> conflictCounts = new EnumMap<>(MergedField.ConflictType.class);
        Map<MergedField.ConflictType, List<String>> conflictExamples = new EnumMap<>(MergedField.ConflictType.class);

        // Count conflicts across all messages (including nested)
        for (MergedMessage message : schema.getMessages()) {
            collectConflicts(message, conflictCounts, conflictExamples);
        }

        // Remove NONE type if present
        conflictCounts.remove(MergedField.ConflictType.NONE);
        conflictExamples.remove(MergedField.ConflictType.NONE);

        if (conflictCounts.isEmpty()) {
            getLog().info("No type conflicts detected");
            return;
        }

        // Log summary
        StringBuilder summary = new StringBuilder("Type conflicts: ");
        List<String> parts = new ArrayList<>();
        for (Map.Entry<MergedField.ConflictType, Integer> entry : conflictCounts.entrySet()) {
            MergedField.ConflictType type = entry.getKey();
            int count = entry.getValue();
            List<String> examples = conflictExamples.get(type);
            String exampleStr = examples.size() <= 2
                    ? String.join(", ", examples)
                    : examples.get(0) + ", " + examples.get(1) + ", ...";
            parts.add(count + " " + type.name() + " (" + exampleStr + ")");
        }
        summary.append(String.join("; ", parts));
        getLog().info(summary.toString());
    }

    private void collectConflicts(MergedMessage message,
                                   Map<MergedField.ConflictType, Integer> counts,
                                   Map<MergedField.ConflictType, List<String>> examples) {
        for (MergedField field : message.getFieldsSorted()) {
            MergedField.ConflictType conflictType = field.getConflictType();
            if (conflictType != null && conflictType != MergedField.ConflictType.NONE) {
                counts.merge(conflictType, 1, Integer::sum);
                examples.computeIfAbsent(conflictType, k -> new ArrayList<>())
                        .add(message.getName() + "." + field.getName());
            }
        }

        // Recurse into nested messages
        for (MergedMessage nested : message.getNestedMessages()) {
            collectConflicts(nested, counts, examples);
        }
    }

    /**
     * Collect all proto files from all version directories.
     *
     * @return set of all proto file paths (absolute)
     * @throws MojoExecutionException if file collection fails
     */
    private Set<Path> collectAllProtoFiles() throws MojoExecutionException {
        Set<Path> allFiles = new HashSet<>();

        for (ProtoWrapperConfig versionConfig : versions) {
            File protoDir = versionConfig.getResolvedProtoDir();
            if (protoDir == null || !protoDir.isDirectory()) {
                continue;
            }

            try (Stream<Path> stream = Files.walk(protoDir.toPath())) {
                Set<Path> versionFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".proto"))
                    .filter(p -> !isExcluded(p, versionConfig.getExcludeProtos()))
                    .collect(Collectors.toSet());
                allFiles.addAll(versionFiles);
            } catch (IOException e) {
                throw new MojoExecutionException(
                    "Failed to collect proto files from " + protoDir, e);
            }
        }

        getLog().debug("Collected " + allFiles.size() + " proto files for incremental check");
        return allFiles;
    }

    /**
     * Check if a proto file matches any exclude pattern.
     */
    private boolean isExcluded(Path protoFile, String[] excludePatterns) {
        if (excludePatterns == null || excludePatterns.length == 0) {
            return false;
        }

        String fileName = protoFile.getFileName().toString();
        String relativePath = protoFile.toString();

        for (String pattern : excludePatterns) {
            // Simple glob matching: * matches any sequence of characters
            String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");
            if (fileName.matches(regex) || relativePath.matches(".*" + regex)) {
                return true;
            }
        }

        return false;
    }
}
