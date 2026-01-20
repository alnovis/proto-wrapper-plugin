package io.alnovis.protowrapper.incremental;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persistent state for incremental generation.
 * Serialized to JSON in cache directory.
 *
 * <p>This state tracks:
 * <ul>
 *   <li>Plugin version - for cache invalidation on upgrade</li>
 *   <li>Configuration hash - for cache invalidation on config change</li>
 *   <li>Proto file fingerprints - for change detection</li>
 *   <li>Proto dependencies - import graph for transitive change detection</li>
 *   <li>Generated files - for tracking output and detecting external modifications</li>
 *   <li>Last generation time - for diagnostics</li>
 * </ul>
 *
 * <h2>Null Safety</h2>
 * <p>Map fields ({@code protoFingerprints}, {@code protoDependencies}, {@code generatedFiles})
 * are never null - the compact constructor normalizes null to empty maps.</p>
 * <p>String and Instant fields can be null for empty/initial state.</p>
 *
 * <h2>Factory Methods</h2>
 * <p>Prefer using factory methods instead of the constructor directly:</p>
 * <ul>
 *   <li>{@link #empty()} - for initial/invalidated state</li>
 *   <li>{@link #forGeneration(String, String, Map, Map)} - after successful generation</li>
 * </ul>
 *
 * @param pluginVersion version of the plugin that created this state (nullable for empty state)
 * @param configHash hash of generation configuration (nullable for empty state)
 * @param protoFingerprints map of relative path to file fingerprint (never null)
 * @param protoDependencies map of file path to set of imported file paths (never null)
 * @param generatedFiles map of generated file path to file info (never null)
 * @param lastGeneration timestamp of last successful generation (nullable for empty state)
 */
public record IncrementalState(
    String pluginVersion,
    String configHash,
    Map<String, FileFingerprint> protoFingerprints,
    Map<String, Set<String>> protoDependencies,
    Map<String, GeneratedFileInfo> generatedFiles,
    Instant lastGeneration
) {

    /**
     * Compact constructor with null normalization and defensive copies.
     *
     * <p>All Map parameters are normalized: null becomes empty immutable map,
     * non-null values are defensively copied to immutable maps.</p>
     *
     * @param pluginVersion version of the plugin
     * @param configHash hash of generation configuration
     * @param protoFingerprints map of relative path to file fingerprint
     * @param protoDependencies map of file path to set of imports
     * @param generatedFiles map of generated file path to info
     * @param lastGeneration timestamp of last generation
     */
    public IncrementalState {
        // Normalize null maps to empty immutable maps
        protoFingerprints = normalizeMap(protoFingerprints);
        protoDependencies = normalizeDependencyMap(protoDependencies);
        generatedFiles = normalizeMap(generatedFiles);
    }

    // ==================== Factory Methods ====================

    /**
     * Create empty state for first run or after cache invalidation.
     *
     * <p>All map fields are empty, all other fields are null.</p>
     *
     * @return empty state instance
     */
    public static IncrementalState empty() {
        return new IncrementalState(null, null, Map.of(), Map.of(), Map.of(), null);
    }

    /**
     * Create state after successful generation.
     *
     * <p>This is the primary factory method for creating state after a build.
     * The lastGeneration timestamp is set to current time automatically.</p>
     *
     * @param pluginVersion current plugin version (required)
     * @param configHash current configuration hash (required)
     * @param protoFingerprints fingerprints of proto files (may be null or empty)
     * @param protoDependencies import graph (may be null or empty)
     * @return new state instance with current timestamp
     * @throws NullPointerException if pluginVersion or configHash is null
     */
    public static IncrementalState forGeneration(
            String pluginVersion,
            String configHash,
            Map<String, FileFingerprint> protoFingerprints,
            Map<String, Set<String>> protoDependencies) {
        Objects.requireNonNull(pluginVersion, "pluginVersion must not be null");
        Objects.requireNonNull(configHash, "configHash must not be null");
        return new IncrementalState(
            pluginVersion,
            configHash,
            protoFingerprints,
            protoDependencies,
            Map.of(),
            Instant.now()
        );
    }

    /**
     * Create state after successful generation with generated files tracking.
     *
     * @param pluginVersion current plugin version (required)
     * @param configHash current configuration hash (required)
     * @param protoFingerprints fingerprints of proto files (may be null or empty)
     * @param protoDependencies import graph (may be null or empty)
     * @param generatedFiles generated files info (may be null or empty)
     * @return new state instance with current timestamp
     * @throws NullPointerException if pluginVersion or configHash is null
     */
    public static IncrementalState forGeneration(
            String pluginVersion,
            String configHash,
            Map<String, FileFingerprint> protoFingerprints,
            Map<String, Set<String>> protoDependencies,
            Map<String, GeneratedFileInfo> generatedFiles) {
        Objects.requireNonNull(pluginVersion, "pluginVersion must not be null");
        Objects.requireNonNull(configHash, "configHash must not be null");
        return new IncrementalState(
            pluginVersion,
            configHash,
            protoFingerprints,
            protoDependencies,
            generatedFiles,
            Instant.now()
        );
    }

    // ==================== Instance Methods ====================

    /**
     * Check if this state is empty (first run or invalidated).
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return pluginVersion == null && configHash == null && lastGeneration == null;
    }

    /**
     * Check if cache should be fully invalidated due to version or config change.
     *
     * @param currentPluginVersion current plugin version
     * @param currentConfigHash current configuration hash
     * @return true if cache should be invalidated
     */
    public boolean shouldInvalidate(String currentPluginVersion, String currentConfigHash) {
        // Empty state - no previous generation
        if (isEmpty()) {
            return true;
        }

        // Plugin version changed
        if (!Objects.equals(pluginVersion, currentPluginVersion)) {
            return true;
        }

        // Config changed
        return !Objects.equals(configHash, currentConfigHash);
    }

    /**
     * Create a new state with updated values.
     *
     * <p>This method is typically called from {@link IncrementalStateManager#saveCurrentState}
     * to create a new state after generation.</p>
     *
     * @param pluginVersion new plugin version (required)
     * @param configHash new config hash (required)
     * @param protoFingerprints new fingerprints (may be null)
     * @param protoDependencies new dependencies (may be null)
     * @param generatedFiles new generated files info (may be null)
     * @return new state instance with current timestamp
     */
    public IncrementalState withUpdates(
            String pluginVersion,
            String configHash,
            Map<String, FileFingerprint> protoFingerprints,
            Map<String, Set<String>> protoDependencies,
            Map<String, GeneratedFileInfo> generatedFiles) {
        return IncrementalState.forGeneration(
            pluginVersion,
            configHash,
            protoFingerprints,
            protoDependencies,
            generatedFiles
        );
    }

    /**
     * Write state to cache file in JSON format.
     *
     * @param cacheFile path to cache file
     * @throws IOException if write fails
     */
    public void writeTo(Path cacheFile) throws IOException {
        Objects.requireNonNull(cacheFile, "cacheFile must not be null");
        Files.createDirectories(cacheFile.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(cacheFile)) {
            writer.write("{\n");

            // Plugin version
            writer.write("  \"pluginVersion\": ");
            writeJsonString(writer, pluginVersion);
            writer.write(",\n");

            // Config hash
            writer.write("  \"configHash\": ");
            writeJsonString(writer, configHash);
            writer.write(",\n");

            // Last generation
            writer.write("  \"lastGeneration\": ");
            writeJsonString(writer, lastGeneration != null ? lastGeneration.toString() : null);
            writer.write(",\n");

            // Proto fingerprints
            writer.write("  \"protoFingerprints\": {\n");
            writeFingerprints(writer, protoFingerprints);
            writer.write("  },\n");

            // Proto dependencies
            writer.write("  \"protoDependencies\": {\n");
            writeDependencies(writer, protoDependencies);
            writer.write("  },\n");

            // Generated files
            writer.write("  \"generatedFiles\": {\n");
            writeGeneratedFiles(writer, generatedFiles);
            writer.write("  }\n");

            writer.write("}\n");
        }
    }

    /**
     * Read state from cache file.
     *
     * @param cacheFile path to cache file
     * @return parsed state, or empty state if file doesn't exist or is invalid
     */
    public static IncrementalState readFrom(Path cacheFile) {
        Objects.requireNonNull(cacheFile, "cacheFile must not be null");

        if (!Files.exists(cacheFile)) {
            return empty();
        }

        try {
            String content = Files.readString(cacheFile);
            return parseJson(content);
        } catch (Exception e) {
            // Corrupted cache - return empty for full regeneration
            return empty();
        }
    }

    private static IncrementalState parseJson(String json) {
        String pluginVersion = extractOptionalJsonString(json, "pluginVersion");
        String configHash = extractOptionalJsonString(json, "configHash");
        String lastGenStr = extractOptionalJsonString(json, "lastGeneration");
        Instant lastGeneration = lastGenStr != null ? Instant.parse(lastGenStr) : null;

        Map<String, FileFingerprint> fingerprints = parseFingerprints(json);
        Map<String, Set<String>> dependencies = parseDependencies(json);
        Map<String, GeneratedFileInfo> generated = parseGeneratedFiles(json);

        return new IncrementalState(pluginVersion, configHash, fingerprints, dependencies, generated, lastGeneration);
    }

    private static Map<String, FileFingerprint> parseFingerprints(String json) {
        Map<String, FileFingerprint> result = new HashMap<>();

        int start = json.indexOf("\"protoFingerprints\"");
        if (start == -1) return result;

        start = json.indexOf("{", start + 1);
        if (start == -1) return result;

        int depth = 1;
        int objStart = -1;
        String currentKey = null;

        for (int i = start + 1; i < json.length() && depth > 0; i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                int keyEnd = json.indexOf("\"", i + 1);
                if (keyEnd > i && objStart == -1) {
                    // Check if this is a key (followed by :)
                    int colonPos = json.indexOf(":", keyEnd);
                    if (colonPos > keyEnd && json.substring(keyEnd + 1, colonPos).trim().isEmpty()) {
                        currentKey = json.substring(i + 1, keyEnd);
                        i = keyEnd;
                    }
                }
            } else if (c == '{') {
                if (depth == 1) {
                    objStart = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 1 && objStart > 0 && currentKey != null) {
                    String objJson = json.substring(objStart, i + 1);
                    try {
                        FileFingerprint fp = FileFingerprint.fromJson(objJson);
                        result.put(currentKey, fp);
                    } catch (Exception ignored) {
                        // Skip malformed entries
                    }
                    objStart = -1;
                    currentKey = null;
                }
            }
        }

        return result;
    }

    private static Map<String, Set<String>> parseDependencies(String json) {
        Map<String, Set<String>> result = new HashMap<>();

        int start = json.indexOf("\"protoDependencies\"");
        if (start == -1) return result;

        start = json.indexOf("{", start + 1);
        if (start == -1) return result;

        int depth = 1;
        int arrStart = -1;
        String currentKey = null;

        for (int i = start + 1; i < json.length() && depth > 0; i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                int keyEnd = json.indexOf("\"", i + 1);
                if (keyEnd > i && arrStart == -1) {
                    int colonPos = json.indexOf(":", keyEnd);
                    if (colonPos > keyEnd && json.substring(keyEnd + 1, colonPos).trim().isEmpty()) {
                        currentKey = json.substring(i + 1, keyEnd);
                        i = keyEnd;
                    }
                }
            } else if (c == '[') {
                if (depth == 1) {
                    arrStart = i;
                }
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 1 && arrStart > 0 && currentKey != null) {
                    String arrJson = json.substring(arrStart + 1, i);
                    Set<String> deps = parseStringArray(arrJson);
                    result.put(currentKey, deps);
                    arrStart = -1;
                    currentKey = null;
                }
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
        }

        return result;
    }

    private static Set<String> parseStringArray(String arrContent) {
        Set<String> result = new HashSet<>();
        int i = 0;
        while (i < arrContent.length()) {
            int start = arrContent.indexOf("\"", i);
            if (start == -1) break;
            int end = arrContent.indexOf("\"", start + 1);
            while (end > 0 && arrContent.charAt(end - 1) == '\\') {
                end = arrContent.indexOf("\"", end + 1);
            }
            if (end == -1) break;
            result.add(arrContent.substring(start + 1, end));
            i = end + 1;
        }
        return result;
    }

    private static String extractOptionalJsonString(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return null;

        start += pattern.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        if (start >= json.length()) return null;

        if (json.charAt(start) == 'n') {
            // null
            return null;
        }

        if (json.charAt(start) != '"') return null;

        int end = json.indexOf("\"", start + 1);
        while (end > 0 && json.charAt(end - 1) == '\\') {
            end = json.indexOf("\"", end + 1);
        }
        if (end == -1) return null;

        return unescapeJson(json.substring(start + 1, end));
    }

    private static void writeJsonString(BufferedWriter writer, String value) throws IOException {
        if (value == null) {
            writer.write("null");
        } else {
            writer.write("\"");
            writer.write(escapeJson(value));
            writer.write("\"");
        }
    }

    private static void writeFingerprints(BufferedWriter writer, Map<String, FileFingerprint> fingerprints)
            throws IOException {
        boolean first = true;
        for (Map.Entry<String, FileFingerprint> entry : fingerprints.entrySet()) {
            if (!first) {
                writer.write(",\n");
            }
            first = false;
            writer.write("    \"");
            writer.write(escapeJson(entry.getKey()));
            writer.write("\": ");
            writer.write(entry.getValue().toJson());
        }
        if (!fingerprints.isEmpty()) {
            writer.write("\n");
        }
    }

    private static void writeDependencies(BufferedWriter writer, Map<String, Set<String>> dependencies)
            throws IOException {
        boolean first = true;
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            if (!first) {
                writer.write(",\n");
            }
            first = false;
            writer.write("    \"");
            writer.write(escapeJson(entry.getKey()));
            writer.write("\": [");

            String depsStr = entry.getValue().stream()
                .map(s -> "\"" + escapeJson(s) + "\"")
                .collect(Collectors.joining(", "));
            writer.write(depsStr);

            writer.write("]");
        }
        if (!dependencies.isEmpty()) {
            writer.write("\n");
        }
    }

    private static void writeGeneratedFiles(BufferedWriter writer, Map<String, GeneratedFileInfo> generatedFiles)
            throws IOException {
        boolean first = true;
        for (Map.Entry<String, GeneratedFileInfo> entry : generatedFiles.entrySet()) {
            if (!first) {
                writer.write(",\n");
            }
            first = false;
            writer.write("    \"");
            writer.write(escapeJson(entry.getKey()));
            writer.write("\": ");
            writer.write(entry.getValue().toJson());
        }
        if (!generatedFiles.isEmpty()) {
            writer.write("\n");
        }
    }

    private static Map<String, GeneratedFileInfo> parseGeneratedFiles(String json) {
        Map<String, GeneratedFileInfo> result = new HashMap<>();

        int start = json.indexOf("\"generatedFiles\"");
        if (start == -1) return result;

        start = json.indexOf("{", start + 1);
        if (start == -1) return result;

        int depth = 1;
        int objStart = -1;
        String currentKey = null;

        for (int i = start + 1; i < json.length() && depth > 0; i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                int keyEnd = json.indexOf("\"", i + 1);
                if (keyEnd > i && objStart == -1) {
                    int colonPos = json.indexOf(":", keyEnd);
                    if (colonPos > keyEnd && json.substring(keyEnd + 1, colonPos).trim().isEmpty()) {
                        currentKey = json.substring(i + 1, keyEnd);
                        i = keyEnd;
                    }
                }
            } else if (c == '{') {
                if (depth == 1) {
                    objStart = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 1 && objStart > 0 && currentKey != null) {
                    String objJson = json.substring(objStart, i + 1);
                    try {
                        GeneratedFileInfo info = GeneratedFileInfo.fromJson(objJson);
                        result.put(currentKey, info);
                    } catch (Exception ignored) {
                        // Skip malformed entries
                    }
                    objStart = -1;
                    currentKey = null;
                }
            }
        }

        return result;
    }

    /**
     * Normalize a map: null becomes empty, non-null is defensively copied.
     */
    private static <V> Map<String, V> normalizeMap(Map<String, V> map) {
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new HashMap<>(map));
    }

    /**
     * Normalize a dependency map with deep copy of set values.
     */
    private static Map<String, Set<String>> normalizeDependencyMap(Map<String, Set<String>> map) {
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(deepCopyDependencies(map));
    }

    private static Map<String, Set<String>> deepCopyDependencies(Map<String, Set<String>> original) {
        Map<String, Set<String>> copy = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : original.entrySet()) {
            copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return copy;
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private static String unescapeJson(String value) {
        return value
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
}
