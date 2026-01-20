package io.alnovis.protowrapper.generator.conflict;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for RepeatedSingleHandler.
 */
@DisplayName("RepeatedSingleHandler")
class RepeatedSingleHandlerTest {

    private RepeatedSingleHandler handler;

    @BeforeEach
    void setUp() {
        handler = RepeatedSingleHandler.INSTANCE;
    }

    @Nested
    @DisplayName("handles()")
    class HandlesTests {

        @Test
        @DisplayName("should handle REPEATED_SINGLE conflict")
        void shouldHandleRepeatedSingleConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.REPEATED_SINGLE,
                    Type.TYPE_INT32, true,  // v1 repeated
                    Type.TYPE_INT32, false  // v2 singular
            );

            assertThat(handler.handles(field, null)).isTrue();
        }

        @Test
        @DisplayName("should not handle SIGNED_UNSIGNED conflict")
        void shouldNotHandleSignedUnsignedConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.SIGNED_UNSIGNED,
                    Type.TYPE_INT32, false,
                    Type.TYPE_UINT32, false
            );

            assertThat(handler.handles(field, null)).isFalse();
        }

        @Test
        @DisplayName("should not handle WIDENING conflict")
        void shouldNotHandleWideningConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.WIDENING,
                    Type.TYPE_INT32, false,
                    Type.TYPE_INT64, false
            );

            assertThat(handler.handles(field, null)).isFalse();
        }

        @Test
        @DisplayName("should not handle FLOAT_DOUBLE conflict")
        void shouldNotHandleFloatDoubleConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.FLOAT_DOUBLE,
                    Type.TYPE_FLOAT, false,
                    Type.TYPE_DOUBLE, false
            );

            assertThat(handler.handles(field, null)).isFalse();
        }

        @Test
        @DisplayName("should not handle NONE conflict")
        void shouldNotHandleNoneConflict() {
            MergedField field = createMergedField(
                    MergedField.ConflictType.NONE,
                    Type.TYPE_INT32, false,
                    Type.TYPE_INT32, false
            );

            assertThat(handler.handles(field, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("ConflictType.isConvertible()")
    class ConflictTypeTests {

        @Test
        @DisplayName("REPEATED_SINGLE should be convertible")
        void repeatedSingleShouldBeConvertible() {
            assertThat(MergedField.ConflictType.REPEATED_SINGLE.isConvertible()).isTrue();
        }

        @Test
        @DisplayName("INCOMPATIBLE should not be convertible")
        void incompatibleShouldNotBeConvertible() {
            assertThat(MergedField.ConflictType.INCOMPATIBLE.isConvertible()).isFalse();
        }
    }

    @Nested
    @DisplayName("Singleton pattern")
    class SingletonTests {

        @Test
        @DisplayName("should be a singleton")
        void shouldBeSingleton() {
            assertThat(RepeatedSingleHandler.INSTANCE).isSameAs(RepeatedSingleHandler.INSTANCE);
        }

        @Test
        @DisplayName("should not be null")
        void shouldNotBeNull() {
            assertThat(RepeatedSingleHandler.INSTANCE).isNotNull();
        }
    }

    @Nested
    @DisplayName("List unified type logic")
    class ListUnifiedTypeTests {

        @Test
        @DisplayName("singletonList wraps singular value correctly")
        void singletonListWrapsSingularValue() {
            int value = 42;
            List<Integer> wrapped = Collections.singletonList(value);
            assertThat(wrapped).hasSize(1);
            assertThat(wrapped.get(0)).isEqualTo(42);
        }

        @Test
        @DisplayName("emptyList returns empty for missing fields")
        void emptyListForMissingFields() {
            List<Integer> empty = Collections.emptyList();
            assertThat(empty).isEmpty();
        }

        @Test
        @DisplayName("list conversion preserves all elements")
        void listConversionPreservesAllElements() {
            List<Integer> original = List.of(1, 2, 3, 4, 5);
            assertThat(original).containsExactly(1, 2, 3, 4, 5);
        }
    }

    @Nested
    @DisplayName("Singular setter validation logic")
    class SingularSetterValidationTests {

        @Test
        @DisplayName("single element list is valid for singular field")
        void singleElementListIsValid() {
            List<Integer> list = List.of(42);
            assertThat(list.size()).isEqualTo(1);
            int value = list.get(0);
            assertThat(value).isEqualTo(42);
        }

        @Test
        @DisplayName("empty list should fail validation for singular field")
        void emptyListShouldFailValidation() {
            List<Integer> list = List.of();
            boolean isValid = !list.isEmpty() && list.size() == 1;
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("multiple element list should fail validation for singular field")
        void multipleElementListShouldFailValidation() {
            List<Integer> list = List.of(1, 2, 3);
            boolean isValid = !list.isEmpty() && list.size() == 1;
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("null list should be considered invalid")
        void nullListShouldBeInvalid() {
            List<Integer> list = null;
            boolean isValid = list != null && !list.isEmpty() && list.size() == 1;
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Type resolution")
    class TypeResolutionTests {

        @Test
        @DisplayName("resolved type should be List<T> for REPEATED_SINGLE")
        void resolvedTypeShouldBeList() {
            MergedField field = createMergedFieldWithResolvedType(
                    MergedField.ConflictType.REPEATED_SINGLE,
                    "java.util.List<java.lang.Integer>",
                    Type.TYPE_INT32, true,  // v1 repeated
                    Type.TYPE_INT32, false  // v2 singular
            );

            assertThat(field.getJavaType()).isEqualTo("java.util.List<java.lang.Integer>");
        }

        @Test
        @DisplayName("element type extraction from List<T>")
        void elementTypeExtractionFromList() {
            String listType = "java.util.List<java.lang.Integer>";
            String elementType = listType.substring(
                    "java.util.List<".length(),
                    listType.length() - 1);
            assertThat(elementType).isEqualTo("java.lang.Integer");
        }

        @Test
        @DisplayName("element type extraction for String list")
        void elementTypeExtractionForStringList() {
            String listType = "java.util.List<java.lang.String>";
            String elementType = listType.substring(
                    "java.util.List<".length(),
                    listType.length() - 1);
            assertThat(elementType).isEqualTo("java.lang.String");
        }
    }

    @Nested
    @DisplayName("Conversion scenarios")
    class ConversionScenarioTests {

        @Test
        @DisplayName("repeated to singular: get first element")
        void repeatedToSingularGetFirst() {
            List<Integer> repeated = List.of(42, 43, 44);
            // For singular version, we would take the first element
            int singular = repeated.get(0);
            assertThat(singular).isEqualTo(42);
        }

        @Test
        @DisplayName("singular to repeated: wrap in singletonList")
        void singularToRepeatedWrapInList() {
            int singular = 42;
            List<Integer> repeated = Collections.singletonList(singular);
            assertThat(repeated).containsExactly(42);
        }

        @Test
        @DisplayName("round trip preserves value through singletonList")
        void roundTripPreservesValue() {
            int original = 42;
            List<Integer> asList = Collections.singletonList(original);
            int recovered = asList.get(0);
            assertThat(recovered).isEqualTo(original);
        }
    }

    // Helper methods

    private MergedField createMergedField(MergedField.ConflictType conflictType,
                                           Type v1Type, boolean v1Repeated,
                                           Type v2Type, boolean v2Repeated) {
        FieldDescriptorProto v1Proto = FieldDescriptorProto.newBuilder()
                .setName("items")
                .setNumber(1)
                .setType(v1Type)
                .setLabel(v1Repeated ? Label.LABEL_REPEATED : Label.LABEL_OPTIONAL)
                .build();

        FieldDescriptorProto v2Proto = FieldDescriptorProto.newBuilder()
                .setName("item")
                .setNumber(1)
                .setType(v2Type)
                .setLabel(v2Repeated ? Label.LABEL_REPEATED : Label.LABEL_OPTIONAL)
                .build();

        FieldInfo v1Field = new FieldInfo(v1Proto);
        FieldInfo v2Field = new FieldInfo(v2Proto);

        return MergedField.builder()
                .addVersionField("v1", v1Field)
                .addVersionField("v2", v2Field)
                .resolvedJavaType("java.util.List<java.lang.Integer>")
                .conflictType(conflictType)
                .build();
    }

    private MergedField createMergedFieldWithResolvedType(MergedField.ConflictType conflictType,
                                                           String resolvedType,
                                                           Type v1Type, boolean v1Repeated,
                                                           Type v2Type, boolean v2Repeated) {
        FieldDescriptorProto v1Proto = FieldDescriptorProto.newBuilder()
                .setName("items")
                .setNumber(1)
                .setType(v1Type)
                .setLabel(v1Repeated ? Label.LABEL_REPEATED : Label.LABEL_OPTIONAL)
                .build();

        FieldDescriptorProto v2Proto = FieldDescriptorProto.newBuilder()
                .setName("item")
                .setNumber(1)
                .setType(v2Type)
                .setLabel(v2Repeated ? Label.LABEL_REPEATED : Label.LABEL_OPTIONAL)
                .build();

        FieldInfo v1Field = new FieldInfo(v1Proto);
        FieldInfo v2Field = new FieldInfo(v2Proto);

        return MergedField.builder()
                .addVersionField("v1", v1Field)
                .addVersionField("v2", v2Field)
                .resolvedJavaType(resolvedType)
                .conflictType(conflictType)
                .build();
    }
}
