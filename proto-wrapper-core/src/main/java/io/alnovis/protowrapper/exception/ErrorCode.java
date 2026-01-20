package io.alnovis.protowrapper.exception;

/**
 * Error codes for proto-wrapper exceptions.
 *
 * <p>Each error code has a unique identifier and category for programmatic
 * error handling and potential i18n support.</p>
 *
 * <h2>Code Format</h2>
 * <p>Error codes follow the pattern: {@code CATEGORY_SPECIFIC_ERROR}</p>
 * <ul>
 *   <li>SCHEMA_* - Schema validation errors</li>
 *   <li>CONVERSION_* - Runtime conversion errors</li>
 *   <li>VERSION_* - Version-related errors</li>
 *   <li>FIELD_* - Field-specific errors</li>
 * </ul>
 */
public enum ErrorCode {

    // ==================== Schema Validation Errors ====================

    /**
     * Field types are incompatible across versions and cannot be unified.
     */
    SCHEMA_INCOMPATIBLE_TYPES("SCHEMA-001", "Incompatible field types across versions"),

    /**
     * Field number conflict detected (duplicate or reserved).
     */
    SCHEMA_FIELD_NUMBER_CONFLICT("SCHEMA-002", "Field number conflict"),

    /**
     * Oneof structure conflict between versions.
     */
    SCHEMA_ONEOF_CONFLICT("SCHEMA-003", "Oneof structure conflict"),

    /**
     * Circular dependency detected in message references.
     */
    SCHEMA_CIRCULAR_DEPENDENCY("SCHEMA-004", "Circular message dependency"),

    /**
     * Reserved field number or name is being used.
     */
    SCHEMA_RESERVED_FIELD_USED("SCHEMA-005", "Reserved field used"),

    // ==================== Conversion Errors ====================

    /**
     * Enum value is not valid for the target protocol version.
     */
    CONVERSION_ENUM_VALUE_NOT_SUPPORTED("CONV-001", "Enum value not supported in version"),

    /**
     * Numeric value exceeds the range of the target type.
     */
    CONVERSION_TYPE_RANGE_EXCEEDED("CONV-002", "Value exceeds type range"),

    /**
     * Field is not available in the target protocol version.
     */
    CONVERSION_FIELD_NOT_AVAILABLE("CONV-003", "Field not available in version"),

    /**
     * Cannot convert between incompatible types.
     */
    CONVERSION_TYPE_MISMATCH("CONV-004", "Type conversion mismatch"),

    // ==================== Version Errors ====================

    /**
     * Requested protocol version is not supported.
     */
    VERSION_NOT_SUPPORTED("VER-001", "Protocol version not supported"),

    /**
     * Message type does not exist in the requested version.
     */
    VERSION_MESSAGE_NOT_FOUND("VER-002", "Message not found in version"),

    // ==================== Field Errors ====================

    /**
     * Map key not found.
     */
    FIELD_MAP_KEY_NOT_FOUND("FIELD-001", "Map key not found"),

    /**
     * Required field is not set.
     */
    FIELD_REQUIRED_NOT_SET("FIELD-002", "Required field not set"),

    /**
     * Field value is null when non-null expected.
     */
    FIELD_NULL_VALUE("FIELD-003", "Null value for non-nullable field");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * Returns the unique error code identifier.
     *
     * @return error code (e.g., "SCHEMA-001", "CONV-002")
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the default error message for this code.
     *
     * @return default message description
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }

    /**
     * Returns formatted string with code and message.
     *
     * @return formatted error string
     */
    public String format() {
        return String.format("[%s] %s", code, defaultMessage);
    }

    @Override
    public String toString() {
        return code;
    }
}
