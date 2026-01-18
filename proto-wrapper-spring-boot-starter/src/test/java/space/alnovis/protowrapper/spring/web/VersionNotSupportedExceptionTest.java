package space.alnovis.protowrapper.spring.web;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VersionNotSupportedExceptionTest {

    @Test
    void constructor_shouldStoreRequestedVersion() {
        VersionNotSupportedException ex = new VersionNotSupportedException("v3", Arrays.asList("v1", "v2"));

        assertEquals("v3", ex.getRequestedVersion());
    }

    @Test
    void constructor_shouldStoreSupportedVersions() {
        List<String> supported = Arrays.asList("v1", "v2");
        VersionNotSupportedException ex = new VersionNotSupportedException("v3", supported);

        assertEquals(supported, ex.getSupportedVersions());
    }

    @Test
    void constructor_shouldCreateImmutableSupportedVersionsList() {
        List<String> supported = Arrays.asList("v1", "v2");
        VersionNotSupportedException ex = new VersionNotSupportedException("v3", supported);

        assertThrows(UnsupportedOperationException.class, () -> ex.getSupportedVersions().add("v3"));
    }

    @Test
    void getMessage_shouldContainRequestedVersion() {
        VersionNotSupportedException ex = new VersionNotSupportedException("v3", Arrays.asList("v1", "v2"));

        assertTrue(ex.getMessage().contains("v3"));
    }

    @Test
    void getMessage_shouldContainSupportedVersions() {
        VersionNotSupportedException ex = new VersionNotSupportedException("v3", Arrays.asList("v1", "v2"));

        assertTrue(ex.getMessage().contains("v1"));
        assertTrue(ex.getMessage().contains("v2"));
    }
}
