package io.alnovis.protowrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer;
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import io.alnovis.protowrapper.merger.VersionMerger;
import io.alnovis.protowrapper.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for oneof field support.
 */
class OneofSupportTest {

    @TempDir
    Path tempDir;

    private Path v1Dir;
    private Path v2Dir;

    @BeforeEach
    void setup() throws Exception {
        v1Dir = tempDir.resolve("v1");
        v2Dir = tempDir.resolve("v2");
        Files.createDirectories(v1Dir);
        Files.createDirectories(v2Dir);
    }

    @Test
    void testOneofInfoExtraction() throws Exception {
        // Create proto with oneof
        Path protoFile = v1Dir.resolve("test.proto");
        Files.writeString(protoFile, """
            syntax = "proto3";
            package test;
            option java_package = "test.v1";

            message Payment {
                string id = 1;
                oneof method {
                    string card = 2;
                    string bank = 3;
                }
            }
            """);

        // Generate descriptor
        Path descriptorFile = v1Dir.resolve("test.pb");
        ProcessBuilder pb = new ProcessBuilder(
                "protoc",
                "--descriptor_set_out=" + descriptorFile,
                "--proto_path=" + v1Dir,
                protoFile.toString()
        );
        pb.inheritIO();
        int exitCode = pb.start().waitFor();
        assertThat(exitCode).isZero();

        // Analyze
        ProtoAnalyzer analyzer = new ProtoAnalyzer();
        VersionSchema schema = analyzer.analyze(descriptorFile, "v1");

        // Check MessageInfo has oneof
        Optional<MessageInfo> paymentOpt = schema.getMessages().stream()
                .filter(m -> m.getName().equals("Payment"))
                .findFirst();
        assertThat(paymentOpt).isPresent();

        MessageInfo payment = paymentOpt.get();
        assertThat(payment.hasOneofGroups()).isTrue();
        assertThat(payment.getOneofGroups()).hasSize(1);

        OneofInfo methodOneof = payment.getOneofGroups().get(0);
        assertThat(methodOneof.getProtoName()).isEqualTo("method");
        assertThat(methodOneof.getJavaName()).isEqualTo("Method");
        assertThat(methodOneof.getCaseEnumName()).isEqualTo("MethodCase");
        assertThat(methodOneof.getFieldNumbers()).containsExactlyInAnyOrder(2, 3);

        // Check fields have oneof info
        List<FieldInfo> oneofFields = payment.getFields().stream()
                .filter(FieldInfo::isInOneof)
                .toList();
        assertThat(oneofFields).hasSize(2);

        for (FieldInfo field : oneofFields) {
            assertThat(field.getOneofName()).isEqualTo("method");
            assertThat(field.getOneofIndex()).isEqualTo(0);
        }
    }

    @Test
    void testOneofMerging() throws Exception {
        // Create v1 proto with 2 oneof fields
        Path protoV1 = v1Dir.resolve("msg.proto");
        Files.writeString(protoV1, """
            syntax = "proto3";
            package test;
            option java_package = "test.v1";

            message Msg {
                oneof choice {
                    int32 int_val = 1;
                    string str_val = 2;
                }
            }
            """);

        // Create v2 proto with 3 oneof fields
        Path protoV2 = v2Dir.resolve("msg.proto");
        Files.writeString(protoV2, """
            syntax = "proto3";
            package test;
            option java_package = "test.v2";

            message Msg {
                oneof choice {
                    int32 int_val = 1;
                    string str_val = 2;
                    bool bool_val = 3;
                }
            }
            """);

        // Generate descriptors
        Path descV1 = v1Dir.resolve("msg.pb");
        Path descV2 = v2Dir.resolve("msg.pb");

        runProtoc(protoV1, descV1, v1Dir);
        runProtoc(protoV2, descV2, v2Dir);

        // Analyze
        ProtoAnalyzer analyzer = new ProtoAnalyzer();
        VersionSchema schemaV1 = analyzer.analyze(descV1, "v1");
        VersionSchema schemaV2 = analyzer.analyze(descV2, "v2");

        // Merge
        VersionMerger merger = new VersionMerger();
        MergedSchema merged = merger.merge(List.of(schemaV1, schemaV2));

        // Check merged message has oneof
        Optional<MergedMessage> msgOpt = merged.getMessages().stream()
                .filter(m -> m.getName().equals("Msg"))
                .findFirst();
        assertThat(msgOpt).isPresent();

        MergedMessage msg = msgOpt.get();
        assertThat(msg.hasOneofGroups()).isTrue();
        assertThat(msg.getOneofGroups()).hasSize(1);

        MergedOneof choiceOneof = msg.getOneofGroups().get(0);
        assertThat(choiceOneof.getProtoName()).isEqualTo("choice");
        assertThat(choiceOneof.getPresentInVersions()).containsExactlyInAnyOrder("v1", "v2");

        // Oneof should have 3 fields (union of v1 and v2)
        assertThat(choiceOneof.getFields()).hasSize(3);
        assertThat(choiceOneof.getAllFieldNumbers()).containsExactlyInAnyOrder(1, 2, 3);

        // Check hasFieldDifferences() returns true (v1 has 2 fields, v2 has 3)
        assertThat(choiceOneof.hasFieldDifferences()).isTrue();
    }

    @Test
    void testSyntheticOneofSkipped() throws Exception {
        // Proto3 optional creates synthetic oneof - should be skipped
        Path protoFile = v1Dir.resolve("optional.proto");
        Files.writeString(protoFile, """
            syntax = "proto3";
            package test;
            option java_package = "test.v1";

            message OptionalTest {
                optional string name = 1;
                optional int32 value = 2;
            }
            """);

        Path descriptorFile = v1Dir.resolve("optional.pb");
        runProtoc(protoFile, descriptorFile, v1Dir);

        ProtoAnalyzer analyzer = new ProtoAnalyzer();
        VersionSchema schema = analyzer.analyze(descriptorFile, "v1");

        Optional<MessageInfo> msgOpt = schema.getMessages().stream()
                .filter(m -> m.getName().equals("OptionalTest"))
                .findFirst();
        assertThat(msgOpt).isPresent();

        MessageInfo msg = msgOpt.get();
        // Synthetic oneofs should NOT appear in oneofGroups
        assertThat(msg.hasOneofGroups()).isFalse();
        assertThat(msg.getOneofGroups()).isEmpty();

        // Fields should NOT be marked as in oneof
        for (FieldInfo field : msg.getFields()) {
            assertThat(field.isInOneof()).isFalse();
        }
    }

    @Test
    void testMergedOneofCaseConstants() throws Exception {
        Path protoFile = v1Dir.resolve("case.proto");
        Files.writeString(protoFile, """
            syntax = "proto3";
            package test;
            option java_package = "test.v1";

            message Container {
                oneof content {
                    string text = 1;
                    int32 number = 2;
                }
            }
            """);

        Path descriptorFile = v1Dir.resolve("case.pb");
        runProtoc(protoFile, descriptorFile, v1Dir);

        ProtoAnalyzer analyzer = new ProtoAnalyzer();
        VersionSchema schema = analyzer.analyze(descriptorFile, "v1");

        VersionMerger merger = new VersionMerger();
        MergedSchema merged = merger.merge(List.of(schema));

        MergedMessage container = merged.getMessages().stream()
                .filter(m -> m.getName().equals("Container"))
                .findFirst()
                .orElseThrow();

        MergedOneof contentOneof = container.getOneofGroups().get(0);

        List<MergedOneof.CaseConstant> constants = contentOneof.getCaseConstants();
        assertThat(constants).hasSize(3); // TEXT, NUMBER, CONTENT_NOT_SET

        // Check constants
        assertThat(constants.stream().map(MergedOneof.CaseConstant::name))
                .containsExactly("TEXT", "NUMBER", "CONTENT_NOT_SET");

        // Check field numbers
        assertThat(constants.get(0).fieldNumber()).isEqualTo(1); // TEXT
        assertThat(constants.get(1).fieldNumber()).isEqualTo(2); // NUMBER
        assertThat(constants.get(2).fieldNumber()).isEqualTo(0); // NOT_SET

        // Check isNotSet()
        assertThat(constants.get(0).isNotSet()).isFalse();
        assertThat(constants.get(1).isNotSet()).isFalse();
        assertThat(constants.get(2).isNotSet()).isTrue();
    }

    private void runProtoc(Path protoFile, Path descriptorFile, Path protoPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "protoc",
                "--descriptor_set_out=" + descriptorFile,
                "--proto_path=" + protoPath,
                protoFile.toString()
        );
        pb.inheritIO();
        int exitCode = pb.start().waitFor();
        assertThat(exitCode).withFailMessage("protoc failed").isZero();
    }
}
