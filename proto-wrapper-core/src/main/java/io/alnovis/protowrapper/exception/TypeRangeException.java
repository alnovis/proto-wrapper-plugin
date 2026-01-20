package io.alnovis.protowrapper.exception;

/**
 * Exception thrown when a numeric value exceeds the range of the target type.
 *
 * <p>This exception occurs during type narrowing conversions, such as when
 * a long value from the unified wrapper exceeds the int range of a specific
 * protocol version.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Unified type: long (from v2)
 * // v1 type: int32
 *
 * long value = 3_000_000_000L;  // Exceeds Integer.MAX_VALUE
 * builder.setCount(value);      // Throws TypeRangeException when targeting v1
 * }</pre>
 *
 * @see ConversionException
 */
public class TypeRangeException extends ConversionException {

    /** The source type name. */
    private final String sourceType;
    /** The target type name. */
    private final String targetType;
    /** The value that exceeded range. */
    private final Number value;
    /** The minimum valid value. */
    private final Number minValue;
    /** The maximum valid value. */
    private final Number maxValue;

    /**
     * Creates a new type range exception.
     *
     * @param message the detail message
     * @param context the error context
     * @param sourceType the source type name
     * @param targetType the target type name
     * @param value the value that exceeded range
     * @param minValue the minimum valid value
     * @param maxValue the maximum valid value
     */
    public TypeRangeException(String message, ErrorContext context,
                               String sourceType, String targetType,
                               Number value, Number minValue, Number maxValue) {
        super(ErrorCode.CONVERSION_TYPE_RANGE_EXCEEDED, message, context);
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.value = value;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Returns the source type name.
     *
     * @return source type (e.g., "long")
     */
    public String getSourceType() {
        return sourceType;
    }

    /**
     * Returns the target type name.
     *
     * @return target type (e.g., "int")
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * Returns the value that exceeded the range.
     *
     * @return the out-of-range value
     */
    public Number getValue() {
        return value;
    }

    /**
     * Returns the minimum valid value for the target type.
     *
     * @return minimum value
     */
    public Number getMinValue() {
        return minValue;
    }

    /**
     * Returns the maximum valid value for the target type.
     *
     * @return maximum value
     */
    public Number getMaxValue() {
        return maxValue;
    }

    /**
     * Creates a type range exception for long to int conversion.
     *
     * @param fieldName the field name
     * @param value the value that exceeded range
     * @param version the target version
     * @return new exception instance
     */
    public static TypeRangeException longToInt(String fieldName, long value, String version) {
        ErrorContext context = ErrorContext.builder()
                .fieldName(fieldName)
                .version(version)
                .addDetail("value", value)
                .addDetail("targetType", "int32")
                .build();
        String message = String.format(
                "Value %d exceeds int32 range [%d, %d] for field '%s' in version %s",
                value, Integer.MIN_VALUE, Integer.MAX_VALUE, fieldName, version);
        return new TypeRangeException(
                message, context, "long", "int",
                value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Creates a type range exception for double to float conversion.
     *
     * @param fieldName the field name
     * @param value the value that exceeded range
     * @param version the target version
     * @return new exception instance
     */
    public static TypeRangeException doubleToFloat(String fieldName, double value, String version) {
        ErrorContext context = ErrorContext.builder()
                .fieldName(fieldName)
                .version(version)
                .addDetail("value", value)
                .addDetail("targetType", "float")
                .build();
        String message = String.format(
                "Value %f exceeds float range for field '%s' in version %s",
                value, fieldName, version);
        return new TypeRangeException(
                message, context, "double", "float",
                value, -Float.MAX_VALUE, Float.MAX_VALUE);
    }
}
