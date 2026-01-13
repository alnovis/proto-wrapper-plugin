package space.alnovis.protowrapper.golden;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import space.alnovis.protowrapper.golden.proto3.wrapper.api.AllFieldTypes;
import space.alnovis.protowrapper.golden.proto3.wrapper.api.NestedMessage;
import space.alnovis.protowrapper.golden.proto3.wrapper.api.TestEnum;
import space.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden tests for proto3 field variations.
 *
 * <p>Tests document the behavior of the wrapper API for proto3 fields:
 * <ul>
 *   <li>Singular scalars (no 'optional') - return value directly, has*() always false</li>
 *   <li>Singular message - return value directly (default instance when unset)</li>
 *   <li>Optional fields (with 'optional' keyword) - return null when unset, has*() works</li>
 *   <li>Repeated fields - return lists</li>
 *   <li>Oneof fields - return null when not the active field</li>
 * </ul>
 *
 * <p><b>Proto3 semantics:</b> For singular scalar fields without 'optional' keyword,
 * there is no way to distinguish "not set" from "set to default value".
 * The wrapper returns the value directly (0, false, "", etc. for unset).</p>
 */
@DisplayName("Proto3 Golden Tests")
class Proto3GoldenTest {

    static Stream<VersionContext> allVersions() {
        return Stream.of(
            VersionContext.forVersion(1),
            VersionContext.forVersion(2)
        );
    }

    @Nested
    @DisplayName("Singular Fields (no 'optional' keyword)")
    class SingularFields {

        // ====== Singular scalars return values directly ======

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("singular int32 - set and get returns value")
        void singularInt32_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setSingularInt32(42)
                .build();

            // Proto3 singular: value returned directly
            assertThat(msg.getSingularInt32()).isEqualTo(42);
            // Note: Proto3 implicit scalars have NO has*() method - this is by design
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("singular int32 - unset returns default (0)")
        void singularInt32_unset_returnsDefault(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();

            // Proto3 default for int32 is 0
            assertThat(msg.getSingularInt32()).isEqualTo(0);
            // Note: Proto3 implicit scalars have NO has*() method - this is by design
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("singular bool - set and get returns value")
        void singularBool_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setSingularBool(true)
                .build();

            assertThat(msg.isSingularBool()).isTrue();
            // Note: Proto3 implicit scalars have NO has*() method - this is by design
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("singular bool - unset returns default (false)")
        void singularBool_unset_returnsDefault(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();

            // Proto3 default for bool is false
            assertThat(msg.isSingularBool()).isFalse();
            // Note: Proto3 implicit scalars have NO has*() method - this is by design
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("singular string - set and get returns value")
        void singularString_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setSingularString("hello world")
                .build();

            assertThat(msg.getSingularString()).isEqualTo("hello world");
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("singular string - unset returns empty string")
        void singularString_unset_returnsEmptyString(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();

            // Proto3 default for string is empty string
            assertThat(msg.getSingularString()).isEmpty();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("singular enum - set and get returns value")
        void singularEnum_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setSingularEnum(TestEnum.ENUM_VALUE_TWO)
                .build();

            assertThat(msg.getSingularEnum()).isEqualTo(TestEnum.ENUM_VALUE_TWO);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("singular enum - unset returns default (first value)")
        void singularEnum_unset_returnsDefault(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();

            // Proto3 default for enum is first value (0 = UNKNOWN)
            assertThat(msg.getSingularEnum()).isEqualTo(TestEnum.ENUM_UNKNOWN);
        }

        // ====== Message fields ======

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("singular message - set and get returns value")
        void singularMessage_setAndGet_returnsValue(VersionContext ctx) {
            NestedMessage nested = ctx.newNestedMessageBuilder()
                .setId(42)
                .setName("nested")
                .build();

            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setSingularMessage(nested)
                .build();

            assertThat(msg.getSingularMessage()).isNotNull();
            assertThat(msg.getSingularMessage().getId()).isEqualTo(42);
            assertThat(msg.getSingularMessage().getName()).isEqualTo("nested");
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("singular message - unset returns null")
        void singularMessage_unset_returnsNull(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();

            // Message returns null when unset (consistent with has*() returning false)
            assertThat(msg.getSingularMessage()).isNull();
            assertThat(msg.hasSingularMessage()).isFalse();
        }
    }

    @Nested
    @DisplayName("Optional Fields (proto3 'optional' keyword)")
    class OptionalFields {

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("optional int32 - set and get returns value")
        void optionalInt32_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setOptionalInt32(123)
                .build();

            assertThat(msg.getOptionalInt32()).isEqualTo(123);
            assertThat(msg.hasOptionalInt32()).isTrue();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("optional int32 - unset returns null")
        void optionalInt32_unset_returnsNull(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();

            assertThat(msg.getOptionalInt32()).isNull();
            assertThat(msg.hasOptionalInt32()).isFalse();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("optional int32 - set to 0 is distinguishable from unset")
        void optionalInt32_setToZero_distinguishableFromUnset(VersionContext ctx) {
            AllFieldTypes msgWithZero = ctx.newAllFieldTypesBuilder()
                .setOptionalInt32(0)
                .build();

            AllFieldTypes msgUnset = ctx.newAllFieldTypesBuilder().build();

            // Zero is explicitly set
            assertThat(msgWithZero.getOptionalInt32()).isEqualTo(0);
            assertThat(msgWithZero.hasOptionalInt32()).isTrue();

            // Unset returns null
            assertThat(msgUnset.getOptionalInt32()).isNull();
            assertThat(msgUnset.hasOptionalInt32()).isFalse();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("optional bool - can distinguish false from unset")
        void optionalBool_distinguishFalseFromUnset(VersionContext ctx) {
            AllFieldTypes msgWithFalse = ctx.newAllFieldTypesBuilder()
                .setOptionalBool(false)
                .build();

            AllFieldTypes msgUnset = ctx.newAllFieldTypesBuilder().build();

            // false is explicitly set
            assertThat(msgWithFalse.isOptionalBool()).isFalse();
            assertThat(msgWithFalse.hasOptionalBool()).isTrue();

            // unset returns null
            assertThat(msgUnset.isOptionalBool()).isNull();
            assertThat(msgUnset.hasOptionalBool()).isFalse();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("optional string - set and get returns value")
        void optionalString_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setOptionalString("optional value")
                .build();

            assertThat(msg.getOptionalString()).isEqualTo("optional value");
            assertThat(msg.hasOptionalString()).isTrue();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("optional string - unset returns empty string")
        void optionalString_unset_returnsEmptyString(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();

            // String returns empty (proto default), not null
            assertThat(msg.getOptionalString()).isEmpty();
            assertThat(msg.hasOptionalString()).isFalse();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("optional message - set and get returns value")
        void optionalMessage_setAndGet_returnsValue(VersionContext ctx) {
            NestedMessage nested = ctx.newNestedMessageBuilder()
                .setId(42)
                .setName("optional nested")
                .build();

            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setOptionalMessage(nested)
                .build();

            assertThat(msg.getOptionalMessage()).isNotNull();
            assertThat(msg.getOptionalMessage().getId()).isEqualTo(42);
            assertThat(msg.hasOptionalMessage()).isTrue();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("optional message - unset returns null")
        void optionalMessage_unset_returnsNull(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();

            // Message returns null when unset (consistent with has*() returning false)
            assertThat(msg.getOptionalMessage()).isNull();
            assertThat(msg.hasOptionalMessage()).isFalse();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("optional enum - set and get returns value")
        void optionalEnum_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setOptionalEnum(TestEnum.ENUM_VALUE_TWO)
                .build();

            assertThat(msg.getOptionalEnum()).isEqualTo(TestEnum.ENUM_VALUE_TWO);
            assertThat(msg.hasOptionalEnum()).isTrue();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("optional enum - unset returns default")
        void optionalEnum_unset_returnsDefault(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();

            // Enum returns default value (first enum = UNKNOWN)
            assertThat(msg.getOptionalEnum()).isEqualTo(TestEnum.ENUM_UNKNOWN);
            assertThat(msg.hasOptionalEnum()).isFalse();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("optional fields - round trip preserves values")
        void optionalFields_roundTrip_preservesValues(VersionContext ctx) throws Exception {
            AllFieldTypes original = ctx.newAllFieldTypesBuilder()
                .setOptionalInt32(42)
                .setOptionalBool(false)  // false is set, not default
                .setOptionalString("optional")
                .setOptionalEnum(TestEnum.ENUM_VALUE_TWO)
                .build();

            byte[] bytes = original.toBytes();
            AllFieldTypes restored = ctx.parseAllFieldTypesFromBytes(bytes);

            assertThat(restored.getOptionalInt32()).isEqualTo(42);
            assertThat(restored.isOptionalBool()).isFalse();
            assertThat(restored.hasOptionalBool()).isTrue();
            assertThat(restored.getOptionalString()).isEqualTo("optional");
            assertThat(restored.getOptionalEnum()).isEqualTo(TestEnum.ENUM_VALUE_TWO);
        }
    }

    @Nested
    @DisplayName("Repeated Fields")
    class RepeatedFields {

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("repeated int32 - add and get returns list")
        void repeatedInt32_addAndGet_returnsList(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .addRepeatedInt32(1)
                .addRepeatedInt32(2)
                .addRepeatedInt32(3)
                .build();

            assertThat(msg.getRepeatedInt32()).containsExactly(1, 2, 3);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("repeated int32 - empty returns empty list")
        void repeatedInt32_empty_returnsEmptyList(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();

            assertThat(msg.getRepeatedInt32()).isEmpty();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("repeated message - add and get returns list")
        void repeatedMessage_addAndGet_returnsList(VersionContext ctx) {
            NestedMessage msg1 = ctx.newNestedMessageBuilder().setId(1).setName("first").build();
            NestedMessage msg2 = ctx.newNestedMessageBuilder().setId(2).setName("second").build();

            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
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
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("repeated enum - add and get returns list")
        void repeatedEnum_addAndGet_returnsList(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .addRepeatedEnum(TestEnum.ENUM_VALUE_ONE)
                .addRepeatedEnum(TestEnum.ENUM_VALUE_TWO)
                .build();

            assertThat(msg.getRepeatedEnum())
                .containsExactly(TestEnum.ENUM_VALUE_ONE, TestEnum.ENUM_VALUE_TWO);
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("repeated fields - round trip preserves values")
        void repeatedFields_roundTrip_preservesValues(VersionContext ctx) throws Exception {
            AllFieldTypes original = ctx.newAllFieldTypesBuilder()
                .addRepeatedInt32(10)
                .addRepeatedInt32(20)
                .addRepeatedString("a")
                .addRepeatedString("b")
                .addRepeatedMessage(ctx.newNestedMessageBuilder().setId(1).setName("test").build())
                .addRepeatedEnum(TestEnum.ENUM_VALUE_THREE)
                .build();

            byte[] bytes = original.toBytes();
            AllFieldTypes restored = ctx.parseAllFieldTypesFromBytes(bytes);

            assertThat(restored.getRepeatedInt32()).containsExactly(10, 20);
            assertThat(restored.getRepeatedString()).containsExactly("a", "b");
            assertThat(restored.getRepeatedMessage()).hasSize(1);
            assertThat(restored.getRepeatedMessage().get(0).getName()).isEqualTo("test");
            assertThat(restored.getRepeatedEnum()).containsExactly(TestEnum.ENUM_VALUE_THREE);
        }
    }

    @Nested
    @DisplayName("Oneof Fields")
    class OneofFields {

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("oneof int32 - set and get returns value")
        void oneofInt32_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setOneofInt32(42)
                .build();

            assertThat(msg.getOneofInt32()).isEqualTo(42);
            assertThat(msg.hasOneofInt32()).isTrue();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("oneof string - set and get returns value")
        void oneofString_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setOneofString("oneof value")
                .build();

            assertThat(msg.getOneofString()).isEqualTo("oneof value");
            assertThat(msg.hasOneofString()).isTrue();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("oneof message - set and get returns value")
        void oneofMessage_setAndGet_returnsValue(VersionContext ctx) {
            NestedMessage nested = ctx.newNestedMessageBuilder()
                .setId(99)
                .setName("oneof nested")
                .build();

            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setOneofMessage(nested)
                .build();

            assertThat(msg.getOneofMessage()).isNotNull();
            assertThat(msg.getOneofMessage().getId()).isEqualTo(99);
            assertThat(msg.hasOneofMessage()).isTrue();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("oneof enum - set and get returns value")
        void oneofEnum_setAndGet_returnsValue(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setOneofEnum(TestEnum.ENUM_VALUE_THREE)
                .build();

            assertThat(msg.getOneofEnum()).isEqualTo(TestEnum.ENUM_VALUE_THREE);
            assertThat(msg.hasOneofEnum()).isTrue();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("oneof - only one field can be set at a time")
        void oneof_onlyOneFieldSet(VersionContext ctx) {
            // Set int32, then string - should only have string
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder()
                .setOneofInt32(42)
                .setOneofString("overwrites int")
                .build();

            assertThat(msg.hasOneofString()).isTrue();
            assertThat(msg.getOneofString()).isEqualTo("overwrites int");
            assertThat(msg.hasOneofInt32()).isFalse();
            assertThat(msg.getOneofInt32()).isNull();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("oneof - unset fields return null or default")
        void oneof_unset_returnsNullOrDefault(VersionContext ctx) {
            AllFieldTypes msg = ctx.newAllFieldTypesBuilder().build();

            // Oneof fields when none is set - ALL return null (including enum)
            // Per CONTRACT-MATRIX.md: oneof fields are NULLABLE
            assertThat(msg.getOneofInt32()).isNull();
            assertThat(msg.hasOneofString()).isFalse();
            assertThat(msg.hasOneofMessage()).isFalse();
            // Enum in oneof also returns null when not active (consistent with other oneof fields)
            assertThat(msg.getOneofEnum()).isNull();
        }

        @ParameterizedTest(name = "Version {0}")
        @MethodSource("space.alnovis.protowrapper.golden.Proto3GoldenTest#allVersions")
        @DisplayName("oneof - round trip preserves value")
        void oneof_roundTrip_preservesValue(VersionContext ctx) throws Exception {
            AllFieldTypes original = ctx.newAllFieldTypesBuilder()
                .setOneofMessage(ctx.newNestedMessageBuilder().setId(99).setName("oneof").build())
                .build();

            byte[] bytes = original.toBytes();
            AllFieldTypes restored = ctx.parseAllFieldTypesFromBytes(bytes);

            assertThat(restored.getOneofMessage()).isNotNull();
            assertThat(restored.getOneofMessage().getId()).isEqualTo(99);
            assertThat(restored.getOneofMessage().getName()).isEqualTo("oneof");
            assertThat(restored.getOneofInt32()).isNull();
        }
    }

    @Nested
    @DisplayName("Cross-Version Consistency")
    class CrossVersionConsistency {

        @Test
        @DisplayName("Same values produce same results across versions")
        void sameValues_acrossVersions_producesSameResults() {
            VersionContext v1 = VersionContext.forVersion(1);
            VersionContext v2 = VersionContext.forVersion(2);

            AllFieldTypes msgV1 = v1.newAllFieldTypesBuilder()
                .setSingularInt32(42)
                .setOptionalInt32(100)
                .setOptionalString("test")
                .addRepeatedInt32(1)
                .addRepeatedInt32(2)
                .build();

            AllFieldTypes msgV2 = v2.newAllFieldTypesBuilder()
                .setSingularInt32(42)
                .setOptionalInt32(100)
                .setOptionalString("test")
                .addRepeatedInt32(1)
                .addRepeatedInt32(2)
                .build();

            // Both versions should return same values
            assertThat(msgV1.getSingularInt32()).isEqualTo(msgV2.getSingularInt32());
            assertThat(msgV1.getOptionalInt32()).isEqualTo(msgV2.getOptionalInt32());
            assertThat(msgV1.getOptionalString()).isEqualTo(msgV2.getOptionalString());
            assertThat(msgV1.getRepeatedInt32()).isEqualTo(msgV2.getRepeatedInt32());
        }
    }
}
