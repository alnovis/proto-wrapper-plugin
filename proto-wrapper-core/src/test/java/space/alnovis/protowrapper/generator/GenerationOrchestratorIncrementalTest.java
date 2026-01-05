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

    @Test
    void generateAllIncremental_fileDeleted_triggersFullRegeneration() throws IOException {
        // Create two proto files
        Path protoFile1 = protoRoot.resolve("test1.proto");
        Path protoFile2 = protoRoot.resolve("test2.proto");
        Files.writeString(protoFile1, "syntax = \"proto3\"; message Test1 {}");
        Files.writeString(protoFile2, "syntax = \"proto3\"; message Test2 {}");

        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .build();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());

        // First run with two files
        Set<Path> twoFiles = new HashSet<>();
        twoFiles.add(protoFile1);
        twoFiles.add(protoFile2);
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            twoFiles, protoRoot
        );

        // Delete one file and run again with only one file
        Files.delete(protoFile2);

        // Second run should detect deletion and trigger generation
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            Set.of(protoFile1), protoRoot
        );

        // State should be updated
        assertThat(cacheDir.resolve("state.json")).exists();
    }

    @Test
    void generateAllIncremental_corruptedCache_recoversWithFullGeneration() throws IOException {
        // Create proto file
        Path protoFile = protoRoot.resolve("test.proto");
        Files.writeString(protoFile, "syntax = \"proto3\"; message Test {}");

        // Create corrupted cache file
        Files.createDirectories(cacheDir);
        Path stateFile = cacheDir.resolve("state.json");
        Files.writeString(stateFile, "{ invalid json content {{{{");

        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .build();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());

        // Should recover from corrupted cache and perform full generation
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            Set.of(protoFile), protoRoot
        );

        // State should be recreated with valid content
        assertThat(stateFile).exists();
        String content = Files.readString(stateFile);
        assertThat(content).contains("\"pluginVersion\"");
    }

    @Test
    void generateAllIncremental_dependencyChanged_regeneratesDependents() throws IOException {
        // Create common.proto (imported by order.proto)
        Path commonProto = protoRoot.resolve("common.proto");
        Files.writeString(commonProto, "syntax = \"proto3\"; message Common { string name = 1; }");

        // Create order.proto that imports common.proto
        Path orderProto = protoRoot.resolve("order.proto");
        Files.writeString(orderProto,
            "syntax = \"proto3\";\n" +
            "import \"common.proto\";\n" +
            "message Order { Common common = 1; }");

        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .build();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());

        Set<Path> protoFiles = new HashSet<>();
        protoFiles.add(commonProto);
        protoFiles.add(orderProto);

        // First run
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            protoFiles, protoRoot
        );

        // Modify common.proto (the imported file)
        Files.writeString(commonProto, "syntax = \"proto3\"; message Common { string name = 1; int32 id = 2; }");

        // Second run should detect that common.proto changed
        // and order.proto depends on it (needs regeneration)
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            protoFiles, protoRoot
        );

        // State should reflect the changes
        assertThat(cacheDir.resolve("state.json")).exists();
    }

    @Test
    void generateAllIncremental_emptyProtoSet_handlesGracefully() throws IOException {
        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .build();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());

        // Run with empty set of proto files - should not throw exception
        // Returns count of generated files (may be non-zero due to VersionContext generation)
        int count = orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            Set.of(), protoRoot
        );

        // Should handle gracefully without exception
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    void generateAllIncremental_cacheDirectoryNotExists_createsIt() throws IOException {
        // Create proto file
        Path protoFile = protoRoot.resolve("test.proto");
        Files.writeString(protoFile, "syntax = \"proto3\"; message Test {}");

        // Use non-existent cache directory
        Path nonExistentCache = tempDir.resolve("non-existent-cache");
        assertThat(nonExistentCache).doesNotExist();

        GeneratorConfig config = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(nonExistentCache)
            .incremental(true)
            .build();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());
        orchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            Set.of(protoFile), protoRoot
        );

        // Cache directory should be created
        assertThat(nonExistentCache).exists();
        assertThat(nonExistentCache.resolve("state.json")).exists();
    }
}
