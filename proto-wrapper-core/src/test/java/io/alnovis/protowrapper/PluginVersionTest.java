package io.alnovis.protowrapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PluginVersion}.
 */
class PluginVersionTest {

    @AfterEach
    void tearDown() {
        PluginVersion.clearCache();
    }

    @Test
    void get_returnsVersion() {
        String version = PluginVersion.get();

        assertThat(version).isNotNull();
        assertThat(version).isNotEmpty();
    }

    @Test
    void get_cachedValue() {
        String version1 = PluginVersion.get();
        String version2 = PluginVersion.get();

        assertThat(version1).isSameAs(version2);
    }

    @Test
    void isKnown_afterMavenBuild() {
        // After Maven build with resource filtering, version should be known
        // In IDE without Maven, it might be "unknown"
        String version = PluginVersion.get();

        if (version.matches("\\d+\\.\\d+\\.\\d+.*")) {
            assertThat(PluginVersion.isKnown()).isTrue();
        }
    }

    @Test
    void get_handlesUnfilteredProperties() {
        // When running from IDE without Maven build,
        // the version.properties file contains ${project.version}
        // In this case, should return "unknown" or valid version
        String version = PluginVersion.get();

        assertThat(version).doesNotContain("${");
    }
}
