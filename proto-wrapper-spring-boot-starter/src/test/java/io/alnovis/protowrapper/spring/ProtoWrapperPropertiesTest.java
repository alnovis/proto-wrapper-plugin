package io.alnovis.protowrapper.spring;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtoWrapperPropertiesTest {

    @Test
    void validate_shouldPassWithValidConfiguration() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();
        props.setBasePackage("com.example.api");
        props.setVersions(Arrays.asList("v1", "v2"));
        props.setDefaultVersion("v2");

        assertDoesNotThrow(props::validate);
    }

    @Test
    void validate_shouldPassWithoutDefaultVersion() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();
        props.setBasePackage("com.example.api");
        props.setVersions(Arrays.asList("v1", "v2"));

        assertDoesNotThrow(props::validate);
    }

    @Test
    void validate_shouldFailWhenBasePackageIsNull() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();
        props.setVersions(Arrays.asList("v1", "v2"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, props::validate);
        assertTrue(ex.getMessage().contains("base-package"));
    }

    @Test
    void validate_shouldFailWhenBasePackageIsBlank() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();
        props.setBasePackage("   ");
        props.setVersions(Arrays.asList("v1", "v2"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, props::validate);
        assertTrue(ex.getMessage().contains("base-package"));
    }

    @Test
    void validate_shouldFailWhenVersionsIsEmptyForReflectiveProvider() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();
        props.setBasePackage("com.example.api");
        props.setVersions(Collections.emptyList());
        props.setProviderType(ProtoWrapperProperties.ProviderType.REFLECTIVE);

        IllegalStateException ex = assertThrows(IllegalStateException.class, props::validate);
        assertTrue(ex.getMessage().contains("versions"));
    }

    @Test
    void validate_shouldPassWhenVersionsIsEmptyForFactoryProvider() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();
        props.setBasePackage("com.example.api");
        props.setVersions(Collections.emptyList());
        props.setProviderType(ProtoWrapperProperties.ProviderType.FACTORY);

        // Factory provider doesn't require versions - it gets them from generated factory
        assertDoesNotThrow(props::validate);
    }

    @Test
    void validate_shouldFailWhenDefaultVersionNotInListForReflectiveProvider() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();
        props.setBasePackage("com.example.api");
        props.setVersions(Arrays.asList("v1", "v2"));
        props.setDefaultVersion("v3");
        props.setProviderType(ProtoWrapperProperties.ProviderType.REFLECTIVE);

        IllegalStateException ex = assertThrows(IllegalStateException.class, props::validate);
        assertTrue(ex.getMessage().contains("default-version"));
        assertTrue(ex.getMessage().contains("v3"));
    }

    @Test
    void validate_shouldPassWhenDefaultVersionNotInListForFactoryProvider() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();
        props.setBasePackage("com.example.api");
        props.setVersions(Arrays.asList("v1", "v2"));
        props.setDefaultVersion("v3");
        props.setProviderType(ProtoWrapperProperties.ProviderType.FACTORY);

        // Factory provider ignores default-version from config - uses factory's default
        assertDoesNotThrow(props::validate);
    }

    @Test
    void getEffectiveDefaultVersion_shouldReturnConfiguredDefault() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();
        props.setVersions(Arrays.asList("v1", "v2", "v3"));
        props.setDefaultVersion("v2");

        assertEquals("v2", props.getEffectiveDefaultVersion());
    }

    @Test
    void getEffectiveDefaultVersion_shouldReturnFirstVersionWhenNoDefault() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();
        props.setVersions(Arrays.asList("v1", "v2", "v3"));

        assertEquals("v1", props.getEffectiveDefaultVersion());
    }

    @Test
    void getEffectiveDefaultVersion_shouldReturnNullWhenNoVersions() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();

        assertNull(props.getEffectiveDefaultVersion());
    }

    @Test
    void defaultValues_shouldBeCorrect() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();

        assertEquals("X-Protocol-Version", props.getVersionHeader());
        assertTrue(props.isRequestScoped());
        assertTrue(props.isExceptionHandling());
        assertTrue(props.getVersions().isEmpty());
        assertNull(props.getBasePackage());
        assertNull(props.getDefaultVersion());
        assertEquals(ProtoWrapperProperties.ProviderType.FACTORY, props.getProviderType());
    }

    @Test
    void settersAndGetters_shouldWork() {
        ProtoWrapperProperties props = new ProtoWrapperProperties();

        props.setBasePackage("com.test");
        props.setVersions(List.of("v1"));
        props.setDefaultVersion("v1");
        props.setVersionHeader("X-Custom-Version");
        props.setRequestScoped(false);
        props.setExceptionHandling(false);
        props.setProviderType(ProtoWrapperProperties.ProviderType.REFLECTIVE);

        assertEquals("com.test", props.getBasePackage());
        assertEquals(List.of("v1"), props.getVersions());
        assertEquals("v1", props.getDefaultVersion());
        assertEquals("X-Custom-Version", props.getVersionHeader());
        assertFalse(props.isRequestScoped());
        assertFalse(props.isExceptionHandling());
        assertEquals(ProtoWrapperProperties.ProviderType.REFLECTIVE, props.getProviderType());
    }
}
