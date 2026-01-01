package space.alnovis.protowrapper.exception;

import java.util.Set;

/**
 * Exception thrown when accessing a field not available in the current version.
 *
 * <p>This exception occurs when trying to get or set a field that exists in
 * the unified wrapper but is not present in the specific protocol version
 * being used.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // v1: message Order { string name = 1; }
 * // v2: message Order { string name = 1; string description = 2; }
 *
 * // When using v1:
 * builder.setDescription("text");  // Throws FieldNotAvailableException
 * }</pre>
 *
 * @see ConversionException
 */
public class FieldNotAvailableException extends ConversionException {

    private final String fieldName;
    private final Set<String> availableInVersions;

    /**
     * Creates a new field not available exception.
     *
     * @param message the detail message
     * @param context the error context
     * @param fieldName the unavailable field name
     * @param availableInVersions versions where the field exists
     */
    public FieldNotAvailableException(String message, ErrorContext context,
                                       String fieldName, Set<String> availableInVersions) {
        super(ErrorCode.CONVERSION_FIELD_NOT_AVAILABLE, message, context);
        this.fieldName = fieldName;
        this.availableInVersions = availableInVersions != null
                ? Set.copyOf(availableInVersions) : Set.of();
    }

    /**
     * Returns the name of the unavailable field.
     *
     * @return field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the versions where this field is available.
     *
     * @return set of version identifiers
     */
    public Set<String> getAvailableInVersions() {
        return availableInVersions;
    }

    /**
     * Creates a field not available exception with full details.
     *
     * @param messageType the message type name
     * @param fieldName the unavailable field name
     * @param currentVersion the current version
     * @param availableInVersions versions where the field exists
     * @return new exception instance
     */
    public static FieldNotAvailableException of(
            String messageType, String fieldName,
            String currentVersion, Set<String> availableInVersions) {
        ErrorContext context = ErrorContext.builder()
                .messageType(messageType)
                .fieldName(fieldName)
                .fieldPath(messageType + "." + fieldName)
                .version(currentVersion)
                .availableVersions(availableInVersions)
                .build();
        String message = String.format(
                "Field '%s' is not available in protocol version %s. " +
                "This field exists only in versions: %s",
                fieldName, currentVersion, availableInVersions);
        return new FieldNotAvailableException(message, context, fieldName, availableInVersions);
    }
}
