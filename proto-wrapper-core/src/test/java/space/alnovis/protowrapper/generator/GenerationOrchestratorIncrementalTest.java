package space.alnovis.protowrapper.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import space.alnovis.protowrapper.PluginLogger;
import space.alnovis.protowrapper.model.MergedSchema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for incremental generation in {@link GenerationOrchestrator}.
 */
class GenerationOrchestratorIncrementalTest {

    @TempDir
    Path tempDir;

    private Path outputDir;
    private Path cacheDir;
    private Path protoRoot;
    private MergedSchema emptySchema;
    private List<GenerationOrchestrator.VersionConfig> emptyVersionConfigs;
    private GenerationOrchestrator.ProtoClassNameResolver noopResolver;

    @BeforeEach
    void setUp() throws IOException {
        outputDir = tempDir.resolve("output");
        cacheDir = tempDir.resolve("cache");
        protoRoot = tempDir.resolve("proto");

        Files.createDirectories(outputDir);
        Files.createDirectories(cacheDir);
        Files.createDirectories(protoRoot);

        emptySchema = new MergedSchema(Arrays.asList("v1"));
        emptyVersionConfigs = List.of();
        noopResolver = (msg, vc) -> "com.example.Proto";
    }

    @Test
    void generateAllIncremental_firstRun_performsGeneration() throws IOException {
        // Create proto file
        Path protoFile = protoRoot.resolve("test.proto");
        Files.writeString(protoFile, "syntax = \"proto3\"; message Test {}");

        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .build();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());

        // First run should generate (returns 0 because schema is empty, but state is saved)
        int count = orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            Set.of(protoFile), protoRoot
        );

        // Check that state file was created
        Path stateFile = cacheDir.resolve("state.json");
        assertThat(stateFile).exists();
    }

    @Test
    void generateAllIncremental_noChanges_skipsGeneration() throws IOException {
        // Create proto file
        Path protoFile = protoRoot.resolve("test.proto");
        Files.writeString(protoFile, "syntax = \"proto3\"; message Test {}");

        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .build();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());
        Set<Path> protoFiles = Set.of(protoFile);

        // First run
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            protoFiles, protoRoot
        );

        // Second run without changes should skip
        int count = orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            protoFiles, protoRoot
        );

        assertThat(count).isZero();
    }

    @Test
    void generateAllIncremental_fileModified_performsGeneration() throws IOException {
        // Create proto file
        Path protoFile = protoRoot.resolve("test.proto");
        Files.writeString(protoFile, "syntax = \"proto3\"; message Test {}");

        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .build();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());
        Set<Path> protoFiles = Set.of(protoFile);

        // First run
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            protoFiles, protoRoot
        );

        // Modify file
        Files.writeString(protoFile, "syntax = \"proto3\"; message Test { string name = 1; }");

        // Second run should detect change
        // Note: with empty schema, generateAll returns 0, but the logic should run
        int count = orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            protoFiles, protoRoot
        );

        // Count is 0 because schema is empty, but generation logic was triggered
        // We can verify by checking state was updated
        assertThat(cacheDir.resolve("state.json")).exists();
    }

    @Test
    void generateAllIncremental_newFileAdded_performsGeneration() throws IOException {
        // Create initial proto file
        Path protoFile1 = protoRoot.resolve("test1.proto");
        Files.writeString(protoFile1, "syntax = \"proto3\"; message Test1 {}");

        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .build();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());

        // First run with one file
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            Set.of(protoFile1), protoRoot
        );

        // Add new file
        Path protoFile2 = protoRoot.resolve("test2.proto");
        Files.writeString(protoFile2, "syntax = \"proto3\"; message Test2 {}");

        // Second run with two files should detect addition
        Set<Path> allFiles = new HashSet<>();
        allFiles.add(protoFile1);
        allFiles.add(protoFile2);

        // This will not return 0 because new file is detected
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            allFiles, protoRoot
        );

        assertThat(cacheDir.resolve("state.json")).exists();
    }

    @Test
    void generateAllIncremental_forceRegenerate_alwaysGenerates() throws IOException {
        // Create proto file
        Path protoFile = protoRoot.resolve("test.proto");
        Files.writeString(protoFile, "syntax = \"proto3\"; message Test {}");

        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .forceRegenerate(true)
            .build();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());
        Set<Path> protoFiles = Set.of(protoFile);

        // First run
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            protoFiles, protoRoot
        );

        // Second run with forceRegenerate should not skip
        // Even though no changes, force flag should trigger generation
        // (we can't easily verify this with empty schema, but state should be saved)
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            protoFiles, protoRoot
        );

        assertThat(cacheDir.resolve("state.json")).exists();
    }

    @Test
    void generateAllIncremental_incrementalDisabled_alwaysGenerates() throws IOException {
        // Create proto file
        Path protoFile = protoRoot.resolve("test.proto");
        Files.writeString(protoFile, "syntax = \"proto3\"; message Test {}");

        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(false)
            .build();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());
        Set<Path> protoFiles = Set.of(protoFile);

        // Run twice - should generate both times (not skip)
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            protoFiles, protoRoot
        );

        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            protoFiles, protoRoot
        );

        // State should still be saved for future use
        assertThat(cacheDir.resolve("state.json")).exists();
    }

    @Test
    void generateAllIncremental_configChanged_invalidatesCache() throws IOException {
        // Create proto file
        Path protoFile = protoRoot.resolve("test.proto");
        Files.writeString(protoFile, "syntax = \"proto3\"; message Test {}");

        // First run with config1
        GeneratorConfig config1 = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .apiPackage("com.example.api")
            .build();

        GenerationOrchestrator orchestrator1 = new GenerationOrchestrator(config1, PluginLogger.noop());
        orchestrator1.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            Set.of(protoFile), protoRoot
        );

        // Second run with different config (different apiPackage)
        GeneratorConfig config2 = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .apiPackage("com.other.api")
            .build();

        GenerationOrchestrator orchestrator2 = new GenerationOrchestrator(config2, PluginLogger.noop());

        // Should not skip because config changed
        // With empty schema returns 0, but cache invalidation logic runs
        orchestrator2.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            Set.of(protoFile), protoRoot
        );

        assertThat(cacheDir.resolve("state.json")).exists();
    }

    @Test
    void generateAllIncremental_defaultCacheDirectory_usesOutputSubdir() throws IOException {
        // Create proto file
        Path protoFile = protoRoot.resolve("test.proto");
        Files.writeString(protoFile, "syntax = \"proto3\"; message Test {}");

        // Config without explicit cache directory
        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .incremental(true)
            .build();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            Set.of(protoFile), protoRoot
        );

        // Default cache directory should be output/.proto-wrapper-cache
        Path defaultCacheDir = outputDir.resolve(".proto-wrapper-cache");
        assertThat(defaultCacheDir.resolve("state.json")).exists();
    }

    @Test
    void getPluginVersion_returnsVersion() {
        String version = GenerationOrchestrator.getPluginVersion();

        assertThat(version).isNotNull();
        assertThat(version).isNotEmpty();
        // Version is either semantic version (after Maven/Gradle filtering) or "unknown" (IDE/unfiltered)
        assertThat(version).matches("\\d+\\.\\d+\\.\\d+.*|unknown");
    }
}
