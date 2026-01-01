package space.alnovis.protowrapper.exception;

/**
 * Exception thrown when schema validation fails.
 *
 * <p>This exception indicates problems detected during schema merging or
 * validation, such as incompatible types, field conflicts, or structural issues.</p>
 *
 * <h2>Common Causes</h2>
 * <ul>
 *   <li>Incompatible field types between versions (string vs int)</li>
 *   <li>Field number conflicts</li>
 *   <li>Oneof structure inconsistencies</li>
 *   <li>Reserved field violations</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     MergedSchema schema = merger.merge(schemas);
 * } catch (SchemaValidationException e) {
 *     if (e instanceof FieldConflictException fce) {
 *         log.error("Field conflict: {} vs {}",
 *             fce.getType1(), fce.getType2());
 *     }
 * }
 * }</pre>
 *
 * @see FieldConflictException
 * @see OneofConflictException
 */
public class SchemaValidationException extends ProtoWrapperException {

    /**
     * Creates a new schema validation exception.
     *
     * @param errorCode the error code
     * @param message the detail message
     */
    public SchemaValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates a new schema validation exception with context.
     *
     * @param errorCode the error code
     * @param message the detail message
     * @param context the error context
     */
    public SchemaValidationException(ErrorCode errorCode, String message, ErrorContext context) {
        super(errorCode, message, context);
    }

    /**
     * Creates a new schema validation exception with cause.
     *
     * @param errorCode the error code
     * @param message the detail message
     * @param cause the cause
     */
    public SchemaValidationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * Creates a schema validation exception for incompatible types.
     *
     * @param messageType the message type name
     * @param fieldName the field name
     * @param type1 first type
     * @param type2 second type
     * @return new exception instance
     */
    public static SchemaValidationException incompatibleTypes(
            String messageType, String fieldName, String type1, String type2) {
        ErrorContext context = ErrorContext.forField(messageType, fieldName);
        String message = String.format(
                "Incompatible field types: '%s' and '%s' cannot be unified",
                type1, type2);
        return new FieldConflictException(
                ErrorCode.SCHEMA_INCOMPATIBLE_TYPES, message, context, type1, type2);
    }

    /**
     * Creates a schema validation exception for field number conflict.
     *
     * @param messageType the message type name
     * @param fieldNumber the conflicting field number
     * @param existingField the existing field name
     * @param newField the new field name
     * @return new exception instance
     */
    public static SchemaValidationException fieldNumberConflict(
            String messageType, int fieldNumber, String existingField, String newField) {
        ErrorContext context = ErrorContext.builder()
                .messageType(messageType)
                .fieldNumber(fieldNumber)
                .addDetail("existingField", existingField)
                .addDetail("newField", newField)
                .build();
        String message = String.format(
                "Field number %d is used by both '%s' and '%s'",
                fieldNumber, existingField, newField);
        return new SchemaValidationException(ErrorCode.SCHEMA_FIELD_NUMBER_CONFLICT, message, context);
    }
}
