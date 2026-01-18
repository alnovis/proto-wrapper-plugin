package space.alnovis.protowrapper.golden;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import space.alnovis.protowrapper.golden.proto2.wrapper.api.AllFieldTypes;
import space.alnovis.protowrapper.golden.proto2.wrapper.api.NestedMessage;
import space.alnovis.protowrapper.golden.proto2.wrapper.api.TestEnum;
import space.alnovis.protowrapper.golden.proto2.wrapper.api.VersionContext;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden tests for proto2 field variations.
 *
 * <p>Tests that all field types (required, optional, repeated) work correctly
 * across multiple protocol versions with round-trip serialization.</p>
 *
 * <p>This test class would have caught the bug where proto2 required fields
 * were incorrectly returning null from getters.</p>
 */
@DisplayName("Proto2 Golden Tests")
class Proto2GoldenTest {

    static Stream<VersionContext> allVersions() {
        return Stream.of(
            VersionContext.forVersionId("v1"),
            VersionContext.forVersionId("v2")
        );
    }

    @Nested
    @DisplayName("Required Fields")
    class RequiredFields {

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("required int32 - set and get returns value")
        void requiredInt32_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .setRequiredInt32(42)
                .build();

            assertThat(msg.getRequiredInt32()).isEqualTo(42);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("required int64 - set and get returns value")
        void requiredInt64_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .setRequiredInt64(Long.MAX_VALUE)
                .build();

            assertThat(msg.getRequiredInt64()).isEqualTo(Long.MAX_VALUE);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("required bool - set and get returns value")
        void requiredBool_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .setRequiredBool(true)
                .build();

            assertThat(msg.isRequiredBool()).isTrue();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("required string - set and get returns value")
        void requiredString_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .setRequiredString("hello world")
                .build();

            assertThat(msg.getRequiredString()).isEqualTo("hello world");
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("required bytes - set and get returns value")
        void requiredBytes_setAndGet_returnsValue(VersionContext ctx) {
            byte[] expected = "test bytes".getBytes();
            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .setRequiredBytes(expected)
                .build();

            assertThat(msg.getRequiredBytes()).isEqualTo(expected);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("required float - set and get returns value")
        void requiredFloat_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .setRequiredFloat(3.14f)
                .build();

            assertThat(msg.getRequiredFloat()).isEqualTo(3.14f);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("required double - set and get returns value")
        void requiredDouble_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .setRequiredDouble(Math.PI)
                .build();

            assertThat(msg.getRequiredDouble()).isEqualTo(Math.PI);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("required message - set and get returns value")
        void requiredMessage_setAndGet_returnsValue(VersionContext ctx) {
            NestedMessage nested = ctx.newNestedMessageBuilder()
                .setId(42)
                .setName("nested")
                .build();

            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .setRequiredMessage(nested)
                .build();

            assertThat(msg.getRequiredMessage()).isNotNull();
            assertThat(msg.getRequiredMessage().getId()).isEqualTo(42);
            assertThat(msg.getRequiredMessage().getName()).isEqualTo("nested");
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("required enum - set and get returns value")
        void requiredEnum_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .setRequiredEnum(TestEnum.TWO)
                .build();

            assertThat(msg.getRequiredEnum()).isEqualTo(TestEnum.TWO);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("required fields - round trip through proto")
        void requiredFields_roundTrip_preservesValues(VersionContext ctx) throws Exception {
            // Arrange
            AllFieldTypes original = buildWithRequiredFields(ctx)
                .setRequiredInt32(42)
                .setRequiredInt64(Long.MAX_VALUE)
                .setRequiredBool(true)
                .setRequiredString("round trip test")
                .setRequiredBytes("bytes".getBytes())
                .setRequiredFloat(3.14f)
                .setRequiredDouble(Math.E)
                .setRequiredMessage(ctx.newNestedMessageBuilder().setId(99).setName("nested").build())
                .setRequiredEnum(TestEnum.THREE)
                .build();

            // Act - round trip through bytes serialization
            byte[] bytes = original.toBytes();
            AllFieldTypes restored = ctx.parseAllFieldTypesFromBytes(bytes);

            // Assert
            assertThat(restored.getRequiredInt32()).isEqualTo(42);
            assertThat(restored.getRequiredInt64()).isEqualTo(Long.MAX_VALUE);
            assertThat(restored.isRequiredBool()).isTrue();
            assertThat(restored.getRequiredString()).isEqualTo("round trip test");
            assertThat(restored.getRequiredBytes()).isEqualTo("bytes".getBytes());
            assertThat(restored.getRequiredFloat()).isEqualTo(3.14f);
            assertThat(restored.getRequiredDouble()).isEqualTo(Math.E);
            assertThat(restored.getRequiredMessage().getId()).isEqualTo(99);
            assertThat(restored.getRequiredMessage().getName()).isEqualTo("nested");
            assertThat(restored.getRequiredEnum()).isEqualTo(TestEnum.THREE);
        }
    }

    @Nested
    @DisplayName("Optional Fields")
    class OptionalFields {

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("optional int32 - set and get returns value")
        void optionalInt32_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .setOptionalInt32(123)
                .build();

            assertThat(msg.getOptionalInt32()).isEqualTo(123);
            assertThat(msg.hasOptionalInt32()).isTrue();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("optional int32 - unset returns null")
        void optionalInt32_unset_returnsNull(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx).build();

            assertThat(msg.getOptionalInt32()).isNull();
            assertThat(msg.hasOptionalInt32()).isFalse();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("optional message - set and get returns value")
        void optionalMessage_setAndGet_returnsValue(VersionContext ctx) {
            NestedMessage nested = ctx.newNestedMessageBuilder()
                .setId(42)
                .setName("optional nested")
                .build();

            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .setOptionalMessage(nested)
                .build();

            assertThat(msg.getOptionalMessage()).isNotNull();
            assertThat(msg.getOptionalMessage().getId()).isEqualTo(42);
            assertThat(msg.hasOptionalMessage()).isTrue();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("optional message - unset returns null")
        void optionalMessage_unset_returnsNull(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx).build();

            // Message returns null when unset (consistent with has*() returning false)
            assertThat(msg.getOptionalMessage()).isNull();
            assertThat(msg.hasOptionalMessage()).isFalse();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("optional enum - set and get returns value")
        void optionalEnum_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .setOptionalEnum(TestEnum.TWO)
                .build();

            assertThat(msg.getOptionalEnum()).isEqualTo(TestEnum.TWO);
            assertThat(msg.hasOptionalEnum()).isTrue();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("optional fields - round trip preserves values")
        void optionalFields_roundTrip_preservesValues(VersionContext ctx) throws Exception {
            AllFieldTypes original = buildWithRequiredFields(ctx)
                .setOptionalInt32(42)
                .setOptionalInt64(100L)
                .setOptionalBool(true)
                .setOptionalString("optional string")
                .setOptionalMessage(ctx.newNestedMessageBuilder().setId(1).build())
                .setOptionalEnum(TestEnum.ONE)
                .build();

            byte[] bytes = original.toBytes();
            AllFieldTypes restored = ctx.parseAllFieldTypesFromBytes(bytes);

            assertThat(restored.getOptionalInt32()).isEqualTo(42);
            assertThat(restored.getOptionalInt64()).isEqualTo(100L);
            assertThat(restored.isOptionalBool()).isTrue();
            assertThat(restored.getOptionalString()).isEqualTo("optional string");
            assertThat(restored.getOptionalMessage().getId()).isEqualTo(1);
            assertThat(restored.getOptionalEnum()).isEqualTo(TestEnum.ONE);
        }
    }

    @Nested
    @DisplayName("Repeated Fields")
    class RepeatedFields {

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("repeated int32 - add and get returns list")
        void repeatedInt32_addAndGet_returnsList(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .addRepeatedInt32(1)
                .addRepeatedInt32(2)
                .addRepeatedInt32(3)
                .build();

            assertThat(msg.getRepeatedInt32()).containsExactly(1, 2, 3);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("repeated int32 - empty returns empty list")
        void repeatedInt32_empty_returnsEmptyList(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx).build();

            assertThat(msg.getRepeatedInt32()).isEmpty();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("repeated message - add and get returns list")
        void repeatedMessage_addAndGet_returnsList(VersionContext ctx) {
            NestedMessage msg1 = ctx.newNestedMessageBuilder().setId(1).setName("first").build();
            NestedMessage msg2 = ctx.newNestedMessageBuilder().setId(2).setName("second").build();

            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .addRepeatedMessage(msg1)
                .addRepeatedMessage(msg2)
                .build();

            List<? extends NestedMessage> list = msg.getRepeatedMessage();
            assertThat(list).hasSize(2);
            assertThat(list.get(0).getId()).isEqualTo(1);
            assertThat(list.get(0).getName()).isEqualTo("first");
            assertThat(list.get(1).getId()).isEqualTo(2);
            assertThat(list.get(1).getName()).isEqualTo("second");
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("repeated enum - add and get returns list")
        void repeatedEnum_addAndGet_returnsList(VersionContext ctx) {
            AllFieldTypes msg = buildWithRequiredFields(ctx)
                .addRepeatedEnum(TestEnum.ONE)
                .addRepeatedEnum(TestEnum.TWO)
                .addRepeatedEnum(TestEnum.THREE)
                .build();

            assertThat(msg.getRepeatedEnum())
                .containsExactly(TestEnum.ONE, TestEnum.TWO, TestEnum.THREE);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto2GoldenTest#allVersions")
        @DisplayName("repeated fields - round trip preserves values")
        void repeatedFields_roundTrip_preservesValues(VersionContext ctx) throws Exception {
            AllFieldTypes original = buildWithRequiredFields(ctx)
                .addRepeatedInt32(10)
                .addRepeatedInt32(20)
                .addRepeatedString("a")
                .addRepeatedString("b")
                .addRepeatedMessage(ctx.newNestedMessageBuilder().setId(1).build())
                .addRepeatedEnum(TestEnum.TWO)
                .build();

            byte[] bytes = original.toBytes();
            AllFieldTypes restored = ctx.parseAllFieldTypesFromBytes(bytes);

            assertThat(restored.getRepeatedInt32()).containsExactly(10, 20);
            assertThat(restored.getRepeatedString()).containsExactly("a", "b");
            assertThat(restored.getRepeatedMessage()).hasSize(1);
            assertThat(restored.getRepeatedMessage().get(0).getId()).isEqualTo(1);
            assertThat(restored.getRepeatedEnum()).containsExactly(TestEnum.TWO);
        }
    }

    @Nested
    @DisplayName("Cross-Version Consistency")
    class CrossVersionConsistency {

        @Test
        @DisplayName("Same values produce same results across versions")
        void sameValues_acrossVersions_producesSameResults() {
            VersionContext v1 = VersionContext.forVersionId("v1");
            VersionContext v2 = VersionContext.forVersionId("v2");

            AllFieldTypes msgV1 = buildWithRequiredFields(v1)
                .setOptionalInt32(42)
                .setOptionalString("test")
                .addRepeatedInt32(1)
                .addRepeatedInt32(2)
                .build();

            AllFieldTypes msgV2 = buildWithRequiredFields(v2)
                .setOptionalInt32(42)
                .setOptionalString("test")
                .addRepeatedInt32(1)
                .addRepeatedInt32(2)
                .build();

            // Both versions should return same values
            assertThat(msgV1.getRequiredInt32()).isEqualTo(msgV2.getRequiredInt32());
            assertThat(msgV1.getOptionalInt32()).isEqualTo(msgV2.getOptionalInt32());
            assertThat(msgV1.getOptionalString()).isEqualTo(msgV2.getOptionalString());
            assertThat(msgV1.getRepeatedInt32()).isEqualTo(msgV2.getRepeatedInt32());
        }
    }

    // Helper method to build AllFieldTypes with all required fields set
    private AllFieldTypes.Builder buildWithRequiredFields(VersionContext ctx) {
        return ctx.newAllFieldTypesBuilder()
            .setRequiredInt32(1)
            .setRequiredInt64(1L)
            .setRequiredUint32(1)
            .setRequiredUint64(1L)
            .setRequiredSint32(1)
            .setRequiredSint64(1L)
            .setRequiredFixed32(1)
            .setRequiredFixed64(1L)
            .setRequiredSfixed32(1)
            .setRequiredSfixed64(1L)
            .setRequiredFloat(1.0f)
            .setRequiredDouble(1.0)
            .setRequiredBool(true)
            .setRequiredString("required")
            .setRequiredBytes("bytes".getBytes())
            .setRequiredMessage(ctx.newNestedMessageBuilder().setId(1).build())
            .setRequiredEnum(TestEnum.ONE);
    }
}
