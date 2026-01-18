package space.alnovis.protowrapper.spring.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtoWrapperExceptionHandlerTest {

    private ProtoWrapperExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProtoWrapperExceptionHandler();
    }

    @Test
    void handleVersionNotSupported_shouldReturn400() {
        List<String> supportedVersions = Arrays.asList("v1", "v2");
        VersionNotSupportedException ex = new VersionNotSupportedException("v3", supportedVersions);

        ResponseEntity<ProtoWrapperErrorResponse> response = handler.handleVersionNotSupported(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleVersionNotSupported_shouldReturnCorrectErrorCode() {
        List<String> supportedVersions = Arrays.asList("v1", "v2");
        VersionNotSupportedException ex = new VersionNotSupportedException("v3", supportedVersions);

        ResponseEntity<ProtoWrapperErrorResponse> response = handler.handleVersionNotSupported(ex);
        ProtoWrapperErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals("VERSION_NOT_SUPPORTED", body.errorCode());
    }

    @Test
    void handleVersionNotSupported_shouldIncludeRequestedVersionInDetails() {
        List<String> supportedVersions = Arrays.asList("v1", "v2");
        VersionNotSupportedException ex = new VersionNotSupportedException("v3", supportedVersions);

        ResponseEntity<ProtoWrapperErrorResponse> response = handler.handleVersionNotSupported(ex);
        ProtoWrapperErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals("v3", body.details().get("requestedVersion"));
    }

    @Test
    void handleVersionNotSupported_shouldIncludeSupportedVersionsInDetails() {
        List<String> supportedVersions = Arrays.asList("v1", "v2");
        VersionNotSupportedException ex = new VersionNotSupportedException("v3", supportedVersions);

        ResponseEntity<ProtoWrapperErrorResponse> response = handler.handleVersionNotSupported(ex);
        ProtoWrapperErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(supportedVersions, body.details().get("supportedVersions"));
    }

    @Test
    void handleIllegalState_shouldReturn500ForVersionContextError() {
        IllegalStateException ex = new IllegalStateException("VersionContext not set");

        ResponseEntity<ProtoWrapperErrorResponse> response = handler.handleIllegalState(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VERSION_CONTEXT_ERROR", response.getBody().errorCode());
    }

    @Test
    void handleIllegalState_shouldRethrowNonVersionContextError() {
        IllegalStateException ex = new IllegalStateException("Some other error");

        assertThrows(IllegalStateException.class, () -> handler.handleIllegalState(ex));
    }

    @Test
    void handleIllegalState_shouldRethrowWhenMessageIsNull() {
        IllegalStateException ex = new IllegalStateException((String) null);

        assertThrows(IllegalStateException.class, () -> handler.handleIllegalState(ex));
    }
}
