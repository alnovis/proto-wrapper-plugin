package io.alnovis.protowrapper.incremental;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GeneratedFileInfo}.
 */
class GeneratedFileInfoTest {

    @TempDir
    Path tempDir;

    @Test
    void compute_createsInfoFromFile() throws IOException {
        Path file = tempDir.resolve("Test.java");
        Files.writeString(file, "public class Test {}");

        GeneratedFileInfo info = GeneratedFileInfo.compute(file, Set.of("test.proto"));

        assertThat(info.contentHash()).isNotNull();
        assertThat(info.contentHash()).hasSize(64); // SHA-256 = 64 hex chars
        assertThat(info.lastModified()).isGreaterThan(0);
        assertThat(info.sourceProtos()).containsExactly("test.proto");
    }

    @Test
    void compute_differentContentProducesDifferentHash() throws IOException {
        Path file1 = tempDir.resolve("File1.java");
        Path file2 = tempDir.resolve("File2.java");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        GeneratedFileInfo info1 = GeneratedFileInfo.compute(file1, Set.of());
        GeneratedFileInfo info2 = GeneratedFileInfo.compute(file2, Set.of());

        assertThat(info1.contentHash()).isNotEqualTo(info2.contentHash());
    }

    @Test
    void wasModifiedExternally_returnsFalseForUnchangedFile() throws IOException {
        Path file = tempDir.resolve("Test.java");
        Files.writeString(file, "original content");

        GeneratedFileInfo info = GeneratedFileInfo.compute(file, Set.of());

        assertThat(info.wasModifiedExternally(file)).isFalse();
    }

    @Test
    void wasModifiedExternally_returnsTrueForModifiedFile() throws IOException, InterruptedException {
        Path file = tempDir.resolve("Test.java");
        Files.writeString(file, "original content");

        GeneratedFileInfo info = GeneratedFileInfo.compute(file, Set.of());

        // Modify the file
        Thread.sleep(10); // Ensure timestamp changes
        Files.writeString(file, "modified content");

        assertThat(info.wasModifiedExternally(file)).isTrue();
    }

    @Test
    void wasModifiedExternally_returnsTrueForDeletedFile() throws IOException {
        Path file = tempDir.resolve("Test.java");
        Files.writeString(file, "content");

        GeneratedFileInfo info = GeneratedFileInfo.compute(file, Set.of());

        Files.delete(file);

        assertThat(info.wasModifiedExternally(file)).isTrue();
    }

    @Test
    void sourceProtos_isImmutable() {
        GeneratedFileInfo info = new GeneratedFileInfo("hash", 1000, Set.of("test.proto"));

        assertThatThrownBy(() -> info.sourceProtos().add("other.proto"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void toJson_createsValidJson() {
        GeneratedFileInfo info = new GeneratedFileInfo(
            "abc123def456",
            1000,
            Set.of("v1/test.proto", "v2/test.proto")
        );

        String json = info.toJson();

        assertThat(json).contains("\"contentHash\":\"abc123def456\"");
        assertThat(json).contains("\"lastModified\":1000");
        assertThat(json).contains("\"sourceProtos\":");
        assertThat(json).contains("v1/test.proto");
        assertThat(json).contains("v2/test.proto");
    }

    @Test
    void fromJson_parsesValidJson() {
        String json = "{\"contentHash\":\"abc123\",\"lastModified\":2000,\"sourceProtos\":[\"a.proto\",\"b.proto\"]}";

        GeneratedFileInfo info = GeneratedFileInfo.fromJson(json);

        assertThat(info.contentHash()).isEqualTo("abc123");
        assertThat(info.lastModified()).isEqualTo(2000);
        assertThat(info.sourceProtos()).containsExactlyInAnyOrder("a.proto", "b.proto");
    }

    @Test
    void roundTrip_preservesData() {
        GeneratedFileInfo original = new GeneratedFileInfo(
            "hashValue123",
            5000,
            Set.of("proto1.proto", "proto2.proto")
        );

        String json = original.toJson();
        GeneratedFileInfo restored = GeneratedFileInfo.fromJson(json);

        assertThat(restored.contentHash()).isEqualTo(original.contentHash());
        assertThat(restored.lastModified()).isEqualTo(original.lastModified());
        assertThat(restored.sourceProtos()).isEqualTo(original.sourceProtos());
    }

    @Test
    void constructor_rejectsNullContentHash() {
        assertThatThrownBy(() -> new GeneratedFileInfo(null, 1000, Set.of()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_handlesNullSourceProtos() {
        GeneratedFileInfo info = new GeneratedFileInfo("hash", 1000, null);

        assertThat(info.sourceProtos()).isEmpty();
    }

    @Test
    void emptySourceProtos_serializesCorrectly() {
        GeneratedFileInfo info = new GeneratedFileInfo("hash", 1000, Set.of());

        String json = info.toJson();
        GeneratedFileInfo restored = GeneratedFileInfo.fromJson(json);

        assertThat(restored.sourceProtos()).isEmpty();
    }
}
