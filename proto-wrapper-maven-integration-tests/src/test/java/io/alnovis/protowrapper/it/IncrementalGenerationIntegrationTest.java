package io.alnovis.protowrapper.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import io.alnovis.protowrapper.PluginLogger;
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer;
import io.alnovis.protowrapper.analyzer.ProtocExecutor;
import io.alnovis.protowrapper.generator.GenerationOrchestrator;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.merger.VersionMerger;
import io.alnovis.protowrapper.model.MergedSchema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for incremental generation using real proto files.
 *
 * <p>These tests verify that incremental generation works correctly
 * in a realistic scenario with multiple proto versions.
 */
class IncrementalGenerationIntegrationTest {

    private Path protoRoot;
    private Path v1Dir;
    private Path v2Dir;

    @TempDir
    Path tempDir;

    private Path outputDir;
    private Path cacheDir;
    private PluginLogger logger;

    @BeforeEach
    void setUp() throws IOException {
        // Use the shared test-protos directory
        protoRoot = Path.of("../test-protos/scenarios/generation").toAbsolutePath().normalize();
        v1Dir = protoRoot.resolve("v1");
        v2Dir = protoRoot.resolve("v2");

        outputDir = tempDir.resolve("output");
        cacheDir = tempDir.resolve("cache");

        Files.createDirectories(outputDir);
        Files.createDirectories(cacheDir);

        logger = PluginLogger.noop();
    }

    static boolean isProtocAvailable() {
        try {
            Process process = new ProcessBuilder("protoc", "--version")
                .redirectErrorStream(true)
                .start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void incrementalGeneration_firstRun_generatesAllFiles() throws Exception {
        MergedSchema schema = createMergedSchema();
        GeneratorConfig config = createConfig(true);
        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, logger);

        Set<Path> protoFiles = collectProtoFiles();
        int count = orchestrator.generateAllIncremental(
            schema,
            createVersionConfigs(),
            (msg, vc) -> "com.example.Proto",
            protoFiles,
            protoRoot
        );

        // Should generate files on first run
        assertThat(count).isGreaterThan(0);

        // Cache should be created
        Path stateFile = cacheDir.resolve("state.json");
        assertThat(stateFile).exists();

        // Output directory should have generated files
        long javaFileCount = countJavaFiles(outputDir);
        assertThat(javaFileCount).isGreaterThan(0);
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void incrementalGeneration_noChanges_skipsGeneration() throws Exception {
        MergedSchema schema = createMergedSchema();
        GeneratorConfig config = createConfig(true);
        Set<Path> protoFiles = collectProtoFiles();

        // First run - full generation
        GenerationOrchestrator orchestrator1 = new GenerationOrchestrator(config, logger);
        int firstCount = orchestrator1.generateAllIncremental(
            schema, createVersionConfigs(),
            (msg, vc) -> "com.example.Proto",
            protoFiles, protoRoot
        );

        assertThat(firstCount).isGreaterThan(0);

        // Second run - should skip due to no changes
        GenerationOrchestrator orchestrator2 = new GenerationOrchestrator(config, logger);
        int secondCount = orchestrator2.generateAllIncremental(
            schema, createVersionConfigs(),
            (msg, vc) -> "com.example.Proto",
            protoFiles, protoRoot
        );

        assertThat(secondCount).isEqualTo(0);
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void incrementalGeneration_forceRegenerate_regeneratesAll() throws Exception {
        MergedSchema schema = createMergedSchema();
        Set<Path> protoFiles = collectProtoFiles();

        // First run with incremental
        GeneratorConfig config1 = createConfig(true);
        GenerationOrchestrator orchestrator1 = new GenerationOrchestrator(config1, logger);
        orchestrator1.generateAllIncremental(
            schema, createVersionConfigs(),
            (msg, vc) -> "com.example.Proto",
            protoFiles, protoRoot
        );

        // Second run with forceRegenerate
        GeneratorConfig config2 = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .forceRegenerate(true)
            .apiPackage("com.example.api")
            .implPackagePattern("com.example.{version}")
            .protoPackagePattern("com.example.proto.{version}")
            .build();

        GenerationOrchestrator orchestrator2 = new GenerationOrchestrator(config2, logger);
        int count = orchestrator2.generateAllIncremental(
            schema, createVersionConfigs(),
            (msg, vc) -> "com.example.Proto",
            protoFiles, protoRoot
        );

        // Should regenerate despite no changes
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void incrementalGeneration_incrementalDisabled_alwaysRegenerates() throws Exception {
        MergedSchema schema = createMergedSchema();
        Set<Path> protoFiles = collectProtoFiles();

        // First run
        GeneratorConfig config1 = createConfig(false);
        GenerationOrchestrator orchestrator1 = new GenerationOrchestrator(config1, logger);
        int firstCount = orchestrator1.generateAllIncremental(
            schema, createVersionConfigs(),
            (msg, vc) -> "com.example.Proto",
            protoFiles, protoRoot
        );

        assertThat(firstCount).isGreaterThan(0);

        // Second run - should still regenerate because incremental=false
        GeneratorConfig config2 = createConfig(false);
        GenerationOrchestrator orchestrator2 = new GenerationOrchestrator(config2, logger);
        int secondCount = orchestrator2.generateAllIncremental(
            schema, createVersionConfigs(),
            (msg, vc) -> "com.example.Proto",
            protoFiles, protoRoot
        );

        assertThat(secondCount).isGreaterThan(0);
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void incrementalGeneration_cacheStateContainsExpectedData() throws Exception {
        MergedSchema schema = createMergedSchema();
        GeneratorConfig config = createConfig(true);
        Set<Path> protoFiles = collectProtoFiles();

        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, logger);
        orchestrator.generateAllIncremental(
            schema, createVersionConfigs(),
            (msg, vc) -> "com.example.Proto",
            protoFiles, protoRoot
        );

        Path stateFile = cacheDir.resolve("state.json");
        String content = Files.readString(stateFile);

        // Verify state file contains expected fields
        assertThat(content).contains("pluginVersion");
        assertThat(content).contains("configHash");
        assertThat(content).contains("protoFingerprints");
        assertThat(content).contains("protoDependencies");
        assertThat(content).contains("lastGeneration");
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void incrementalGeneration_corruptedCache_recoversGracefully() throws Exception {
        MergedSchema schema = createMergedSchema();
        GeneratorConfig config = createConfig(true);
        Set<Path> protoFiles = collectProtoFiles();

        // Create corrupted cache
        Path stateFile = cacheDir.resolve("state.json");
        Files.writeString(stateFile, "{ invalid json content }");

        // Should recover and generate
        GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, logger);
        int count = orchestrator.generateAllIncremental(
            schema, createVersionConfigs(),
            (msg, vc) -> "com.example.Proto",
            protoFiles, protoRoot
        );

        assertThat(count).isGreaterThan(0);

        // Cache should be valid now
        String newContent = Files.readString(stateFile);
        assertThat(newContent).contains("pluginVersion");
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void incrementalGeneration_configChanged_triggersFullRegeneration() throws Exception {
        MergedSchema schema = createMergedSchema();
        Set<Path> protoFiles = collectProtoFiles();

        // First run with one config
        GeneratorConfig config1 = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .apiPackage("com.example.api")
            .implPackagePattern("com.example.{version}")
            .protoPackagePattern("com.example.proto.{version}")
            .generateBuilders(false)
            .build();

        GenerationOrchestrator orchestrator1 = new GenerationOrchestrator(config1, logger);
        orchestrator1.generateAllIncremental(
            schema, createVersionConfigs(),
            (msg, vc) -> "com.example.Proto",
            protoFiles, protoRoot
        );

        // Second run with different config (generateBuilders=true)
        GeneratorConfig config2 = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .apiPackage("com.example.api")
            .implPackagePattern("com.example.{version}")
            .protoPackagePattern("com.example.proto.{version}")
            .generateBuilders(true)  // Changed!
            .build();

        GenerationOrchestrator orchestrator2 = new GenerationOrchestrator(config2, logger);
        int count = orchestrator2.generateAllIncremental(
            schema, createVersionConfigs(),
            (msg, vc) -> "com.example.Proto",
            protoFiles, protoRoot
        );

        // Should regenerate due to config change
        assertThat(count).isGreaterThan(0);
    }

    // ============ Helper Methods ============

    private MergedSchema createMergedSchema() throws Exception {
        ProtocExecutor protocExecutor = new ProtocExecutor();
        ProtoAnalyzer analyzer = new ProtoAnalyzer();

        Path v1Descriptor = tempDir.resolve("v1-descriptor.pb");
        Path v2Descriptor = tempDir.resolve("v2-descriptor.pb");

        protocExecutor.generateDescriptor(v1Dir, v1Descriptor, new String[0], protoRoot);
        protocExecutor.generateDescriptor(v2Dir, v2Descriptor, new String[0], protoRoot);

        ProtoAnalyzer.VersionSchema v1Schema = analyzer.analyze(v1Descriptor, "v1");
        ProtoAnalyzer.VersionSchema v2Schema = analyzer.analyze(v2Descriptor, "v2");

        VersionMerger merger = new VersionMerger(logger);
        return merger.merge(Arrays.asList(v1Schema, v2Schema));
    }

    private GeneratorConfig createConfig(boolean incremental) {
        return GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(incremental)
            .apiPackage("com.example.api")
            .implPackagePattern("com.example.{version}")
            .protoPackagePattern("com.example.proto.{version}")
            .build();
    }

    private List<GenerationOrchestrator.VersionConfig> createVersionConfigs() {
        return Arrays.asList(
            () -> "v1",
            () -> "v2"
        );
    }

    private Set<Path> collectProtoFiles() throws IOException {
        Set<Path> files = new HashSet<>();
        collectProtoFilesFromDir(v1Dir, files);
        collectProtoFilesFromDir(v2Dir, files);
        return files;
    }

    private void collectProtoFilesFromDir(Path dir, Set<Path> files) throws IOException {
        if (!Files.exists(dir)) return;

        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(p -> p.toString().endsWith(".proto"))
                  .filter(Files::isRegularFile)
                  .forEach(files::add);
        }
    }

    private long countJavaFiles(Path dir) throws IOException {
        if (!Files.exists(dir)) return 0;

        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(p -> p.toString().endsWith(".java"))
                         .filter(Files::isRegularFile)
                         .count();
        }
    }
}
