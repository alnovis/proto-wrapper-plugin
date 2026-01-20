package io.alnovis.protowrapper.incremental;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ChangeDetector}.
 */
class ChangeDetectorTest {

    /** Test version constant - use for tests that don't depend on actual plugin version */
    private static final String TEST_VERSION = "1.0.0-test";
    private static final String TEST_CONFIG = "test-config-hash";

    @TempDir
    Path tempDir;

    private Path protoRoot;

    @BeforeEach
    void setUp() throws IOException {
        protoRoot = tempDir.resolve("proto");
        Files.createDirectories(protoRoot);
    }

    @Test
    void detectChanges_detectsNewFiles() throws IOException {
        // Previous state: empty
        IncrementalState previous = IncrementalState.empty();

        // Current: one file
        Path newFile = protoRoot.resolve("new.proto");
        Files.writeString(newFile, "syntax = \"proto3\";");

        ChangeDetector detector = new ChangeDetector(protoRoot, previous);
        ChangeDetector.ChangeResult result = detector.detectChanges(Set.of(newFile));

        assertThat(result.added()).containsExactly("new.proto");
        assertThat(result.modified()).isEmpty();
        assertThat(result.deleted()).isEmpty();
        assertThat(result.hasChanges()).isTrue();
    }

    @Test
    void detectChanges_detectsModifiedFiles() throws IOException {
        // Create file and get its fingerprint
        Path file = protoRoot.resolve("test.proto");
        Files.writeString(file, "original content");
        FileFingerprint originalFp = FileFingerprint.compute(file, protoRoot);

        // Previous state with original fingerprint
        IncrementalState previous = new IncrementalState(
            TEST_VERSION, TEST_CONFIG,
            Map.of("test.proto", originalFp),
            Map.of(),
            null,
            Instant.now()
        );

        // Modify file
        Files.writeString(file, "modified content");

        ChangeDetector detector = new ChangeDetector(protoRoot, previous);
        ChangeDetector.ChangeResult result = detector.detectChanges(Set.of(file));

        assertThat(result.added()).isEmpty();
        assertThat(result.modified()).containsExactly("test.proto");
        assertThat(result.deleted()).isEmpty();
        assertThat(result.hasChanges()).isTrue();
    }

    @Test
    void detectChanges_detectsDeletedFiles() throws IOException {
        // Previous state with a file that no longer exists
        FileFingerprint deletedFp = new FileFingerprint("deleted.proto", "hash", 1000, 100);
        IncrementalState previous = new IncrementalState(
            TEST_VERSION, TEST_CONFIG,
            Map.of("deleted.proto", deletedFp),
            Map.of(),
            null,
            Instant.now()
        );

        // Current: no files
        ChangeDetector detector = new ChangeDetector(protoRoot, previous);
        ChangeDetector.ChangeResult result = detector.detectChanges(Set.of());

        assertThat(result.added()).isEmpty();
        assertThat(result.modified()).isEmpty();
        assertThat(result.deleted()).containsExactly("deleted.proto");
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.hasDeleted()).isTrue();
    }

    @Test
    void detectChanges_noChangesForIdenticalFiles() throws IOException {
        // Create file
        Path file = protoRoot.resolve("unchanged.proto");
        Files.writeString(file, "unchanged content");
        FileFingerprint fp = FileFingerprint.compute(file, protoRoot);

        // Previous state with same fingerprint
        IncrementalState previous = new IncrementalState(
            TEST_VERSION, TEST_CONFIG,
            Map.of("unchanged.proto", fp),
            Map.of(),
            null,
            Instant.now()
        );

        ChangeDetector detector = new ChangeDetector(protoRoot, previous);
        ChangeDetector.ChangeResult result = detector.detectChanges(Set.of(file));

        assertThat(result.added()).isEmpty();
        assertThat(result.modified()).isEmpty();
        assertThat(result.deleted()).isEmpty();
        assertThat(result.hasChanges()).isFalse();
    }

    @Test
    void detectChanges_handlesMultipleChanges() throws IOException {
        // Create existing file
        Path existing = protoRoot.resolve("existing.proto");
        Files.writeString(existing, "original");
        FileFingerprint existingFp = FileFingerprint.compute(existing, protoRoot);

        // Previous state
        FileFingerprint deletedFp = new FileFingerprint("deleted.proto", "hash", 1000, 100);
        Map<String, FileFingerprint> fingerprints = new HashMap<>();
        fingerprints.put("existing.proto", existingFp);
        fingerprints.put("deleted.proto", deletedFp);

        IncrementalState previous = new IncrementalState(
            TEST_VERSION, TEST_CONFIG, fingerprints, Map.of(), null, Instant.now()
        );

        // Modify existing and add new
        Files.writeString(existing, "modified");
        Path newFile = protoRoot.resolve("new.proto");
        Files.writeString(newFile, "new content");

        ChangeDetector detector = new ChangeDetector(protoRoot, previous);
        ChangeDetector.ChangeResult result = detector.detectChanges(Set.of(existing, newFile));

        assertThat(result.added()).containsExactly("new.proto");
        assertThat(result.modified()).containsExactly("existing.proto");
        assertThat(result.deleted()).containsExactly("deleted.proto");
        assertThat(result.totalChanges()).isEqualTo(3);
    }

    @Test
    void getChangedFiles_combinesAddedAndModified() throws IOException {
        Path added = protoRoot.resolve("added.proto");
        Files.writeString(added, "added");

        Path modified = protoRoot.resolve("modified.proto");
        Files.writeString(modified, "original");
        FileFingerprint modifiedFp = FileFingerprint.compute(modified, protoRoot);

        IncrementalState previous = new IncrementalState(
            TEST_VERSION, TEST_CONFIG,
            Map.of("modified.proto", modifiedFp),
            Map.of(),
            null,
            Instant.now()
        );

        Files.writeString(modified, "changed");

        ChangeDetector detector = new ChangeDetector(protoRoot, previous);
        ChangeDetector.ChangeResult result = detector.detectChanges(Set.of(added, modified));

        Set<String> changed = result.getChangedFiles();
        assertThat(changed).containsExactlyInAnyOrder("added.proto", "modified.proto");
    }

    @Test
    void computeFingerprints_computesAllFingerprints() throws IOException {
        Path file1 = protoRoot.resolve("file1.proto");
        Path file2 = protoRoot.resolve("file2.proto");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        ChangeDetector detector = new ChangeDetector(protoRoot, IncrementalState.empty());
        Map<String, FileFingerprint> fingerprints = detector.computeFingerprints(Set.of(file1, file2));

        assertThat(fingerprints).hasSize(2);
        assertThat(fingerprints).containsKey("file1.proto");
        assertThat(fingerprints).containsKey("file2.proto");
    }

    @Test
    void mightHaveChanges_returnsFalseForUnchanged() throws IOException {
        Path file = protoRoot.resolve("test.proto");
        Files.writeString(file, "content");
        FileFingerprint fp = FileFingerprint.compute(file, protoRoot);

        IncrementalState previous = new IncrementalState(
            TEST_VERSION, TEST_CONFIG,
            Map.of("test.proto", fp),
            Map.of(),
            null,
            Instant.now()
        );

        ChangeDetector detector = new ChangeDetector(protoRoot, previous);
        boolean might = detector.mightHaveChanges(Set.of(file));

        assertThat(might).isFalse();
    }

    @Test
    void mightHaveChanges_returnsTrueForDifferentFileCount() throws IOException {
        Path file1 = protoRoot.resolve("file1.proto");
        Files.writeString(file1, "content1");
        FileFingerprint fp1 = FileFingerprint.compute(file1, protoRoot);

        IncrementalState previous = new IncrementalState(
            TEST_VERSION, TEST_CONFIG,
            Map.of("file1.proto", fp1),
            Map.of(),
            null,
            Instant.now()
        );

        // Add another file
        Path file2 = protoRoot.resolve("file2.proto");
        Files.writeString(file2, "content2");

        ChangeDetector detector = new ChangeDetector(protoRoot, previous);
        boolean might = detector.mightHaveChanges(Set.of(file1, file2));

        assertThat(might).isTrue();
    }

    @Test
    void mightHaveChanges_returnsTrueForNewFile() throws IOException {
        IncrementalState previous = IncrementalState.empty();

        Path newFile = protoRoot.resolve("new.proto");
        Files.writeString(newFile, "content");

        ChangeDetector detector = new ChangeDetector(protoRoot, previous);
        boolean might = detector.mightHaveChanges(Set.of(newFile));

        assertThat(might).isTrue();
    }

    @Test
    void changeResult_toString_providesUsefulInfo() {
        ChangeDetector.ChangeResult result = new ChangeDetector.ChangeResult(
            Set.of("added.proto"),
            Set.of("modified.proto"),
            Set.of("deleted.proto"),
            Map.of()
        );

        String str = result.toString();

        assertThat(str).contains("added=1");
        assertThat(str).contains("modified=1");
        assertThat(str).contains("deleted=1");
    }

    @Test
    void detectChanges_handlesNestedPaths() throws IOException {
        Files.createDirectories(protoRoot.resolve("v1"));

        Path nested = protoRoot.resolve("v1/nested.proto");
        Files.writeString(nested, "content");

        IncrementalState previous = IncrementalState.empty();
        ChangeDetector detector = new ChangeDetector(protoRoot, previous);
        ChangeDetector.ChangeResult result = detector.detectChanges(Set.of(nested));

        assertThat(result.added()).containsExactly("v1/nested.proto");
        assertThat(result.currentFingerprints()).containsKey("v1/nested.proto");
    }
}
