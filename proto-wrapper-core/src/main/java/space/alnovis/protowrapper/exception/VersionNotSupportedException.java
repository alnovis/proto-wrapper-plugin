package space.alnovis.protowrapper.exception;

import java.util.Set;

/**
 * Exception thrown when a requested protocol version is not supported.
 *
 * <p>This exception indicates that the specified version is not available
 * for the requested operation, such as creating a builder or parsing a message.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Supported versions: v1, v2, v3
 *
 * VersionContext ctx = VersionContext.forVersion("v999");  // Throws VersionNotSupportedException
 * }</pre>
 *
 * @see ProtoWrapperException
 */
public class VersionNotSupportedException extends ProtoWrapperException {

    /** The requested (unsupported) version. */
    private final String requestedVersion;
    /** The set of supported versions. */
    private final Set<String> supportedVersions;

    /**
     * Creates a new version not supported exception.
     *
     * @param message the detail message
     * @param context the error context
     * @param requestedVersion the unsupported version
     * @param supportedVersions available versions
     */
    public VersionNotSupportedException(String message, ErrorContext context,
                                          String requestedVersion, Set<String> supportedVersions) {
        super(ErrorCode.VERSION_NOT_SUPPORTED, message, context);
        this.requestedVersion = requestedVersion;
        this.supportedVersions = supportedVersions != null
                ? Set.copyOf(supportedVersions) : Set.of();
    }

    /**
     * Returns the requested (unsupported) version.
     *
     * @return version string
     */
    public String getRequestedVersion() {
        return requestedVersion;
    }

    /**
     * Returns the set of supported versions.
     *
     * @return set of version identifiers
     */
    public Set<String> getSupportedVersions() {
        return supportedVersions;
    }

    /**
     * Creates a version not supported exception for general version lookup.
     *
     * @param version the unsupported version
     * @param supportedVersions available versions
     * @return new exception instance
     */
    public static VersionNotSupportedException of(String version, Set<String> supportedVersions) {
        ErrorContext context = ErrorContext.builder()
                .version(version)
                .availableVersions(supportedVersions)
                .build();
        String message = String.format(
                "Protocol version '%s' is not supported. Supported versions: %s",
                version, supportedVersions);
        return new VersionNotSupportedException(message, context, version, supportedVersions);
    }

    /**
     * Creates a version not supported exception for message-specific version lookup.
     *
     * @param messageType the message type name
     * @param version the unsupported version
     * @param supportedVersions versions that support this message
     * @return new exception instance
     */
    public static VersionNotSupportedException forMessage(
            String messageType, String version, Set<String> supportedVersions) {
        ErrorContext context = ErrorContext.builder()
                .messageType(messageType)
                .version(version)
                .availableVersions(supportedVersions)
                .build();
        String message = String.format(
                "Message '%s' is not available in protocol version '%s'. " +
                "Supported versions: %s",
                messageType, version, supportedVersions);
        return new VersionNotSupportedException(message, context, version, supportedVersions);
    }
}
