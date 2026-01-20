package io.alnovis.protowrapper.spring.context;

import java.util.List;
import java.util.Optional;

/**
 * Provider interface for resolving VersionContext instances by version string.
 *
 * <p>This interface abstracts the mechanism of obtaining VersionContext instances,
 * allowing different implementations (reflection-based, pre-registered, etc.).
 *
 * <p>The generic type parameter is intentionally omitted to avoid direct dependency
 * on generated VersionContext classes at compile time.
 */
public interface VersionContextProvider {

    /**
     * Get VersionContext for the specified version.
     *
     * @param version version string (e.g., "v1", "v2")
     * @return VersionContext for the version
     * @throws io.alnovis.protowrapper.spring.web.VersionNotSupportedException if version is not supported
     */
    Object getContext(String version);

    /**
     * Get VersionContext for the specified version, or empty if not supported.
     *
     * @param version version string
     * @return Optional containing VersionContext, or empty if not supported
     */
    Optional<Object> findContext(String version);

    /**
     * Get the default VersionContext.
     *
     * @return default VersionContext
     */
    Object getDefaultContext();

    /**
     * Get list of all supported versions.
     *
     * @return list of version strings
     */
    List<String> getSupportedVersions();

    /**
     * Get the default version string.
     *
     * @return default version string
     */
    String getDefaultVersion();

    /**
     * Check if a version is supported.
     *
     * @param version version string
     * @return true if supported
     */
    default boolean isSupported(String version) {
        return version != null && getSupportedVersions().contains(version);
    }
}
