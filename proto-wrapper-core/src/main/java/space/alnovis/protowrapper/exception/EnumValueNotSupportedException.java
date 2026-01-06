package space.alnovis.protowrapper.exception;

import java.util.Set;

/**
 * Exception thrown when an enum value is not valid for the target protocol version.
 *
 * <p>This exception occurs when trying to set an enum value that exists in the
 * unified wrapper enum but is not present in the specific protocol version
 * being targeted.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Unified enum: ACTIVE(1), INACTIVE(2), PENDING(3), DELETED(4)
 * // v1 enum: ACTIVE(1), INACTIVE(2)
 * // v2 enum: ACTIVE(1), INACTIVE(2), PENDING(3), DELETED(4)
 *
 * // When targeting v1:
 * builder.setStatus(StatusEnum.DELETED);  // Throws EnumValueNotSupportedException
 * }</pre>
 *
 * @see ConversionException
 */
public class EnumValueNotSupportedException extends ConversionException {

    /** The enum type name. */
    private final String enumTypeName;
    /** The invalid enum value name. */
    private final String enumValue;
    /** The enum value number. */
    private final int enumNumber;
    /** Valid values for the target version. */
    private final Set<String> validValues;

    /**
     * Creates a new enum value not supported exception.
     *
     * @param message the detail message
     * @param context the error context
     * @param enumTypeName the enum type name
     * @param enumValue the invalid enum value name
     * @param enumNumber the enum value number
     * @param validValues valid values for the target version
     */
    public EnumValueNotSupportedException(String message, ErrorContext context,
                                           String enumTypeName, String enumValue,
                                           int enumNumber, Set<String> validValues) {
        super(ErrorCode.CONVERSION_ENUM_VALUE_NOT_SUPPORTED, message, context);
        this.enumTypeName = enumTypeName;
        this.enumValue = enumValue;
        this.enumNumber = enumNumber;
        this.validValues = validValues != null ? Set.copyOf(validValues) : Set.of();
    }

    /**
     * Returns the enum type name.
     *
     * @return enum type name (e.g., "StatusEnum")
     */
    public String getEnumTypeName() {
        return enumTypeName;
    }

    /**
     * Returns the invalid enum value name.
     *
     * @return enum value name (e.g., "DELETED")
     */
    public String getEnumValue() {
        return enumValue;
    }

    /**
     * Returns the enum value number.
     *
     * @return enum number (e.g., 4)
     */
    public int getEnumNumber() {
        return enumNumber;
    }

    /**
     * Returns the valid enum values for the target version.
     *
     * @return set of valid value names
     */
    public Set<String> getValidValues() {
        return validValues;
    }

    /**
     * Creates an enum value not supported exception with full details.
     *
     * @param enumTypeName the enum type name
     * @param enumValue the invalid enum value
     * @param enumNumber the enum number
     * @param version the target version
     * @param validValues valid values for the version
     * @return new exception instance
     */
    public static EnumValueNotSupportedException of(
            String enumTypeName, String enumValue, int enumNumber,
            String version, Set<String> validValues) {
        ErrorContext context = ErrorContext.builder()
                .version(version)
                .addDetail("enumType", enumTypeName)
                .addDetail("enumValue", enumValue)
                .addDetail("enumNumber", enumNumber)
                .addDetail("validValues", validValues)
                .build();
        String message = String.format(
                "%s.%s (value=%d) is not supported in protocol version %s. Valid values: %s",
                enumTypeName, enumValue, enumNumber, version, validValues);
        return new EnumValueNotSupportedException(
                message, context, enumTypeName, enumValue, enumNumber, validValues);
    }
}
