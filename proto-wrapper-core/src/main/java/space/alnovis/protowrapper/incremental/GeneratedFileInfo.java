package space.alnovis.protowrapper.incremental;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Information about a generated Java source file.
 * Tracks the content hash, modification time, and source proto files that contributed to it.
 *
 * <p>This information is used for:
 * <ul>
 *   <li>Detecting external modifications to generated files</li>
 *   <li>Mapping generated files back to source protos for incremental regeneration</li>
 *   <li>Cleaning up orphaned generated files when protos are removed</li>
 * </ul>
 *
 * @param contentHash SHA-256 hash of generated file content
 * @param lastModified file modification timestamp in milliseconds
 * @param sourceProtos set of relative proto file paths that contributed to this file
 */
public record GeneratedFileInfo(
    String contentHash,
    long lastModified,
    Set<String> sourceProtos
) {

    /**
     * Canonical constructor with defensive copy.
     */
    public GeneratedFileInfo {
        Objects.requireNonNull(contentHash, "contentHash must not be null");
        sourceProtos = sourceProtos != null
            ? Collections.unmodifiableSet(new HashSet<>(sourceProtos))
            : Collections.emptySet();
    }

    /**
     * Compute info for a generated file.
     *
     * @param file path to the generated file
     * @param sourceProtos set of source proto files
     * @return computed info
     * @throws IOException if file cannot be read
     */
    public static GeneratedFileInfo compute(Path file, Set<String> sourceProtos) throws IOException {
        Objects.requireNonNull(file, "file must not be null");

        byte[] content = Files.readAllBytes(file);
        String hash = computeSha256(content);
        long lastModified = Files.getLastModifiedTime(file).toMillis();

        return new GeneratedFileInfo(hash, lastModified, sourceProtos);
    }

    /**
     * Check if the generated file was modified externally.
     *
     * @param file path to the file to check
     * @return true if file was modified, false if unchanged
     * @throws IOException if file cannot be read
     */
    public boolean wasModifiedExternally(Path file) throws IOException {
        if (!Files.exists(file)) {
            return true;
        }

        long currentLastModified = Files.getLastModifiedTime(file).toMillis();
        if (currentLastModified == this.lastModified) {
            return false;
        }

        byte[] content = Files.readAllBytes(file);
        String currentHash = computeSha256(content);
        return !currentHash.equals(this.contentHash);
    }

    /**
     * Serialize to JSON string.
     *
     * @return JSON representation
     */
    public String toJson() {
        String protosJson = sourceProtos.stream()
            .map(s -> "\"" + escapeJson(s) + "\"")
            .collect(Collectors.joining(", "));

        return String.format(
            "{\"contentHash\":\"%s\",\"lastModified\":%d,\"sourceProtos\":[%s]}",
            escapeJson(contentHash),
            lastModified,
            protosJson
        );
    }

    /**
     * Deserialize from JSON string.
     *
     * @param json JSON representation
     * @return parsed GeneratedFileInfo
     * @throws IllegalArgumentException if JSON is invalid
     */
    public static GeneratedFileInfo fromJson(String json) {
        Objects.requireNonNull(json, "json must not be null");

        String contentHash = extractJsonString(json, "contentHash");
        long lastModified = extractJsonLong(json, "lastModified");
        Set<String> sourceProtos = extractJsonStringArray(json, "sourceProtos");

        return new GeneratedFileInfo(contentHash, lastModified, sourceProtos);
    }

    private static String computeSha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private static String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) {
            throw new IllegalArgumentException("Missing key: " + key);
        }
        start += pattern.length();
        int end = json.indexOf("\"", start);
        while (end > 0 && json.charAt(end - 1) == '\\') {
            end = json.indexOf("\"", end + 1);
        }
        if (end == -1) {
            throw new IllegalArgumentException("Malformed JSON for key: " + key);
        }
        return unescapeJson(json.substring(start, end));
    }

    private static long extractJsonLong(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) {
            throw new IllegalArgumentException("Missing key: " + key);
        }
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for key: " + key, e);
        }
    }

    private static Set<String> extractJsonStringArray(String json, String key) {
        String pattern = "\"" + key + "\":[";
        int start = json.indexOf(pattern);
        if (start == -1) {
            return Collections.emptySet();
        }
        start += pattern.length();
        int end = json.indexOf("]", start);
        if (end == -1) {
            return Collections.emptySet();
        }

        String arrayContent = json.substring(start, end);
        Set<String> result = new HashSet<>();
        int i = 0;
        while (i < arrayContent.length()) {
            int strStart = arrayContent.indexOf("\"", i);
            if (strStart == -1) break;
            int strEnd = arrayContent.indexOf("\"", strStart + 1);
            while (strEnd > 0 && arrayContent.charAt(strEnd - 1) == '\\') {
                strEnd = arrayContent.indexOf("\"", strEnd + 1);
            }
            if (strEnd == -1) break;
            result.add(unescapeJson(arrayContent.substring(strStart + 1, strEnd)));
            i = strEnd + 1;
        }
        return result;
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
