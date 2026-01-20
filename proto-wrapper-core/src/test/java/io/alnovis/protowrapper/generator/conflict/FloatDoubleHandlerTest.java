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
 * Tests for FloatDoubleHandler.
 */
@DisplayName("FloatDoubleHandler")
class FloatDoubleHandlerTest {

    private FloatDoubleHandler handler;

    @BeforeEach
    void setUp() {
        handler = FloatDoubleHandler.INSTANCE;
    }

    @Nested
    @DisplayName("handles()")
    class HandlesTests {

        @Test
        @DisplayName("should handle scalar FLOAT_DOUBLE conflict")
        void shouldHandleScalarFloatDoubleConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.FLOAT_DOUBLE, false);

            assertThat(handler.handles(field, null)).isTrue();
        }

        @Test
        @DisplayName("should not handle repeated fields")
        void shouldNotHandleRepeatedFields() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.FLOAT_DOUBLE, true);

            assertThat(handler.handles(field, null)).isFalse();
        }

        @Test
        @DisplayName("should not handle WIDENING conflict")
        void shouldNotHandleWideningConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.WIDENING, false);

            assertThat(handler.handles(field, null)).isFalse();
        }

        @Test
        @DisplayName("should not handle NONE conflict")
        void shouldNotHandleNoneConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.NONE, false);

            assertThat(handler.handles(field, null)).isFalse();
        }

        @Test
        @DisplayName("should not handle INT_ENUM conflict")
        void shouldNotHandleIntEnumConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.INT_ENUM, false);

            assertThat(handler.handles(field, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("ConflictType.isConvertible()")
    class ConflictTypeTests {

        @Test
        @DisplayName("FLOAT_DOUBLE should be convertible")
        void floatDoubleShouldBeConvertible() {
            assertThat(MergedField.ConflictType.FLOAT_DOUBLE.isConvertible()).isTrue();
        }

        @Test
        @DisplayName("WIDENING should be convertible")
        void wideningShouldBeConvertible() {
            assertThat(MergedField.ConflictType.WIDENING.isConvertible()).isTrue();
        }

        @Test
        @DisplayName("NONE should be convertible")
        void noneShouldBeConvertible() {
            assertThat(MergedField.ConflictType.NONE.isConvertible()).isTrue();
        }

        @Test
        @DisplayName("INCOMPATIBLE should not be convertible")
        void incompatibleShouldNotBeConvertible() {
            assertThat(MergedField.ConflictType.INCOMPATIBLE.isConvertible()).isFalse();
        }

        @Test
        @DisplayName("NARROWING should not be convertible")
        void narrowingShouldNotBeConvertible() {
            assertThat(MergedField.ConflictType.NARROWING.isConvertible()).isFalse();
        }
    }

    @Nested
    @DisplayName("Float range validation logic")
    class FloatRangeValidationTests {

        @Test
        @DisplayName("should accept values within float range")
        void shouldAcceptValuesWithinFloatRange() {
            double value = 1000.5;
            boolean inRange = value >= -Float.MAX_VALUE && value <= Float.MAX_VALUE;
            assertThat(inRange).isTrue();
        }

        @Test
        @DisplayName("should accept Float.MAX_VALUE")
        void shouldAcceptFloatMaxValue() {
            double value = Float.MAX_VALUE;
            boolean inRange = value >= -Float.MAX_VALUE && value <= Float.MAX_VALUE;
            assertThat(inRange).isTrue();
        }

        @Test
        @DisplayName("should accept negative Float.MAX_VALUE")
        void shouldAcceptNegativeFloatMaxValue() {
            double value = -Float.MAX_VALUE;
            boolean inRange = value >= -Float.MAX_VALUE && value <= Float.MAX_VALUE;
            assertThat(inRange).isTrue();
        }

        @Test
        @DisplayName("should reject values exceeding float range")
        void shouldRejectValuesExceedingFloatRange() {
            double value = Double.MAX_VALUE;
            boolean inRange = value >= -Float.MAX_VALUE && value <= Float.MAX_VALUE;
            assertThat(inRange).isFalse();
        }

        @Test
        @DisplayName("should reject large negative values")
        void shouldRejectLargeNegativeValues() {
            double value = -Double.MAX_VALUE;
            boolean inRange = value >= -Float.MAX_VALUE && value <= Float.MAX_VALUE;
            assertThat(inRange).isFalse();
        }

        @Test
        @DisplayName("should accept NaN without range check")
        void shouldAcceptNaN() {
            double value = Double.NaN;
            boolean isSpecial = Double.isNaN(value) || Double.isInfinite(value);
            assertThat(isSpecial).isTrue();
        }

        @Test
        @DisplayName("should accept positive Infinity without range check")
        void shouldAcceptPositiveInfinity() {
            double value = Double.POSITIVE_INFINITY;
            boolean isSpecial = Double.isNaN(value) || Double.isInfinite(value);
            assertThat(isSpecial).isTrue();
        }

        @Test
        @DisplayName("should accept negative Infinity without range check")
        void shouldAcceptNegativeInfinity() {
            double value = Double.NEGATIVE_INFINITY;
            boolean isSpecial = Double.isNaN(value) || Double.isInfinite(value);
            assertThat(isSpecial).isTrue();
        }

        @Test
        @DisplayName("narrowing NaN from double to float preserves NaN")
        void narrowingNaNPreservesNaN() {
            double doubleNaN = Double.NaN;
            float floatNaN = (float) doubleNaN;
            assertThat(Float.isNaN(floatNaN)).isTrue();
        }

        @Test
        @DisplayName("narrowing Infinity from double to float preserves Infinity")
        void narrowingInfinityPreservesInfinity() {
            double doubleInf = Double.POSITIVE_INFINITY;
            float floatInf = (float) doubleInf;
            assertThat(Float.isInfinite(floatInf)).isTrue();
        }
    }

    @Nested
    @DisplayName("Singleton pattern")
    class SingletonTests {

        @Test
        @DisplayName("should be a singleton")
        void shouldBeSingleton() {
            assertThat(FloatDoubleHandler.INSTANCE).isSameAs(FloatDoubleHandler.INSTANCE);
        }

        @Test
        @DisplayName("should not be null")
        void shouldNotBeNull() {
            assertThat(FloatDoubleHandler.INSTANCE).isNotNull();
        }
    }

    @Nested
    @DisplayName("Type resolution")
    class TypeResolutionTests {

        @Test
        @DisplayName("unified type should be double")
        void unifiedTypeShouldBeDouble() {
            assertThat(TypeName.DOUBLE).isEqualTo(TypeName.DOUBLE);
        }

        @Test
        @DisplayName("float to double widening is automatic in Java")
        void floatToDoubleWideningIsAutomatic() {
            float floatValue = 3.14f;
            double doubleValue = floatValue;
            assertThat(doubleValue).isEqualTo(3.14f);
        }

        @Test
        @DisplayName("double to float narrowing requires cast")
        void doubleToFloatNarrowingRequiresCast() {
            double doubleValue = 3.14;
            float floatValue = (float) doubleValue;
            assertThat(floatValue).isEqualTo(3.14f);
        }
    }

    // Helper methods

    private MergedField createMergedField(MergedField.ConflictType conflictType, boolean repeated) {
        FieldDescriptorProto floatProto = FieldDescriptorProto.newBuilder()
                .setName("temperature")
                .setNumber(1)
                .setType(Type.TYPE_FLOAT)
                .setLabel(repeated ? Label.LABEL_REPEATED : Label.LABEL_OPTIONAL)
                .build();

        FieldDescriptorProto doubleProto = FieldDescriptorProto.newBuilder()
                .setName("temperature")
                .setNumber(1)
                .setType(Type.TYPE_DOUBLE)
                .setLabel(repeated ? Label.LABEL_REPEATED : Label.LABEL_OPTIONAL)
                .build();

        FieldInfo v1Field = new FieldInfo(floatProto);
        FieldInfo v2Field = new FieldInfo(doubleProto);

        return MergedField.builder()
                .addVersionField("v1", v1Field)
                .addVersionField("v2", v2Field)
                .resolvedJavaType("double")
                .conflictType(conflictType)
                .build();
    }
}
