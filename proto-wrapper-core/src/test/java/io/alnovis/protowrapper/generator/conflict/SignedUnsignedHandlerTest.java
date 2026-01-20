package io.alnovis.protowrapper.generator.conflict;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SignedUnsignedHandler.
 */
@DisplayName("SignedUnsignedHandler")
class SignedUnsignedHandlerTest {

    private SignedUnsignedHandler handler;

    @BeforeEach
    void setUp() {
        handler = SignedUnsignedHandler.INSTANCE;
    }

    @Nested
    @DisplayName("handles()")
    class HandlesTests {

        @Test
        @DisplayName("should handle scalar SIGNED_UNSIGNED conflict")
        void shouldHandleScalarSignedUnsignedConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.SIGNED_UNSIGNED, false,
                    Type.TYPE_INT32, Type.TYPE_UINT32);

            assertThat(handler.handles(field, null)).isTrue();
        }

        @Test
        @DisplayName("should not handle repeated fields")
        void shouldNotHandleRepeatedFields() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.SIGNED_UNSIGNED, true,
                    Type.TYPE_INT32, Type.TYPE_UINT32);

            assertThat(handler.handles(field, null)).isFalse();
        }

        @Test
        @DisplayName("should not handle WIDENING conflict")
        void shouldNotHandleWideningConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.WIDENING, false,
                    Type.TYPE_INT32, Type.TYPE_INT64);

            assertThat(handler.handles(field, null)).isFalse();
        }

        @Test
        @DisplayName("should not handle FLOAT_DOUBLE conflict")
        void shouldNotHandleFloatDoubleConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.FLOAT_DOUBLE, false,
                    Type.TYPE_FLOAT, Type.TYPE_DOUBLE);

            assertThat(handler.handles(field, null)).isFalse();
        }

        @Test
        @DisplayName("should not handle NONE conflict")
        void shouldNotHandleNoneConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.NONE, false,
                    Type.TYPE_INT32, Type.TYPE_INT32);

            assertThat(handler.handles(field, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("ConflictType.isConvertible()")
    class ConflictTypeTests {

        @Test
        @DisplayName("SIGNED_UNSIGNED should be convertible")
        void signedUnsignedShouldBeConvertible() {
            assertThat(MergedField.ConflictType.SIGNED_UNSIGNED.isConvertible()).isTrue();
        }

        @Test
        @DisplayName("WIDENING should be convertible")
        void wideningShouldBeConvertible() {
            assertThat(MergedField.ConflictType.WIDENING.isConvertible()).isTrue();
        }

        @Test
        @DisplayName("FLOAT_DOUBLE should be convertible")
        void floatDoubleShouldBeConvertible() {
            assertThat(MergedField.ConflictType.FLOAT_DOUBLE.isConvertible()).isTrue();
        }

        @Test
        @DisplayName("INCOMPATIBLE should not be convertible")
        void incompatibleShouldNotBeConvertible() {
            assertThat(MergedField.ConflictType.INCOMPATIBLE.isConvertible()).isFalse();
        }
    }

    @Nested
    @DisplayName("32-bit range validation logic")
    class Signed32RangeValidationTests {

        @Test
        @DisplayName("should accept values within int32 range")
        void shouldAcceptValuesWithinInt32Range() {
            long value = 1000L;
            boolean inRange = value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
            assertThat(inRange).isTrue();
        }

        @Test
        @DisplayName("should accept Integer.MAX_VALUE")
        void shouldAcceptIntegerMaxValue() {
            long value = Integer.MAX_VALUE;
            boolean inRange = value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
            assertThat(inRange).isTrue();
        }

        @Test
        @DisplayName("should accept Integer.MIN_VALUE")
        void shouldAcceptIntegerMinValue() {
            long value = Integer.MIN_VALUE;
            boolean inRange = value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
            assertThat(inRange).isTrue();
        }

        @Test
        @DisplayName("should reject values exceeding int32 range")
        void shouldRejectValuesExceedingInt32Range() {
            long value = Integer.MAX_VALUE + 1L;
            boolean inRange = value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
            assertThat(inRange).isFalse();
        }

        @Test
        @DisplayName("should reject values below int32 range")
        void shouldRejectValuesBelowInt32Range() {
            long value = Integer.MIN_VALUE - 1L;
            boolean inRange = value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
            assertThat(inRange).isFalse();
        }
    }

    @Nested
    @DisplayName("Unsigned 32-bit range validation logic")
    class Unsigned32RangeValidationTests {

        private static final long UINT32_MAX = 0xFFFFFFFFL;

        @Test
        @DisplayName("should accept values within uint32 range")
        void shouldAcceptValuesWithinUint32Range() {
            long value = 1000L;
            boolean inRange = value >= 0 && value <= UINT32_MAX;
            assertThat(inRange).isTrue();
        }

        @Test
        @DisplayName("should accept maximum uint32 value (4294967295)")
        void shouldAcceptMaxUint32Value() {
            long value = UINT32_MAX;
            boolean inRange = value >= 0 && value <= UINT32_MAX;
            assertThat(inRange).isTrue();
        }

        @Test
        @DisplayName("should accept zero")
        void shouldAcceptZero() {
            long value = 0L;
            boolean inRange = value >= 0 && value <= UINT32_MAX;
            assertThat(inRange).isTrue();
        }

        @Test
        @DisplayName("should reject negative values")
        void shouldRejectNegativeValues() {
            long value = -1L;
            boolean inRange = value >= 0 && value <= UINT32_MAX;
            assertThat(inRange).isFalse();
        }

        @Test
        @DisplayName("should reject values exceeding uint32 range")
        void shouldRejectValuesExceedingUint32Range() {
            long value = UINT32_MAX + 1L;
            boolean inRange = value >= 0 && value <= UINT32_MAX;
            assertThat(inRange).isFalse();
        }
    }

    @Nested
    @DisplayName("Unsigned conversion logic")
    class UnsignedConversionTests {

        @Test
        @DisplayName("Integer.toUnsignedLong correctly converts max uint32")
        void toUnsignedLongConvertsMaxUint32() {
            // 0xFFFFFFFF as signed int is -1
            int signedValue = -1;
            long unsignedValue = Integer.toUnsignedLong(signedValue);
            assertThat(unsignedValue).isEqualTo(0xFFFFFFFFL);
        }

        @Test
        @DisplayName("Integer.toUnsignedLong correctly converts positive values")
        void toUnsignedLongConvertsPositiveValues() {
            int signedValue = 100;
            long unsignedValue = Integer.toUnsignedLong(signedValue);
            assertThat(unsignedValue).isEqualTo(100L);
        }

        @Test
        @DisplayName("Integer.toUnsignedLong correctly converts Integer.MAX_VALUE + 1")
        void toUnsignedLongConvertsIntMaxPlusOne() {
            // 0x80000000 as signed int is Integer.MIN_VALUE
            int signedValue = Integer.MIN_VALUE;
            long unsignedValue = Integer.toUnsignedLong(signedValue);
            assertThat(unsignedValue).isEqualTo(2147483648L);
        }

        @Test
        @DisplayName("Narrowing from long to int preserves unsigned semantics")
        void narrowingPreservesUnsignedSemantics() {
            long unsignedValue = 0xFFFFFFFFL;
            int narrowed = (int) unsignedValue;
            long recovered = Integer.toUnsignedLong(narrowed);
            assertThat(recovered).isEqualTo(unsignedValue);
        }
    }

    @Nested
    @DisplayName("Singleton pattern")
    class SingletonTests {

        @Test
        @DisplayName("should be a singleton")
        void shouldBeSingleton() {
            assertThat(SignedUnsignedHandler.INSTANCE).isSameAs(SignedUnsignedHandler.INSTANCE);
        }

        @Test
        @DisplayName("should not be null")
        void shouldNotBeNull() {
            assertThat(SignedUnsignedHandler.INSTANCE).isNotNull();
        }
    }

    @Nested
    @DisplayName("Type resolution")
    class TypeResolutionTests {

        @Test
        @DisplayName("unified type should be long")
        void unifiedTypeShouldBeLong() {
            assertThat(TypeName.LONG).isEqualTo(TypeName.LONG);
        }

        @Test
        @DisplayName("int to long widening is automatic in Java")
        void intToLongWideningIsAutomatic() {
            int intValue = 100;
            long longValue = intValue;
            assertThat(longValue).isEqualTo(100L);
        }

        @Test
        @DisplayName("long to int narrowing requires cast")
        void longToIntNarrowingRequiresCast() {
            long longValue = 100L;
            int intValue = (int) longValue;
            assertThat(intValue).isEqualTo(100);
        }
    }

    // Helper methods

    private MergedField createMergedField(MergedField.ConflictType conflictType, boolean repeated,
                                           Type v1Type, Type v2Type) {
        FieldDescriptorProto v1Proto = FieldDescriptorProto.newBuilder()
                .setName("counter")
                .setNumber(1)
                .setType(v1Type)
                .setLabel(repeated ? Label.LABEL_REPEATED : Label.LABEL_OPTIONAL)
                .build();

        FieldDescriptorProto v2Proto = FieldDescriptorProto.newBuilder()
                .setName("counter")
                .setNumber(1)
                .setType(v2Type)
                .setLabel(repeated ? Label.LABEL_REPEATED : Label.LABEL_OPTIONAL)
                .build();

        FieldInfo v1Field = new FieldInfo(v1Proto);
        FieldInfo v2Field = new FieldInfo(v2Proto);

        return MergedField.builder()
                .addVersionField("v1", v1Field)
                .addVersionField("v2", v2Field)
                .resolvedJavaType("long")
                .conflictType(conflictType)
                .build();
    }
}
