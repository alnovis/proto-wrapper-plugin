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
            "1.6.0",
            "abc123",
            Map.of(),
            Map.of(),
            Instant.now()
        );

        assertThat(state.isEmpty()).isFalse();
    }

    @Test
    void shouldInvalidate_returnsTrueForEmptyState() {
        IncrementalState state = IncrementalState.empty();

        assertThat(state.shouldInvalidate("1.6.0", "abc")).isTrue();
    }

    @Test
    void shouldInvalidate_returnsTrueForDifferentVersion() {
        IncrementalState state = new IncrementalState(
            "1.5.0", "abc", Map.of(), Map.of(), Instant.now()
        );

        assertThat(state.shouldInvalidate("1.6.0", "abc")).isTrue();
    }

    @Test
    void shouldInvalidate_returnsTrueForDifferentConfig() {
        IncrementalState state = new IncrementalState(
            "1.6.0", "abc", Map.of(), Map.of(), Instant.now()
        );

        assertThat(state.shouldInvalidate("1.6.0", "xyz")).isTrue();
    }

    @Test
    void shouldInvalidate_returnsFalseForSameVersionAndConfig() {
        IncrementalState state = new IncrementalState(
            "1.6.0", "abc", Map.of(), Map.of(), Instant.now()
        );

        assertThat(state.shouldInvalidate("1.6.0", "abc")).isFalse();
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

        IncrementalState updated = original.withUpdates("1.6.0", "config", fingerprints, dependencies);

        assertThat(updated.pluginVersion()).isEqualTo("1.6.0");
        assertThat(updated.configHash()).isEqualTo("config");
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
            "1.6.0",
            "configHash",
            fingerprints,
            dependencies,
            Instant.parse("2026-01-05T10:00:00Z")
        );

        Path cacheFile = tempDir.resolve("state.json");
        state.writeTo(cacheFile);

        assertThat(cacheFile).exists();

        String content = Files.readString(cacheFile);
        assertThat(content).contains("\"pluginVersion\"");
        assertThat(content).contains("\"1.6.0\"");
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
            "1.6.0",
            "configHash",
            fingerprints,
            dependencies,
            Instant.parse("2026-01-05T10:00:00Z")
        );

        Path cacheFile = tempDir.resolve("state.json");
        original.writeTo(cacheFile);

        // Read it back
        IncrementalState loaded = IncrementalState.readFrom(cacheFile);

        assertThat(loaded.pluginVersion()).isEqualTo("1.6.0");
        assertThat(loaded.configHash()).isEqualTo("configHash");
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

        IncrementalState state = new IncrementalState("1.6.0", "cfg", mutable, Map.of(), null);

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

        IncrementalState state = new IncrementalState("1.6.0", "cfg", Map.of(), mutable, null);

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
            "1.6.0", "configHash123", fingerprints, dependencies, now
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
}
