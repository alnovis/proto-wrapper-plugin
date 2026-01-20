package io.alnovis.protowrapper.spring.web;

import java.util.List;

/**
 * Exception thrown when a requested protocol version is not supported.
 */
public class VersionNotSupportedException extends RuntimeException {

    private final String requestedVersion;
    private final List<String> supportedVersions;

    /**
     * Creates a new VersionNotSupportedException.
     *
     * @param requestedVersion the version that was requested
     * @param supportedVersions list of supported versions
     */
    public VersionNotSupportedException(String requestedVersion, List<String> supportedVersions) {
        super(String.format("Version '%s' is not supported. Supported versions: %s",
            requestedVersion, supportedVersions));
        this.requestedVersion = requestedVersion;
        this.supportedVersions = List.copyOf(supportedVersions);
    }

    /**
     * Get the version that was requested.
     *
     * @return requested version string
     */
    public String getRequestedVersion() {
        return requestedVersion;
    }

    /**
     * Get the list of supported versions.
     *
     * @return list of supported version strings
     */
    public List<String> getSupportedVersions() {
        return supportedVersions;
    }
}
