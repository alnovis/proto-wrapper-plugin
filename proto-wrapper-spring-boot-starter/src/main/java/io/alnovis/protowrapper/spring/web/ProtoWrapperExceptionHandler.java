package io.alnovis.protowrapper.spring.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * Global exception handler for proto-wrapper exceptions.
 *
 * <p>Provides consistent error responses for:
 * <ul>
 *   <li>{@link VersionNotSupportedException} - 400 Bad Request</li>
 * </ul>
 */
@ControllerAdvice
public class ProtoWrapperExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProtoWrapperExceptionHandler.class);

    /**
     * Handles VersionNotSupportedException.
     *
     * @param ex the exception
     * @return error response with 400 status
     */
    @ExceptionHandler(VersionNotSupportedException.class)
    public ResponseEntity<ProtoWrapperErrorResponse> handleVersionNotSupported(
            VersionNotSupportedException ex) {

        log.warn("Version not supported: {}", ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ProtoWrapperErrorResponse(
                "VERSION_NOT_SUPPORTED",
                ex.getMessage(),
                Map.of(
                    "requestedVersion", ex.getRequestedVersion(),
                    "supportedVersions", ex.getSupportedVersions()
                )
            ));
    }

    /**
     * Handles IllegalStateException from RequestScopedVersionContext.
     *
     * @param ex the exception
     * @return error response with 500 status
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProtoWrapperErrorResponse> handleIllegalState(
            IllegalStateException ex) {

        // Only handle proto-wrapper related IllegalStateException
        if (ex.getMessage() != null && ex.getMessage().contains("VersionContext")) {
            log.error("VersionContext error: {}", ex.getMessage());

            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ProtoWrapperErrorResponse(
                    "VERSION_CONTEXT_ERROR",
                    ex.getMessage()
                ));
        }

        // Re-throw non-proto-wrapper exceptions
        throw ex;
    }
}
