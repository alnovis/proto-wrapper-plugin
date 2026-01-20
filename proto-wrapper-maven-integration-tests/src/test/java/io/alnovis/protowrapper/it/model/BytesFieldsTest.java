package io.alnovis.protowrapper.it.model;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.it.proto.v1.Conflicts;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for bytes field handling in builders.
 *
 * <p>This test verifies that:</p>
 * <ul>
 *   <li>Bytes fields can be set via builder using byte[]</li>
 *   <li>The plugin correctly converts byte[] to ByteString</li>
 *   <li>Round-trip serialization works correctly</li>
 *   <li>Nested messages with bytes fields work correctly</li>
 * </ul>
 *
 * <p>This test was added to verify the fix for the bug where
 * bytes fields in builders caused compilation errors:
 * "incompatible types: byte[] cannot be converted to com.google.protobuf.ByteString"</p>
 */
@DisplayName("Bytes Fields Builder Tests")
class BytesFieldsTest {

    private static final byte[] SAMPLE_BINARY_DATA = new byte[]{
            (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF,
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08
    };

    private static final byte[] SAMPLE_SIGNATURE = new byte[]{
            (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE
    };

    // ==================== V1 Bytes Field Tests ====================

    @Nested
    @DisplayName("V1 BytesFieldsTest Builder")
    class V1BytesFieldsBuilderTest {

        @Test
        @DisplayName("Create BytesFieldsTest with required bytes field via builder")
        void createWithRequiredBytesField() {
            // Create via static newBuilder()
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest wrapper =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.newBuilder()
                            .setId("TEST-001")
                            .setBinaryData(SAMPLE_BINARY_DATA)
                            .build();

            assertThat(wrapper.getId()).isEqualTo("TEST-001");
            assertThat(wrapper.getBinaryData()).isEqualTo(SAMPLE_BINARY_DATA);
        }

        @Test
        @DisplayName("Create BytesFieldsTest with optional bytes field via builder")
        void createWithOptionalBytesField() {
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest wrapper =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.newBuilder()
                            .setId("TEST-002")
                            .setBinaryData(SAMPLE_BINARY_DATA)
                            .setOptionalBinary(SAMPLE_SIGNATURE)
                            .build();

            assertThat(wrapper.getId()).isEqualTo("TEST-002");
            assertThat(wrapper.getBinaryData()).isEqualTo(SAMPLE_BINARY_DATA);
            assertThat(wrapper.hasOptionalBinary()).isTrue();
            assertThat(wrapper.getOptionalBinary()).isEqualTo(SAMPLE_SIGNATURE);
        }

        @Test
        @DisplayName("Create BytesFieldsTest with repeated bytes field via builder")
        void createWithRepeatedBytesField() {
            byte[] data1 = "first".getBytes(StandardCharsets.UTF_8);
            byte[] data2 = "second".getBytes(StandardCharsets.UTF_8);
            byte[] data3 = "third".getBytes(StandardCharsets.UTF_8);

            io.alnovis.protowrapper.it.model.api.BytesFieldsTest wrapper =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.newBuilder()
                            .setId("TEST-003")
                            .setBinaryData(SAMPLE_BINARY_DATA)
                            .setBinaryList(Arrays.asList(data1, data2, data3))
                            .build();

            assertThat(wrapper.getId()).isEqualTo("TEST-003");
            assertThat(wrapper.getBinaryList()).hasSize(3);
            assertThat(wrapper.getBinaryList().get(0)).isEqualTo(data1);
            assertThat(wrapper.getBinaryList().get(1)).isEqualTo(data2);
            assertThat(wrapper.getBinaryList().get(2)).isEqualTo(data3);
        }

        @Test
        @DisplayName("Create BytesFieldsTest with nested message containing bytes via proto")
        void createWithNestedBytesPayload() {
            // Create via proto builder since nested wrapper builders require more setup
            Conflicts.BytesFieldsTest proto = Conflicts.BytesFieldsTest.newBuilder()
                    .setId("TEST-004")
                    .setBinaryData(ByteString.copyFrom(SAMPLE_BINARY_DATA))
                    .setPayload(Conflicts.BytesFieldsTest.BinaryPayload.newBuilder()
                            .setContent(ByteString.copyFrom(SAMPLE_BINARY_DATA))
                            .setSignature(ByteString.copyFrom(SAMPLE_SIGNATURE))
                            .setContentType("application/octet-stream")
                            .build())
                    .build();

            // Wrap the proto
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest wrapper =
                    new io.alnovis.protowrapper.it.model.v1.BytesFieldsTest(proto);

            assertThat(wrapper.getId()).isEqualTo("TEST-004");
            assertThat(wrapper.hasPayload()).isTrue();
            assertThat(wrapper.getPayload().getContent()).isEqualTo(SAMPLE_BINARY_DATA);
            assertThat(wrapper.getPayload().getSignature()).isEqualTo(SAMPLE_SIGNATURE);
            assertThat(wrapper.getPayload().getContentType()).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("Modify BytesFieldsTest bytes fields via toBuilder()")
        void modifyBytesFieldsViaToBuilder() {
            // Create initial proto
            Conflicts.BytesFieldsTest initial = Conflicts.BytesFieldsTest.newBuilder()
                    .setId("INITIAL")
                    .setBinaryData(ByteString.copyFrom(SAMPLE_BINARY_DATA))
                    .build();

            io.alnovis.protowrapper.it.model.api.BytesFieldsTest wrapper =
                    new io.alnovis.protowrapper.it.model.v1.BytesFieldsTest(initial);

            // Modify via toBuilder()
            byte[] newData = "modified data".getBytes(StandardCharsets.UTF_8);
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest modified = wrapper.toBuilder()
                    .setId("MODIFIED")
                    .setBinaryData(newData)
                    .setOptionalBinary(SAMPLE_SIGNATURE)
                    .build();

            assertThat(modified.getId()).isEqualTo("MODIFIED");
            assertThat(modified.getBinaryData()).isEqualTo(newData);
            assertThat(modified.getOptionalBinary()).isEqualTo(SAMPLE_SIGNATURE);
        }

        @Test
        @DisplayName("BytesFieldsTest round-trip serialization")
        void bytesFieldsRoundTrip() throws InvalidProtocolBufferException {
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest wrapper =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.newBuilder()
                            .setId("ROUNDTRIP-001")
                            .setBinaryData(SAMPLE_BINARY_DATA)
                            .setOptionalBinary(SAMPLE_SIGNATURE)
                            .setDescription("Test round-trip")
                            .build();

            // Serialize
            byte[] bytes = wrapper.toBytes();

            // Deserialize
            Conflicts.BytesFieldsTest parsed = Conflicts.BytesFieldsTest.parseFrom(bytes);
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest restored =
                    new io.alnovis.protowrapper.it.model.v1.BytesFieldsTest(parsed);

            // Verify
            assertThat(restored.getId()).isEqualTo("ROUNDTRIP-001");
            assertThat(restored.getBinaryData()).isEqualTo(SAMPLE_BINARY_DATA);
            assertThat(restored.getOptionalBinary()).isEqualTo(SAMPLE_SIGNATURE);
            assertThat(restored.getDescription()).isEqualTo("Test round-trip");
        }

        @Test
        @DisplayName("Clear optional bytes field via builder")
        void clearOptionalBytesField() {
            // Create with optional field set
            Conflicts.BytesFieldsTest initial = Conflicts.BytesFieldsTest.newBuilder()
                    .setId("WITH-OPTIONAL")
                    .setBinaryData(ByteString.copyFrom(SAMPLE_BINARY_DATA))
                    .setOptionalBinary(ByteString.copyFrom(SAMPLE_SIGNATURE))
                    .build();

            io.alnovis.protowrapper.it.model.api.BytesFieldsTest wrapper =
                    new io.alnovis.protowrapper.it.model.v1.BytesFieldsTest(initial);

            assertThat(wrapper.hasOptionalBinary()).isTrue();

            // Clear via builder
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest cleared = wrapper.toBuilder()
                    .clearOptionalBinary()
                    .build();

            assertThat(cleared.hasOptionalBinary()).isFalse();
            assertThat(cleared.getId()).isEqualTo("WITH-OPTIONAL");
            assertThat(cleared.getBinaryData()).isEqualTo(SAMPLE_BINARY_DATA);
        }

        @Test
        @DisplayName("Empty bytes field handling")
        void emptyBytesFieldHandling() {
            byte[] emptyBytes = new byte[0];

            io.alnovis.protowrapper.it.model.api.BytesFieldsTest wrapper =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.newBuilder()
                            .setId("EMPTY-BYTES")
                            .setBinaryData(emptyBytes)
                            .build();

            assertThat(wrapper.getBinaryData()).isEmpty();
            assertThat(wrapper.getBinaryData()).isEqualTo(emptyBytes);
        }

        @Test
        @DisplayName("Large bytes field handling")
        void largeBytesFieldHandling() {
            // Create 1MB of data
            byte[] largeData = new byte[1024 * 1024];
            for (int i = 0; i < largeData.length; i++) {
                largeData[i] = (byte) (i % 256);
            }

            io.alnovis.protowrapper.it.model.api.BytesFieldsTest wrapper =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.newBuilder()
                            .setId("LARGE-BYTES")
                            .setBinaryData(largeData)
                            .build();

            assertThat(wrapper.getBinaryData()).hasSize(1024 * 1024);
            assertThat(wrapper.getBinaryData()).isEqualTo(largeData);
        }
    }

    // ==================== V2 Bytes Field Tests ====================

    @Nested
    @DisplayName("V2 BytesFieldsTest Builder")
    class V2BytesFieldsBuilderTest {

        @Test
        @DisplayName("V2 BytesFieldsTest with all bytes fields")
        void v2CreateWithAllBytesFields() {
            byte[] data1 = "item1".getBytes(StandardCharsets.UTF_8);
            byte[] data2 = "item2".getBytes(StandardCharsets.UTF_8);

            io.alnovis.protowrapper.it.model.api.BytesFieldsTest wrapper =
                    io.alnovis.protowrapper.it.model.v2.BytesFieldsTest.newBuilder()
                            .setId("V2-TEST-001")
                            .setBinaryData(SAMPLE_BINARY_DATA)
                            .setOptionalBinary(SAMPLE_SIGNATURE)
                            .setBinaryList(Arrays.asList(data1, data2))
                            .setDescription("V2 test")
                            .setMetadata("extra metadata")  // V2-only field
                            .build();

            assertThat(wrapper.getId()).isEqualTo("V2-TEST-001");
            assertThat(wrapper.getBinaryData()).isEqualTo(SAMPLE_BINARY_DATA);
            assertThat(wrapper.getOptionalBinary()).isEqualTo(SAMPLE_SIGNATURE);
            assertThat(wrapper.getBinaryList()).hasSize(2);
            assertThat(wrapper.getDescription()).isEqualTo("V2 test");
            assertThat(wrapper.getMetadata()).isEqualTo("extra metadata");
        }

        @Test
        @DisplayName("V2 BytesFieldsTest round-trip")
        void v2RoundTrip() throws InvalidProtocolBufferException {
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest wrapper =
                    io.alnovis.protowrapper.it.model.v2.BytesFieldsTest.newBuilder()
                            .setId("V2-ROUNDTRIP")
                            .setBinaryData(SAMPLE_BINARY_DATA)
                            .setOptionalBinary(SAMPLE_SIGNATURE)
                            .setMetadata("v2 metadata")
                            .build();

            byte[] bytes = wrapper.toBytes();

            io.alnovis.protowrapper.it.proto.v2.Conflicts.BytesFieldsTest parsed =
                    io.alnovis.protowrapper.it.proto.v2.Conflicts.BytesFieldsTest.parseFrom(bytes);
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest restored =
                    new io.alnovis.protowrapper.it.model.v2.BytesFieldsTest(parsed);

            assertThat(restored.getId()).isEqualTo("V2-ROUNDTRIP");
            assertThat(restored.getBinaryData()).isEqualTo(SAMPLE_BINARY_DATA);
            assertThat(restored.getOptionalBinary()).isEqualTo(SAMPLE_SIGNATURE);
            assertThat(restored.getMetadata()).isEqualTo("v2 metadata");
        }
    }

    // ==================== Cross-Version Tests ====================

    @Nested
    @DisplayName("Cross-Version Bytes Fields")
    class CrossVersionBytesTest {

        @Test
        @DisplayName("Process bytes fields uniformly across versions")
        void processUniformly() {
            // Create V1
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest v1 =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.newBuilder()
                            .setId("V1")
                            .setBinaryData(SAMPLE_BINARY_DATA)
                            .build();

            // Create V2
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest v2 =
                    io.alnovis.protowrapper.it.model.v2.BytesFieldsTest.newBuilder()
                            .setId("V2")
                            .setBinaryData(SAMPLE_BINARY_DATA)
                            .build();

            // Process uniformly
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest[] wrappers = {v1, v2};

            for (io.alnovis.protowrapper.it.model.api.BytesFieldsTest wrapper : wrappers) {
                assertThat(wrapper.getBinaryData()).isEqualTo(SAMPLE_BINARY_DATA);
                assertThat(wrapper.toBytes()).isNotEmpty();
            }
        }
    }
}
