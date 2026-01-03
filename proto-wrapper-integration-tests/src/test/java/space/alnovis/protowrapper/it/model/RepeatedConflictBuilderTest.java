package space.alnovis.protowrapper.it.model;

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for builder methods on repeated fields with type conflicts.
 *
 * <p>V1.4.0 feature: Enables builder methods (add, addAll, set, clear)
 * for repeated fields with type conflicts.</p>
 *
 * <p>Supported conflict types:</p>
 * <ul>
 *   <li>WIDENING: repeated int32 vs repeated int64 -> List<Long></li>
 *   <li>FLOAT_DOUBLE: repeated float vs repeated double -> List<Double></li>
 *   <li>INT_ENUM: repeated int32 vs repeated SomeEnum -> List<Integer></li>
 *   <li>STRING_BYTES: repeated string vs repeated bytes -> List<String></li>
 * </ul>
 */
@DisplayName("Repeated Conflict Builder Tests (v1.4.0)")
class RepeatedConflictBuilderTest {

    // ==================== WIDENING: repeated int32 -> int64 ====================

    @Nested
    @DisplayName("WIDENING: repeated int32 vs int64 (numbers field)")
    class WideningRepeatedBuilderTest {

        @Test
        @DisplayName("V1 addNumbers with int-range values succeeds")
        void v1AddNumbersWithIntRangeSucceeds() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH-V1")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto)
                            .toBuilder()
                            .addNumbers(100L)
                            .addNumbers(200L)
                            .addNumbers(300L)
                            .build();

            assertThat(result.getNumbers()).containsExactly(100L, 200L, 300L);
        }

        @Test
        @DisplayName("V1 addNumbers with out-of-range value throws IllegalArgumentException")
        void v1AddNumbersWithOutOfRangeThrows() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH-V1")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts.Builder builder =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto).toBuilder();

            assertThatThrownBy(() -> builder.addNumbers(9_999_999_999L).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds int32 range");
        }

        @Test
        @DisplayName("V2 addNumbers accepts long values exceeding int range")
        void v2AddNumbersAcceptsLongValues() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH-V2")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .addNumbers(9_999_999_999L)
                            .addNumbers(-5_000_000_000L)
                            .build();

            assertThat(result.getNumbers()).containsExactly(9_999_999_999L, -5_000_000_000L);
        }

        @Test
        @DisplayName("addAllNumbers adds multiple elements")
        void addAllNumbersAddsMultiple() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .addAllNumbers(List.of(100L, 200L, 300L))
                            .build();

            assertThat(result.getNumbers()).containsExactly(100L, 200L, 300L);
        }

        @Test
        @DisplayName("setNumbers replaces all elements")
        void setNumbersReplacesAll() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .addNumbers(1L)
                            .addNumbers(2L)
                            .setBatchId("BATCH")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .setNumbers(List.of(100L, 200L, 300L))
                            .build();

            assertThat(result.getNumbers()).containsExactly(100L, 200L, 300L);
        }

        @Test
        @DisplayName("clearNumbers removes all elements")
        void clearNumbersRemovesAll() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .addNumbers(1L)
                            .addNumbers(2L)
                            .addNumbers(3L)
                            .setBatchId("BATCH")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .clearNumbers()
                            .build();

            assertThat(result.getNumbers()).isEmpty();
        }
    }

    // ==================== FLOAT_DOUBLE: repeated float -> double ====================

    @Nested
    @DisplayName("FLOAT_DOUBLE: repeated float vs double (values field)")
    class FloatDoubleRepeatedBuilderTest {

        @Test
        @DisplayName("V1 addValues with float-range values succeeds")
        void v1AddValuesWithFloatRange() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH-V1")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto)
                            .toBuilder()
                            .addValues(1.5)
                            .addValues(2.5)
                            .addValues(3.5)
                            .build();

            assertThat(result.getValues()).containsExactly(1.5, 2.5, 3.5);
        }

        @Test
        @DisplayName("V2 addValues with high-precision doubles")
        void v2AddValuesWithHighPrecision() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH-V2")
                            .build();

            double pi = 3.141592653589793;
            double e = 2.718281828459045;

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .addValues(pi)
                            .addValues(e)
                            .build();

            assertThat(result.getValues()).containsExactly(pi, e);
        }

        @Test
        @DisplayName("addAllValues and setValues work correctly")
        void addAllAndSetValues() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .addValues(1.0)
                            .setBatchId("BATCH")
                            .build();

            // Test addAll
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts withAddAll =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .addAllValues(List.of(2.0, 3.0))
                            .build();
            assertThat(withAddAll.getValues()).containsExactly(1.0, 2.0, 3.0);

            // Test set (replaces)
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts withSet =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .setValues(List.of(5.0, 6.0))
                            .build();
            assertThat(withSet.getValues()).containsExactly(5.0, 6.0);
        }
    }

    // ==================== INT_ENUM: repeated int32 -> enum ====================

    @Nested
    @DisplayName("INT_ENUM: repeated int32 vs enum (codes field)")
    class IntEnumRepeatedBuilderTest {

        @Test
        @DisplayName("V1 addCodes with int values")
        void v1AddCodesWithInt() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH-V1")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto)
                            .toBuilder()
                            .addCodes(1)
                            .addCodes(2)
                            .addCodes(3)
                            .build();

            assertThat(result.getCodes()).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("V2 addCodes with int values (converted to enum)")
        void v2AddCodesWithInt() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH-V2")
                            .build();

            // Add int values - they will be converted to enum via forNumber()
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .addCodes(1)  // CODE_SUCCESS
                            .addCodes(2)  // CODE_WARNING
                            .addCodes(3)  // CODE_ERROR
                            .build();

            assertThat(result.getCodes()).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("addAllCodes and setCodes work correctly")
        void addAllAndSetCodes() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts.newBuilder()
                            .addCodes(0)
                            .setBatchId("BATCH")
                            .build();

            // Test addAll
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts withAddAll =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto)
                            .toBuilder()
                            .addAllCodes(List.of(1, 2))
                            .build();
            assertThat(withAddAll.getCodes()).containsExactly(0, 1, 2);

            // Test set (replaces)
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts withSet =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto)
                            .toBuilder()
                            .setCodes(List.of(3, 4))
                            .build();
            assertThat(withSet.getCodes()).containsExactly(3, 4);

            // Test clear
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts cleared =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto)
                            .toBuilder()
                            .clearCodes()
                            .build();
            assertThat(cleared.getCodes()).isEmpty();
        }
    }

    // ==================== STRING_BYTES: repeated string -> bytes ====================

    @Nested
    @DisplayName("STRING_BYTES: repeated string vs bytes (texts field)")
    class StringBytesRepeatedBuilderTest {

        @Test
        @DisplayName("V1 addTexts with String values")
        void v1AddTextsWithString() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH-V1")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto)
                            .toBuilder()
                            .addTexts("hello")
                            .addTexts("world")
                            .build();

            assertThat(result.getTexts()).containsExactly("hello", "world");
        }

        @Test
        @DisplayName("V2 addTexts with String values (converted to bytes)")
        void v2AddTextsWithString() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH-V2")
                            .build();

            // Add String values - they will be converted to ByteString via UTF-8
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .addTexts("hello")
                            .addTexts("world")
                            .build();

            assertThat(result.getTexts()).containsExactly("hello", "world");
        }

        @Test
        @DisplayName("V2 addTexts with Unicode strings")
        void v2AddTextsWithUnicode() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH-V2")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .addTexts("привет")
                            .addTexts("мир")
                            .build();

            assertThat(result.getTexts()).containsExactly("привет", "мир");
        }

        @Test
        @DisplayName("addAllTexts and setTexts work correctly")
        void addAllAndSetTexts() {
            space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v1.Conflicts.RepeatedConflicts.newBuilder()
                            .addTexts("initial")
                            .setBatchId("BATCH")
                            .build();

            // Test addAll
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts withAddAll =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto)
                            .toBuilder()
                            .addAllTexts(List.of("a", "b"))
                            .build();
            assertThat(withAddAll.getTexts()).containsExactly("initial", "a", "b");

            // Test set (replaces)
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts withSet =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto)
                            .toBuilder()
                            .setTexts(List.of("x", "y"))
                            .build();
            assertThat(withSet.getTexts()).containsExactly("x", "y");

            // Test clear
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts cleared =
                    new space.alnovis.protowrapper.it.model.v1.RepeatedConflicts(proto)
                            .toBuilder()
                            .clearTexts()
                            .build();
            assertThat(cleared.getTexts()).isEmpty();
        }
    }

    // ==================== Mixed Operations ====================

    @Nested
    @DisplayName("Mixed operations: combine multiple repeated conflict fields")
    class MixedOperationsTest {

        @Test
        @DisplayName("Build with multiple repeated conflict fields")
        void buildWithMultipleFields() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("BATCH")
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .addNumbers(1L)
                            .addNumbers(2L)
                            .addValues(1.5)
                            .addValues(2.5)
                            .addCodes(1)
                            .addCodes(2)
                            .addTexts("a")
                            .addTexts("b")
                            .build();

            assertThat(result.getNumbers()).containsExactly(1L, 2L);
            assertThat(result.getValues()).containsExactly(1.5, 2.5);
            assertThat(result.getCodes()).containsExactly(1, 2);
            assertThat(result.getTexts()).containsExactly("a", "b");
        }

        @Test
        @DisplayName("Modify existing repeated conflict fields preserves values")
        void modifyExistingFields() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .addNumbers(100L)
                            .addValues(1.0)
                            .setBatchId("BATCH")
                            .build();

            // Add more numbers but replace values
            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .addNumbers(200L)  // Add to existing
                            .setValues(List.of(2.0, 3.0))  // Replace all
                            .build();

            assertThat(result.getNumbers()).containsExactly(100L, 200L);
            assertThat(result.getValues()).containsExactly(2.0, 3.0);
        }

        @Test
        @DisplayName("Builder with non-conflicting fields alongside repeated conflict fields")
        void builderWithMixedFields() {
            space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts proto =
                    space.alnovis.protowrapper.it.proto.v2.Conflicts.RepeatedConflicts.newBuilder()
                            .setBatchId("INITIAL")
                            .setItemCount(10)
                            .build();

            space.alnovis.protowrapper.it.model.api.RepeatedConflicts result =
                    new space.alnovis.protowrapper.it.model.v2.RepeatedConflicts(proto)
                            .toBuilder()
                            .setBatchId("MODIFIED")  // Non-conflicting scalar
                            .setItemCount(20)         // Non-conflicting scalar
                            .addNumbers(1L)           // Repeated conflict
                            .addTexts("test")         // Repeated conflict
                            .build();

            assertThat(result.getBatchId()).isEqualTo("MODIFIED");
            assertThat(result.getItemCount()).isEqualTo(20);
            assertThat(result.getNumbers()).containsExactly(1L);
            assertThat(result.getTexts()).containsExactly("test");
        }
    }
}
