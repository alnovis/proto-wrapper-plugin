package io.alnovis.protowrapper.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer;
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import io.alnovis.protowrapper.analyzer.ProtocExecutor;
import io.alnovis.protowrapper.diff.SchemaDiff;
import io.alnovis.protowrapper.diff.formatter.DiffFormatter;
import io.alnovis.protowrapper.diff.formatter.JsonDiffFormatter;
import io.alnovis.protowrapper.diff.formatter.MarkdownDiffFormatter;
import io.alnovis.protowrapper.diff.formatter.TextDiffFormatter;
import io.alnovis.protowrapper.diff.model.BreakingChange;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Maven goal that compares two protobuf schema versions and generates a diff report.
 *
 * <p>This goal can be used to:</p>
 * <ul>
 *   <li>Detect breaking changes between schema versions</li>
 *   <li>Generate migration reports</li>
 *   <li>Validate backward compatibility in CI/CD pipelines</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>
 * # Basic usage
 * mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2
 *
 * # With output format
 * mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2 -Dformat=markdown
 *
 * # Write to file
 * mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2 -Doutput=diff-report.md
 *
 * # Fail on breaking changes (CI/CD)
 * mvn proto-wrapper:diff -Dv1=proto/v1 -Dv2=proto/v2 -DfailOnBreaking=true
 * </pre>
 */
@Mojo(name = "diff", requiresProject = false)
public class DiffMojo extends AbstractMojo {

    /**
     * Directory containing the source (older) version proto files.
     */
    @Parameter(property = "v1", required = true)
    private File v1Directory;

    /**
     * Directory containing the target (newer) version proto files.
     */
    @Parameter(property = "v2", required = true)
    private File v2Directory;

    /**
     * Name/label for the source version (used in reports).
     * Defaults to "v1".
     */
    @Parameter(property = "v1Name", defaultValue = "v1")
    private String v1Name;

    /**
     * Name/label for the target version (used in reports).
     * Defaults to "v2".
     */
    @Parameter(property = "v2Name", defaultValue = "v2")
    private String v2Name;

    /**
     * Output format: text, json, or markdown.
     */
    @Parameter(property = "format", defaultValue = "text")
    private String outputFormat;

    /**
     * Output file path. If not specified, output is written to console.
     */
    @Parameter(property = "output")
    private File outputFile;

    /**
     * If true, only show breaking changes in the output.
     */
    @Parameter(property = "breakingOnly", defaultValue = "false")
    private boolean breakingOnly;

    /**
     * If true, fail the build when breaking changes are detected.
     * Useful for CI/CD pipelines.
     */
    @Parameter(property = "failOnBreaking", defaultValue = "false")
    private boolean failOnBreaking;

    /**
     * If true, treat warnings as errors when using failOnBreaking.
     */
    @Parameter(property = "failOnWarning", defaultValue = "false")
    private boolean failOnWarning;

    /**
     * Path to protoc executable. If not set, uses 'protoc' from PATH.
     */
    @Parameter(property = "protoc.path")
    private String protocPath;

    /**
     * Directory for temporary files (descriptors).
     */
    @Parameter(property = "tempDir", defaultValue = "${project.build.directory}/proto-wrapper-diff-tmp")
    private File tempDirectory;

    /**
     * Include path for proto imports. If not specified, uses v1Directory and v2Directory.
     */
    @Parameter(property = "includePath")
    private File includePath;

    private ProtocExecutor protocExecutor;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Proto Schema Diff Tool");
        getLog().info("Comparing: " + v1Directory + " -> " + v2Directory);

        // Validate directories
        validateDirectories();

        // Initialize protoc
        initializeProtoc();

        // Create temp directory
        createTempDirectory();

        try {
            // Analyze both versions
            VersionSchema v1Schema = analyzeVersion(v1Directory, v1Name);
            VersionSchema v2Schema = analyzeVersion(v2Directory, v2Name);

            // Compare schemas
            getLog().info("Comparing schemas...");
            SchemaDiff diff = SchemaDiff.compare(v1Schema, v2Schema);

            // Format output
            String output = formatOutput(diff);

            // Write or display output
            if (outputFile != null) {
                writeToFile(output);
            } else {
                getLog().info("\n" + output);
            }

            // Log summary
            logSummary(diff);

            // Check for breaking changes
            checkBreakingChanges(diff);

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to compare schemas", e);
        }
    }

    /**
     * Validates that input directories exist and are readable.
     */
    private void validateDirectories() throws MojoExecutionException {
        if (v1Directory == null) {
            throw new MojoExecutionException("v1 directory is required. Use -Dv1=<path>");
        }
        if (!v1Directory.exists()) {
            throw new MojoExecutionException("v1 directory does not exist: " + v1Directory.getAbsolutePath());
        }
        if (!v1Directory.isDirectory()) {
            throw new MojoExecutionException("v1 is not a directory: " + v1Directory.getAbsolutePath());
        }

        if (v2Directory == null) {
            throw new MojoExecutionException("v2 directory is required. Use -Dv2=<path>");
        }
        if (!v2Directory.exists()) {
            throw new MojoExecutionException("v2 directory does not exist: " + v2Directory.getAbsolutePath());
        }
        if (!v2Directory.isDirectory()) {
            throw new MojoExecutionException("v2 is not a directory: " + v2Directory.getAbsolutePath());
        }

        getLog().debug("v1 directory: " + v1Directory.getAbsolutePath());
        getLog().debug("v2 directory: " + v2Directory.getAbsolutePath());
    }

    /**
     * Initializes the protoc executor.
     */
    private void initializeProtoc() throws MojoExecutionException {
        protocExecutor = new ProtocExecutor(MavenLogger.from(getLog()));

        if (protocPath != null && !protocPath.isEmpty()) {
            protocExecutor.setProtocPath(protocPath);
        }

        if (!protocExecutor.isProtocAvailable()) {
            throw new MojoExecutionException(
                "protoc not found. Please install protobuf compiler or set -Dprotoc.path=<path>");
        }

        getLog().info("Using " + protocExecutor.getProtocVersion());
    }

    /**
     * Creates the temporary directory for descriptor files.
     */
    private void createTempDirectory() throws MojoExecutionException {
        if (tempDirectory == null) {
            tempDirectory = new File(System.getProperty("java.io.tmpdir"), "proto-wrapper-diff");
        }

        if (!tempDirectory.exists() && !tempDirectory.mkdirs()) {
            throw new MojoExecutionException("Failed to create temp directory: " + tempDirectory);
        }
    }

    /**
     * Analyzes a proto version directory and returns the schema.
     */
    private VersionSchema analyzeVersion(File protoDir, String versionName) throws IOException, MojoExecutionException {
        getLog().info("Analyzing " + versionName + " from " + protoDir.getName() + "...");

        // Generate descriptor file
        Path descriptorFile = tempDirectory.toPath().resolve(versionName + "-descriptor.pb");

        // Determine include path
        Path includeDir = includePath != null ? includePath.toPath() : protoDir.toPath();

        protocExecutor.generateDescriptor(
            protoDir.toPath(),
            descriptorFile,
            new String[0],  // No exclusions
            includeDir
        );

        // Analyze descriptor
        ProtoAnalyzer analyzer = new ProtoAnalyzer();
        VersionSchema schema = analyzer.analyze(descriptorFile, versionName);

        getLog().info("  Found " + schema.getMessages().size() + " messages, " +
                      schema.getEnums().size() + " enums");

        return schema;
    }

    /**
     * Formats the diff output according to the selected format.
     */
    private String formatOutput(SchemaDiff diff) {
        DiffFormatter formatter = createFormatter();

        if (breakingOnly) {
            return formatter.formatBreakingOnly(diff);
        }
        return formatter.format(diff);
    }

    /**
     * Creates the appropriate formatter based on outputFormat parameter.
     */
    private DiffFormatter createFormatter() {
        return switch (outputFormat.toLowerCase()) {
            case "json" -> new JsonDiffFormatter();
            case "markdown", "md" -> new MarkdownDiffFormatter();
            default -> new TextDiffFormatter();
        };
    }

    /**
     * Writes output to a file.
     */
    private void writeToFile(String output) throws MojoExecutionException {
        try {
            // Create parent directories if needed
            File parent = outputFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new MojoExecutionException("Failed to create output directory: " + parent);
            }

            Files.writeString(outputFile.toPath(), output);
            getLog().info("Report written to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write output file: " + outputFile, e);
        }
    }

    /**
     * Logs a summary of the diff results.
     */
    private void logSummary(SchemaDiff diff) {
        SchemaDiff.DiffSummary summary = diff.getSummary();

        getLog().info("");
        getLog().info("=== Summary ===");
        getLog().info("Messages: +" + summary.addedMessages() +
                      " / -" + summary.removedMessages() +
                      " / ~" + summary.modifiedMessages());
        getLog().info("Enums: +" + summary.addedEnums() +
                      " / -" + summary.removedEnums() +
                      " / ~" + summary.modifiedEnums());

        if (diff.hasBreakingChanges()) {
            getLog().warn("Breaking changes: " + summary.errorCount() + " errors, " +
                          summary.warningCount() + " warnings");
        } else {
            getLog().info("No breaking changes detected");
        }
    }

    /**
     * Checks for breaking changes and fails the build if configured to do so.
     */
    private void checkBreakingChanges(SchemaDiff diff) throws MojoFailureException {
        if (!failOnBreaking) {
            return;
        }

        List<BreakingChange> errors = diff.getErrors();
        List<BreakingChange> warnings = diff.getWarnings();

        int failureCount = errors.size();
        if (failOnWarning) {
            failureCount += warnings.size();
        }

        if (failureCount > 0) {
            StringBuilder message = new StringBuilder();
            message.append("Breaking changes detected!\n\n");

            if (!errors.isEmpty()) {
                message.append("ERRORS (").append(errors.size()).append("):\n");
                for (BreakingChange bc : errors) {
                    message.append("  - ").append(bc.entityPath())
                           .append(": ").append(bc.description()).append("\n");
                }
            }

            if (failOnWarning && !warnings.isEmpty()) {
                message.append("\nWARNINGS (").append(warnings.size()).append("):\n");
                for (BreakingChange bc : warnings) {
                    message.append("  - ").append(bc.entityPath())
                           .append(": ").append(bc.description()).append("\n");
                }
            }

            throw new MojoFailureException(message.toString());
        }
    }
}
