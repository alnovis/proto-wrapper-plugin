package space.alnovis.protowrapper.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import space.alnovis.protowrapper.PluginLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

class ProtocExecutorTest {

    @TempDir
    Path tempDir;

    private ProtocExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ProtocExecutor(PluginLogger.noop());
    }

    @Test
    void shouldFindProtoFiles() throws IOException {
        // Create test proto files
        Path protoDir = tempDir.resolve("proto");
        Files.createDirectories(protoDir);
        Files.createDirectories(protoDir.resolve("sub"));

        Files.writeString(protoDir.resolve("common.proto"), "syntax = \"proto3\";");
        Files.writeString(protoDir.resolve("message.proto"), "syntax = \"proto3\";");
        Files.writeString(protoDir.resolve("sub/nested.proto"), "syntax = \"proto3\";");
        Files.writeString(protoDir.resolve("readme.txt"), "Not a proto file");

        List<Path> protoFiles = executor.findProtoFiles(protoDir);

        assertThat(protoFiles).hasSize(3);
        assertThat(protoFiles).allMatch(p -> p.toString().endsWith(".proto"));
    }

    @Test
    void shouldFindProtoFilesWithPattern() throws IOException {
        Path protoDir = tempDir.resolve("proto");
        Files.createDirectories(protoDir.resolve("include"));
        Files.createDirectories(protoDir.resolve("exclude"));

        Files.writeString(protoDir.resolve("include/a.proto"), "syntax = \"proto3\";");
        Files.writeString(protoDir.resolve("include/b.proto"), "syntax = \"proto3\";");
        Files.writeString(protoDir.resolve("exclude/c.proto"), "syntax = \"proto3\";");

        List<Path> files = executor.findProtoFiles(protoDir, "include/*.proto", null);

        assertThat(files).hasSize(2);
    }

    @Test
    void shouldExtractJavaPackage() throws IOException {
        Path protoFile = tempDir.resolve("test.proto");
        Files.writeString(protoFile,
            "syntax = \"proto3\";\n" +
            "package example;\n" +
            "option java_package = \"org.example.proto\";\n" +
            "option java_outer_classname = \"TestProto\";\n" +
            "\n" +
            "message Test {\n" +
            "    int32 id = 1;\n" +
            "}\n");

        String javaPackage = executor.extractJavaPackage(protoFile);
        assertThat(javaPackage).isEqualTo("org.example.proto");
    }

    @Test
    void shouldExtractJavaOuterClassname() throws IOException {
        Path protoFile = tempDir.resolve("test.proto");
        Files.writeString(protoFile,
            "syntax = \"proto3\";\n" +
            "option java_outer_classname = \"MyProtos\";\n");

        String className = executor.extractJavaOuterClassname(protoFile);
        assertThat(className).isEqualTo("MyProtos");
    }

    @Test
    void shouldExtractProtoPackage() throws IOException {
        Path protoFile = tempDir.resolve("test.proto");
        Files.writeString(protoFile,
            "syntax = \"proto3\";\n" +
            "package org.example.api;\n");

        String protoPackage = executor.extractProtoPackage(protoFile);
        assertThat(protoPackage).isEqualTo("org.example.api");
    }

    @Test
    void shouldBuildProtoMappings() throws IOException {
        Path protoDir = tempDir.resolve("proto");
        Files.createDirectories(protoDir);

        Files.writeString(protoDir.resolve("common.proto"),
            "syntax = \"proto3\";\n" +
            "option java_package = \"org.test.proto\";\n" +
            "option java_outer_classname = \"Common\";\n" +
            "\n" +
            "message Money {\n" +
            "    int64 bills = 1;\n" +
            "    int32 coins = 2;\n" +
            "}\n" +
            "\n" +
            "message DateTime {\n" +
            "    int32 year = 1;\n" +
            "}\n");

        var mappings = executor.buildProtoMappings(protoDir, "org.test.proto");

        assertThat(mappings).containsEntry("Money", "org.test.proto.Common.Money");
        assertThat(mappings).containsEntry("DateTime", "org.test.proto.Common.DateTime");
    }

    @Test
    void shouldCheckProtocAvailability() {
        // This test depends on protoc being installed
        boolean available = executor.isProtocAvailable();

        if (available) {
            String version = executor.getProtocVersion();
            assertThat(version).contains("libprotoc");
        }
    }

    @Test
    void shouldGenerateDescriptor() throws IOException {
        // Skip if protoc not available
        assumeThat(executor.isProtocAvailable())
            .as("protoc must be available")
            .isTrue();

        // Create a valid proto file
        Path protoDir = tempDir.resolve("proto");
        Files.createDirectories(protoDir);

        Files.writeString(protoDir.resolve("test.proto"),
            "syntax = \"proto3\";\n" +
            "package test;\n" +
            "\n" +
            "message TestMessage {\n" +
            "    int32 id = 1;\n" +
            "    string name = 2;\n" +
            "}\n");

        Path outputFile = tempDir.resolve("output.pb");

        Path result = executor.generateDescriptor(protoDir, outputFile);

        assertThat(result).exists();
        assertThat(Files.size(result)).isGreaterThan(0);
    }
}
