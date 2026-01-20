package io.alnovis.protowrapper.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.alnovis.protowrapper.PluginLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtocResolverTest {

    @TempDir
    Path tempDir;

    private ProtocResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ProtocResolver(PluginLogger.noop(), tempDir);
    }

    @Test
    void shouldDetectLinuxX86_64Classifier() {
        // Test classifier detection logic
        String classifier = resolver.detectClassifier();
        assertThat(classifier).matches("(linux|osx|windows)-(x86_64|aarch_64)");
    }

    @Test
    void shouldBuildCorrectDownloadUrl() {
        resolver.setProtocVersion("4.28.2");
        String url = resolver.buildDownloadUrl("linux-x86_64");

        assertThat(url).isEqualTo(
            "https://repo1.maven.org/maven2/com/google/protobuf/protoc/4.28.2/protoc-4.28.2-linux-x86_64.exe"
        );
    }

    @Test
    void shouldBuildCorrectFileName() {
        resolver.setProtocVersion("4.28.2");

        String fileName = resolver.buildFileName("linux-x86_64");

        // On Unix, no .exe extension in cached file name
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            assertThat(fileName).isEqualTo("protoc-4.28.2-linux-x86_64.exe");
        } else {
            assertThat(fileName).isEqualTo("protoc-4.28.2-linux-x86_64");
        }
    }

    @Test
    void shouldUseCustomPathIfProvided() throws IOException {
        // Create a fake protoc executable
        Path fakeProtoc = tempDir.resolve("fake-protoc");
        Files.writeString(fakeProtoc, "#!/bin/bash\necho 'libprotoc 4.28.2'");
        assertThat(fakeProtoc.toFile().setExecutable(true)).isTrue();

        Path resolved = resolver.resolve(fakeProtoc.toString());

        assertThat(resolved).isEqualTo(fakeProtoc);
    }

    @Test
    void shouldThrowIfCustomPathNotFound() {
        String nonExistentPath = "/non/existent/protoc";

        assertThatThrownBy(() -> resolver.resolve(nonExistentPath))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Custom protoc not found");
    }

    @Test
    void shouldReturnDefaultCacheDir() {
        Path defaultCacheDir = ProtocResolver.getDefaultCacheDir();

        assertThat(defaultCacheDir).isNotNull();
        assertThat(defaultCacheDir.toString()).contains("proto-wrapper");
    }

    @Test
    void shouldSetAndGetProtocVersion() {
        resolver.setProtocVersion("25.1");

        assertThat(resolver.getProtocVersion()).isEqualTo("25.1");
    }

    @Test
    void shouldGetCacheDir() {
        assertThat(resolver.getCacheDir()).isEqualTo(tempDir);
    }

    @Test
    void shouldClearCache() throws IOException {
        // Create some files in cache
        Files.writeString(tempDir.resolve("protoc-test"), "test");
        Files.writeString(tempDir.resolve("protoc-test2"), "test2");

        resolver.clearCache();

        assertThat(Files.list(tempDir).count()).isZero();
    }

    @Test
    void shouldUseCachedProtocIfExists() throws IOException {
        // Pre-create a cached protoc
        resolver.setProtocVersion("4.28.2");
        String classifier = resolver.detectClassifier();
        String fileName = resolver.buildFileName(classifier);
        Path cachedProtoc = tempDir.resolve(fileName);

        Files.writeString(cachedProtoc, "#!/bin/bash\necho 'libprotoc 4.28.2'");
        assertThat(cachedProtoc.toFile().setExecutable(true)).isTrue();

        // Should return cached file without downloading
        Path resolved = resolver.resolveEmbedded();

        assertThat(resolved).isEqualTo(cachedProtoc);
    }

    @Test
    void shouldFindSystemProtocIfAvailable() {
        // This test depends on system protoc being installed
        Path systemProtoc = resolver.findSystemProtoc();

        if (systemProtoc != null) {
            assertThat(systemProtoc.toString()).isEqualTo("protoc");
        }
        // If not installed, null is acceptable
    }
}
