package io.alnovis.protowrapper.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GeneratorConfig}.
 */
class GeneratorConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void builder_setsDefaultValues() {
        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .build();

        // Incremental defaults
        assertThat(config.isIncremental()).isTrue();
        assertThat(config.isForceRegenerate()).isFalse();
        assertThat(config.getCacheDirectory()).isNull();
    }

    @Test
    void builder_setsIncrementalSettings() {
        Path cacheDir = tempDir.resolve("cache");

        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .incremental(false)
            .cacheDirectory(cacheDir)
            .forceRegenerate(true)
            .build();

        assertThat(config.isIncremental()).isFalse();
        assertThat(config.getCacheDirectory()).isEqualTo(cacheDir);
        assertThat(config.isForceRegenerate()).isTrue();
    }

    @Test
    void computeConfigHash_returnsSameHashForSameConfig() {
        GeneratorConfig config1 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .apiPackage("com.example.api")
            .generateInterfaces(true)
            .generateBuilders(false)
            .build();

        GeneratorConfig config2 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .apiPackage("com.example.api")
            .generateInterfaces(true)
            .generateBuilders(false)
            .build();

        assertThat(config1.computeConfigHash()).isEqualTo(config2.computeConfigHash());
    }

    @Test
    void computeConfigHash_returnsDifferentHashForDifferentApiPackage() {
        GeneratorConfig config1 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .apiPackage("com.example.api")
            .build();

        GeneratorConfig config2 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .apiPackage("com.other.api")
            .build();

        assertThat(config1.computeConfigHash()).isNotEqualTo(config2.computeConfigHash());
    }

    @Test
    void computeConfigHash_returnsDifferentHashForDifferentGenerateInterfaces() {
        GeneratorConfig config1 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .generateInterfaces(true)
            .build();

        GeneratorConfig config2 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .generateInterfaces(false)
            .build();

        assertThat(config1.computeConfigHash()).isNotEqualTo(config2.computeConfigHash());
    }

    @Test
    void computeConfigHash_returnsDifferentHashForDifferentGenerateBuilders() {
        GeneratorConfig config1 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .generateBuilders(true)
            .build();

        GeneratorConfig config2 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .generateBuilders(false)
            .build();

        assertThat(config1.computeConfigHash()).isNotEqualTo(config2.computeConfigHash());
    }

    @Test
    void computeConfigHash_returns16CharHexString() {
        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .build();

        String hash = config.computeConfigHash();

        assertThat(hash).hasSize(16);
        assertThat(hash).matches("[0-9a-f]+");
    }

    @Test
    void computeConfigHash_includesCustomTypeMappings() {
        GeneratorConfig config1 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .customTypeMapping("MyType", "java.lang.String")
            .build();

        GeneratorConfig config2 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .build();

        assertThat(config1.computeConfigHash()).isNotEqualTo(config2.computeConfigHash());
    }

    @Test
    void computeConfigHash_includesMessageFilters() {
        GeneratorConfig config1 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .includeMessage("Person")
            .build();

        GeneratorConfig config2 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .build();

        assertThat(config1.computeConfigHash()).isNotEqualTo(config2.computeConfigHash());
    }

    @Test
    void computeConfigHash_includesExcludedMessages() {
        GeneratorConfig config1 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .excludeMessage("InternalMessage")
            .build();

        GeneratorConfig config2 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .build();

        assertThat(config1.computeConfigHash()).isNotEqualTo(config2.computeConfigHash());
    }

    @Test
    void computeConfigHash_isStableAcrossCalls() {
        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .apiPackage("com.example.api")
            .generateInterfaces(true)
            .build();

        String hash1 = config.computeConfigHash();
        String hash2 = config.computeConfigHash();
        String hash3 = config.computeConfigHash();

        assertThat(hash1).isEqualTo(hash2).isEqualTo(hash3);
    }

    @Test
    void builder_requiresOutputDirectory() {
        assertThatThrownBy(() -> GeneratorConfig.builder().build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Output directory");
    }

    @Test
    void incrementalSettings_doNotAffectConfigHash() {
        // Incremental settings should NOT affect config hash
        // because they control generation behavior, not output content
        GeneratorConfig config1 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .incremental(true)
            .forceRegenerate(false)
            .build();

        GeneratorConfig config2 = GeneratorConfig.builder()
            .outputDirectory(tempDir)
            .incremental(false)
            .forceRegenerate(true)
            .build();

        assertThat(config1.computeConfigHash()).isEqualTo(config2.computeConfigHash());
    }
}
