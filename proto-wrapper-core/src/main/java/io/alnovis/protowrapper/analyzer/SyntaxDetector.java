package io.alnovis.protowrapper.analyzer;

import io.alnovis.protowrapper.model.ProtoSyntax;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for detecting proto syntax from .proto files.
 *
 * <p>Detects syntax by parsing the {@code syntax = "proto2|proto3";} declaration
 * in .proto files. Files without explicit syntax declaration default to proto2
 * per the Protocol Buffers specification.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * // Detect from a single file
 * ProtoSyntax syntax = SyntaxDetector.detectFromFile(path);
 *
 * // Detect dominant syntax from a directory
 * ProtoSyntax syntax = SyntaxDetector.detectFromDirectory(protoDir);
 * </pre>
 */
public final class SyntaxDetector {

    /**
     * Pattern to match syntax declaration in .proto files.
     * Matches: syntax = "proto2"; or syntax = "proto3";
     * Handles optional whitespace and both quote styles.
     */
    private static final Pattern SYNTAX_PATTERN = Pattern.compile(
            "^\\s*syntax\\s*=\\s*[\"']?(proto[23])[\"']?\\s*;",
            Pattern.MULTILINE
    );

    private SyntaxDetector() {
        // Utility class
    }

    /**
     * Detect proto syntax from a .proto file content.
     *
     * @param content the content of a .proto file
     * @return PROTO3 if syntax = "proto3" is found, PROTO2 otherwise
     */
    public static ProtoSyntax detectFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return ProtoSyntax.PROTO2;
        }

        Matcher matcher = SYNTAX_PATTERN.matcher(content);
        if (matcher.find()) {
            String syntax = matcher.group(1);
            return "proto3".equals(syntax) ? ProtoSyntax.PROTO3 : ProtoSyntax.PROTO2;
        }

        // No syntax declaration = proto2 (protobuf default)
        return ProtoSyntax.PROTO2;
    }

    /**
     * Detect proto syntax from a .proto file.
     *
     * @param protoFile path to a .proto file
     * @return PROTO3 if syntax = "proto3" is found, PROTO2 otherwise
     * @throws IOException if file cannot be read
     */
    public static ProtoSyntax detectFromFile(Path protoFile) throws IOException {
        String content = Files.readString(protoFile);
        return detectFromContent(content);
    }

    /**
     * Detect dominant proto syntax from a directory of .proto files.
     *
     * <p>If all files use the same syntax, returns that syntax.
     * If files have mixed syntax, returns PROTO2 (conservative choice).
     * If no .proto files found, returns PROTO2.</p>
     *
     * @param protoDirectory directory containing .proto files
     * @return the dominant syntax, or PROTO2 if mixed/empty
     * @throws IOException if files cannot be read
     */
    public static ProtoSyntax detectFromDirectory(Path protoDirectory) throws IOException {
        if (!Files.isDirectory(protoDirectory)) {
            return ProtoSyntax.PROTO2;
        }

        List<Path> protoFiles;
        try (var stream = Files.walk(protoDirectory)) {
            protoFiles = stream
                    .filter(p -> p.toString().endsWith(".proto"))
                    .filter(Files::isRegularFile)
                    .toList();
        }

        return detectFromFiles(protoFiles);
    }

    /**
     * Detect dominant proto syntax from a collection of .proto files.
     *
     * <p>If all files use the same syntax, returns that syntax.
     * If files have mixed syntax, returns PROTO2 (conservative choice).
     * If no files provided, returns PROTO2.</p>
     *
     * @param protoFiles collection of paths to .proto files
     * @return the dominant syntax, or PROTO2 if mixed/empty
     * @throws IOException if files cannot be read
     */
    public static ProtoSyntax detectFromFiles(Collection<Path> protoFiles) throws IOException {
        if (protoFiles == null || protoFiles.isEmpty()) {
            return ProtoSyntax.PROTO2;
        }

        boolean hasProto2 = false;
        boolean hasProto3 = false;

        for (Path file : protoFiles) {
            ProtoSyntax syntax = detectFromFile(file);
            if (syntax == ProtoSyntax.PROTO2) {
                hasProto2 = true;
            } else if (syntax == ProtoSyntax.PROTO3) {
                hasProto3 = true;
            }

            // Early exit if mixed
            if (hasProto2 && hasProto3) {
                // Mixed syntax - return proto2 as conservative default
                return ProtoSyntax.PROTO2;
            }
        }

        // All files use the same syntax
        return hasProto3 ? ProtoSyntax.PROTO3 : ProtoSyntax.PROTO2;
    }

    /**
     * Resolve AUTO syntax to concrete PROTO2/PROTO3 by detecting from directory.
     *
     * @param configured the configured syntax (may be AUTO)
     * @param protoDirectory directory to scan if configured is AUTO
     * @return resolved syntax (never AUTO)
     * @throws IOException if files cannot be read when AUTO
     */
    public static ProtoSyntax resolve(ProtoSyntax configured, Path protoDirectory) throws IOException {
        if (configured == null || configured.isAuto()) {
            return detectFromDirectory(protoDirectory);
        }
        return configured;
    }
}
