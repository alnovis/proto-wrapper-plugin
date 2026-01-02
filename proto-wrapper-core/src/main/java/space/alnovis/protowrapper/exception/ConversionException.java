package space.alnovis.protowrapper.exception;

/**
 * Exception thrown when runtime type conversion fails.
 *
 * <p>This exception indicates problems that occur during value conversion
 * between wrapper types and protocol-specific types, such as enum value
 * validation or range checks.</p>
 *
 * <h2>Common Causes</h2>
 * <ul>
 *   <li>Enum value not supported in target version</li>
 *   <li>Numeric value exceeds target type range</li>
 *   <li>Field not available in target version</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     builder.setStatus(StatusEnum.DELETED);
 * } catch (ConversionException e) {
 *     if (e instanceof EnumValueNotSupportedException evns) {
 *         log.warn("Enum {} not valid for version {}",
 *             evns.getEnumValue(), evns.getVersion());
 *     }
 * }
 * }</pre>
 *
 * @see EnumValueNotSupportedException
 * @see TypeRangeException
 * @see FieldNotAvailableException
 */
public class ConversionException extends ProtoWrapperException {

    /**
     * Creates a new conversion exception.
     *
     * @param errorCode the error code
     * @param message the detail message
     */
    public ConversionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates a new conversion exception with context.
     *
     * @param errorCode the error code
     * @param message the detail message
     * @param context the error context
     */
    public ConversionException(ErrorCode errorCode, String message, ErrorContext context) {
        super(errorCode, message, context);
    }

    /**
     * Creates a new conversion exception with cause.
     *
     * @param errorCode the error code
     * @param message the detail message
     * @param cause the cause
     */
    public ConversionException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * Creates a new conversion exception with context and cause.
     *
     * @param errorCode the error code
     * @param message the detail message
     * @param context the error context
     * @param cause the cause
     */
    public ConversionException(ErrorCode errorCode, String message, ErrorContext context, Throwable cause) {
        super(errorCode, message, context, cause);
    }
}
