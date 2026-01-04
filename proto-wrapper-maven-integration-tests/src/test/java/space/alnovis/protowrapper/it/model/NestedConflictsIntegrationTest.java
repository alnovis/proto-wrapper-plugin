package space.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import space.alnovis.protowrapper.it.model.api.EnumCode;
import space.alnovis.protowrapper.it.model.api.NestedAllConflicts;
import space.alnovis.protowrapper.it.model.api.SimpleNestedConflicts;
import space.alnovis.protowrapper.it.model.api.TypeCode;
import space.alnovis.protowrapper.it.model.api.VersionContext;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Comprehensive integration tests for ALL conflict types in nested structures.
 *
 * <p>Tests the following conflict types in nested messages:</p>
 * <ul>
 *   <li>WIDENING (int32 to int64)</li>
 *   <li>INT_ENUM (int32 to enum)</li>
 *   <li>STRING_BYTES (string to bytes)</li>
 *   <li>FLOAT_DOUBLE (float to double)</li>
 *   <li>SIGNED_UNSIGNED (int32 to uint32)</li>
 * </ul>
 */
@DisplayName("Nested Conflicts Integration Tests")
class NestedConflictsIntegrationTest {

    // ========== WIDENING in nested structures ==========

    @Nested
    @DisplayName("WIDENING conflict in nested structures")
    class WideningNestedTests {

        @Test
        @DisplayName("v1: should widen int32 to long in getter")
        void v1ShouldWidenToLong() {
            VersionContext v1 = VersionContext.forVersion(1);
            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v1)
                    .setWideningValue(42)
                    .setEnumCode(1)
                    .setPrecision(1.5)
                    .setCounter(100)
                    .build();

            assertThat(nestedData.getWideningValue()).isEqualTo(42L);
        }

        @Test
        @DisplayName("v1: should validate int range when setting long value")
        void v1ShouldValidateIntRange() {
            VersionContext v1 = VersionContext.forVersion(1);
            NestedAllConflicts.NestedData.Builder builder = NestedAllConflicts.NestedData.newBuilder(v1)
                    .setEnumCode(1)
                    .setPrecision(1.5)
                    .setCounter(100);

            assertThatThrownBy(() -> builder.setWideningValue(Long.MAX_VALUE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("v2: should accept full long range")
        void v2ShouldAcceptFullLongRange() {
            VersionContext v2 = VersionContext.forVersion(2);
            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v2)
                    .setWideningValue(Long.MAX_VALUE)
                    .setEnumCode(1)
                    .setPrecision(1.5)
                    .setCounter(100)
                    .build();

            assertThat(nestedData.getWideningValue()).isEqualTo(Long.MAX_VALUE);
        }
    }

    // ========== INT_ENUM in nested structures ==========

    @Nested
    @DisplayName("INT_ENUM conflict in nested structures")
    class IntEnumNestedTests {

        @Test
        @DisplayName("v1: should get raw int value")
        void v1ShouldGetRawInt() {
            VersionContext v1 = VersionContext.forVersion(1);
            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v1)
                    .setWideningValue(1)
                    .setEnumCode(2)
                    .setPrecision(1.5)
                    .setCounter(100)
                    .build();

            assertThat(nestedData.getEnumCode()).isEqualTo(2);
        }

        @Test
        @DisplayName("v1: should get unified enum from int")
        void v1ShouldGetUnifiedEnum() {
            VersionContext v1 = VersionContext.forVersion(1);
            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v1)
                    .setWideningValue(1)
                    .setEnumCode(1) // CODE_SUCCESS
                    .setPrecision(1.5)
                    .setCounter(100)
                    .build();

            EnumCode enumValue = nestedData.getEnumCodeEnum();
            assertThat(enumValue).isNotNull();
            assertThat(enumValue.getValue()).isEqualTo(1);
        }

        @Test
        @DisplayName("v2: should set enum value")
        void v2ShouldSetEnumValue() {
            VersionContext v2 = VersionContext.forVersion(2);
            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v2)
                    .setWideningValue(1)
                    .setEnumCode(EnumCode.fromProtoValue(2))
                    .setPrecision(1.5)
                    .setCounter(100)
                    .build();

            assertThat(nestedData.getEnumCode()).isEqualTo(2);
        }
    }

    // ========== STRING_BYTES in nested structures ==========

    @Nested
    @DisplayName("STRING_BYTES conflict in nested structures")
    class StringBytesNestedTests {

        @Test
        @DisplayName("v1: should get string directly")
        void v1ShouldGetStringDirectly() {
            VersionContext v1 = VersionContext.forVersion(1);
            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v1)
                    .setWideningValue(1)
                    .setEnumCode(1)
                    .setTextData("hello")
                    .setPrecision(1.5)
                    .setCounter(100)
                    .build();

            assertThat(nestedData.getTextData()).isEqualTo("hello");
        }

        @Test
        @DisplayName("v1: should convert string to bytes")
        void v1ShouldConvertStringToBytes() {
            VersionContext v1 = VersionContext.forVersion(1);
            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v1)
                    .setWideningValue(1)
                    .setEnumCode(1)
                    .setTextData("hello")
                    .setPrecision(1.5)
                    .setCounter(100)
                    .build();

            assertThat(nestedData.getTextDataBytes()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("v2: should get bytes as string")
        void v2ShouldGetBytesAsString() {
            VersionContext v2 = VersionContext.forVersion(2);
            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v2)
                    .setWideningValue(1)
                    .setEnumCode(1)
                    .setTextDataBytes("world".getBytes(StandardCharsets.UTF_8))
                    .setPrecision(1.5)
                    .setCounter(100)
                    .build();

            assertThat(nestedData.getTextData()).isEqualTo("world");
        }

        @Test
        @DisplayName("v2: should get raw bytes")
        void v2ShouldGetRawBytes() {
            VersionContext v2 = VersionContext.forVersion(2);
            byte[] rawBytes = {0x00, 0x01, 0x02, 0x03};
            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v2)
                    .setWideningValue(1)
                    .setEnumCode(1)
                    .setTextDataBytes(rawBytes)
                    .setPrecision(1.5)
                    .setCounter(100)
                    .build();

            assertThat(nestedData.getTextDataBytes()).isEqualTo(rawBytes);
        }
    }

    // ========== FLOAT_DOUBLE in nested structures ==========

    @Nested
    @DisplayName("FLOAT_DOUBLE conflict in nested structures")
    class FloatDoubleNestedTests {

        @Test
        @DisplayName("v1: should widen float to double in getter")
        void v1ShouldWidenToDouble() {
            VersionContext v1 = VersionContext.forVersion(1);
            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v1)
                    .setWideningValue(1)
                    .setEnumCode(1)
                    .setPrecision(3.14f)
                    .setCounter(100)
                    .build();

            // Float to double should preserve value
            assertThat(nestedData.getPrecision()).isCloseTo(3.14, within(0.001));
        }

        @Test
        @DisplayName("v1: should validate float range when setting double")
        void v1ShouldValidateFloatRange() {
            VersionContext v1 = VersionContext.forVersion(1);
            NestedAllConflicts.NestedData.Builder builder = NestedAllConflicts.NestedData.newBuilder(v1)
                    .setWideningValue(1)
                    .setEnumCode(1)
                    .setCounter(100);

            // Double.MAX_VALUE exceeds float range
            assertThatThrownBy(() -> builder.setPrecision(Double.MAX_VALUE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("v2: should accept full double range")
        void v2ShouldAcceptFullDoubleRange() {
            VersionContext v2 = VersionContext.forVersion(2);
            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v2)
                    .setWideningValue(1)
                    .setEnumCode(1)
                    .setPrecision(Double.MAX_VALUE)
                    .setCounter(100)
                    .build();

            assertThat(nestedData.getPrecision()).isEqualTo(Double.MAX_VALUE);
        }
    }

    // ========== SIGNED_UNSIGNED in nested structures ==========

    @Nested
    @DisplayName("SIGNED_UNSIGNED conflict in nested structures")
    class SignedUnsignedNestedTests {

        @Test
        @DisplayName("v1: should widen int32 to long in getter")
        void v1ShouldWidenToLong() {
            VersionContext v1 = VersionContext.forVersion(1);
            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v1)
                    .setWideningValue(1)
                    .setEnumCode(1)
                    .setPrecision(1.5)
                    .setCounter(1000)
                    .build();

            assertThat(nestedData.getCounter()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("v1: should validate int range for signed")
        void v1ShouldValidateSignedRange() {
            VersionContext v1 = VersionContext.forVersion(1);
            NestedAllConflicts.NestedData.Builder builder = NestedAllConflicts.NestedData.newBuilder(v1)
                    .setWideningValue(1)
                    .setEnumCode(1)
                    .setPrecision(1.5);

            // Negative values should be valid for signed int32
            NestedAllConflicts.NestedData data = builder.setCounter(-100).build();
            assertThat(data.getCounter()).isEqualTo(-100L);
        }

        @Test
        @DisplayName("v2: should accept uint32 range (up to 4 billion)")
        void v2ShouldAcceptUint32Range() {
            VersionContext v2 = VersionContext.forVersion(2);
            long largeUnsigned = 3_000_000_000L; // > Integer.MAX_VALUE

            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v2)
                    .setWideningValue(1)
                    .setEnumCode(1)
                    .setPrecision(1.5)
                    .setCounter(largeUnsigned)
                    .build();

            assertThat(nestedData.getCounter()).isEqualTo(largeUnsigned);
        }

        @Test
        @DisplayName("v2: should reject negative values for uint32")
        void v2ShouldRejectNegativeValues() {
            VersionContext v2 = VersionContext.forVersion(2);
            NestedAllConflicts.NestedData.Builder builder = NestedAllConflicts.NestedData.newBuilder(v2)
                    .setWideningValue(1)
                    .setEnumCode(1)
                    .setPrecision(1.5);

            assertThatThrownBy(() -> builder.setCounter(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ========== Cross-version conversion tests ==========

    @Nested
    @DisplayName("Cross-version conversion tests for nested conflicts")
    class CrossVersionNestedTests {

        @Test
        @DisplayName("should convert parent with nested from v1 to v2 (SimpleNestedConflicts)")
        void shouldConvertSimpleNestedV1ToV2() {
            // Using SimpleNestedConflicts which doesn't have FLOAT_DOUBLE conflict in nested
            VersionContext v1 = VersionContext.forVersion(1);

            SimpleNestedConflicts.InnerData innerData = SimpleNestedConflicts.InnerData.newBuilder(v1)
                    .setValue(42)
                    .setTypeCode(1)
                    .setData("test")
                    .build();

            SimpleNestedConflicts v1Instance = SimpleNestedConflicts.newBuilder(v1)
                    .setId("simple-1")
                    .setData(innerData)
                    .setDescription("description")
                    .build();

            // Convert to v2
            space.alnovis.protowrapper.it.model.v2.SimpleNestedConflicts v2Instance =
                    v1Instance.asVersion(space.alnovis.protowrapper.it.model.v2.SimpleNestedConflicts.class);

            assertThat(v2Instance.getId()).isEqualTo("simple-1");
            assertThat(v2Instance.getWrapperVersion()).isEqualTo(2);

            SimpleNestedConflicts.InnerData v2Data = v2Instance.getData();
            assertThat(v2Data.getValue()).isEqualTo(42L);
            assertThat(v2Data.getTypeCode()).isEqualTo(1);
            assertThat(v2Data.getData()).isEqualTo("test");
        }

        @Test
        @DisplayName("should convert parent with nested from v2 to v1 (SimpleNestedConflicts)")
        void shouldConvertSimpleNestedV2ToV1() {
            VersionContext v2 = VersionContext.forVersion(2);

            SimpleNestedConflicts.InnerData innerData = SimpleNestedConflicts.InnerData.newBuilder(v2)
                    .setValue(100L)
                    .setTypeCode(TypeCode.fromProtoValue(2))
                    .setDataBytes("binary".getBytes(StandardCharsets.UTF_8))
                    .setMetadata("v2-only")
                    .build();

            SimpleNestedConflicts v2Instance = SimpleNestedConflicts.newBuilder(v2)
                    .setId("simple-2")
                    .setData(innerData)
                    .setDescription("v2-description")
                    .build();

            // Convert to v1
            space.alnovis.protowrapper.it.model.v1.SimpleNestedConflicts v1Instance =
                    v2Instance.asVersion(space.alnovis.protowrapper.it.model.v1.SimpleNestedConflicts.class);

            assertThat(v1Instance.getId()).isEqualTo("simple-2");
            assertThat(v1Instance.getWrapperVersion()).isEqualTo(1);

            SimpleNestedConflicts.InnerData v1Data = v1Instance.getData();
            assertThat(v1Data.getValue()).isEqualTo(100L);
            assertThat(v1Data.getTypeCode()).isEqualTo(2);
            // String/bytes conversion should work
            assertThat(v1Data.getData()).isEqualTo("binary");
        }

        @Test
        @DisplayName("FLOAT_DOUBLE in nested causes proto wire format incompatibility")
        void floatDoubleNestedCausesWireFormatIncompatibility() {
            // This test documents that FLOAT_DOUBLE conflict in nested messages
            // causes protobuf wire format incompatibility during cross-version conversion.
            // This is expected behavior because float and double have different wire formats.

            VersionContext v1 = VersionContext.forVersion(1);

            NestedAllConflicts.NestedData nestedData = NestedAllConflicts.NestedData.newBuilder(v1)
                    .setWideningValue(42)
                    .setEnumCode(1)
                    .setTextData("test")
                    .setPrecision(3.14f)
                    .setCounter(100)
                    .build();

            NestedAllConflicts v1Instance = NestedAllConflicts.newBuilder(v1)
                    .setId("test-1")
                    .setData(nestedData)
                    .build();

            // Cross-version conversion fails due to wire format incompatibility
            assertThatThrownBy(() ->
                    v1Instance.asVersion(space.alnovis.protowrapper.it.model.v2.NestedAllConflicts.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("precision");
        }
    }

    // ========== SimpleNestedConflicts tests (existing nested structure) ==========

    @Nested
    @DisplayName("SimpleNestedConflicts (existing nested with WIDENING, INT_ENUM, STRING_BYTES)")
    class SimpleNestedConflictsTests {

        @Test
        @DisplayName("v1: should handle all conflict types in InnerData")
        void v1ShouldHandleAllConflicts() {
            VersionContext v1 = VersionContext.forVersion(1);

            SimpleNestedConflicts.InnerData innerData = SimpleNestedConflicts.InnerData.newBuilder(v1)
                    .setValue(42)           // WIDENING
                    .setTypeCode(1)         // INT_ENUM
                    .setData("hello")       // STRING_BYTES
                    .build();

            SimpleNestedConflicts parent = SimpleNestedConflicts.newBuilder(v1)
                    .setId("simple-1")
                    .setData(innerData)
                    .build();

            assertThat(parent.getData().getValue()).isEqualTo(42L);
            assertThat(parent.getData().getTypeCode()).isEqualTo(1);
            assertThat(parent.getData().getData()).isEqualTo("hello");
        }

        @Test
        @DisplayName("v2: should handle all conflict types in InnerData")
        void v2ShouldHandleAllConflicts() {
            VersionContext v2 = VersionContext.forVersion(2);

            SimpleNestedConflicts.InnerData innerData = SimpleNestedConflicts.InnerData.newBuilder(v2)
                    .setValue(Long.MAX_VALUE)   // WIDENING: full long range
                    .setTypeCode(TypeCode.fromProtoValue(2))  // INT_ENUM: enum
                    .setDataBytes(new byte[]{0x01, 0x02})     // STRING_BYTES: bytes
                    .setMetadata("v2-metadata")               // v2-only field
                    .build();

            SimpleNestedConflicts parent = SimpleNestedConflicts.newBuilder(v2)
                    .setId("simple-2")
                    .setData(innerData)
                    .build();

            assertThat(parent.getData().getValue()).isEqualTo(Long.MAX_VALUE);
            assertThat(parent.getData().getTypeCode()).isEqualTo(2);
            assertThat(parent.getData().getDataBytes()).isEqualTo(new byte[]{0x01, 0x02});
            assertThat(parent.getData().getMetadata()).isEqualTo("v2-metadata");
        }
    }
}
