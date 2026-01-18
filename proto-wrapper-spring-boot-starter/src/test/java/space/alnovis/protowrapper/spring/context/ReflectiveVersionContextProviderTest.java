package space.alnovis.protowrapper.spring.context;

import org.junit.jupiter.api.Test;
import space.alnovis.protowrapper.spring.mock.v1.VersionContextV1;
import space.alnovis.protowrapper.spring.mock.v2.VersionContextV2;
import space.alnovis.protowrapper.spring.web.VersionNotSupportedException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReflectiveVersionContextProviderTest {

    private static final String BASE_PACKAGE = "space.alnovis.protowrapper.spring.mock";

    @Test
    void constructor_shouldLoadAllVersionContexts() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            "v1"
        );

        assertEquals(2, provider.getSupportedVersions().size());
    }

    @Test
    void constructor_shouldUseFirstVersionAsDefaultWhenNotSpecified() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            null
        );

        assertEquals("v1", provider.getDefaultVersion());
    }

    @Test
    void constructor_shouldFailWhenVersionContextNotFound() {
        assertThrows(IllegalStateException.class, () ->
            new ReflectiveVersionContextProvider(
                "com.nonexistent",
                List.of("v1"),
                "v1"
            )
        );
    }

    @Test
    void getContext_shouldReturnCorrectVersionContext() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            "v1"
        );

        VersionContextV1 v1 = (VersionContextV1) provider.getContext("v1");
        VersionContextV2 v2 = (VersionContextV2) provider.getContext("v2");

        assertEquals("v1", v1.getVersion());
        assertEquals("v2", v2.getVersion());
    }

    @Test
    void getContext_shouldThrowForUnsupportedVersion() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            "v1"
        );

        VersionNotSupportedException ex = assertThrows(
            VersionNotSupportedException.class,
            () -> provider.getContext("v3")
        );

        assertEquals("v3", ex.getRequestedVersion());
        assertEquals(Arrays.asList("v1", "v2"), ex.getSupportedVersions());
    }

    @Test
    void getContext_shouldThrowForNullVersion() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            "v1"
        );

        assertThrows(VersionNotSupportedException.class, () -> provider.getContext(null));
    }

    @Test
    void findContext_shouldReturnOptionalWithContext() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            "v1"
        );

        Optional<Object> result = provider.findContext("v2");

        assertTrue(result.isPresent());
        assertInstanceOf(VersionContextV2.class, result.get());
    }

    @Test
    void findContext_shouldReturnEmptyForUnsupportedVersion() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            "v1"
        );

        Optional<Object> result = provider.findContext("v3");

        assertTrue(result.isEmpty());
    }

    @Test
    void findContext_shouldReturnEmptyForNull() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            "v1"
        );

        Optional<Object> result = provider.findContext(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getDefaultContext_shouldReturnConfiguredDefault() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            "v2"
        );

        Object context = provider.getDefaultContext();

        assertInstanceOf(VersionContextV2.class, context);
    }

    @Test
    void getSupportedVersions_shouldReturnImmutableList() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            "v1"
        );

        List<String> versions = provider.getSupportedVersions();

        assertThrows(UnsupportedOperationException.class, () -> versions.add("v3"));
    }

    @Test
    void isSupported_shouldReturnTrueForSupportedVersion() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            "v1"
        );

        assertTrue(provider.isSupported("v1"));
        assertTrue(provider.isSupported("v2"));
    }

    @Test
    void isSupported_shouldReturnFalseForUnsupportedVersion() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            "v1"
        );

        assertFalse(provider.isSupported("v3"));
        assertFalse(provider.isSupported(null));
    }

    @Test
    void getContext_shouldCacheInstances() {
        ReflectiveVersionContextProvider provider = new ReflectiveVersionContextProvider(
            BASE_PACKAGE,
            Arrays.asList("v1", "v2"),
            "v1"
        );

        Object first = provider.getContext("v1");
        Object second = provider.getContext("v1");

        assertSame(first, second, "Should return cached instance");
    }
}
