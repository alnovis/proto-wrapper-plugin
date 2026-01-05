package space.alnovis.protowrapper.incremental;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FileFingerprint}.
 */
class FileFingerprintTest {

    @TempDir
    Path tempDir;

    private Path testFile;

    @BeforeEach
    void setUp() throws IOException {
        testFile = tempDir.resolve("test.proto");
        Files.writeString(testFile, "syntax = \"proto3\";\nmessage Test {}");
    }

    @Test
    void compute_createsValidFingerprint() throws IOException {
        FileFingerprint fp = FileFingerprint.compute(testFile, tempDir);

        assertThat(fp.relativePath()).isEqualTo("test.proto");
        assertThat(fp.contentHash()).isNotEmpty();
        assertThat(fp.contentHash()).hasSize(64); // SHA-256 hex = 64 chars
        assertThat(fp.lastModified()).isPositive();
        assertThat(fp.fileSize()).isPositive();
    }

    @Test
    void compute_handlesNestedPath() throws IOException {
        Path nested = tempDir.resolve("v1/common/test.proto");
        Files.createDirectories(nested.getParent());
        Files.writeString(nested, "syntax = \"proto3\";");

        FileFingerprint fp = FileFingerprint.compute(nested, tempDir);

        assertThat(fp.relativePath()).isEqualTo("v1/common/test.proto");
    }

    @Test
    void compute_usesForwardSlashesOnAllPlatforms() throws IOException {
        Path nested = tempDir.resolve("v1").resolve("test.proto");
        Files.createDirectories(nested.getParent());
        Files.writeString(nested, "content");

        FileFingerprint fp = FileFingerprint.compute(nested, tempDir);

        assertThat(fp.relativePath()).doesNotContain("\\");
        assertThat(fp.relativePath()).contains("/");
    }

    @Test
    void matches_returnsTrueForSameContent() throws IOException {
        FileFingerprint fp1 = FileFingerprint.compute(testFile, tempDir);

        // Create another file with same content
        Path otherFile = tempDir.resolve("other.proto");
        Files.writeString(otherFile, Files.readString(testFile));

        FileFingerprint fp2 = FileFingerprint.compute(otherFile, tempDir);

        assertThat(fp1.matches(fp2)).isTrue();
    }

    @Test
    void matches_returnsFalseForDifferentContent() throws IOException {
        FileFingerprint fp1 = FileFingerprint.compute(testFile, tempDir);

        Files.writeString(testFile, "different content");
        FileFingerprint fp2 = FileFingerprint.compute(testFile, tempDir);

        assertThat(fp1.matches(fp2)).isFalse();
    }

    @Test
    void matches_returnsFalseForNull() throws IOException {
        FileFingerprint fp = FileFingerprint.compute(testFile, tempDir);

        assertThat(fp.matches(null)).isFalse();
    }

    @Test
    void mightHaveChanged_returnsTrueForNull() throws IOException {
        FileFingerprint fp = FileFingerprint.compute(testFile, tempDir);

        assertThat(fp.mightHaveChanged(null)).isTrue();
    }

    @Test
    void mightHaveChanged_returnsFalseForSameFile() throws IOException {
        FileFingerprint fp = FileFingerprint.compute(testFile, tempDir);

        assertThat(fp.mightHaveChanged(fp)).isFalse();
    }

    @Test
    void mightHaveChanged_returnsTrueForDifferentSize() throws IOException {
        FileFingerprint fp1 = FileFingerprint.compute(testFile, tempDir);

        FileFingerprint fp2 = new FileFingerprint(
            fp1.relativePath(),
            fp1.contentHash(),
            fp1.lastModified(),
            fp1.fileSize() + 1 // Different size
        );

        assertThat(fp1.mightHaveChanged(fp2)).isTrue();
    }

    @Test
    void toJson_createsValidJson() throws IOException {
        FileFingerprint fp = FileFingerprint.compute(testFile, tempDir);

        String json = fp.toJson();

        assertThat(json).contains("\"relativePath\":");
        assertThat(json).contains("\"contentHash\":");
        assertThat(json).contains("\"lastModified\":");
        assertThat(json).contains("\"fileSize\":");
        assertThat(json).contains("test.proto");
    }

    @Test
    void fromJson_parsesValidJson() throws IOException {
        FileFingerprint original = FileFingerprint.compute(testFile, tempDir);
        String json = original.toJson();

        FileFingerprint parsed = FileFingerprint.fromJson(json);

        assertThat(parsed.relativePath()).isEqualTo(original.relativePath());
        assertThat(parsed.contentHash()).isEqualTo(original.contentHash());
        assertThat(parsed.lastModified()).isEqualTo(original.lastModified());
        assertThat(parsed.fileSize()).isEqualTo(original.fileSize());
    }

    @Test
    void fromJson_handlesEscapedCharacters() {
        String json = "{\"relativePath\":\"path\\\\with\\\"quotes\",\"contentHash\":\"abc\",\"lastModified\":123,\"fileSize\":456}";

        FileFingerprint fp = FileFingerprint.fromJson(json);

        assertThat(fp.relativePath()).isEqualTo("path\\with\"quotes");
    }

    @Test
    void fromJson_throwsForMissingKey() {
        String json = "{\"contentHash\":\"abc\",\"lastModified\":123,\"fileSize\":456}";

        assertThatThrownBy(() -> FileFingerprint.fromJson(json))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("relativePath");
    }

    @Test
    void constructor_rejectsNullRelativePath() {
        assertThatThrownBy(() -> new FileFingerprint(null, "hash", 0, 0))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsNegativeFileSize() {
        assertThatThrownBy(() -> new FileFingerprint("path", "hash", 0, -1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isNewerThan_comparesTimestamps() {
        FileFingerprint older = new FileFingerprint("path", "hash", 1000, 100);
        FileFingerprint newer = new FileFingerprint("path", "hash", 2000, 100);

        assertThat(newer.isNewerThan(older)).isTrue();
        assertThat(older.isNewerThan(newer)).isFalse();
        assertThat(newer.isNewerThan(null)).isTrue();
    }
}
