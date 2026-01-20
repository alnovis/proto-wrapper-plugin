package io.alnovis.protowrapper.exception;

/**
 * Base exception class for all proto-wrapper errors.
 *
 * <p>This is the root of the proto-wrapper exception hierarchy. All exceptions
 * thrown by the library extend this class, enabling unified error handling.</p>
 *
 * <h2>Exception Hierarchy</h2>
 * <pre>
 * ProtoWrapperException
 * +-- SchemaValidationException
 * |   +-- FieldConflictException
 * |   +-- OneofConflictException
 * +-- ConversionException
 * |   +-- EnumValueNotSupportedException
 * |   +-- TypeRangeException
 * |   +-- FieldNotAvailableException
 * +-- VersionNotSupportedException
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     wrapper.setStatus(StatusEnum.DELETED);
 * } catch (ProtoWrapperException e) {
 *     ErrorCode code = e.getErrorCode();
 *     ErrorContext context = e.getContext();
 *     log.error("Proto error [{}] at {}: {}",
 *         code, context.getLocation(), e.getMessage());
 * }
 * }</pre>
 *
 * @see ErrorCode
 * @see ErrorContext
 */
public class ProtoWrapperException extends RuntimeException {

    /** The error code. */
    private final ErrorCode errorCode;
    /** The error context. */
    private final ErrorContext context;

    /**
     * Creates a new exception with error code and message.
     *
     * @param errorCode the error code
     * @param message the detail message
     */
    public ProtoWrapperException(ErrorCode errorCode, String message) {
        super(formatMessage(errorCode, message));
        this.errorCode = errorCode;
        this.context = null;
    }

    /**
     * Creates a new exception with error code, message, and context.
     *
     * @param errorCode the error code
     * @param message the detail message
     * @param context the error context
     */
    public ProtoWrapperException(ErrorCode errorCode, String message, ErrorContext context) {
        super(formatMessage(errorCode, message, context));
        this.errorCode = errorCode;
        this.context = context;
    }

    /**
     * Creates a new exception with error code, message, and cause.
     *
     * @param errorCode the error code
     * @param message the detail message
     * @param cause the cause
     */
    public ProtoWrapperException(ErrorCode errorCode, String message, Throwable cause) {
        super(formatMessage(errorCode, message), cause);
        this.errorCode = errorCode;
        this.context = null;
    }

    /**
     * Creates a new exception with all parameters.
     *
     * @param errorCode the error code
     * @param message the detail message
     * @param context the error context
     * @param cause the cause
     */
    public ProtoWrapperException(ErrorCode errorCode, String message, ErrorContext context, Throwable cause) {
        super(formatMessage(errorCode, message, context), cause);
        this.errorCode = errorCode;
        this.context = context;
    }

    /**
     * Returns the error code for this exception.
     *
     * @return the error code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the error context with additional details.
     *
     * @return the error context, may be null
     */
    public ErrorContext getContext() {
        return context;
    }

    /**
     * Returns the message type from context if available.
     *
     * @return the message type or null
     */
    public String getMessageType() {
        return context != null ? context.getMessageType() : null;
    }

    /**
     * Returns the field path from context if available.
     *
     * @return the field path or null
     */
    public String getFieldPath() {
        return context != null ? context.getFieldPath() : null;
    }

    /**
     * Returns the version from context if available.
     *
     * @return the version or null
     */
    public String getVersion() {
        return context != null ? context.getVersion() : null;
    }

    /**
     * Returns the location string for error reporting.
     *
     * @return location string or "unknown location"
     */
    public String getLocation() {
        return context != null ? context.getLocation() : "unknown location";
    }

    /**
     * Formats the exception message with error code prefix.
     *
     * @param code the error code
     * @param message the message
     * @return the formatted message
     */
    private static String formatMessage(ErrorCode code, String message) {
        return String.format("[%s] %s", code.getCode(), message);
    }

    /**
     * Formats the exception message with error code and context location.
     *
     * @param code the error code
     * @param message the message
     * @param context the error context
     * @return the formatted message
     */
    private static String formatMessage(ErrorCode code, String message, ErrorContext context) {
        if (context == null) {
            return formatMessage(code, message);
        }
        return String.format("[%s] %s (at %s)", code.getCode(), message, context.getLocation());
    }
}
