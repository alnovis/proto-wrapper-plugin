package io.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for nested type builder support.
 *
 * <p>This test verifies that nested wrapper types have getTypedProto() method
 * which is required for builder's extractProto() to work via reflection.</p>
 *
 * <p>This test was added to verify the fix for the bug where nested types
 * didn't have getTypedProto() method, causing builder.setXxx(nestedWrapper)
 * to fail with NoSuchMethodException.</p>
 */
@DisplayName("Nested Type Builder Tests")
class NestedTypeBuilderTest {

    private static final byte[] SAMPLE_CONTENT = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SAMPLE_SIGNATURE = new byte[]{0x01, 0x02, 0x03, 0x04};

    @Nested
    @DisplayName("V1 Nested Type Builder")
    class V1NestedTypeBuilderTest {

        @Test
        @DisplayName("Nested type wrapper should have getTypedProto() method")
        void nestedTypeHasGetTypedProto() throws Exception {
            // Create a nested wrapper via builder
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest.BinaryPayload payload =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.BinaryPayload.newBuilder()
                            .setContent(SAMPLE_CONTENT)
                            .setSignature(SAMPLE_SIGNATURE)
                            .setContentType("application/octet-stream")
                            .build();

            // Verify getTypedProto() exists and works
            var method = payload.getClass().getMethod("getTypedProto");
            assertThat(method).isNotNull();

            Object proto = method.invoke(payload);
            assertThat(proto).isNotNull();
            assertThat(proto.getClass().getSimpleName()).isEqualTo("BinaryPayload");
        }

        @Test
        @DisplayName("Parent builder should accept nested wrapper created via newBuilder()")
        void parentBuilderAcceptsNestedWrapper() {
            // Create nested wrapper via static newBuilder()
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest.BinaryPayload payload =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.BinaryPayload.newBuilder()
                            .setContent(SAMPLE_CONTENT)
                            .setSignature(SAMPLE_SIGNATURE)
                            .setContentType("text/plain")
                            .build();

            // Use nested wrapper in parent builder - this was failing before the fix
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest parent =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.newBuilder()
                            .setId("TEST-NESTED-001")
                            .setBinaryData(SAMPLE_CONTENT)
                            .setPayload(payload)  // This line was failing with NoSuchMethodException
                            .build();

            // Verify
            assertThat(parent.getId()).isEqualTo("TEST-NESTED-001");
            assertThat(parent.hasPayload()).isTrue();
            assertThat(parent.getPayload().getContent()).isEqualTo(SAMPLE_CONTENT);
            assertThat(parent.getPayload().getSignature()).isEqualTo(SAMPLE_SIGNATURE);
            assertThat(parent.getPayload().getContentType()).isEqualTo("text/plain");
        }

        @Test
        @DisplayName("Modify nested wrapper via toBuilder() and set in parent")
        void modifyNestedAndSetInParent() {
            // Create initial parent with nested payload
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest.BinaryPayload initialPayload =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.BinaryPayload.newBuilder()
                            .setContent(SAMPLE_CONTENT)
                            .setContentType("text/plain")
                            .build();

            io.alnovis.protowrapper.it.model.api.BytesFieldsTest parent =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.newBuilder()
                            .setId("TEST-MODIFY-001")
                            .setBinaryData(SAMPLE_CONTENT)
                            .setPayload(initialPayload)
                            .build();

            // Modify the nested payload via toBuilder()
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest.BinaryPayload modifiedPayload =
                    parent.getPayload().toBuilder()
                            .setContentType("application/json")
                            .setSignature(SAMPLE_SIGNATURE)
                            .build();

            // Create new parent with modified payload
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest modifiedParent =
                    parent.toBuilder()
                            .setPayload(modifiedPayload)
                            .build();

            // Verify
            assertThat(modifiedParent.getPayload().getContentType()).isEqualTo("application/json");
            assertThat(modifiedParent.getPayload().getSignature()).isEqualTo(SAMPLE_SIGNATURE);
            assertThat(modifiedParent.getPayload().getContent()).isEqualTo(SAMPLE_CONTENT);
        }

        @Test
        @DisplayName("Round-trip with nested type created via wrapper builder")
        void roundTripWithNestedTypeBuilder() throws Exception {
            // Create nested type via wrapper builder
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest.BinaryPayload payload =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.BinaryPayload.newBuilder()
                            .setContent(SAMPLE_CONTENT)
                            .setSignature(SAMPLE_SIGNATURE)
                            .setContentType("application/octet-stream")
                            .build();

            // Create parent with nested type
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest original =
                    io.alnovis.protowrapper.it.model.v1.BytesFieldsTest.newBuilder()
                            .setId("ROUNDTRIP-NESTED")
                            .setBinaryData(SAMPLE_CONTENT)
                            .setPayload(payload)
                            .setDescription("Round-trip test")
                            .build();

            // Serialize
            byte[] bytes = original.toBytes();

            // Deserialize
            io.alnovis.protowrapper.it.proto.v1.Conflicts.BytesFieldsTest parsed =
                    io.alnovis.protowrapper.it.proto.v1.Conflicts.BytesFieldsTest.parseFrom(bytes);
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest restored =
                    new io.alnovis.protowrapper.it.model.v1.BytesFieldsTest(parsed);

            // Verify
            assertThat(restored.getId()).isEqualTo("ROUNDTRIP-NESTED");
            assertThat(restored.hasPayload()).isTrue();
            assertThat(restored.getPayload().getContent()).isEqualTo(SAMPLE_CONTENT);
            assertThat(restored.getPayload().getSignature()).isEqualTo(SAMPLE_SIGNATURE);
            assertThat(restored.getPayload().getContentType()).isEqualTo("application/octet-stream");
            assertThat(restored.getDescription()).isEqualTo("Round-trip test");
        }
    }

    @Nested
    @DisplayName("V2 Nested Type Builder")
    class V2NestedTypeBuilderTest {

        @Test
        @DisplayName("V2 nested type wrapper should have getTypedProto() method")
        void v2NestedTypeHasGetTypedProto() throws Exception {
            // Create a nested wrapper via builder
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest.BinaryPayload payload =
                    io.alnovis.protowrapper.it.model.v2.BytesFieldsTest.BinaryPayload.newBuilder()
                            .setContent(SAMPLE_CONTENT)
                            .setSignature(SAMPLE_SIGNATURE)
                            .setContentType("application/octet-stream")
                            .build();

            // Verify getTypedProto() exists and works
            var method = payload.getClass().getMethod("getTypedProto");
            assertThat(method).isNotNull();

            Object proto = method.invoke(payload);
            assertThat(proto).isNotNull();
        }

        @Test
        @DisplayName("V2 parent builder should accept nested wrapper")
        void v2ParentBuilderAcceptsNestedWrapper() {
            // Create nested wrapper
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest.BinaryPayload payload =
                    io.alnovis.protowrapper.it.model.v2.BytesFieldsTest.BinaryPayload.newBuilder()
                            .setContent(SAMPLE_CONTENT)
                            .setContentType("application/xml")
                            .build();

            // Use in parent
            io.alnovis.protowrapper.it.model.api.BytesFieldsTest parent =
                    io.alnovis.protowrapper.it.model.v2.BytesFieldsTest.newBuilder()
                            .setId("V2-NESTED-001")
                            .setBinaryData(SAMPLE_CONTENT)
                            .setPayload(payload)
                            .build();

            // Verify
            assertThat(parent.getId()).isEqualTo("V2-NESTED-001");
            assertThat(parent.hasPayload()).isTrue();
            assertThat(parent.getPayload().getContent()).isEqualTo(SAMPLE_CONTENT);
            assertThat(parent.getPayload().getContentType()).isEqualTo("application/xml");
        }
    }

    @Nested
    @DisplayName("Deeply Nested Type Builder")
    class DeeplyNestedTypeBuilderTest {

        @Test
        @DisplayName("InnerData nested type should have getTypedProto()")
        void innerDataHasGetTypedProto() throws Exception {
            // SimpleNestedConflicts.InnerData is a nested type
            io.alnovis.protowrapper.it.model.api.SimpleNestedConflicts.InnerData innerData =
                    io.alnovis.protowrapper.it.model.v1.SimpleNestedConflicts.InnerData.newBuilder()
                            .setValue(42)
                            .setTypeCode(1)
                            .setData("test data")
                            .build();

            // Verify getTypedProto() exists
            var method = innerData.getClass().getMethod("getTypedProto");
            assertThat(method).isNotNull();

            Object proto = method.invoke(innerData);
            assertThat(proto).isNotNull();
        }

        @Test
        @DisplayName("SimpleNestedConflicts builder accepts InnerData wrapper")
        void parentBuilderAcceptsInnerData() {
            // Create nested InnerData via wrapper builder
            io.alnovis.protowrapper.it.model.api.SimpleNestedConflicts.InnerData innerData =
                    io.alnovis.protowrapper.it.model.v1.SimpleNestedConflicts.InnerData.newBuilder()
                            .setValue(100)
                            .setTypeCode(2)
                            .setData("inner content")
                            .build();

            // Use in parent builder
            io.alnovis.protowrapper.it.model.api.SimpleNestedConflicts parent =
                    io.alnovis.protowrapper.it.model.v1.SimpleNestedConflicts.newBuilder()
                            .setId("DEEPLY-NESTED-001")
                            .setData(innerData)  // This uses extractProto() which calls getTypedProto()
                            .setDescription("Test deeply nested")
                            .build();

            // Verify
            assertThat(parent.getId()).isEqualTo("DEEPLY-NESTED-001");
            assertThat(parent.getData().getValue()).isEqualTo(100);
            assertThat(parent.getData().getTypeCode()).isEqualTo(2);
            assertThat(parent.getData().getData()).isEqualTo("inner content");
            assertThat(parent.getDescription()).isEqualTo("Test deeply nested");
        }
    }
}
