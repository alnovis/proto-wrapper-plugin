package io.alnovis.protowrapper.analyzer;

import io.alnovis.protowrapper.model.ProtoSyntax;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SyntaxDetector}.
 */
class SyntaxDetectorTest {

    @TempDir
    Path tempDir;

    // ==================== detectFromContent tests ====================

    @Test
    void detectFromContent_proto3() {
        String content = """
                syntax = "proto3";
                package test;
                message Foo {}
                """;
        assertEquals(ProtoSyntax.PROTO3, SyntaxDetector.detectFromContent(content));
    }

    @Test
    void detectFromContent_proto2() {
        String content = """
                syntax = "proto2";
                package test;
                message Foo {}
                """;
        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.detectFromContent(content));
    }

    @Test
    void detectFromContent_noSyntax_defaultsToProto2() {
        String content = """
                package test;
                message Foo {}
                """;
        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.detectFromContent(content));
    }

    @Test
    void detectFromContent_singleQuotes() {
        String content = "syntax = 'proto3';\npackage test;";
        assertEquals(ProtoSyntax.PROTO3, SyntaxDetector.detectFromContent(content));
    }

    @Test
    void detectFromContent_extraWhitespace() {
        String content = "  syntax  =  \"proto3\"  ;  \npackage test;";
        assertEquals(ProtoSyntax.PROTO3, SyntaxDetector.detectFromContent(content));
    }

    @Test
    void detectFromContent_emptyContent() {
        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.detectFromContent(""));
        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.detectFromContent(null));
    }

    @Test
    void detectFromContent_syntaxAfterComment() {
        // Comment should not affect syntax detection - syntax line is still valid
        String content = """
                // This is a proto3 file
                syntax = "proto3";
                package test;
                """;
        assertEquals(ProtoSyntax.PROTO3, SyntaxDetector.detectFromContent(content));
    }

    @Test
    void detectFromContent_syntaxInComment_ignored() {
        // "syntax = proto3" inside a comment should be ignored
        // Since we only check lines starting with syntax (with optional whitespace),
        // this commented line should be ignored
        String content = """
                // syntax = "proto3";
                syntax = "proto2";
                package test;
                """;
        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.detectFromContent(content));
    }

    // ==================== detectFromFile tests ====================

    @Test
    void detectFromFile_proto3() throws IOException {
        Path file = tempDir.resolve("test.proto");
        Files.writeString(file, "syntax = \"proto3\";\npackage test;");

        assertEquals(ProtoSyntax.PROTO3, SyntaxDetector.detectFromFile(file));
    }

    @Test
    void detectFromFile_proto2() throws IOException {
        Path file = tempDir.resolve("test.proto");
        Files.writeString(file, "syntax = \"proto2\";\npackage test;");

        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.detectFromFile(file));
    }

    // ==================== detectFromDirectory tests ====================

    @Test
    void detectFromDirectory_allProto3() throws IOException {
        Files.writeString(tempDir.resolve("a.proto"), "syntax = \"proto3\";\npackage a;");
        Files.writeString(tempDir.resolve("b.proto"), "syntax = \"proto3\";\npackage b;");

        assertEquals(ProtoSyntax.PROTO3, SyntaxDetector.detectFromDirectory(tempDir));
    }

    @Test
    void detectFromDirectory_allProto2() throws IOException {
        Files.writeString(tempDir.resolve("a.proto"), "syntax = \"proto2\";\npackage a;");
        Files.writeString(tempDir.resolve("b.proto"), "syntax = \"proto2\";\npackage b;");

        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.detectFromDirectory(tempDir));
    }

    @Test
    void detectFromDirectory_mixed_returnsProto2() throws IOException {
        Files.writeString(tempDir.resolve("a.proto"), "syntax = \"proto2\";\npackage a;");
        Files.writeString(tempDir.resolve("b.proto"), "syntax = \"proto3\";\npackage b;");

        // Mixed syntax should return proto2 as conservative default
        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.detectFromDirectory(tempDir));
    }

    @Test
    void detectFromDirectory_emptyDirectory() throws IOException {
        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.detectFromDirectory(tempDir));
    }

    @Test
    void detectFromDirectory_notDirectory() throws IOException {
        Path file = tempDir.resolve("notdir");
        Files.writeString(file, "content");

        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.detectFromDirectory(file));
    }

    // ==================== detectFromFiles tests ====================

    @Test
    void detectFromFiles_emptyList() throws IOException {
        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.detectFromFiles(List.of()));
        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.detectFromFiles(null));
    }

    @Test
    void detectFromFiles_singleProto3() throws IOException {
        Path file = tempDir.resolve("test.proto");
        Files.writeString(file, "syntax = \"proto3\";\npackage test;");

        assertEquals(ProtoSyntax.PROTO3, SyntaxDetector.detectFromFiles(List.of(file)));
    }

    // ==================== resolve tests ====================

    @Test
    void resolve_notAuto_returnsConfigured() throws IOException {
        assertEquals(ProtoSyntax.PROTO2, SyntaxDetector.resolve(ProtoSyntax.PROTO2, tempDir));
        assertEquals(ProtoSyntax.PROTO3, SyntaxDetector.resolve(ProtoSyntax.PROTO3, tempDir));
    }

    @Test
    void resolve_auto_detectsFromDirectory() throws IOException {
        Files.writeString(tempDir.resolve("a.proto"), "syntax = \"proto3\";\npackage a;");

        assertEquals(ProtoSyntax.PROTO3, SyntaxDetector.resolve(ProtoSyntax.AUTO, tempDir));
    }

    @Test
    void resolve_null_detectsFromDirectory() throws IOException {
        Files.writeString(tempDir.resolve("a.proto"), "syntax = \"proto3\";\npackage a;");

        assertEquals(ProtoSyntax.PROTO3, SyntaxDetector.resolve(null, tempDir));
    }
}
