package io.alnovis.protowrapper.incremental;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.alnovis.protowrapper.PluginLogger;
import io.alnovis.protowrapper.generator.GenerationOrchestrator;
import io.alnovis.protowrapper.generator.GeneratorConfig;
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
 * Performance benchmarks for incremental generation.
 *
 * <p>This test class measures and reports the performance improvements
 * from incremental generation. Run with:
 * <pre>
 * mvn test -Dtest=IncrementalGenerationBenchmark -Dgroups=benchmark
 * </pre>
 *
 * <p>Expected results:
 * <ul>
 *   <li>No changes: >80% reduction (only fingerprint comparison)</li>
 *   <li>Single file change: 50-70% reduction (regenerate subset)</li>
 * </ul>
 */
@Tag("benchmark")
class IncrementalGenerationBenchmark {

    private static final int WARMUP_ITERATIONS = 2;
    private static final int MEASUREMENT_ITERATIONS = 5;

    @TempDir
    Path tempDir;

    private Path outputDir;
    private Path cacheDir;
    private Path protoRoot;
    private MergedSchema emptySchema;
    private List<GenerationOrchestrator.VersionConfig> emptyVersionConfigs;
    private GenerationOrchestrator.ProtoClassNameResolver noopResolver;
    private Set<Path> protoFiles;

    @BeforeEach
    void setUp() throws IOException {
        outputDir = tempDir.resolve("output");
        cacheDir = tempDir.resolve("cache");
        protoRoot = tempDir.resolve("proto");

        Files.createDirectories(outputDir);
        Files.createDirectories(cacheDir);
        Files.createDirectories(protoRoot);
        Files.createDirectories(protoRoot.resolve("v1"));
        Files.createDirectories(protoRoot.resolve("v2"));

        emptySchema = new MergedSchema(Arrays.asList("v1", "v2"));
        emptyVersionConfigs = List.of();
        noopResolver = (msg, vc) -> "com.example.Proto";

        // Create test proto files
        protoFiles = createTestProtoFiles();
    }

    /**
     * Create a realistic set of proto files for benchmarking.
     */
    private Set<Path> createTestProtoFiles() throws IOException {
        Set<Path> files = new HashSet<>();

        // Create v1 files
        for (int i = 1; i <= 10; i++) {
            Path file = protoRoot.resolve("v1/message" + i + ".proto");
            Files.writeString(file, generateProtoContent("v1", "Message" + i, i));
            files.add(file);
        }

        // Create v2 files with some changes
        for (int i = 1; i <= 10; i++) {
            Path file = protoRoot.resolve("v2/message" + i + ".proto");
            // V2 has additional fields
            Files.writeString(file, generateProtoContent("v2", "Message" + i, i + 5));
            files.add(file);
        }

        // Create common files
        Path commonV1 = protoRoot.resolve("v1/common.proto");
        Files.writeString(commonV1, generateCommonProto("v1"));
        files.add(commonV1);

        Path commonV2 = protoRoot.resolve("v2/common.proto");
        Files.writeString(commonV2, generateCommonProto("v2"));
        files.add(commonV2);

        return files;
    }

    private String generateProtoContent(String version, String messageName, int fieldCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("syntax = \"proto3\";\n");
        sb.append("package com.example.").append(version).append(";\n\n");
        sb.append("import \"common.proto\";\n\n");
        sb.append("message ").append(messageName).append(" {\n");
        for (int i = 1; i <= fieldCount; i++) {
            sb.append("    string field").append(i).append(" = ").append(i).append(";\n");
        }
        sb.append("    Common common = ").append(fieldCount + 1).append(";\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String generateCommonProto(String version) {
        return """
            syntax = "proto3";
            package com.example.%s;

            message Common {
                string id = 1;
                string name = 2;
                int64 timestamp = 3;
            }

            enum Status {
                UNKNOWN = 0;
                ACTIVE = 1;
                INACTIVE = 2;
            }
            """.formatted(version);
    }

    @Test
    void benchmarkFullVsIncremental() throws IOException {
        System.out.println("\n========================================");
        System.out.println("INCREMENTAL GENERATION BENCHMARK");
        System.out.println("========================================");
        System.out.println("Proto files: " + protoFiles.size());
        System.out.println("Warmup iterations: " + WARMUP_ITERATIONS);
        System.out.println("Measurement iterations: " + MEASUREMENT_ITERATIONS);
        System.out.println();

        // --- Full Generation Benchmark ---
        System.out.println("--- Full Generation (no cache) ---");
        long[] fullTimes = new long[MEASUREMENT_ITERATIONS];

        for (int i = 0; i < WARMUP_ITERATIONS + MEASUREMENT_ITERATIONS; i++) {
            // Clear cache for each full generation run
            clearCache();

            GeneratorConfig config = GeneratorConfig.builder()
                .outputDirectory(outputDir)
                .cacheDirectory(cacheDir)
                .incremental(true)
                .build();

            GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());

            long start = System.nanoTime();
            orchestrator.generateAllIncremental(
                emptySchema, emptyVersionConfigs, noopResolver,
                protoFiles, protoRoot
            );
            long elapsed = System.nanoTime() - start;

            if (i >= WARMUP_ITERATIONS) {
                fullTimes[i - WARMUP_ITERATIONS] = elapsed;
                System.out.printf("  Run %d: %.2f ms%n", i - WARMUP_ITERATIONS + 1, elapsed / 1_000_000.0);
            }
        }

        double avgFullMs = average(fullTimes) / 1_000_000.0;
        System.out.printf("  Average: %.2f ms%n%n", avgFullMs);

        // --- Incremental (No Changes) Benchmark ---
        System.out.println("--- Incremental (no changes) ---");
        long[] noChangeTimes = new long[MEASUREMENT_ITERATIONS];

        // First, populate the cache
        clearCache();
        GeneratorConfig setupConfig = GeneratorConfig.builder()
            .outputDirectory(outputDir)
            .cacheDirectory(cacheDir)
            .incremental(true)
            .build();
        GenerationOrchestrator setupOrchestrator = new GenerationOrchestrator(setupConfig, PluginLogger.noop());
        setupOrchestrator.generateAllIncremental(
            emptySchema, emptyVersionConfigs, noopResolver,
            protoFiles, protoRoot
        );

        // Now measure incremental with no changes
        for (int i = 0; i < WARMUP_ITERATIONS + MEASUREMENT_ITERATIONS; i++) {
            GeneratorConfig config = GeneratorConfig.builder()
                .outputDirectory(outputDir)
                .cacheDirectory(cacheDir)
                .incremental(true)
                .build();

            GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());

            long start = System.nanoTime();
            orchestrator.generateAllIncremental(
                emptySchema, emptyVersionConfigs, noopResolver,
                protoFiles, protoRoot
            );
            long elapsed = System.nanoTime() - start;

            if (i >= WARMUP_ITERATIONS) {
                noChangeTimes[i - WARMUP_ITERATIONS] = elapsed;
                System.out.printf("  Run %d: %.2f ms%n", i - WARMUP_ITERATIONS + 1, elapsed / 1_000_000.0);
            }
        }

        double avgNoChangeMs = average(noChangeTimes) / 1_000_000.0;
        System.out.printf("  Average: %.2f ms%n%n", avgNoChangeMs);

        // --- Incremental (Single File Changed) Benchmark ---
        System.out.println("--- Incremental (single file changed) ---");
        long[] singleChangeTimes = new long[MEASUREMENT_ITERATIONS];

        for (int i = 0; i < WARMUP_ITERATIONS + MEASUREMENT_ITERATIONS; i++) {
            // Modify one file before each measurement
            Path fileToModify = protoRoot.resolve("v1/message1.proto");
            String originalContent = Files.readString(fileToModify);
            Files.writeString(fileToModify, originalContent + "\n// Modified at run " + i);

            GeneratorConfig config = GeneratorConfig.builder()
                .outputDirectory(outputDir)
                .cacheDirectory(cacheDir)
                .incremental(true)
                .build();

            GenerationOrchestrator orchestrator = new GenerationOrchestrator(config, PluginLogger.noop());

            long start = System.nanoTime();
            orchestrator.generateAllIncremental(
                emptySchema, emptyVersionConfigs, noopResolver,
                protoFiles, protoRoot
            );
            long elapsed = System.nanoTime() - start;

            if (i >= WARMUP_ITERATIONS) {
                singleChangeTimes[i - WARMUP_ITERATIONS] = elapsed;
                System.out.printf("  Run %d: %.2f ms%n", i - WARMUP_ITERATIONS + 1, elapsed / 1_000_000.0);
            }
        }

        double avgSingleChangeMs = average(singleChangeTimes) / 1_000_000.0;
        System.out.printf("  Average: %.2f ms%n%n", avgSingleChangeMs);

        // --- Results Summary ---
        System.out.println("========================================");
        System.out.println("RESULTS SUMMARY");
        System.out.println("========================================");
        System.out.printf("Full generation:           %.2f ms%n", avgFullMs);
        System.out.printf("Incremental (no changes):  %.2f ms%n", avgNoChangeMs);
        System.out.printf("Incremental (1 file):      %.2f ms%n", avgSingleChangeMs);
        System.out.println();

        double noChangeImprovement = (1 - avgNoChangeMs / avgFullMs) * 100;
        double singleChangeImprovement = (1 - avgSingleChangeMs / avgFullMs) * 100;

        System.out.printf("Improvement (no changes):  %.1f%%%n", noChangeImprovement);
        System.out.printf("Improvement (1 file):      %.1f%%%n", singleChangeImprovement);
        System.out.println("========================================\n");

        // Assertions
        assertThat(noChangeImprovement)
            .as("Incremental with no changes should be at least 50% faster")
            .isGreaterThan(50.0);
    }

    private void clearCache() throws IOException {
        try (Stream<Path> files = Files.walk(cacheDir)) {
            files.filter(Files::isRegularFile)
                .forEach(f -> {
                    try {
                        Files.delete(f);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }

    private double average(long[] values) {
        long sum = 0;
        for (long v : values) {
            sum += v;
        }
        return (double) sum / values.length;
    }
}
