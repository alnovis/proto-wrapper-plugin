package io.alnovis.protowrapper.incremental;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable fingerprint of a file for change detection.
 * Uses content hash (SHA-256), modification time, and file size.
 *
 * <p>The fingerprint provides two levels of change detection:
 * <ul>
 *   <li>Quick check via {@link #mightHaveChanged(FileFingerprint)} - uses size and timestamp</li>
 *   <li>Full check via {@link #matches(FileFingerprint)} - uses content hash</li>
 * </ul>
 *
 * @param relativePath relative path from proto root (using forward slashes)
 * @param contentHash SHA-256 hash of file content
 * @param lastModified file modification timestamp in milliseconds
 * @param fileSize file size in bytes
 */
public record FileFingerprint(
    String relativePath,
    String contentHash,
    long lastModified,
    long fileSize
) {

    /**
     * Canonical constructor with validation.
     *
     * @param relativePath relative path from proto root
     * @param contentHash SHA-256 hash of file content
     * @param lastModified file modification timestamp
     * @param fileSize file size in bytes
     */
    public FileFingerprint {
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        Objects.requireNonNull(contentHash, "contentHash must not be null");
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize must be non-negative");
        }
    }

    /**
     * Compute fingerprint for a file.
     *
     * @param file absolute path to file
     * @param root root directory for computing relative path
     * @return computed fingerprint
     * @throws IOException if file cannot be read
     */
    public static FileFingerprint compute(Path file, Path root) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(root, "root must not be null");

        String relativePath = root.relativize(file).toString().replace('\\', '/');
        byte[] content = Files.readAllBytes(file);
        String hash = computeSha256(content);
        long lastModified = Files.getLastModifiedTime(file).toMillis();
        long size = content.length;

        return new FileFingerprint(relativePath, hash, lastModified, size);
    }

    /**
     * Quick check if file might have changed based on size and timestamp.
     *
     * <p>This is an optimization to avoid computing hash for unchanged files.
     * If this method returns {@code false}, the file definitely has not changed.
     * If it returns {@code true}, use {@link #matches(FileFingerprint)} for definitive check.
     *
     * @param previous previous fingerprint (may be null)
     * @return true if file might have changed, false if definitely unchanged
     */
    public boolean mightHaveChanged(FileFingerprint previous) {
        if (previous == null) {
            return true;
        }
        return this.fileSize != previous.fileSize ||
               this.lastModified != previous.lastModified;
    }

    /**
     * Full equality check using content hash.
     *
     * <p>This is the definitive check for file content changes.
     *
     * @param other other fingerprint to compare (may be null)
     * @return true if content matches, false otherwise
     */
    public boolean matches(FileFingerprint other) {
        if (other == null) {
            return false;
        }
        return this.contentHash.equals(other.contentHash);
    }

    /**
     * Check if this fingerprint represents a newer version of the same file.
     *
     * @param other other fingerprint
     * @return true if this is newer based on modification time
     */
    public boolean isNewerThan(FileFingerprint other) {
        if (other == null) {
            return true;
        }
        return this.lastModified > other.lastModified;
    }

    /**
     * Serialize to JSON string.
     *
     * @return JSON representation
     */
    public String toJson() {
        return String.format(
            "{\"relativePath\":\"%s\",\"contentHash\":\"%s\",\"lastModified\":%d,\"fileSize\":%d}",
            escapeJson(relativePath),
            escapeJson(contentHash),
            lastModified,
            fileSize
        );
    }

    /**
     * Deserialize from JSON string.
     *
     * @param json JSON representation
     * @return parsed FileFingerprint
     * @throws IllegalArgumentException if JSON is invalid
     */
    public static FileFingerprint fromJson(String json) {
        Objects.requireNonNull(json, "json must not be null");

        String relativePath = extractJsonString(json, "relativePath");
        String contentHash = extractJsonString(json, "contentHash");
        long lastModified = extractJsonLong(json, "lastModified");
        long fileSize = extractJsonLong(json, "fileSize");

        return new FileFingerprint(relativePath, contentHash, lastModified, fileSize);
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

    private static String unescapeJson(String value) {
        return value
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
}
