package io.alnovis.protowrapper.incremental;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds and queries proto file dependency graph based on import statements.
 *
 * <p>The graph tracks two relationships:
 * <ul>
 *   <li>Imports: which files a given file imports</li>
 *   <li>Dependents: which files import a given file (reverse index)</li>
 * </ul>
 *
 * <p>This enables transitive dependency tracking: when a file changes,
 * all files that depend on it (directly or transitively) need to be regenerated.
 *
 * <p>Example:
 * <pre>
 * // If common.proto is imported by order.proto and user.proto:
 * graph.getTransitiveDependents("v1/common.proto")
 * // Returns: {"v1/order.proto", "v1/user.proto"}
 * </pre>
 */
public class ProtoDependencyGraph {

    /**
     * Pattern to match import statements in proto files.
     * Matches: import "path/to/file.proto";
     * Also matches: import public "path/to/file.proto";
     * Also matches: import weak "path/to/file.proto";
     * Uses word boundary (\b) to avoid matching inside other identifiers.
     */
    private static final Pattern IMPORT_PATTERN =
        Pattern.compile("\\bimport\\s+(?:public\\s+|weak\\s+)?\"([^\"]+)\"\\s*;");

    /**
     * Prefixes for imports that should be ignored (external dependencies).
     */
    private static final Set<String> IGNORED_PREFIXES = Set.of(
        "google/protobuf/",
        "google/api/",
        "google/rpc/",
        "google/type/"
    );

    // file -> files it imports
    private final Map<String, Set<String>> imports;

    // file -> files that import it (reverse index)
    private final Map<String, Set<String>> dependents;

    private ProtoDependencyGraph(Map<String, Set<String>> imports) {
        this.imports = Collections.unmodifiableMap(new HashMap<>(imports));
        this.dependents = Collections.unmodifiableMap(buildReverseIndex(imports));
    }

    /**
     * Build dependency graph from proto files.
     *
     * @param protoFiles set of proto file paths (absolute)
     * @param protoRoot root directory for relative path computation
     * @return dependency graph
     * @throws IOException if files cannot be read
     */
    public static ProtoDependencyGraph build(Set<Path> protoFiles, Path protoRoot) throws IOException {
        Objects.requireNonNull(protoFiles, "protoFiles must not be null");
        Objects.requireNonNull(protoRoot, "protoRoot must not be null");

        Map<String, Set<String>> imports = new HashMap<>();

        for (Path protoFile : protoFiles) {
            String relativePath = protoRoot.relativize(protoFile)
                .toString().replace('\\', '/');
            Set<String> fileImports = parseImports(protoFile);
            imports.put(relativePath, fileImports);
        }

        return new ProtoDependencyGraph(imports);
    }

    /**
     * Build dependency graph from existing imports map.
     * Used when restoring from cached state.
     *
     * @param imports map of file path to imported files
     * @return dependency graph
     */
    public static ProtoDependencyGraph fromImports(Map<String, Set<String>> imports) {
        Objects.requireNonNull(imports, "imports must not be null");
        return new ProtoDependencyGraph(imports);
    }

    /**
     * Get all files that depend on the given file (transitively).
     *
     * <p>If common.proto changes, this returns all files that import it
     * directly or indirectly. These files need to be regenerated.
     *
     * @param protoFile relative path of the changed file
     * @return set of dependent file paths (relative)
     */
    public Set<String> getTransitiveDependents(String protoFile) {
        Objects.requireNonNull(protoFile, "protoFile must not be null");

        Set<String> result = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(protoFile);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            Set<String> directDependents = dependents.getOrDefault(current, Set.of());

            for (String dependent : directDependents) {
                if (result.add(dependent)) {
                    queue.add(dependent);
                }
            }
        }

        return result;
    }

    /**
     * Get all files that the given file depends on (transitively).
     *
     * <p>This returns all files that are imported directly or indirectly.
     *
     * @param protoFile relative path of the file
     * @return set of dependency file paths (relative)
     */
    public Set<String> getTransitiveDependencies(String protoFile) {
        Objects.requireNonNull(protoFile, "protoFile must not be null");

        Set<String> result = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(protoFile);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            Set<String> directImports = imports.getOrDefault(current, Set.of());

            for (String imported : directImports) {
                if (result.add(imported)) {
                    queue.add(imported);
                }
            }
        }

        return result;
    }

    /**
     * Get direct imports of a file.
     *
     * @param protoFile relative path of the file
     * @return set of directly imported file paths
     */
    public Set<String> getDirectImports(String protoFile) {
        return imports.getOrDefault(protoFile, Set.of());
    }

    /**
     * Get direct dependents of a file.
     *
     * @param protoFile relative path of the file
     * @return set of files that directly import this file
     */
    public Set<String> getDirectDependents(String protoFile) {
        return dependents.getOrDefault(protoFile, Set.of());
    }

    /**
     * Get the imports map for serialization.
     *
     * @return unmodifiable map of file to imports
     */
    public Map<String, Set<String>> getImports() {
        return imports;
    }

    /**
     * Get all files in the graph.
     *
     * @return set of all file paths
     */
    public Set<String> getAllFiles() {
        return imports.keySet();
    }

    /**
     * Check if a file exists in the graph.
     *
     * @param protoFile relative path of the file
     * @return true if file is tracked
     */
    public boolean containsFile(String protoFile) {
        return imports.containsKey(protoFile);
    }

    /**
     * Get the number of files in the graph.
     *
     * @return file count
     */
    public int size() {
        return imports.size();
    }

    /**
     * Parse import statements from a proto file.
     *
     * @param protoFile path to proto file
     * @return set of imported file paths (relative, with forward slashes)
     * @throws IOException if file cannot be read
     */
    static Set<String> parseImports(Path protoFile) throws IOException {
        String content = Files.readString(protoFile);
        Set<String> result = new HashSet<>();

        Matcher matcher = IMPORT_PATTERN.matcher(content);
        while (matcher.find()) {
            String importPath = matcher.group(1);
            // Skip external imports (google/protobuf, etc.)
            if (!isIgnoredImport(importPath)) {
                result.add(importPath);
            }
        }

        return result;
    }

    private static boolean isIgnoredImport(String importPath) {
        for (String prefix : IGNORED_PREFIXES) {
            if (importPath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Set<String>> buildReverseIndex(Map<String, Set<String>> imports) {
        Map<String, Set<String>> reverse = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : imports.entrySet()) {
            String file = entry.getKey();
            for (String imported : entry.getValue()) {
                reverse.computeIfAbsent(imported, k -> new HashSet<>()).add(file);
            }
        }

        // Make all sets unmodifiable
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : reverse.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }

        return result;
    }

    @Override
    public String toString() {
        return "ProtoDependencyGraph{files=" + imports.size() +
            ", totalImports=" + imports.values().stream().mapToInt(Set::size).sum() + "}";
    }
}
