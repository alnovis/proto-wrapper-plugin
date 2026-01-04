package space.alnovis.protowrapper.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DiffMojo.
 *
 * <p>These tests require protoc to be available on the system PATH.</p>
 */
class DiffMojoTest {

    private DiffMojo mojo;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mojo = new DiffMojo();
    }

    /**
     * Check if protoc is available on the system.
     */
    static boolean isProtocAvailable() {
        try {
            Process process = new ProcessBuilder("protoc", "--version")
                .redirectErrorStream(true)
                .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void validateDirectories_throwsWhenV1IsNull() throws Exception {
        setField(mojo, "v1Directory", null);
        setField(mojo, "v2Directory", new File("some/path"));

        MojoExecutionException ex = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(ex.getMessage().contains("v1 directory is required"));
    }

    @Test
    void validateDirectories_throwsWhenV1DoesNotExist() throws Exception {
        setField(mojo, "v1Directory", new File("nonexistent/v1"));
        setField(mojo, "v2Directory", new File("some/path"));

        MojoExecutionException ex = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(ex.getMessage().contains("v1 directory does not exist"));
    }

    @Test
    void validateDirectories_throwsWhenV2IsNull() throws Exception {
        Path v1 = tempDir.resolve("v1");
        Files.createDirectory(v1);

        setField(mojo, "v1Directory", v1.toFile());
        setField(mojo, "v2Directory", null);

        MojoExecutionException ex = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(ex.getMessage().contains("v2 directory is required"));
    }

    @Test
    void validateDirectories_throwsWhenV1IsNotDirectory() throws Exception {
        Path v1File = tempDir.resolve("v1.txt");
        Files.createFile(v1File);

        setField(mojo, "v1Directory", v1File.toFile());
        setField(mojo, "v2Directory", new File("some/path"));

        MojoExecutionException ex = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(ex.getMessage().contains("v1 is not a directory"));
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void execute_comparesSchemas() throws Exception {
        // Copy test protos to temp directories
        Path v1Dir = copyTestProtos("proto/v1", "v1");
        Path v2Dir = copyTestProtos("proto/v2", "v2");

        Path outputFile = tempDir.resolve("diff-report.txt");

        setField(mojo, "v1Directory", v1Dir.toFile());
        setField(mojo, "v2Directory", v2Dir.toFile());
        setField(mojo, "v1Name", "v1");
        setField(mojo, "v2Name", "v2");
        setField(mojo, "outputFormat", "text");
        setField(mojo, "outputFile", outputFile.toFile());
        setField(mojo, "breakingOnly", false);
        setField(mojo, "failOnBreaking", false);
        setField(mojo, "tempDirectory", tempDir.resolve("tmp").toFile());

        mojo.execute();

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);

        // Verify diff output contains expected changes
        assertTrue(content.contains("Schema Comparison: v1 -> v2"));
        assertTrue(content.contains("Profile")); // Added message
        assertTrue(content.contains("DeprecatedMessage")); // Removed message
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void execute_generatesJsonOutput() throws Exception {
        Path v1Dir = copyTestProtos("proto/v1", "v1");
        Path v2Dir = copyTestProtos("proto/v2", "v2");

        Path outputFile = tempDir.resolve("diff-report.json");

        setField(mojo, "v1Directory", v1Dir.toFile());
        setField(mojo, "v2Directory", v2Dir.toFile());
        setField(mojo, "v1Name", "v1");
        setField(mojo, "v2Name", "v2");
        setField(mojo, "outputFormat", "json");
        setField(mojo, "outputFile", outputFile.toFile());
        setField(mojo, "breakingOnly", false);
        setField(mojo, "failOnBreaking", false);
        setField(mojo, "tempDirectory", tempDir.resolve("tmp").toFile());

        mojo.execute();

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);

        // Verify JSON structure
        assertTrue(content.startsWith("{"));
        assertTrue(content.contains("\"v1\": \"v1\""));
        assertTrue(content.contains("\"v2\": \"v2\""));
        assertTrue(content.contains("\"summary\""));
        assertTrue(content.contains("\"breakingChanges\""));
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void execute_generatesMarkdownOutput() throws Exception {
        Path v1Dir = copyTestProtos("proto/v1", "v1");
        Path v2Dir = copyTestProtos("proto/v2", "v2");

        Path outputFile = tempDir.resolve("diff-report.md");

        setField(mojo, "v1Directory", v1Dir.toFile());
        setField(mojo, "v2Directory", v2Dir.toFile());
        setField(mojo, "v1Name", "v1");
        setField(mojo, "v2Name", "v2");
        setField(mojo, "outputFormat", "markdown");
        setField(mojo, "outputFile", outputFile.toFile());
        setField(mojo, "breakingOnly", false);
        setField(mojo, "failOnBreaking", false);
        setField(mojo, "tempDirectory", tempDir.resolve("tmp").toFile());

        mojo.execute();

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);

        // Verify Markdown structure
        assertTrue(content.contains("# Schema Comparison: v1 -> v2"));
        assertTrue(content.contains("## Summary"));
        assertTrue(content.contains("| Category | Added | Modified | Removed |"));
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void execute_failsOnBreakingChanges() throws Exception {
        Path v1Dir = copyTestProtos("proto/v1", "v1");
        Path v2Dir = copyTestProtos("proto/v2", "v2");

        setField(mojo, "v1Directory", v1Dir.toFile());
        setField(mojo, "v2Directory", v2Dir.toFile());
        setField(mojo, "v1Name", "v1");
        setField(mojo, "v2Name", "v2");
        setField(mojo, "outputFormat", "text");
        setField(mojo, "breakingOnly", false);
        setField(mojo, "failOnBreaking", true);
        setField(mojo, "failOnWarning", false);
        setField(mojo, "tempDirectory", tempDir.resolve("tmp").toFile());

        // Should fail because there are breaking changes (removed fields, removed message, etc.)
        MojoFailureException ex = assertThrows(MojoFailureException.class, mojo::execute);
        assertTrue(ex.getMessage().contains("Breaking changes detected"));
    }

    @Test
    @EnabledIf("isProtocAvailable")
    void execute_breakingOnlyOutput() throws Exception {
        Path v1Dir = copyTestProtos("proto/v1", "v1");
        Path v2Dir = copyTestProtos("proto/v2", "v2");

        Path outputFile = tempDir.resolve("breaking-only.txt");

        setField(mojo, "v1Directory", v1Dir.toFile());
        setField(mojo, "v2Directory", v2Dir.toFile());
        setField(mojo, "v1Name", "v1");
        setField(mojo, "v2Name", "v2");
        setField(mojo, "outputFormat", "text");
        setField(mojo, "outputFile", outputFile.toFile());
        setField(mojo, "breakingOnly", true);
        setField(mojo, "failOnBreaking", false);
        setField(mojo, "tempDirectory", tempDir.resolve("tmp").toFile());

        mojo.execute();

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);

        // Should contain breaking changes
        assertTrue(content.contains("Breaking Changes"));
    }

    /**
     * Copies test proto files from resources to a temp directory.
     */
    private Path copyTestProtos(String resourceDir, String targetName) throws IOException {
        Path targetDir = tempDir.resolve(targetName);
        Files.createDirectories(targetDir);

        // Copy proto file from resources
        String resourcePath = "/" + resourceDir + "/test.proto";
        try (var is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.copy(is, targetDir.resolve("test.proto"));
        }

        return targetDir;
    }

    /**
     * Sets a private field on an object using reflection.
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        if (field == null) {
            throw new NoSuchFieldException("Field not found: " + fieldName);
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
