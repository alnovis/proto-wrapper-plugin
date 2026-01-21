package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static io.alnovis.protowrapper.generator.ProtobufConstants.GENERATED_FILE_COMMENT;

/**
 * Generates ProtocolVersions utility class with version constants.
 *
 * <p>Generated class provides:</p>
 * <ul>
 *   <li>Constants for each protocol version (e.g., V1 = "v1")</li>
 *   <li>{@code supported()} - returns set of all supported version IDs</li>
 *   <li>{@code isSupported(String)} - checks if version ID is supported</li>
 *   <li>{@code requireSupported(String)} - validates and returns version ID or throws</li>
 * </ul>
 *
 * <p>Example generated code:</p>
 * <pre>{@code
 * public final class ProtocolVersions {
 *     public static final String V1 = "v1";
 *     public static final String V2 = "v2";
 *
 *     private static final Set<String> SUPPORTED = Set.of(V1, V2);
 *
 *     public static Set<String> supported() { return SUPPORTED; }
 *     public static boolean isSupported(String versionId) { return SUPPORTED.contains(versionId); }
 *     public static String requireSupported(String versionId) { ... }
 *
 *     private ProtocolVersions() {}
 * }
 * }</pre>
 *
 * @since 2.1.0
 */
public class ProtocolVersionsGenerator extends BaseGenerator<List<String>> {

    private final List<String> versions;

    /**
     * Create a new ProtocolVersionsGenerator.
     *
     * @param config the generator configuration
     * @param versions list of version identifiers (e.g., ["v1", "v2"])
     */
    public ProtocolVersionsGenerator(GeneratorConfig config, List<String> versions) {
        super(config);
        this.versions = versions;
    }

    /**
     * Generate the ProtocolVersions class.
     *
     * @return generated JavaFile
     */
    public JavaFile generate() {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("ProtocolVersions")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Protocol version constants and utilities.\n")
                .addJavadoc("\n")
                .addJavadoc("<p>This class provides compile-time constants for all supported protocol versions.\n")
                .addJavadoc("Use these constants instead of string literals for type safety.</p>\n")
                .addJavadoc("\n")
                .addJavadoc("@since 2.1.0\n");

        // Add version constants (e.g., public static final String V1 = "v1";)
        for (String version : versions) {
            String constantName = toConstantName(version);
            classBuilder.addField(FieldSpec.builder(String.class, constantName,
                            Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", version)
                    .addJavadoc("Version identifier: $L\n", version)
                    .build());
        }

        // Add DEFAULT constant
        String defaultVersionId = getDefaultVersionId();
        String defaultConstantName = toConstantName(defaultVersionId);
        classBuilder.addField(FieldSpec.builder(String.class, "DEFAULT",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", defaultConstantName)
                .addJavadoc("Default protocol version.\n")
                .addJavadoc("\n")
                .addJavadoc("@see #$L\n", defaultConstantName)
                .build());

        // Add SUPPORTED set field
        addSupportedField(classBuilder);

        // Add supported() method
        classBuilder.addMethod(MethodSpec.methodBuilder("supported")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(Set.class, String.class))
                .addJavadoc("Get all supported protocol version IDs.\n")
                .addJavadoc("\n")
                .addJavadoc("@return unmodifiable set of supported version IDs\n")
                .addStatement("return SUPPORTED")
                .build());

        // Add isSupported() method
        classBuilder.addMethod(MethodSpec.methodBuilder("isSupported")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(boolean.class)
                .addParameter(String.class, "versionId")
                .addJavadoc("Check if a version ID is supported.\n")
                .addJavadoc("\n")
                .addJavadoc("@param versionId the version ID to check\n")
                .addJavadoc("@return true if the version is supported\n")
                .addStatement("return SUPPORTED.contains(versionId)")
                .build());

        // Add requireSupported() method
        classBuilder.addMethod(MethodSpec.methodBuilder("requireSupported")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(String.class)
                .addParameter(String.class, "versionId")
                .addJavadoc("Validate that a version ID is supported.\n")
                .addJavadoc("\n")
                .addJavadoc("@param versionId the version ID to validate\n")
                .addJavadoc("@return the version ID if supported\n")
                .addJavadoc("@throws IllegalArgumentException if the version is not supported\n")
                .beginControlFlow("if (!isSupported(versionId))")
                .addStatement("throw new $T($S + versionId + $S + SUPPORTED)",
                        IllegalArgumentException.class,
                        "Unsupported protocol version: ",
                        ". Supported versions: ")
                .endControlFlow()
                .addStatement("return versionId")
                .build());

        // Private constructor
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build());

        TypeSpec typeSpec = classBuilder.build();

        return JavaFile.builder(config.getApiPackage(), typeSpec)
                .addFileComment(GENERATED_FILE_COMMENT)
                .indent("    ")
                .build();
    }

    /**
     * Add SUPPORTED field with Set.of() or Collections.unmodifiableSet() based on Java version.
     */
    private void addSupportedField(TypeSpec.Builder classBuilder) {
        ParameterizedTypeName setType = ParameterizedTypeName.get(Set.class, String.class);

        if (config.isJava8Compatible()) {
            // Java 8: Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))
            String constantsList = versions.stream()
                    .map(this::toConstantName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            classBuilder.addField(FieldSpec.builder(setType, "SUPPORTED",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$T.unmodifiableSet(new $T<>($T.asList($L)))",
                            java.util.Collections.class,
                            java.util.HashSet.class,
                            java.util.Arrays.class,
                            constantsList)
                    .build());
        } else {
            // Java 9+: Set.of(...)
            String constantsList = versions.stream()
                    .map(this::toConstantName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            classBuilder.addField(FieldSpec.builder(setType, "SUPPORTED",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$T.of($L)", Set.class, constantsList)
                    .build());
        }
    }

    /**
     * Convert version ID to constant name (e.g., "v1" -> "V1", "v2" -> "V2").
     */
    private String toConstantName(String versionId) {
        return versionId.toUpperCase();
    }

    /**
     * Get the default version ID from config or fall back to last version in list.
     *
     * @return the default version ID
     */
    private String getDefaultVersionId() {
        String configuredDefault = config.getDefaultVersion();
        if (configuredDefault != null && !configuredDefault.isEmpty()) {
            return configuredDefault;
        }
        // Fallback to last version in list
        return versions.get(versions.size() - 1);
    }

    /**
     * Generate and write ProtocolVersions class.
     *
     * @return path to the generated file
     * @throws IOException if writing fails
     */
    public Path generateAndWrite() throws IOException {
        JavaFile javaFile = generate();
        writeToFile(javaFile);

        String relativePath = config.getApiPackage().replace('.', '/')
                + "/ProtocolVersions.java";
        return config.getOutputDirectory().resolve(relativePath);
    }
}
