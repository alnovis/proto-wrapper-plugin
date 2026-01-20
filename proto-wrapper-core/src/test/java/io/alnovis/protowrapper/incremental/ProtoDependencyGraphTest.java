package io.alnovis.protowrapper.incremental;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProtoDependencyGraph}.
 */
class ProtoDependencyGraphTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create a directory structure with proto files
        Files.createDirectories(tempDir.resolve("v1"));
        Files.createDirectories(tempDir.resolve("v2"));
    }

    @Test
    void build_createsEmptyGraphForNoFiles() throws IOException {
        ProtoDependencyGraph graph = ProtoDependencyGraph.build(Set.of(), tempDir);

        assertThat(graph.size()).isZero();
        assertThat(graph.getAllFiles()).isEmpty();
    }

    @Test
    void build_parsesImportsFromProtoFiles() throws IOException {
        // Create common.proto
        Path common = tempDir.resolve("v1/common.proto");
        Files.writeString(common, """
            syntax = "proto3";
            package test.v1;
            message Common {}
            """);

        // Create order.proto that imports common.proto
        Path order = tempDir.resolve("v1/order.proto");
        Files.writeString(order, """
            syntax = "proto3";
            package test.v1;
            import "v1/common.proto";
            message Order {}
            """);

        ProtoDependencyGraph graph = ProtoDependencyGraph.build(Set.of(common, order), tempDir);

        assertThat(graph.size()).isEqualTo(2);
        assertThat(graph.getDirectImports("v1/order.proto")).contains("v1/common.proto");
        assertThat(graph.getDirectImports("v1/common.proto")).isEmpty();
    }

    @Test
    void build_handlesPublicImports() throws IOException {
        Path file = tempDir.resolve("test.proto");
        Files.writeString(file, """
            syntax = "proto3";
            import public "other.proto";
            message Test {}
            """);

        ProtoDependencyGraph graph = ProtoDependencyGraph.build(Set.of(file), tempDir);

        assertThat(graph.getDirectImports("test.proto")).contains("other.proto");
    }

    @Test
    void build_handlesWeakImports() throws IOException {
        Path file = tempDir.resolve("test.proto");
        Files.writeString(file, """
            syntax = "proto3";
            import weak "other.proto";
            message Test {}
            """);

        ProtoDependencyGraph graph = ProtoDependencyGraph.build(Set.of(file), tempDir);

        assertThat(graph.getDirectImports("test.proto")).contains("other.proto");
    }

    @Test
    void build_ignoresGoogleProtobufImports() throws IOException {
        Path file = tempDir.resolve("test.proto");
        Files.writeString(file, """
            syntax = "proto3";
            import "google/protobuf/timestamp.proto";
            import "google/protobuf/any.proto";
            import "local.proto";
            message Test {}
            """);

        ProtoDependencyGraph graph = ProtoDependencyGraph.build(Set.of(file), tempDir);

        assertThat(graph.getDirectImports("test.proto"))
            .contains("local.proto")
            .doesNotContain("google/protobuf/timestamp.proto")
            .doesNotContain("google/protobuf/any.proto");
    }

    @Test
    void getTransitiveDependents_findsDependentFiles() throws IOException {
        // Create chain: common <- types <- order
        Path common = tempDir.resolve("common.proto");
        Files.writeString(common, "syntax = \"proto3\"; message Common {}");

        Path types = tempDir.resolve("types.proto");
        Files.writeString(types, "syntax = \"proto3\"; import \"common.proto\"; message Types {}");

        Path order = tempDir.resolve("order.proto");
        Files.writeString(order, "syntax = \"proto3\"; import \"types.proto\"; message Order {}");

        ProtoDependencyGraph graph = ProtoDependencyGraph.build(Set.of(common, types, order), tempDir);

        // common is imported by types, types is imported by order
        Set<String> dependents = graph.getTransitiveDependents("common.proto");

        assertThat(dependents).containsExactlyInAnyOrder("types.proto", "order.proto");
    }

    @Test
    void getTransitiveDependents_returnsEmptyForLeafNode() throws IOException {
        Path leaf = tempDir.resolve("leaf.proto");
        Files.writeString(leaf, "syntax = \"proto3\"; message Leaf {}");

        ProtoDependencyGraph graph = ProtoDependencyGraph.build(Set.of(leaf), tempDir);

        assertThat(graph.getTransitiveDependents("leaf.proto")).isEmpty();
    }

    @Test
    void getTransitiveDependencies_findsDependencies() throws IOException {
        // Create chain: common <- types <- order
        Path common = tempDir.resolve("common.proto");
        Files.writeString(common, "syntax = \"proto3\"; message Common {}");

        Path types = tempDir.resolve("types.proto");
        Files.writeString(types, "syntax = \"proto3\"; import \"common.proto\"; message Types {}");

        Path order = tempDir.resolve("order.proto");
        Files.writeString(order, "syntax = \"proto3\"; import \"types.proto\"; message Order {}");

        ProtoDependencyGraph graph = ProtoDependencyGraph.build(Set.of(common, types, order), tempDir);

        // order imports types, types imports common
        Set<String> dependencies = graph.getTransitiveDependencies("order.proto");

        assertThat(dependencies).containsExactlyInAnyOrder("types.proto", "common.proto");
    }

    @Test
    void getDirectDependents_findsOnlyDirectDependents() throws IOException {
        // common is directly imported by types, not order
        Path common = tempDir.resolve("common.proto");
        Files.writeString(common, "syntax = \"proto3\"; message Common {}");

        Path types = tempDir.resolve("types.proto");
        Files.writeString(types, "syntax = \"proto3\"; import \"common.proto\"; message Types {}");

        Path order = tempDir.resolve("order.proto");
        Files.writeString(order, "syntax = \"proto3\"; import \"types.proto\"; message Order {}");

        ProtoDependencyGraph graph = ProtoDependencyGraph.build(Set.of(common, types, order), tempDir);

        assertThat(graph.getDirectDependents("common.proto")).containsExactly("types.proto");
    }

    @Test
    void fromImports_createsGraphFromMap() {
        Map<String, Set<String>> imports = Map.of(
            "order.proto", Set.of("common.proto"),
            "common.proto", Set.of()
        );

        ProtoDependencyGraph graph = ProtoDependencyGraph.fromImports(imports);

        assertThat(graph.size()).isEqualTo(2);
        assertThat(graph.getDirectImports("order.proto")).contains("common.proto");
        assertThat(graph.getDirectDependents("common.proto")).contains("order.proto");
    }

    @Test
    void getImports_returnsUnmodifiableMap() throws IOException {
        Path file = tempDir.resolve("test.proto");
        Files.writeString(file, "syntax = \"proto3\"; message Test {}");

        ProtoDependencyGraph graph = ProtoDependencyGraph.build(Set.of(file), tempDir);

        Map<String, Set<String>> imports = graph.getImports();

        assertThat(imports).isNotNull();
        // Should be usable for serialization
        assertThat(imports.keySet()).contains("test.proto");
    }

    @Test
    void containsFile_returnsCorrectValue() throws IOException {
        Path file = tempDir.resolve("test.proto");
        Files.writeString(file, "syntax = \"proto3\";");

        ProtoDependencyGraph graph = ProtoDependencyGraph.build(Set.of(file), tempDir);

        assertThat(graph.containsFile("test.proto")).isTrue();
        assertThat(graph.containsFile("other.proto")).isFalse();
    }

    @Test
    void diamondDependency_handledCorrectly() throws IOException {
        // Diamond: base <- left, base <- right, left <- top, right <- top
        Path base = tempDir.resolve("base.proto");
        Files.writeString(base, "syntax = \"proto3\";");

        Path left = tempDir.resolve("left.proto");
        Files.writeString(left, "syntax = \"proto3\"; import \"base.proto\";");

        Path right = tempDir.resolve("right.proto");
        Files.writeString(right, "syntax = \"proto3\"; import \"base.proto\";");

        Path top = tempDir.resolve("top.proto");
        Files.writeString(top, "syntax = \"proto3\"; import \"left.proto\"; import \"right.proto\";");

        ProtoDependencyGraph graph = ProtoDependencyGraph.build(
            Set.of(base, left, right, top), tempDir);

        // base should have left, right, and top as transitive dependents
        Set<String> dependents = graph.getTransitiveDependents("base.proto");
        assertThat(dependents).containsExactlyInAnyOrder("left.proto", "right.proto", "top.proto");

        // top should have base, left, right as transitive dependencies
        Set<String> dependencies = graph.getTransitiveDependencies("top.proto");
        assertThat(dependencies).containsExactlyInAnyOrder("left.proto", "right.proto", "base.proto");
    }

    @Test
    void toString_providesUsefulInfo() throws IOException {
        Path file = tempDir.resolve("test.proto");
        Files.writeString(file, "syntax = \"proto3\"; import \"other.proto\";");

        ProtoDependencyGraph graph = ProtoDependencyGraph.build(Set.of(file), tempDir);

        String str = graph.toString();
        assertThat(str).contains("files=1");
        assertThat(str).contains("totalImports=1");
    }
}
