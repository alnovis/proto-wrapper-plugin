package space.alnovis.protowrapper.incremental;

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
 * Unit tests for {@link IncrementalState}.
 */
class IncrementalStateTest {

    /** Test version constant - use for tests that don't depend on actual plugin version */
    private static final String TEST_VERSION = "1.0.0-test";
    private static final String TEST_CONFIG = "test-config-hash";

    @TempDir
    Path tempDir;

    @Test
    void empty_createsEmptyState() {
        IncrementalState state = IncrementalState.empty();

        assertThat(state.isEmpty()).isTrue();
        assertThat(state.pluginVersion()).isNull();
        assertThat(state.configHash()).isNull();
        assertThat(state.lastGeneration()).isNull();
        assertThat(state.protoFingerprints()).isEmpty();
        assertThat(state.protoDependencies()).isEmpty();
    }

    @Test
    void isEmpty_returnsFalseForNonEmptyState() {
        IncrementalState state = new IncrementalState(
            TEST_VERSION,
            TEST_CONFIG,
            Map.of(),
            Map.of(),
            null,
            Instant.now()
        );

        assertThat(state.isEmpty()).isFalse();
    }

    @Test
    void shouldInvalidate_returnsTrueForEmptyState() {
        IncrementalState state = IncrementalState.empty();

        assertThat(state.shouldInvalidate(TEST_VERSION, TEST_CONFIG)).isTrue();
    }

    @Test
    void shouldInvalidate_returnsTrueForDifferentVersion() {
        IncrementalState state = new IncrementalState(
            "1.5.0", TEST_CONFIG, Map.of(), Map.of(), null, Instant.now()
        );

        assertThat(state.shouldInvalidate(TEST_VERSION, TEST_CONFIG)).isTrue();
    }

    @Test
    void shouldInvalidate_returnsTrueForDifferentConfig() {
        IncrementalState state = new IncrementalState(
            TEST_VERSION, "oldConfig", Map.of(), Map.of(), null, Instant.now()
        );

        assertThat(state.shouldInvalidate(TEST_VERSION, "newConfig")).isTrue();
    }

    @Test
    void shouldInvalidate_returnsFalseForSameVersionAndConfig() {
        IncrementalState state = new IncrementalState(
            TEST_VERSION, TEST_CONFIG, Map.of(), Map.of(), null, Instant.now()
        );

        assertThat(state.shouldInvalidate(TEST_VERSION, TEST_CONFIG)).isFalse();
    }

    @Test
    void withUpdates_createsNewStateWithUpdatedValues() {
        IncrementalState original = IncrementalState.empty();

        Map<String, FileFingerprint> fingerprints = Map.of(
            "test.proto", new FileFingerprint("test.proto", "hash", 1000, 100)
        );
        Map<String, Set<String>> dependencies = Map.of(
            "test.proto", Set.of("common.proto")
        );

        IncrementalState updated = original.withUpdates(TEST_VERSION, TEST_CONFIG, fingerprints, dependencies, null);

        assertThat(updated.pluginVersion()).isEqualTo(TEST_VERSION);
        assertThat(updated.configHash()).isEqualTo(TEST_CONFIG);
        assertThat(updated.protoFingerprints()).containsKey("test.proto");
        assertThat(updated.protoDependencies()).containsKey("test.proto");
        assertThat(updated.lastGeneration()).isNotNull();
    }

    @Test
    void writeTo_createsJsonFile() throws IOException {
        Map<String, FileFingerprint> fingerprints = new HashMap<>();
        fingerprints.put("v1/test.proto", new FileFingerprint("v1/test.proto", "hash123", 1000, 100));

        Map<String, Set<String>> dependencies = new HashMap<>();
        dependencies.put("v1/test.proto", Set.of("v1/common.proto"));

        IncrementalState state = new IncrementalState(
            TEST_VERSION,
            TEST_CONFIG,
            fingerprints,
            dependencies,
            null,
            Instant.parse("2026-01-05T10:00:00Z")
        );

        Path cacheFile = tempDir.resolve("state.json");
        state.writeTo(cacheFile);

        assertThat(cacheFile).exists();

        String content = Files.readString(cacheFile);
        assertThat(content).contains("\"pluginVersion\"");
        assertThat(content).contains("\"" + TEST_VERSION + "\"");
        assertThat(content).contains("\"configHash\"");
        assertThat(content).contains("\"protoFingerprints\"");
        assertThat(content).contains("\"v1/test.proto\"");
        assertThat(content).contains("\"protoDependencies\"");
    }

    @Test
    void readFrom_parsesJsonFile() throws IOException {
        // Create state and write it
        Map<String, FileFingerprint> fingerprints = new HashMap<>();
        fingerprints.put("v1/test.proto", new FileFingerprint("v1/test.proto", "hash123", 1000, 100));

        Map<String, Set<String>> dependencies = new HashMap<>();
        dependencies.put("v1/test.proto", Set.of("v1/common.proto"));

        IncrementalState original = new IncrementalState(
            TEST_VERSION,
            TEST_CONFIG,
            fingerprints,
            dependencies,
            null,
            Instant.parse("2026-01-05T10:00:00Z")
        );

        Path cacheFile = tempDir.resolve("state.json");
        original.writeTo(cacheFile);

        // Read it back
        IncrementalState loaded = IncrementalState.readFrom(cacheFile);

        assertThat(loaded.pluginVersion()).isEqualTo(TEST_VERSION);
        assertThat(loaded.configHash()).isEqualTo(TEST_CONFIG);
        assertThat(loaded.protoFingerprints()).containsKey("v1/test.proto");
        assertThat(loaded.protoFingerprints().get("v1/test.proto").contentHash()).isEqualTo("hash123");
        assertThat(loaded.protoDependencies()).containsKey("v1/test.proto");
        assertThat(loaded.protoDependencies().get("v1/test.proto")).contains("v1/common.proto");
    }

    @Test
    void readFrom_returnsEmptyForMissingFile() {
        Path nonExistent = tempDir.resolve("missing.json");

        IncrementalState state = IncrementalState.readFrom(nonExistent);

        assertThat(state.isEmpty()).isTrue();
    }

    @Test
    void readFrom_returnsEmptyForCorruptedFile() throws IOException {
        Path corrupted = tempDir.resolve("corrupted.json");
        Files.writeString(corrupted, "not valid json {{{");

        IncrementalState state = IncrementalState.readFrom(corrupted);

        assertThat(state.isEmpty()).isTrue();
    }

    @Test
    void protoFingerprints_isImmutable() {
        Map<String, FileFingerprint> mutable = new HashMap<>();
        mutable.put("test.proto", new FileFingerprint("test.proto", "hash", 1000, 100));

        IncrementalState state = new IncrementalState(TEST_VERSION, TEST_CONFIG, mutable, Map.of(), null, null);

        // Modify original map
        mutable.put("other.proto", new FileFingerprint("other.proto", "hash2", 2000, 200));

        // State should not be affected
        assertThat(state.protoFingerprints()).hasSize(1);
        assertThat(state.protoFingerprints()).containsKey("test.proto");
    }

    @Test
    void protoDependencies_isImmutable() {
        Map<String, Set<String>> mutable = new HashMap<>();
        mutable.put("test.proto", Set.of("common.proto"));

        IncrementalState state = new IncrementalState(TEST_VERSION, TEST_CONFIG, Map.of(), mutable, null, null);

        // Modify original map
        mutable.put("other.proto", Set.of("other.proto"));

        // State should not be affected
        assertThat(state.protoDependencies()).hasSize(1);
    }

    @Test
    void roundTrip_preservesAllData() throws IOException {
        Map<String, FileFingerprint> fingerprints = new HashMap<>();
        fingerprints.put("v1/order.proto", new FileFingerprint("v1/order.proto", "hash1", 1000, 100));
        fingerprints.put("v2/order.proto", new FileFingerprint("v2/order.proto", "hash2", 2000, 200));

        Map<String, Set<String>> dependencies = new HashMap<>();
        dependencies.put("v1/order.proto", Set.of("v1/common.proto", "v1/types.proto"));
        dependencies.put("v2/order.proto", Set.of("v2/common.proto"));

        Instant now = Instant.now();
        IncrementalState original = new IncrementalState(
            TEST_VERSION, TEST_CONFIG, fingerprints, dependencies, null, now
        );

        Path cacheFile = tempDir.resolve("roundtrip.json");
        original.writeTo(cacheFile);

        IncrementalState loaded = IncrementalState.readFrom(cacheFile);

        assertThat(loaded.pluginVersion()).isEqualTo(original.pluginVersion());
        assertThat(loaded.configHash()).isEqualTo(original.configHash());
        assertThat(loaded.protoFingerprints()).hasSize(2);
        assertThat(loaded.protoDependencies()).hasSize(2);
        assertThat(loaded.protoDependencies().get("v1/order.proto")).hasSize(2);
    }

    // ==================== Factory Methods Tests ====================

    @Test
    void forGeneration_createsStateWithTimestamp() {
        Map<String, FileFingerprint> fingerprints = Map.of(
            "test.proto", new FileFingerprint("test.proto", "hash", 1000, 100)
        );
        Map<String, Set<String>> dependencies = Map.of(
            "test.proto", Set.of("common.proto")
        );

        IncrementalState state = IncrementalState.forGeneration(
            TEST_VERSION, TEST_CONFIG, fingerprints, dependencies
        );

        assertThat(state.pluginVersion()).isEqualTo(TEST_VERSION);
        assertThat(state.configHash()).isEqualTo(TEST_CONFIG);
        assertThat(state.protoFingerprints()).containsKey("test.proto");
        assertThat(state.protoDependencies()).containsKey("test.proto");
        assertThat(state.generatedFiles()).isEmpty();
        assertThat(state.lastGeneration()).isNotNull();
        assertThat(state.isEmpty()).isFalse();
    }

    @Test
    void forGeneration_withGeneratedFiles() {
        GeneratedFileInfo fileInfo = new GeneratedFileInfo("hash", 1000, Set.of("test.proto"));
        Map<String, GeneratedFileInfo> generated = Map.of("Test.java", fileInfo);

        IncrementalState state = IncrementalState.forGeneration(
            TEST_VERSION, TEST_CONFIG, Map.of(), Map.of(), generated
        );

        assertThat(state.generatedFiles()).containsKey("Test.java");
        assertThat(state.generatedFiles().get("Test.java").contentHash()).isEqualTo("hash");
    }

    @Test
    void forGeneration_requiresPluginVersion() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () ->
            IncrementalState.forGeneration(null, TEST_CONFIG, Map.of(), Map.of())
        );
    }

    @Test
    void forGeneration_requiresConfigHash() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () ->
            IncrementalState.forGeneration(TEST_VERSION, null, Map.of(), Map.of())
        );
    }

    @Test
    void constructor_normalizesNullMapsToEmpty() {
        IncrementalState state = new IncrementalState(
            TEST_VERSION, TEST_CONFIG, null, null, null, Instant.now()
        );

        assertThat(state.protoFingerprints()).isNotNull().isEmpty();
        assertThat(state.protoDependencies()).isNotNull().isEmpty();
        assertThat(state.generatedFiles()).isNotNull().isEmpty();
    }

    @Test
    void empty_allMapsAreNotNull() {
        IncrementalState state = IncrementalState.empty();

        assertThat(state.protoFingerprints()).isNotNull();
        assertThat(state.protoDependencies()).isNotNull();
        assertThat(state.generatedFiles()).isNotNull();
    }
}
