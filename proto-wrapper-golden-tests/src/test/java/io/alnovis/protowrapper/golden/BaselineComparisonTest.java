package io.alnovis.protowrapper.golden;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Baseline comparison test for generated code regression detection.
 *
 * <p>This test compares the currently generated wrapper code against a baseline
 * snapshot captured before the IR/DSL refactoring. Any differences indicate
 * a regression in the code generation.
 *
 * <p>The baseline was captured with version 2.4.0 before the refactoring started.
 *
 * <p><b>Usage:</b>
 * <ul>
 *   <li>Run {@code mvn compile} to generate fresh code</li>
 *   <li>Run this test to compare against baseline</li>
 *   <li>If test fails, either fix the regression or update the baseline</li>
 * </ul>
 *
 * <p><b>Updating baseline:</b>
 * <pre>
 * rm -rf src/test/resources/baseline
 * mkdir -p src/test/resources/baseline
 * cp -r target/generated-sources/proto2-wrapper src/test/resources/baseline/
 * cp -r target/generated-sources/proto3-wrapper src/test/resources/baseline/
 * </pre>
 */
@DisplayName("Baseline Comparison Tests")
class BaselineComparisonTest {

    private static final Path BASELINE_DIR = Path.of("src/test/resources/baseline");
    private static final Path GENERATED_DIR = Path.of("target/generated-sources");

    @Test
    @DisplayName("Baseline directory exists")
    void baselineDirectoryExists() {
        assertThat(BASELINE_DIR)
            .describedAs("Baseline directory must exist. Run the baseline capture script first.")
            .exists()
            .isDirectory();
    }

    @Test
    @DisplayName("Generated directory exists")
    void generatedDirectoryExists() {
        assertThat(GENERATED_DIR)
            .describedAs("Generated directory must exist. Run 'mvn compile' first.")
            .exists()
            .isDirectory();
    }

    @ParameterizedTest(name = "proto2-wrapper: {0}")
    @MethodSource("proto2WrapperFiles")
    @DisplayName("Proto2 wrapper files match baseline")
    void proto2WrapperFilesMatchBaseline(String relativePath) throws IOException {
        assertFileMatchesBaseline("proto2-wrapper", relativePath);
    }

    @ParameterizedTest(name = "proto3-wrapper: {0}")
    @MethodSource("proto3WrapperFiles")
    @DisplayName("Proto3 wrapper files match baseline")
    void proto3WrapperFilesMatchBaseline(String relativePath) throws IOException {
        assertFileMatchesBaseline("proto3-wrapper", relativePath);
    }

    @Test
    @DisplayName("No extra files in generated proto2-wrapper")
    void noExtraFilesInProto2Wrapper() throws IOException {
        assertNoExtraFiles("proto2-wrapper");
    }

    @Test
    @DisplayName("No extra files in generated proto3-wrapper")
    void noExtraFilesInProto3Wrapper() throws IOException {
        assertNoExtraFiles("proto3-wrapper");
    }

    @Test
    @DisplayName("No missing files in generated proto2-wrapper")
    void noMissingFilesInProto2Wrapper() throws IOException {
        assertNoMissingFiles("proto2-wrapper");
    }

    @Test
    @DisplayName("No missing files in generated proto3-wrapper")
    void noMissingFilesInProto3Wrapper() throws IOException {
        assertNoMissingFiles("proto3-wrapper");
    }

    // ========== Helper Methods ==========

    private void assertFileMatchesBaseline(String wrapperDir, String relativePath) throws IOException {
        Path baselineFile = BASELINE_DIR.resolve(wrapperDir).resolve(relativePath);
        Path generatedFile = GENERATED_DIR.resolve(wrapperDir).resolve(relativePath);

        assertThat(generatedFile)
            .describedAs("Generated file must exist: %s", relativePath)
            .exists();

        String baselineContent = Files.readString(baselineFile);
        String generatedContent = Files.readString(generatedFile);

        if (!baselineContent.equals(generatedContent)) {
            String diff = createDiff(baselineContent, generatedContent, relativePath);
            fail("File content differs from baseline:\n\n%s\n\n%s", relativePath, diff);
        }
    }

    private void assertNoExtraFiles(String wrapperDir) throws IOException {
        Path baselineWrapperDir = BASELINE_DIR.resolve(wrapperDir);
        Path generatedWrapperDir = GENERATED_DIR.resolve(wrapperDir);

        if (!Files.exists(generatedWrapperDir)) {
            return; // Nothing generated, no extras possible
        }

        List<String> extraFiles = new ArrayList<>();

        try (var stream = Files.walk(generatedWrapperDir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".java"))
                  .forEach(generatedPath -> {
                      Path relative = generatedWrapperDir.relativize(generatedPath);
                      Path baselinePath = baselineWrapperDir.resolve(relative);
                      if (!Files.exists(baselinePath)) {
                          extraFiles.add(relative.toString());
                      }
                  });
        }

        assertThat(extraFiles)
            .describedAs("Generated files not in baseline (potential new/unwanted files)")
            .isEmpty();
    }

    private void assertNoMissingFiles(String wrapperDir) throws IOException {
        Path baselineWrapperDir = BASELINE_DIR.resolve(wrapperDir);
        Path generatedWrapperDir = GENERATED_DIR.resolve(wrapperDir);

        if (!Files.exists(baselineWrapperDir)) {
            return; // No baseline, nothing to compare
        }

        List<String> missingFiles = new ArrayList<>();

        try (var stream = Files.walk(baselineWrapperDir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".java"))
                  .forEach(baselinePath -> {
                      Path relative = baselineWrapperDir.relativize(baselinePath);
                      Path generatedPath = generatedWrapperDir.resolve(relative);
                      if (!Files.exists(generatedPath)) {
                          missingFiles.add(relative.toString());
                      }
                  });
        }

        assertThat(missingFiles)
            .describedAs("Baseline files not in generated output (potential missing generation)")
            .isEmpty();
    }

    private String createDiff(String baseline, String generated, String fileName) {
        String[] baselineLines = baseline.split("\n");
        String[] generatedLines = generated.split("\n");

        StringBuilder diff = new StringBuilder();
        diff.append("=== Diff for ").append(fileName).append(" ===\n");

        int maxLines = Math.max(baselineLines.length, generatedLines.length);
        int diffCount = 0;
        int contextLines = 3;

        for (int i = 0; i < maxLines && diffCount < 10; i++) {
            String baselineLine = i < baselineLines.length ? baselineLines[i] : "<EOF>";
            String generatedLine = i < generatedLines.length ? generatedLines[i] : "<EOF>";

            if (!baselineLine.equals(generatedLine)) {
                // Show context before
                int start = Math.max(0, i - contextLines);
                if (start < i) {
                    diff.append(String.format("\n--- Context (line %d-%d) ---\n", start + 1, i));
                    for (int j = start; j < i; j++) {
                        diff.append(String.format("  %4d: %s\n", j + 1,
                            j < baselineLines.length ? baselineLines[j] : "<EOF>"));
                    }
                }

                diff.append(String.format("\n--- Difference at line %d ---\n", i + 1));
                diff.append(String.format("  BASELINE: %s\n", baselineLine));
                diff.append(String.format("  GENERATED: %s\n", generatedLine));
                diffCount++;
            }
        }

        if (diffCount >= 10) {
            diff.append("\n... (truncated, more differences exist)\n");
        }

        diff.append(String.format("\nTotal lines - Baseline: %d, Generated: %d\n",
            baselineLines.length, generatedLines.length));

        return diff.toString();
    }

    // ========== Method Sources ==========

    static Stream<Arguments> proto2WrapperFiles() throws IOException {
        return getJavaFilesRelative("proto2-wrapper");
    }

    static Stream<Arguments> proto3WrapperFiles() throws IOException {
        return getJavaFilesRelative("proto3-wrapper");
    }

    private static Stream<Arguments> getJavaFilesRelative(String wrapperDir) throws IOException {
        Path baselineWrapperDir = BASELINE_DIR.resolve(wrapperDir);

        if (!Files.exists(baselineWrapperDir)) {
            return Stream.empty();
        }

        try (var stream = Files.walk(baselineWrapperDir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .map(p -> baselineWrapperDir.relativize(p).toString())
                .map(Arguments::of)
                .toList()
                .stream();
        }
    }
}
