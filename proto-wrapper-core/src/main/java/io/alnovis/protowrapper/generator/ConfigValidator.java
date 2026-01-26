package io.alnovis.protowrapper.generator;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Utility class for validating GeneratorConfig parameters.
 *
 * <p>This class consolidates validation logic that was previously
 * scattered across GeneratorConfig.Builder to improve separation of concerns.</p>
 *
 * <p>All methods are static and throw {@link IllegalArgumentException}
 * or {@link NullPointerException} when validation fails.</p>
 *
 * @since 2.3.1
 * @see GeneratorConfig
 */
public final class ConfigValidator {

    private ConfigValidator() {
        // Utility class - no instantiation
    }

    /**
     * Validate the complete GeneratorConfig.
     * Called from Builder.build() to ensure configuration is valid.
     *
     * @param config the configuration to validate
     * @throws NullPointerException if required fields are null
     */
    public static void validate(GeneratorConfig config) {
        Objects.requireNonNull(config, "config is required");
        validateOutputDirectory(config.getOutputDirectory());
    }

    /**
     * Validate the output directory.
     *
     * @param outputDirectory the output directory path
     * @throws NullPointerException if outputDirectory is null
     */
    public static void validateOutputDirectory(Path outputDirectory) {
        Objects.requireNonNull(outputDirectory, "Output directory is required");
    }

    /**
     * Validate the protobuf major version.
     *
     * @param version the protobuf major version (2 or 3)
     * @throws IllegalArgumentException if version is not 2 or 3
     * @deprecated Since 2.2.0. Use ProtoSyntax instead.
     */
    @Deprecated(since = "2.2.0", forRemoval = true)
    public static void validateProtobufMajorVersion(int version) {
        if (version < 2 || version > 3) {
            throw new IllegalArgumentException("protobufMajorVersion must be 2 or 3, got: " + version);
        }
    }

    /**
     * Validate the target Java version.
     *
     * @param version the target Java version
     * @throws IllegalArgumentException if version is less than 8
     */
    public static void validateTargetJavaVersion(int version) {
        if (version < 8) {
            throw new IllegalArgumentException("targetJavaVersion must be at least 8, got: " + version);
        }
    }

    /**
     * Validate the number of generation threads.
     *
     * @param threads the number of threads (0 for auto)
     * @throws IllegalArgumentException if threads is negative
     */
    public static void validateGenerationThreads(int threads) {
        if (threads < 0) {
            throw new IllegalArgumentException("generationThreads must be >= 0, got: " + threads);
        }
    }

    /**
     * Validate the validation annotation style.
     *
     * @param style the annotation style ("jakarta" or "javax")
     * @throws IllegalArgumentException if style is not "jakarta" or "javax"
     */
    public static void validateValidationStyle(String style) {
        if (style != null && !"jakarta".equals(style) && !"javax".equals(style)) {
            throw new IllegalArgumentException(
                    "validationAnnotationStyle must be 'jakarta' or 'javax', got: " + style);
        }
    }
}
