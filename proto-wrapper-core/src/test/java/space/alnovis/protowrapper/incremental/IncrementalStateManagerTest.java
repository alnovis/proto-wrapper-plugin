package space.alnovis.protowrapper.incremental;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import space.alnovis.protowrapper.PluginLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link IncrementalStateManager}.
 */
class IncrementalStateManagerTest {

    /** Test version constant - use for tests that don't depend on actual plugin version */
    private static final String TEST_VERSION = "1.0.0-test";
    private static final String TEST_CONFIG = "test-config-hash";

    @TempDir
    Path tempDir;

    private Path cacheDir;
    private Path protoRoot;
    private PluginLogger logger;

    @BeforeEach
    void setUp() throws IOException {
        cacheDir = tempDir.resolve("cache");
        protoRoot = tempDir.resolve("proto");
        Files.createDirectories(cacheDir);
        Files.createDirectories(protoRoot);
        logger = PluginLogger.noop();
    }

    @Test
    void loadPreviousState_loadsEmptyForFirstRun() throws IOException {
        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );

        manager.loadPreviousState();

        assertThat(manager.isStateLoaded()).isTrue();
        assertThat(manager.shouldInvalidateCache()).isTrue();
        assertThat(manager.getInvalidationReason()).contains("No previous state");
    }

    @Test
    void loadPreviousState_loadsExistingState() throws IOException {
        // Create previous state
        IncrementalState previous = new IncrementalState(
            TEST_VERSION, TEST_CONFIG, Map.of(), Map.of(), null, Instant.now()
        );
        previous.writeTo(cacheDir.resolve("state.json"));

        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );
        manager.loadPreviousState();

        assertThat(manager.shouldInvalidateCache()).isFalse();
        assertThat(manager.getInvalidationReason()).isNull();
    }

    @Test
    void shouldInvalidateCache_returnsTrueForVersionChange() throws IOException {
        IncrementalState previous = new IncrementalState(
            "1.5.0", "config", Map.of(), Map.of(), null, Instant.now()
        );
        previous.writeTo(cacheDir.resolve("state.json"));

        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );
        manager.loadPreviousState();

        assertThat(manager.shouldInvalidateCache()).isTrue();
        assertThat(manager.getInvalidationReason()).contains("Plugin version changed");
    }

    @Test
    void shouldInvalidateCache_returnsTrueForConfigChange() throws IOException {
        IncrementalState previous = new IncrementalState(
            TEST_VERSION, "oldConfig", Map.of(), Map.of(), null, Instant.now()
        );
        previous.writeTo(cacheDir.resolve("state.json"));

        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, "newConfig", logger
        );
        manager.loadPreviousState();

        assertThat(manager.shouldInvalidateCache()).isTrue();
        assertThat(manager.getInvalidationReason()).contains("Configuration changed");
    }

    @Test
    void analyzeChanges_detectsNewFiles() throws IOException {
        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );
        manager.loadPreviousState();

        Path newFile = protoRoot.resolve("new.proto");
        Files.writeString(newFile, "syntax = \"proto3\";");

        ChangeDetector.ChangeResult result = manager.analyzeChanges(Set.of(newFile));

        assertThat(result.added()).contains("new.proto");
        assertThat(result.hasChanges()).isTrue();
    }

    @Test
    void analyzeAndGetAffectedFiles_includesDependents() throws IOException {
        // Create files with dependencies
        Path common = protoRoot.resolve("common.proto");
        Files.writeString(common, "syntax = \"proto3\"; message Common {}");
        FileFingerprint commonFp = FileFingerprint.compute(common, protoRoot);

        Path order = protoRoot.resolve("order.proto");
        Files.writeString(order, "syntax = \"proto3\"; import \"common.proto\"; message Order {}");
        FileFingerprint orderFp = FileFingerprint.compute(order, protoRoot);

        // Create previous state
        IncrementalState previous = new IncrementalState(
            TEST_VERSION, TEST_CONFIG,
            Map.of("common.proto", commonFp, "order.proto", orderFp),
            Map.of("order.proto", Set.of("common.proto")),
            null,
            Instant.now()
        );
        previous.writeTo(cacheDir.resolve("state.json"));

        // Modify common.proto
        Files.writeString(common, "syntax = \"proto3\"; message Common { string name = 1; }");

        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );
        manager.loadPreviousState();

        Set<String> affected = manager.analyzeAndGetAffectedFiles(Set.of(common, order));

        // Both should be affected because order depends on common
        assertThat(affected).containsExactlyInAnyOrder("common.proto", "order.proto");
    }

    @Test
    void getAffectedFiles_returnsAllFilesWhenFilesDeleted() throws IOException {
        Path file = protoRoot.resolve("remaining.proto");
        Files.writeString(file, "content");

        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );
        manager.loadPreviousState();

        // Create change result with deleted files
        ChangeDetector.ChangeResult changes = new ChangeDetector.ChangeResult(
            Set.of(),
            Set.of(),
            Set.of("deleted.proto"),
            Map.of("remaining.proto", FileFingerprint.compute(file, protoRoot))
        );

        Set<String> affected = manager.getAffectedFiles(changes);

        // Should return all current files when deletions detected
        assertThat(affected).contains("remaining.proto");
    }

    @Test
    void getAffectedMessages_mapsFilesToMessages() throws IOException {
        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );

        Map<String, Set<String>> fileToMessages = Map.of(
            "order.proto", Set.of("Order", "OrderItem"),
            "common.proto", Set.of("Common")
        );

        Set<String> affectedFiles = Set.of("order.proto");
        Set<String> affectedMessages = manager.getAffectedMessages(affectedFiles, fileToMessages);

        assertThat(affectedMessages).containsExactlyInAnyOrder("Order", "OrderItem");
    }

    @Test
    void saveCurrentState_persistsState() throws IOException {
        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );
        manager.loadPreviousState();

        Path file = protoRoot.resolve("test.proto");
        Files.writeString(file, "syntax = \"proto3\";");
        manager.analyzeChanges(Set.of(file));

        manager.saveCurrentState();

        // Verify state was saved
        Path stateFile = cacheDir.resolve("state.json");
        assertThat(stateFile).exists();

        IncrementalState saved = IncrementalState.readFrom(stateFile);
        assertThat(saved.pluginVersion()).isEqualTo(TEST_VERSION);
        assertThat(saved.configHash()).isEqualTo(TEST_CONFIG);
        assertThat(saved.protoFingerprints()).containsKey("test.proto");
    }

    @Test
    void invalidateCache_deletesStateFile() throws IOException {
        // Create state file
        IncrementalState state = new IncrementalState(
            TEST_VERSION, TEST_CONFIG, Map.of(), Map.of(), null, Instant.now()
        );
        Path stateFile = cacheDir.resolve("state.json");
        state.writeTo(stateFile);
        assertThat(stateFile).exists();

        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );
        manager.loadPreviousState();
        manager.invalidateCache();

        assertThat(stateFile).doesNotExist();
    }

    @Test
    void getters_returnCorrectValues() {
        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );

        assertThat(manager.getCacheDirectory()).isEqualTo(cacheDir);
        assertThat(manager.getProtoRoot()).isEqualTo(protoRoot);
        assertThat(manager.getPluginVersion()).isEqualTo(TEST_VERSION);
        assertThat(manager.getConfigHash()).isEqualTo(TEST_CONFIG);
    }

    @Test
    void shouldInvalidateCache_throwsIfStateNotLoaded() {
        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );

        assertThatThrownBy(manager::shouldInvalidateCache)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not loaded");
    }

    @Test
    void saveCurrentState_throwsIfAnalysisNotDone() throws IOException {
        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );
        manager.loadPreviousState();

        assertThatThrownBy(manager::saveCurrentState)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Analysis not done");
    }

    @Test
    void getDependencyGraph_returnsNullBeforeAnalysis() throws IOException {
        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );
        manager.loadPreviousState();

        assertThat(manager.getDependencyGraph()).isNull();
    }

    @Test
    void getDependencyGraph_returnsGraphAfterAnalysis() throws IOException {
        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );
        manager.loadPreviousState();

        Path file = protoRoot.resolve("test.proto");
        Files.writeString(file, "syntax = \"proto3\";");
        manager.analyzeChanges(Set.of(file));

        assertThat(manager.getDependencyGraph()).isNotNull();
        assertThat(manager.getDependencyGraph().containsFile("test.proto")).isTrue();
    }

    @Test
    void getChangeResult_returnsResultAfterAnalysis() throws IOException {
        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );
        manager.loadPreviousState();

        Path file = protoRoot.resolve("test.proto");
        Files.writeString(file, "syntax = \"proto3\";");
        manager.analyzeChanges(Set.of(file));

        assertThat(manager.getChangeResult()).isNotNull();
        assertThat(manager.getChangeResult().added()).contains("test.proto");
    }

    @Test
    void noChanges_emptyAffectedFiles() throws IOException {
        // Create initial state
        Path file = protoRoot.resolve("test.proto");
        Files.writeString(file, "syntax = \"proto3\";");
        FileFingerprint fp = FileFingerprint.compute(file, protoRoot);

        IncrementalState previous = new IncrementalState(
            TEST_VERSION, TEST_CONFIG,
            Map.of("test.proto", fp),
            Map.of(),
            null,
            Instant.now()
        );
        previous.writeTo(cacheDir.resolve("state.json"));

        IncrementalStateManager manager = new IncrementalStateManager(
            cacheDir, protoRoot, TEST_VERSION, TEST_CONFIG, logger
        );
        manager.loadPreviousState();

        Set<String> affected = manager.analyzeAndGetAffectedFiles(Set.of(file));

        assertThat(affected).isEmpty();
    }
}
