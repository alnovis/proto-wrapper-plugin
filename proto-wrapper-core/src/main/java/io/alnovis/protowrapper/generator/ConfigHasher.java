package io.alnovis.protowrapper.generator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for computing configuration hashes.
 *
 * <p>This class handles hash computation for GeneratorConfig
 * to support incremental generation cache invalidation.</p>
 *
 * <p>All methods are static and thread-safe.</p>
 *
 * @since 2.3.1
 * @see GeneratorConfig
 */
public final class ConfigHasher {

    private ConfigHasher() {
        // Utility class - no instantiation
    }

    /**
     * Compute hash of configuration for cache invalidation.
     * Changes in configuration settings invalidate the cache.
     *
     * @param config the configuration to hash
     * @return 16-character hex string representing configuration hash
     */
    public static String computeHash(GeneratorConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append(config.getApiPackage()).append("|");
        sb.append(config.getImplPackagePattern()).append("|");
        sb.append(config.getProtoPackagePattern()).append("|");
        sb.append(config.getAbstractClassPackage()).append("|");
        sb.append(config.isGenerateInterfaces()).append("|");
        sb.append(config.isGenerateAbstractClasses()).append("|");
        sb.append(config.isGenerateImplClasses()).append("|");
        sb.append(config.isGenerateVersionContext()).append("|");
        sb.append(config.isGenerateBuilders()).append("|");
        sb.append(config.isIncludeVersionSuffix()).append("|");
        sb.append(config.isConvertWellKnownTypes()).append("|");
        sb.append(config.isGenerateRawProtoAccessors()).append("|");
        sb.append(config.getDefaultSyntax()).append("|");
        sb.append(config.getTargetJavaVersion()).append("|");
        // Include custom mappings
        sb.append(config.getCustomTypeMappings()).append("|");
        sb.append(config.getFieldNameOverrides()).append("|");
        // Include message filters
        sb.append(config.getIncludedMessages()).append("|");
        sb.append(config.getExcludedMessages()).append("|");
        // Include field mappings (affects generated code)
        sb.append(config.getFieldMappings()).append("|");
        // Include validation settings (since 2.3.0)
        sb.append(config.isGenerateValidationAnnotations()).append("|");
        sb.append(config.getValidationAnnotationStyle());

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hashCode (should never happen as SHA-256 is always available)
            return String.format("%016x", sb.toString().hashCode());
        }
    }
}
