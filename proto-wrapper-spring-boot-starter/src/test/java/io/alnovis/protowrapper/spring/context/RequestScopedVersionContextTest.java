package io.alnovis.protowrapper.spring.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.spring.mock.v1.VersionContextV1;

import static org.junit.jupiter.api.Assertions.*;

class RequestScopedVersionContextTest {

    private RequestScopedVersionContext context;

    @BeforeEach
    void setUp() {
        context = new RequestScopedVersionContext();
    }

    @Test
    void isPresent_shouldReturnFalseInitially() {
        assertFalse(context.isPresent());
    }

    @Test
    void isPresent_shouldReturnTrueAfterSet() {
        context.set(new Object(), "v1");

        assertTrue(context.isPresent());
    }

    @Test
    void get_shouldThrowWhenNotSet() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, context::get);

        assertTrue(ex.getMessage().contains("VersionContext not set"));
    }

    @Test
    void get_shouldReturnContextAfterSet() {
        VersionContextV1 mockContext = new VersionContextV1();
        context.set(mockContext, "v1");

        VersionContextV1 result = context.get();

        assertSame(mockContext, result);
    }

    @Test
    void getVersion_shouldReturnVersionAfterSet() {
        context.set(new Object(), "v2");

        assertEquals("v2", context.getVersion());
    }

    @Test
    void getVersion_shouldReturnNullInitially() {
        assertNull(context.getVersion());
    }

    @Test
    void getOrNull_shouldReturnNullInitially() {
        assertNull(context.getOrNull());
    }

    @Test
    void getOrNull_shouldReturnContextAfterSet() {
        VersionContextV1 mockContext = new VersionContextV1();
        context.set(mockContext, "v1");

        VersionContextV1 result = context.getOrNull();

        assertSame(mockContext, result);
    }

    @Test
    void set_shouldOverwritePreviousValues() {
        Object first = new Object();
        Object second = new Object();

        context.set(first, "v1");
        context.set(second, "v2");

        assertSame(second, context.get());
        assertEquals("v2", context.getVersion());
    }

    @Test
    void get_shouldSupportGenericCasting() {
        VersionContextV1 mockContext = new VersionContextV1();
        context.set(mockContext, "v1");

        // Test generic type inference
        VersionContextV1 typed = context.get();
        assertEquals("v1", typed.getVersion());
    }
}
