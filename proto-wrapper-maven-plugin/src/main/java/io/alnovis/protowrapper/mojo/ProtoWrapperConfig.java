package io.alnovis.protowrapper.mojo;

import io.alnovis.protowrapper.generator.GenerationOrchestrator;
import io.alnovis.protowrapper.model.ProtoSyntax;

import java.io.File;

/**
 * Simplified configuration for a single proto version.
 *
 * <p>Example usage in pom.xml:</p>
 * <pre>{@code
 * <version>
 *     <protoDir>v1</protoDir>
 *     <name>V1</name> <!-- optional, defaults to uppercase of directory name -->
 *     <protoSyntax>proto2</protoSyntax> <!-- optional: proto2, proto3, or auto (default) -->
 * </version>
 * }</pre>
 *
 * @since 2.2.0 Added protoSyntax configuration
 */
public class ProtoWrapperConfig implements GenerationOrchestrator.VersionConfig {

    /**
     * Directory containing .proto files for this version.
     * Can be absolute or relative to protoRoot.
     * Example: "v1" or "/path/to/protos/v1"
     */
    private String protoDir;

    /**
     * Version name used in generated code (e.g., "V1", "V2").
     * If not provided, derived from protoDir directory name in uppercase.
     */
    private String name;

    /**
     * Proto syntax for this version: "proto2", "proto3", or "auto".
     * When "auto" (default), syntax is detected from .proto files.
     * @since 2.2.0
     */
    private String protoSyntax;

    /**
     * Proto file patterns to exclude (glob patterns).
     * Example: ["updater.proto", "internal.proto"]
     */
    private String[] excludeProtos;

    // ---- Computed/cached fields (not set by user) ----

    /**
     * Resolved absolute path to proto directory.
     * Computed by plugin based on protoDir and protoRoot.
     */
    private transient File resolvedProtoDir;

    /**
     * Auto-detected proto package from proto files.
     * Computed by plugin.
     */
    private transient String detectedProtoPackage;

    /**
     * Generated descriptor file path.
     * Computed by plugin.
     */
    private transient File generatedDescriptorFile;

    /**
     * Resolved proto syntax (never AUTO after resolution).
     * Computed by plugin from protoSyntax config or auto-detection.
     * @since 2.2.0
     */
    private transient ProtoSyntax resolvedSyntax;

    // Getters and setters

    public String getProtoDir() {
        return protoDir;
    }

    public void setProtoDir(String protoDir) {
        this.protoDir = protoDir;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getExcludeProtos() {
        return excludeProtos;
    }

    public void setExcludeProtos(String[] excludeProtos) {
        this.excludeProtos = excludeProtos;
    }

    /**
     * Get the configured proto syntax string.
     * @return "proto2", "proto3", "auto", or null (defaults to auto)
     * @since 2.2.0
     */
    public String getProtoSyntax() {
        return protoSyntax;
    }

    /**
     * Set the proto syntax for this version.
     * @param protoSyntax "proto2", "proto3", or "auto"
     * @since 2.2.0
     */
    public void setProtoSyntax(String protoSyntax) {
        this.protoSyntax = protoSyntax;
    }

    /**
     * Get the resolved proto syntax (never AUTO).
     * @return the resolved syntax, or null if not yet resolved
     * @since 2.2.0
     */
    public ProtoSyntax getResolvedSyntax() {
        return resolvedSyntax;
    }

    /**
     * Set the resolved proto syntax.
     * @param resolvedSyntax the resolved syntax (should not be AUTO)
     * @since 2.2.0
     */
    public void setResolvedSyntax(ProtoSyntax resolvedSyntax) {
        this.resolvedSyntax = resolvedSyntax;
    }

    /**
     * Parse the configured protoSyntax string to ProtoSyntax enum.
     * @return ProtoSyntax.AUTO if not set or "auto", otherwise PROTO2 or PROTO3
     * @since 2.2.0
     */
    public ProtoSyntax getConfiguredSyntax() {
        if (protoSyntax == null || protoSyntax.isEmpty() || protoSyntax.equalsIgnoreCase("auto")) {
            return ProtoSyntax.AUTO;
        } else if (protoSyntax.equalsIgnoreCase("proto2")) {
            return ProtoSyntax.PROTO2;
        } else if (protoSyntax.equalsIgnoreCase("proto3")) {
            return ProtoSyntax.PROTO3;
        } else {
            // Invalid value - treat as auto
            return ProtoSyntax.AUTO;
        }
    }

    public File getResolvedProtoDir() {
        return resolvedProtoDir;
    }

    public void setResolvedProtoDir(File resolvedProtoDir) {
        this.resolvedProtoDir = resolvedProtoDir;
    }

    public String getDetectedProtoPackage() {
        return detectedProtoPackage;
    }

    public void setDetectedProtoPackage(String detectedProtoPackage) {
        this.detectedProtoPackage = detectedProtoPackage;
    }

    public File getGeneratedDescriptorFile() {
        return generatedDescriptorFile;
    }

    public void setGeneratedDescriptorFile(File generatedDescriptorFile) {
        this.generatedDescriptorFile = generatedDescriptorFile;
    }

    /**
     * Get version name, deriving from protoDir if not explicitly set.
     * @return Version name (e.g., "V1")
     */
    public String getEffectiveName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        // Derive from protoDir directory name
        if (protoDir != null) {
            String dirName = protoDir;
            // Handle both absolute and relative paths
            int lastSep = Math.max(dirName.lastIndexOf('/'), dirName.lastIndexOf(File.separatorChar));
            if (lastSep >= 0) {
                dirName = dirName.substring(lastSep + 1);
            }
            // Uppercase first letter: v1 -> V1
            if (!dirName.isEmpty()) {
                return dirName.substring(0, 1).toUpperCase() + dirName.substring(1);
            }
        }
        return "Unknown";
    }

    /**
     * Get version identifier for internal use (lowercase).
     * @return Version identifier (e.g., "v1")
     */
    public String getVersionId() {
        String effectiveName = getEffectiveName();
        return effectiveName.toLowerCase();
    }

    @Override
    public String toString() {
        return String.format("ProtoWrapperConfig[protoDir=%s, name=%s]", protoDir, getEffectiveName());
    }
}
