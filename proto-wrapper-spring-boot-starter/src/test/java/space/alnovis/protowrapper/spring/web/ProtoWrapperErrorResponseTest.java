package space.alnovis.protowrapper.spring.web;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProtoWrapperErrorResponseTest {

    @Test
    void constructor_withThreeArgs_shouldStoreAllFields() {
        Map<String, Object> details = Map.of("key", "value");

        ProtoWrapperErrorResponse response = new ProtoWrapperErrorResponse(
            "ERROR_CODE",
            "Error message",
            details
        );

        assertEquals("ERROR_CODE", response.errorCode());
        assertEquals("Error message", response.message());
        assertEquals(details, response.details());
    }

    @Test
    void constructor_withTwoArgs_shouldUseEmptyDetails() {
        ProtoWrapperErrorResponse response = new ProtoWrapperErrorResponse(
            "ERROR_CODE",
            "Error message"
        );

        assertEquals("ERROR_CODE", response.errorCode());
        assertEquals("Error message", response.message());
        assertTrue(response.details().isEmpty());
    }

    @Test
    void record_shouldImplementEquals() {
        ProtoWrapperErrorResponse response1 = new ProtoWrapperErrorResponse("CODE", "msg", Map.of());
        ProtoWrapperErrorResponse response2 = new ProtoWrapperErrorResponse("CODE", "msg", Map.of());

        assertEquals(response1, response2);
    }

    @Test
    void record_shouldImplementHashCode() {
        ProtoWrapperErrorResponse response1 = new ProtoWrapperErrorResponse("CODE", "msg", Map.of());
        ProtoWrapperErrorResponse response2 = new ProtoWrapperErrorResponse("CODE", "msg", Map.of());

        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    void record_shouldImplementToString() {
        ProtoWrapperErrorResponse response = new ProtoWrapperErrorResponse("CODE", "msg");

        String toString = response.toString();
        assertTrue(toString.contains("CODE"));
        assertTrue(toString.contains("msg"));
    }
}
