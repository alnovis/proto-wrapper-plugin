package io.alnovis.protowrapper.analyzer;

import io.alnovis.protowrapper.PluginLogger;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes protoc to generate descriptor files from .proto sources.
 *
 * <p>Usage:</p>
 * <pre>
 * ProtocExecutor executor = new ProtocExecutor();
 * Path descriptor = executor.generateDescriptor(
 *     sourceDir,
 *     outputFile,
 *     includeDirectories
 * );
 * </pre>
 */
public class ProtocExecutor {

    private static final int TIMEOUT_SECONDS = 60;

    private final PluginLogger logger;
    private final ProtocResolver resolver;
    private String customProtocPath;
    private Path resolvedProtocPath;

    /**
     * Create a ProtocExecutor with console logging.
     */
    public ProtocExecutor() {
        this(PluginLogger.console());
    }

    /**
     * Create a ProtocExecutor with the specified logger.
     *
     * @param logger the logger to use
     */
    public ProtocExecutor(PluginLogger logger) {
        this.logger = logger;
        this.resolver = new ProtocResolver(logger);
    }

    /**
     * Set custom path to protoc executable.
     *
     * <p>If not set, protoc will be resolved automatically:</p>
     * <ol>
     *   <li>System PATH (if protoc is installed)</li>
     *   <li>Embedded (downloaded from Maven Central)</li>
     * </ol>
     *
     * @param protocPath the path to protoc, or null for auto-detection
     */
    public void setProtocPath(String protocPath) {
        this.customProtocPath = protocPath;
        this.resolvedProtocPath = null; // Reset cache
    }

    /**
     * Set the protoc version to use for embedded downloads.
     *
     * <p>Only affects embedded protoc. Ignored if custom path is set
     * or system protoc is found.</p>
     *
     * @param version protoc version (e.g., "4.28.2")
     */
    public void setProtocVersion(String version) {
        resolver.setProtocVersion(version);
        this.resolvedProtocPath = null; // Reset cache
    }

    /**
     * Get the protoc version configured for embedded downloads.
     *
     * @return protoc version
     */
    public String getProtocVersion() {
        return resolver.getProtocVersion();
    }

    /**
     * Get the resolved protoc command path.
     *
     * @return path to protoc executable
     * @throws IOException if protoc cannot be resolved
     */
    private String getProtocCommand() throws IOException {
        if (resolvedProtocPath == null) {
            resolvedProtocPath = resolver.resolve(customProtocPath);
        }
        return resolvedProtocPath.toString();
    }

    /**
     * Generate descriptor file from proto sources.
     *
     * @param sourceDir Directory containing .proto files
     * @param outputFile Output descriptor file (.pb)
     * @param includeDirectories Additional include directories
     * @return Path to generated descriptor
     * @throws IOException if protoc fails
     */
    public Path generateDescriptor(Path sourceDir, Path outputFile, Path... includeDirectories)
            throws IOException {
        return generateDescriptor(sourceDir, outputFile, null, includeDirectories);
    }

    /**
     * Generate descriptor file from proto sources with exclusions.
     *
     * @param sourceDir Directory containing .proto files
     * @param outputFile Output descriptor file (.pb)
     * @param excludePatterns Glob patterns for files to exclude (e.g., "updater.proto")
     * @param includeDirectories Additional include directories
     * @return Path to generated descriptor
     * @throws IOException if protoc fails
     */
    public Path generateDescriptor(Path sourceDir, Path outputFile, String[] excludePatterns, Path... includeDirectories)
            throws IOException {

        // Find all .proto files
        List<Path> protoFiles = findProtoFiles(sourceDir, excludePatterns);
        if (protoFiles.isEmpty()) {
            throw new IOException("No .proto files found in " + sourceDir);
        }

        logger.info("Found " + protoFiles.size() + " proto files in " + sourceDir);

        // Build command
        List<String> command = new ArrayList<>();
        command.add(getProtocCommand());
        command.add("--descriptor_set_out=" + outputFile.toAbsolutePath());
        command.add("--include_imports");

        // Add proto paths
        if (includeDirectories.length > 0) {
            // Use only include directories as proto_path (to avoid duplicate definitions)
            for (Path includeDir : includeDirectories) {
                command.add("--proto_path=" + includeDir.toAbsolutePath());
            }
        } else {
            // No include directories - use source directory as proto_path
            command.add("--proto_path=" + sourceDir.toAbsolutePath());
        }

        // Add all proto files
        for (Path protoFile : protoFiles) {
            command.add(protoFile.toAbsolutePath().toString());
        }

        // Ensure output directory exists
        Files.createDirectories(outputFile.getParent());

        // Execute protoc
        logger.info("Executing: " + String.join(" ", command.subList(0, Math.min(5, command.size()))) + "...");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // Wait for completion
        boolean finished;
        try {
            finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Protoc execution interrupted", e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Protoc timed out after " + TIMEOUT_SECONDS + " seconds");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("Protoc failed with exit code " + exitCode + ":\n" + output);
        }

        if (!Files.exists(outputFile)) {
            throw new IOException("Protoc completed but output file not created: " + outputFile);
        }

        logger.info("Generated descriptor: " + outputFile + " (" + Files.size(outputFile) + " bytes)");
        return outputFile;
    }

    /**
     * Find all .proto files in directory recursively.
     *
     * @param directory the directory to search
     * @return list of proto files
     * @throws IOException if directory cannot be read
     */
    public List<Path> findProtoFiles(Path directory) throws IOException {
        return findProtoFiles(directory, null);
    }

    /**
     * Find all .proto files in directory recursively, excluding specified patterns.
     *
     * @param directory Directory to search
     * @param excludePatterns Glob patterns for files to exclude (e.g., "updater.proto")
     * @return List of proto files
     * @throws IOException if directory cannot be read
     */
    public List<Path> findProtoFiles(Path directory, String[] excludePatterns) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("Not a directory: " + directory);
        }

        // Build exclude matchers
        List<PathMatcher> excludeMatchers = new ArrayList<>();
        if (excludePatterns != null) {
            for (String pattern : excludePatterns) {
                excludeMatchers.add(directory.getFileSystem().getPathMatcher("glob:" + pattern));
            }
        }

        List<Path> result = new ArrayList<>();

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".proto")) {
                    // Check exclusions
                    Path relativePath = directory.relativize(file);
                    String fileName = file.getFileName().toString();

                    boolean excluded = false;
                    for (PathMatcher matcher : excludeMatchers) {
                        // Match against both relative path and filename
                        if (matcher.matches(relativePath) || matcher.matches(file.getFileName())) {
                            excluded = true;
                            break;
                        }
                    }

                    if (!excluded) {
                        result.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return result.stream()
                .sorted()
                .toList();
    }

    /**
     * Find .proto files matching patterns.
     *
     * @param directory Base directory
     * @param includePattern Glob pattern for files to include
     * @param excludePattern Glob pattern for files to exclude (nullable)
     * @return List of matching proto files
     * @throws IOException if directory cannot be read
     */
    public List<Path> findProtoFiles(Path directory, String includePattern, String excludePattern)
            throws IOException {

        if (!Files.isDirectory(directory)) {
            throw new IOException("Not a directory: " + directory);
        }

        // For patterns like "**/*.proto", we need to match both root and nested files
        // PathMatcher with "**/*.proto" doesn't match root-level files, so we also check "*.proto"
        PathMatcher includeMatcher = directory.getFileSystem().getPathMatcher("glob:" + includePattern);
        PathMatcher rootMatcher = null;
        if (includePattern.startsWith("**/")) {
            // Also match files at root level
            String rootPattern = includePattern.substring(3); // Remove "**/"
            rootMatcher = directory.getFileSystem().getPathMatcher("glob:" + rootPattern);
        }
        final PathMatcher finalRootMatcher = rootMatcher;

        PathMatcher excludeMatcher = excludePattern != null
                ? directory.getFileSystem().getPathMatcher("glob:" + excludePattern)
                : null;

        List<Path> result = new ArrayList<>();

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path relativePath = directory.relativize(file);
                boolean matches = includeMatcher.matches(relativePath);
                if (!matches && finalRootMatcher != null) {
                    matches = finalRootMatcher.matches(relativePath);
                }
                if (matches) {
                    if (excludeMatcher == null || !excludeMatcher.matches(relativePath)) {
                        result.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return result.stream()
                .sorted()
                .toList();
    }

    /**
     * Check if protoc is available (either system, custom, or embedded).
     *
     * @return true if protoc can be resolved and executed
     */
    public boolean isProtocAvailable() {
        try {
            String protocCommand = getProtocCommand();
            ProcessBuilder pb = new ProcessBuilder(protocCommand, "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Query the version string from the resolved protoc executable.
     *
     * @return version string (e.g., "libprotoc 4.28.2") or null if not available
     */
    public String queryInstalledProtocVersion() {
        try {
            String protocCommand = getProtocCommand();
            ProcessBuilder pb = new ProcessBuilder(protocCommand, "--version");
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            process.waitFor(5, TimeUnit.SECONDS);
            return output.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract java_package option from proto file.
     *
     * @param protoFile Proto file to analyze
     * @return Java package or null if not found
     * @throws IOException if the file cannot be read
     */
    public String extractJavaPackage(Path protoFile) throws IOException {
        String content = Files.readString(protoFile);

        // Look for: option java_package = "org.example.proto";
        Pattern pattern = Pattern.compile("option\\s+java_package\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Extract java_outer_classname option from proto file.
     *
     * @param protoFile Proto file to analyze
     * @return Outer class name or null if not found
     * @throws IOException if the file cannot be read
     */
    public String extractJavaOuterClassname(Path protoFile) throws IOException {
        String content = Files.readString(protoFile);

        Pattern pattern = Pattern.compile("option\\s+java_outer_classname\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Extract package declaration from proto file.
     *
     * @param protoFile Proto file to analyze
     * @return Proto package or null if not found
     * @throws IOException if the file cannot be read
     */
    public String extractProtoPackage(Path protoFile) throws IOException {
        String content = Files.readString(protoFile);

        // Look for: package org.example;
        Pattern pattern = Pattern.compile("package\\s+([\\w.]+)\\s*;");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Build proto mappings by analyzing proto files.
     *
     * @param sourceDir Directory with proto files
     * @param javaPackage Base Java package for generated classes
     * @return Map of message name to fully qualified Java class name
     * @throws IOException if proto files cannot be read
     */
    public java.util.Map<String, String> buildProtoMappings(Path sourceDir, String javaPackage)
            throws IOException {
        return buildProtoMappings(sourceDir, javaPackage, null);
    }

    /**
     * Build proto mappings by analyzing proto files with exclusions.
     *
     * @param sourceDir Directory with proto files
     * @param javaPackage Base Java package for generated classes
     * @param excludePatterns Glob patterns for files to exclude
     * @return Map of message name to fully qualified Java class name
     * @throws IOException if proto files cannot be read
     */
    public java.util.Map<String, String> buildProtoMappings(Path sourceDir, String javaPackage, String[] excludePatterns)
            throws IOException {

        java.util.Map<String, String> mappings = new java.util.LinkedHashMap<>();

        for (Path protoFile : findProtoFiles(sourceDir, excludePatterns)) {
            String content = Files.readString(protoFile);

            // Get java_package or use provided
            String pkg = extractJavaPackage(protoFile);
            if (pkg == null) {
                pkg = javaPackage;
            }

            // Get outer classname or derive from filename
            String outerClass = extractJavaOuterClassname(protoFile);
            if (outerClass == null) {
                String filename = protoFile.getFileName().toString();
                outerClass = toCamelCase(filename.replace(".proto", ""));
            }

            // Find all message declarations
            Pattern msgPattern = Pattern.compile("^\\s*message\\s+(\\w+)\\s*\\{", Pattern.MULTILINE);
            Matcher msgMatcher = msgPattern.matcher(content);

            while (msgMatcher.find()) {
                String messageName = msgMatcher.group(1);
                String fullClassName = pkg + "." + outerClass + "." + messageName;
                mappings.put(messageName, fullClassName);
            }
        }

        return mappings;
    }

    /**
     * Detects the correct proto_path for protoc based on import patterns in proto files.
     *
     * <p>Proto files may use imports with a version directory prefix
     * (e.g., {@code import "v2/common.proto";}). In this case, the proto_path
     * must be the parent of the version directory, not the version directory itself.</p>
     *
     * @param protoDir the proto version directory to analyze
     * @return the correct include path for protoc
     * @throws IOException if proto files cannot be read
     */
    public static Path detectIncludePath(Path protoDir) throws IOException {
        String dirName = protoDir.getFileName().toString();
        String prefix = "\"" + dirName + "/";

        // Scan proto files for import statements
        List<Path> protoFiles;
        try (var stream = Files.walk(protoDir, 1)) {
            protoFiles = stream
                .filter(p -> p.toString().endsWith(".proto"))
                .limit(5)  // Check only first few files
                .toList();
        }

        for (Path protoFile : protoFiles) {
            List<String> lines = Files.readAllLines(protoFile);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("import ")) {
                    if (trimmed.contains(prefix)) {
                        // Import uses directory prefix — proto_path must be parent
                        return protoDir.getParent();
                    }
                    // Found a non-prefixed import — use directory itself
                    if (trimmed.matches("import\\s+\"[^/\"]+\\.proto\".*")) {
                        return protoDir;
                    }
                }
            }
        }

        // Default: use the directory itself
        return protoDir;
    }

    private String toCamelCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (c == '_' || c == '-' || c == '.') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }

        return result.toString();
    }
}
