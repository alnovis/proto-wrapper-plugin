package space.alnovis.protowrapper.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for proto-wrapper Spring Boot integration.
 *
 * <p>Example configuration in application.yml:
 * <pre>{@code
 * proto-wrapper:
 *   base-package: com.example.model.api
 *   versions:
 *     - v1
 *     - v2
 *     - v3
 *   default-version: v2
 *   version-header: X-Protocol-Version
 *   request-scoped: true
 *   exception-handling: true
 *   provider-type: factory
 * }</pre>
 *
 * @see ProviderType
 */
@ConfigurationProperties(prefix = "proto-wrapper")
public class ProtoWrapperProperties {

    /**
     * Base package where generated wrapper classes are located.
     * Used for reflective discovery of VersionContext implementations.
     * Example: "com.example.model.api"
     */
    private String basePackage;

    /**
     * List of supported protocol versions.
     * Example: ["v1", "v2", "v3"]
     */
    private List<String> versions = new ArrayList<>();

    /**
     * Default protocol version to use when not specified in request.
     * Must be one of the values in 'versions' list.
     * If not specified, defaults to the first version in the list.
     */
    private String defaultVersion;

    /**
     * HTTP header name to extract protocol version from.
     * Default: "X-Protocol-Version"
     */
    private String versionHeader = "X-Protocol-Version";

    /**
     * Enable request-scoped VersionContext.
     * When true, creates RequestScopedVersionContext and VersionContextRequestFilter.
     * Default: true
     */
    private boolean requestScoped = true;

    /**
     * Enable global exception handling for proto-wrapper exceptions.
     * When true, registers ProtoWrapperExceptionHandler as @ControllerAdvice.
     * Default: true
     */
    private boolean exceptionHandling = true;

    /**
     * Type of VersionContextProvider to use.
     * FACTORY (default): Uses generated VersionContextFactory for type-safe access.
     * REFLECTIVE: Uses reflection to discover VersionContext classes.
     * Default: FACTORY
     */
    private ProviderType providerType = ProviderType.FACTORY;

    /**
     * Enum defining available VersionContextProvider implementations.
     */
    public enum ProviderType {
        /**
         * Uses generated VersionContextFactory.
         * Recommended: provides compile-time type safety in generated code.
         */
        FACTORY,

        /**
         * Uses reflection to discover and instantiate VersionContext classes.
         * Fallback for compatibility with older generated code.
         */
        REFLECTIVE
    }

    // Getters and setters

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public List<String> getVersions() {
        return versions;
    }

    public void setVersions(List<String> versions) {
        this.versions = versions;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public String getVersionHeader() {
        return versionHeader;
    }

    public void setVersionHeader(String versionHeader) {
        this.versionHeader = versionHeader;
    }

    public boolean isRequestScoped() {
        return requestScoped;
    }

    public void setRequestScoped(boolean requestScoped) {
        this.requestScoped = requestScoped;
    }

    public boolean isExceptionHandling() {
        return exceptionHandling;
    }

    public void setExceptionHandling(boolean exceptionHandling) {
        this.exceptionHandling = exceptionHandling;
    }

    public ProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(ProviderType providerType) {
        this.providerType = providerType;
    }

    /**
     * Validates configuration on startup.
     *
     * <p>When using {@link ProviderType#FACTORY}, the versions list is optional
     * since the factory provides this information.
     *
     * <p>When using {@link ProviderType#REFLECTIVE}, the versions list is required.
     *
     * @throws IllegalStateException if configuration is invalid
     */
    public void validate() {
        if (basePackage == null || basePackage.isBlank()) {
            throw new IllegalStateException("proto-wrapper.base-package must be configured");
        }

        // When using REFLECTIVE provider, versions list is required
        if (providerType == ProviderType.REFLECTIVE) {
            if (versions == null || versions.isEmpty()) {
                throw new IllegalStateException(
                        "proto-wrapper.versions must contain at least one version " +
                                "when using reflective provider");
            }
            if (defaultVersion != null && !versions.contains(defaultVersion)) {
                throw new IllegalStateException(
                        "proto-wrapper.default-version '" + defaultVersion +
                                "' must be one of configured versions: " + versions);
            }
        }
    }

    /**
     * Get the effective default version.
     * Returns configured default or first version in list.
     *
     * @return default version string
     */
    public String getEffectiveDefaultVersion() {
        if (defaultVersion != null && !defaultVersion.isBlank()) {
            return defaultVersion;
        }
        if (versions != null && !versions.isEmpty()) {
            return versions.get(0);
        }
        return null;
    }
}
