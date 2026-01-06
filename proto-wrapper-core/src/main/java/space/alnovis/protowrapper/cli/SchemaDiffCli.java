package space.alnovis.protowrapper.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import space.alnovis.protowrapper.PluginLogger;
import space.alnovis.protowrapper.analyzer.ProtoAnalyzer;
import space.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import space.alnovis.protowrapper.analyzer.ProtocExecutor;
import space.alnovis.protowrapper.diff.SchemaDiff;
import space.alnovis.protowrapper.diff.formatter.DiffFormatter;
import space.alnovis.protowrapper.diff.formatter.JsonDiffFormatter;
import space.alnovis.protowrapper.diff.formatter.MarkdownDiffFormatter;
import space.alnovis.protowrapper.diff.formatter.TextDiffFormatter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * CLI tool for comparing protobuf schema versions.
 *
 * <p>Usage examples:</p>
 * <pre>
 * # Compare two version directories
 * java -jar proto-wrapper-core-cli.jar diff proto/v1 proto/v2
 *
 * # Output in different formats
 * java -jar proto-wrapper-core-cli.jar diff proto/v1 proto/v2 --format=json
 * java -jar proto-wrapper-core-cli.jar diff proto/v1 proto/v2 --format=markdown
 *
 * # Show only breaking changes
 * java -jar proto-wrapper-core-cli.jar diff proto/v1 proto/v2 --breaking-only
 *
 * # Write output to file
 * java -jar proto-wrapper-core-cli.jar diff proto/v1 proto/v2 --output=diff-report.md
 *
 * # Fail on breaking changes (for CI/CD)
 * java -jar proto-wrapper-core-cli.jar diff proto/v1 proto/v2 --fail-on-breaking
 * </pre>
 */
@Command(
    name = "proto-wrapper",
    mixinStandardHelpOptions = true,
    version = "proto-wrapper 1.5.0",
    description = "Proto Wrapper - version-agnostic protobuf schema tools",
    subcommands = {SchemaDiffCli.DiffCommand.class}
)
public class SchemaDiffCli implements Callable<Integer> {

    /**
     * Main entry point for the CLI.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new SchemaDiffCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // Show help if no subcommand provided
        CommandLine.usage(this, System.out);
        return 0;
    }

    /**
     * Subcommand for schema diff operations.
     */
    @Command(
        name = "diff",
        mixinStandardHelpOptions = true,
        description = "Compare two protobuf schema versions and generate diff report"
    )
    public static class DiffCommand implements Callable<Integer> {

        @Parameters(
            index = "0",
            description = "Source version directory (older version)"
        )
        private File v1Directory;

        @Parameters(
            index = "1",
            description = "Target version directory (newer version)"
        )
        private File v2Directory;

        @Option(
            names = {"--v1-name"},
            description = "Name for source version (default: v1)",
            defaultValue = "v1"
        )
        private String v1Name;

        @Option(
            names = {"--v2-name"},
            description = "Name for target version (default: v2)",
            defaultValue = "v2"
        )
        private String v2Name;

        @Option(
            names = {"-f", "--format"},
            description = "Output format: text, json, markdown (default: text)",
            defaultValue = "text"
        )
        private String format;

        @Option(
            names = {"-o", "--output"},
            description = "Output file (prints to console if not specified)"
        )
        private File outputFile;

        @Option(
            names = {"-b", "--breaking-only"},
            description = "Show only breaking changes",
            defaultValue = "false"
        )
        private boolean breakingOnly;

        @Option(
            names = {"--fail-on-breaking"},
            description = "Exit with code 1 if breaking changes detected (for CI/CD)",
            defaultValue = "false"
        )
        private boolean failOnBreaking;

        @Option(
            names = {"--fail-on-warning"},
            description = "Treat warnings as errors (exit code 1)",
            defaultValue = "false"
        )
        private boolean failOnWarning;

        @Option(
            names = {"--protoc"},
            description = "Path to protoc executable (default: protoc from PATH)"
        )
        private String protocPath;

        @Option(
            names = {"-q", "--quiet"},
            description = "Suppress informational messages",
            defaultValue = "false"
        )
        private boolean quiet;

        private Path tempDir;

        @Override
        public Integer call() {
            try {
                // Validate directories
                if (!validateDirectories()) {
                    return 2;
                }

                // Create temp directory for descriptors
                tempDir = Files.createTempDirectory("proto-wrapper-diff");

                // Initialize protoc executor
                PluginLogger logger = quiet
                    ? PluginLogger.noop()
                    : PluginLogger.console();
                ProtocExecutor protoc = new ProtocExecutor(logger);

                if (protocPath != null && !protocPath.isEmpty()) {
                    protoc.setProtocPath(protocPath);
                }

                // Check protoc availability
                if (!protoc.isProtocAvailable()) {
                    System.err.println("ERROR: protoc not found. Please install protobuf compiler or use --protoc=<path>");
                    return 2;
                }

                if (!quiet) {
                    System.err.println("Using " + protoc.getProtocVersion());
                    System.err.println("Comparing schemas: " + v1Name + " -> " + v2Name);
                }

                // Analyze both versions
                VersionSchema v1Schema = analyzeVersion(protoc, v1Directory.toPath(), v1Name);
                VersionSchema v2Schema = analyzeVersion(protoc, v2Directory.toPath(), v2Name);

                // Perform comparison
                SchemaDiff diff = SchemaDiff.compare(v1Schema, v2Schema);

                // Format output
                String output = formatOutput(diff);

                // Write output
                if (outputFile != null) {
                    Files.writeString(outputFile.toPath(), output);
                    if (!quiet) {
                        System.err.println("Report written to: " + outputFile.getAbsolutePath());
                    }
                } else {
                    System.out.println(output);
                }

                // Print summary if not quiet
                if (!quiet) {
                    printSummary(diff);
                }

                // Check for breaking changes
                if (failOnBreaking && diff.hasBreakingChanges()) {
                    int errors = diff.getSummary().errorCount();
                    System.err.println("ERROR: " + errors + " breaking change(s) detected");
                    return 1;
                }

                if (failOnWarning && diff.getSummary().warningCount() > 0) {
                    int warnings = diff.getSummary().warningCount();
                    System.err.println("ERROR: " + warnings + " warning(s) detected (--fail-on-warning enabled)");
                    return 1;
                }

                if (!quiet && !diff.hasBreakingChanges()) {
                    System.err.println("No breaking changes detected");
                }

                return 0;

            } catch (IOException e) {
                System.err.println("ERROR: " + e.getMessage());
                return 2;
            } catch (Exception e) {
                System.err.println("ERROR: Unexpected error - " + e.getMessage());
                if (!quiet) {
                    e.printStackTrace(System.err);
                }
                return 3;
            } finally {
                // Clean up temp directory
                cleanupTempDir();
            }
        }

        private boolean validateDirectories() {
            if (!v1Directory.exists()) {
                System.err.println("ERROR: Source directory does not exist: " + v1Directory);
                return false;
            }
            if (!v1Directory.isDirectory()) {
                System.err.println("ERROR: Source path is not a directory: " + v1Directory);
                return false;
            }
            if (!v2Directory.exists()) {
                System.err.println("ERROR: Target directory does not exist: " + v2Directory);
                return false;
            }
            if (!v2Directory.isDirectory()) {
                System.err.println("ERROR: Target path is not a directory: " + v2Directory);
                return false;
            }
            return true;
        }

        private VersionSchema analyzeVersion(ProtocExecutor protoc, Path protoDir, String versionName)
                throws IOException {
            if (!quiet) {
                System.err.println("Analyzing " + versionName + " from " + protoDir.getFileName() + "...");
            }

            // Generate descriptor file
            Path descriptorFile = tempDir.resolve(versionName + "-descriptor.pb");

            protoc.generateDescriptor(protoDir, descriptorFile, protoDir);

            // Analyze descriptor
            ProtoAnalyzer analyzer = new ProtoAnalyzer();
            VersionSchema schema = analyzer.analyze(descriptorFile, versionName);

            if (!quiet) {
                System.err.println("  Found " + schema.getMessages().size() + " messages, " +
                                   schema.getEnums().size() + " enums");
            }

            return schema;
        }

        private String formatOutput(SchemaDiff diff) {
            DiffFormatter formatter = switch (format.toLowerCase()) {
                case "json" -> new JsonDiffFormatter();
                case "markdown", "md" -> new MarkdownDiffFormatter();
                default -> new TextDiffFormatter();
            };

            if (breakingOnly) {
                return formatter.formatBreakingOnly(diff);
            }
            return formatter.format(diff);
        }

        private void printSummary(SchemaDiff diff) {
            var summary = diff.getSummary();
            System.err.println();
            System.err.println("Summary:");
            System.err.printf("  Messages: +%d added, ~%d modified, -%d removed%n",
                summary.addedMessages(), summary.modifiedMessages(), summary.removedMessages());
            System.err.printf("  Enums:    +%d added, ~%d modified, -%d removed%n",
                summary.addedEnums(), summary.modifiedEnums(), summary.removedEnums());
            if (summary.errorCount() > 0 || summary.warningCount() > 0) {
                System.err.printf("  Breaking: %d error(s), %d warning(s)%n",
                    summary.errorCount(), summary.warningCount());
            }
        }

        private void cleanupTempDir() {
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
                } catch (IOException ignored) {
                }
            }
        }
    }
}
