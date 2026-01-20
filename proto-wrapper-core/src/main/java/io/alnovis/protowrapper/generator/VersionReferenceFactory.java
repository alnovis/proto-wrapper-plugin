package io.alnovis.protowrapper.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory for generating version reference code blocks.
 *
 * <p>Encapsulates the logic of choosing between string literals ({@code "v1"})
 * and ProtocolVersions constants ({@code ProtocolVersions.V1}) based on configuration.</p>
 *
 * <p>This class eliminates code duplication across generators by providing
 * a single source of truth for version reference generation.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * VersionReferenceFactory vrf = VersionReferenceFactory.create(config);
 *
 * // For return statements
 * vrf.addReturnStatement(methodBuilder, "v1");
 *
 * // For field initializers
 * fieldBuilder.initializer(vrf.fieldInitializer("v1"));
 *
 * // For list formatting
 * String joined = vrf.formatVersionsList(versions);
 * }</pre>
 *
 * @since 2.1.0
 */
public final class VersionReferenceFactory {

    private final ClassName protocolVersionsClass;

    private VersionReferenceFactory(ClassName protocolVersionsClass) {
        this.protocolVersionsClass = protocolVersionsClass;
    }

    /**
     * Create a factory instance based on generator configuration.
     *
     * @param config the generator configuration
     * @return new factory instance
     */
    public static VersionReferenceFactory create(GeneratorConfig config) {
        ClassName pvClass = config.isGenerateProtocolVersions()
                ? ClassName.get(config.getApiPackage(), "ProtocolVersions")
                : null;
        return new VersionReferenceFactory(pvClass);
    }

    /**
     * Check if ProtocolVersions constants should be used.
     *
     * @return true if ProtocolVersions is enabled in config
     */
    public boolean useConstants() {
        return protocolVersionsClass != null;
    }

    /**
     * Get the ProtocolVersions class name, or null if disabled.
     *
     * @return ClassName for ProtocolVersions, or null
     */
    public ClassName getProtocolVersionsClass() {
        return protocolVersionsClass;
    }

    /**
     * Add a return statement for version to the method builder.
     *
     * <p>Generates either:</p>
     * <ul>
     *   <li>{@code return ProtocolVersions.V1;} when enabled</li>
     *   <li>{@code return "v1";} when disabled</li>
     * </ul>
     *
     * @param method the method builder
     * @param version the version string (e.g., "v1")
     */
    public void addReturnStatement(MethodSpec.Builder method, String version) {
        if (useConstants()) {
            method.addStatement("return $T.$L", protocolVersionsClass, toConstant(version));
        } else {
            method.addStatement("return $S", version);
        }
    }

    /**
     * Add a map.put() statement with version key.
     *
     * <p>Generates either:</p>
     * <ul>
     *   <li>{@code map.put(ProtocolVersions.V1, ValueType.INSTANCE);} when enabled</li>
     *   <li>{@code map.put("v1", ValueType.INSTANCE);} when disabled</li>
     * </ul>
     *
     * @param method the method builder
     * @param version the version string (e.g., "v1")
     * @param valueType the value type class name
     * @param valueExpr the value expression (e.g., "INSTANCE")
     */
    public void addMapPut(MethodSpec.Builder method, String version,
                          ClassName valueType, String valueExpr) {
        if (useConstants()) {
            method.addStatement("map.put($T.$L, $T.$L)",
                    protocolVersionsClass, toConstant(version), valueType, valueExpr);
        } else {
            method.addStatement("map.put($S, $T.$L)", version, valueType, valueExpr);
        }
    }

    /**
     * Generate field initializer CodeBlock for a version.
     *
     * <p>Generates either:</p>
     * <ul>
     *   <li>{@code ProtocolVersions.V1} when enabled</li>
     *   <li>{@code "v1"} when disabled</li>
     * </ul>
     *
     * @param version the version string (e.g., "v1")
     * @return CodeBlock for field initializer
     */
    public CodeBlock fieldInitializer(String version) {
        return useConstants()
                ? CodeBlock.of("$T.$L", protocolVersionsClass, toConstant(version))
                : CodeBlock.of("$S", version);
    }

    /**
     * Format a single version for use in a list literal.
     *
     * <p>Returns either:</p>
     * <ul>
     *   <li>{@code ProtocolVersions.V1} when enabled</li>
     *   <li>{@code "v1"} (with quotes) when disabled</li>
     * </ul>
     *
     * @param version the version string (e.g., "v1")
     * @return formatted version string for list element
     */
    public String formatForList(String version) {
        return useConstants()
                ? "ProtocolVersions." + toConstant(version)
                : "\"" + version + "\"";
    }

    /**
     * Format a list of versions as comma-separated string.
     *
     * <p>Returns either:</p>
     * <ul>
     *   <li>{@code ProtocolVersions.V1, ProtocolVersions.V2} when enabled</li>
     *   <li>{@code "v1", "v2"} when disabled</li>
     * </ul>
     *
     * @param versions list of version strings
     * @return comma-separated formatted versions
     */
    public String formatVersionsList(List<String> versions) {
        return versions.stream()
                .map(this::formatForList)
                .collect(Collectors.joining(", "));
    }

    /**
     * Convert version string to constant name.
     *
     * <p>Examples: "v1" → "V1", "v2" → "V2"</p>
     *
     * @param version the version string
     * @return uppercase constant name
     */
    public static String toConstant(String version) {
        return version.toUpperCase();
    }
}
