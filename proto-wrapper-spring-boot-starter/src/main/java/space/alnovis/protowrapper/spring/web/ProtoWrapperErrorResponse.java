package space.alnovis.protowrapper.spring.web;

import java.util.Map;

/**
 * Standard error response for proto-wrapper exceptions.
 *
 * @param errorCode machine-readable error code
 * @param message human-readable error message
 * @param details additional error details
 */
public record ProtoWrapperErrorResponse(
    String errorCode,
    String message,
    Map<String, Object> details
) {

    /**
     * Create error response with just code and message.
     *
     * @param errorCode error code
     * @param message error message
     */
    public ProtoWrapperErrorResponse(String errorCode, String message) {
        this(errorCode, message, Map.of());
    }
}
