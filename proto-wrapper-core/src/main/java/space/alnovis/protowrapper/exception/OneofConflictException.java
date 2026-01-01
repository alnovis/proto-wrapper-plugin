package space.alnovis.protowrapper.exception;

import java.util.Set;

/**
 * Exception thrown when oneof structures conflict between protocol versions.
 *
 * <p>This exception indicates structural inconsistencies in oneof definitions,
 * such as fields moving in/out of oneofs or renamed oneofs.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // v1: oneof choice { string a = 1; string b = 2; }
 * // v2: oneof choice { string a = 1; string c = 3; }  // b removed, c added
 *
 * // Throws OneofConflictException for field set difference
 * }</pre>
 *
 * @see SchemaValidationException
 */
public class OneofConflictException extends SchemaValidationException {

    private final String oneofName;
    private final Set<String> versionsAffected;

    /**
     * Creates a new oneof conflict exception.
     *
     * @param errorCode the error code
     * @param message the detail message
     * @param context the error context
     * @param oneofName the oneof name
     * @param versionsAffected versions with the conflict
     */
    public OneofConflictException(ErrorCode errorCode, String message, ErrorContext context,
                                   String oneofName, Set<String> versionsAffected) {
        super(errorCode, message, context);
        this.oneofName = oneofName;
        this.versionsAffected = versionsAffected != null ? Set.copyOf(versionsAffected) : Set.of();
    }

    /**
     * Returns the name of the conflicting oneof.
     *
     * @return oneof name
     */
    public String getOneofName() {
        return oneofName;
    }

    /**
     * Returns the versions affected by this conflict.
     *
     * @return set of version identifiers
     */
    public Set<String> getVersionsAffected() {
        return versionsAffected;
    }

    /**
     * Creates a oneof conflict exception for field membership change.
     *
     * @param messageType the message type name
     * @param oneofName the oneof name
     * @param fieldName the field that moved
     * @param fromVersions versions where field was in oneof
     * @param toVersions versions where field is not in oneof
     * @return new exception instance
     */
    public static OneofConflictException fieldMembershipChange(
            String messageType, String oneofName, String fieldName,
            Set<String> fromVersions, Set<String> toVersions) {
        ErrorContext context = ErrorContext.builder()
                .messageType(messageType)
                .fieldName(fieldName)
                .addDetail("oneofName", oneofName)
                .addDetail("inOneofVersions", fromVersions)
                .addDetail("notInOneofVersions", toVersions)
                .build();
        String message = String.format(
                "Field '%s' is in oneof '%s' in versions %s but not in versions %s",
                fieldName, oneofName, fromVersions, toVersions);
        return new OneofConflictException(
                ErrorCode.SCHEMA_ONEOF_CONFLICT, message, context, oneofName, fromVersions);
    }
}
