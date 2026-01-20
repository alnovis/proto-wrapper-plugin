package io.alnovis.protowrapper.exception;

/**
 * Exception thrown when field types conflict between protocol versions.
 *
 * <p>This exception provides detailed information about the conflicting types,
 * enabling users to understand and resolve the incompatibility.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // v1: message Order { string status = 1; }
 * // v2: message Order { int32 status = 1; }
 *
 * // Throws FieldConflictException because string and int32 are incompatible
 * }</pre>
 *
 * @see SchemaValidationException
 */
public class FieldConflictException extends SchemaValidationException {

    /** First conflicting type. */
    private final String type1;
    /** Second conflicting type. */
    private final String type2;

    /**
     * Creates a new field conflict exception.
     *
     * @param errorCode the error code
     * @param message the detail message
     * @param context the error context
     * @param type1 first conflicting type
     * @param type2 second conflicting type
     */
    public FieldConflictException(ErrorCode errorCode, String message, ErrorContext context,
                                   String type1, String type2) {
        super(errorCode, message, context);
        this.type1 = type1;
        this.type2 = type2;
    }

    /**
     * Returns the first conflicting type.
     *
     * @return first type (e.g., "String", "int")
     */
    public String getType1() {
        return type1;
    }

    /**
     * Returns the second conflicting type.
     *
     * @return second type (e.g., "Message", "enum")
     */
    public String getType2() {
        return type2;
    }

    /**
     * Creates a field conflict exception for two incompatible types.
     *
     * @param messageType the message type name
     * @param fieldName the field name
     * @param type1 first type
     * @param type2 second type
     * @return new exception instance
     */
    public static FieldConflictException of(String messageType, String fieldName,
                                             String type1, String type2) {
        ErrorContext context = ErrorContext.forField(messageType, fieldName);
        String message = String.format(
                "Field '%s' has incompatible types: '%s' in some versions, '%s' in others",
                fieldName, type1, type2);
        return new FieldConflictException(
                ErrorCode.SCHEMA_INCOMPATIBLE_TYPES, message, context, type1, type2);
    }
}
