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
import space.alnovis.protowrapper.model.MergedEnum;
import space.alnovis.protowrapper.model.MergedMessage;
import space.alnovis.protowrapper.model.MergedSchema;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

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
     * Path to protoc executable. If not set, uses 'protoc' from PATH.
     */
    @Parameter(property = "protoc.path")
    private String protocPath;

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

        // Check protoc availability
        if (!protocExecutor.isProtocAvailable()) {
            throw new MojoExecutionException(
                "protoc not found. Please install protobuf compiler or set protocPath parameter.");
        }
        getLog().info("Using " + protocExecutor.getProtocVersion());

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

            // Configure generators
            GeneratorConfig generatorConfig = buildGeneratorConfig();

            // Use orchestrator for generation
            GenerationOrchestrator orchestrator = new GenerationOrchestrator(
                    generatorConfig, MavenLogger.from(getLog()));

            int generatedFiles = orchestrator.generateAll(
                    mergedSchema,
                    new ArrayList<>(versions),
                    this::getProtoClassName
            );

            getLog().info("Generated " + generatedFiles + " files in " + outputDirectory);

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
                .includeVersionSuffix(includeVersionSuffix);

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
}
