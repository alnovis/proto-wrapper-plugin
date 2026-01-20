package io.alnovis.protowrapper.golden;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Runtime verification tests for the Contract Matrix specification.
 *
 * <p>These tests verify that the generated wrapper API behaves according to the
 * contract matrix defined in {@code proto-wrapper-plugin/docs/CONTRACT-MATRIX.md}.
 * They provide E2E validation that the contract logic correctly drives code generation.</p>
 *
 * <h2>Contract Matrix Rules Tested</h2>
 * <ul>
 *   <li><b>hasMethodExists</b>: Verified via reflection - method exists or doesn't</li>
 *   <li><b>nullable</b>: Verified via runtime behavior - getter returns null or not</li>
 *   <li><b>defaultValue</b>: Verified via runtime behavior - correct default returned</li>
 *   <li><b>getterUsesHasCheck</b>: Implied by nullable + hasMethodExists combination</li>
 * </ul>
 *
 * <h2>Test Coverage</h2>
 * <ul>
 *   <li>Proto2 required fields: hasMethod=YES, nullable=NO</li>
 *   <li>Proto2 optional fields: hasMethod=YES, nullable=YES</li>
 *   <li>Proto3 implicit fields: hasMethod=NO (scalars), nullable=NO</li>
 *   <li>Proto3 explicit optional: hasMethod=YES, nullable=YES</li>
 *   <li>Proto3 message fields: hasMethod=YES, nullable=YES (exception)</li>
 *   <li>Oneof fields: hasMethod=YES, nullable=YES</li>
 *   <li>Repeated fields: hasMethod=NO, nullable=NO, default=[]</li>
 *   <li>Map fields: hasMethod=NO, nullable=NO, default={}</li>
 * </ul>
 *
 * @see <a href="../../../../../../docs/CONTRACT-MATRIX.md">CONTRACT-MATRIX.md</a>
 */
@DisplayName("Contract Matrix Runtime Verification")
class ContractMatrixRuntimeTest {

    // ============================================================================
    // PROTO3 HAS METHOD EXISTENCE (Reflection-based)
    // ============================================================================

    @Nested
    @DisplayName("Proto3 has*() Method Existence")
    class Proto3HasMethodExistence {

        private final Class<?> proto3WrapperClass;

        Proto3HasMethodExistence() throws ClassNotFoundException {
            proto3WrapperClass = Class.forName(
                    "io.alnovis.protowrapper.golden.proto3.wrapper.api.AllFieldTypes");
        }

        @ParameterizedTest(name = "Proto3 singular {0} should NOT have has*() method")
        @MethodSource("proto3ImplicitScalarFields")
        @DisplayName("Proto3 implicit scalars: NO has*() method per CONTRACT-MATRIX.md")
        void proto3ImplicitScalar_noHasMethod(String fieldName, String hasMethodName) {
            assertMethodNotExists(proto3WrapperClass, hasMethodName,
                    "Proto3 implicit scalar '" + fieldName + "' should NOT have has*() method " +
                    "(CONTRACT-MATRIX.md: Proto3 Implicit Presence)");
        }

        static Stream<Arguments> proto3ImplicitScalarFields() {
            return Stream.of(
                    Arguments.of("singular_int32", "hasSingularInt32"),
                    Arguments.of("singular_int64", "hasSingularInt64"),
                    Arguments.of("singular_bool", "hasSingularBool"),
                    Arguments.of("singular_string", "hasSingularString"),
                    Arguments.of("singular_float", "hasSingularFloat"),
                    Arguments.of("singular_double", "hasSingularDouble"),
                    Arguments.of("singular_enum", "hasSingularEnum")
            );
        }

        @Test
        @DisplayName("Proto3 singular message: HAS has*() method (exception per CONTRACT-MATRIX.md)")
        void proto3SingularMessage_hasHasMethod() {
            assertMethodExists(proto3WrapperClass, "hasSingularMessage",
                    "Proto3 message field should have has*() method " +
                    "(CONTRACT-MATRIX.md: Proto3 message fields always have presence)");
        }

        @ParameterizedTest(name = "Proto3 optional {0} should HAVE has*() method")
        @MethodSource("proto3ExplicitOptionalFields")
        @DisplayName("Proto3 explicit optional: HAS has*() method per CONTRACT-MATRIX.md")
        void proto3ExplicitOptional_hasHasMethod(String fieldName, String hasMethodName) {
            assertMethodExists(proto3WrapperClass, hasMethodName,
                    "Proto3 explicit optional '" + fieldName + "' should have has*() method " +
                    "(CONTRACT-MATRIX.md: Proto3 Explicit Presence)");
        }

        static Stream<Arguments> proto3ExplicitOptionalFields() {
            return Stream.of(
                    Arguments.of("optional_int32", "hasOptionalInt32"),
                    Arguments.of("optional_int64", "hasOptionalInt64"),
                    Arguments.of("optional_bool", "hasOptionalBool"),
                    Arguments.of("optional_string", "hasOptionalString"),
                    Arguments.of("optional_message", "hasOptionalMessage"),
                    Arguments.of("optional_enum", "hasOptionalEnum")
            );
        }

        @ParameterizedTest(name = "Proto3 oneof {0} should HAVE has*() method")
        @MethodSource("proto3OneofFields")
        @DisplayName("Proto3 oneof fields: HAS has*() method per CONTRACT-MATRIX.md")
        void proto3Oneof_hasHasMethod(String fieldName, String hasMethodName) {
            assertMethodExists(proto3WrapperClass, hasMethodName,
                    "Proto3 oneof field '" + fieldName + "' should have has*() method " +
                    "(CONTRACT-MATRIX.md: Oneof Fields)");
        }

        static Stream<Arguments> proto3OneofFields() {
            return Stream.of(
                    Arguments.of("oneof_int32", "hasOneofInt32"),
                    Arguments.of("oneof_string", "hasOneofString"),
                    Arguments.of("oneof_message", "hasOneofMessage"),
                    Arguments.of("oneof_enum", "hasOneofEnum")
            );
        }

        @ParameterizedTest(name = "Proto3 repeated {0} should NOT have has*() method")
        @MethodSource("proto3RepeatedFields")
        @DisplayName("Proto3 repeated fields: NO has*() method per CONTRACT-MATRIX.md")
        void proto3Repeated_noHasMethod(String fieldName, String hasMethodName) {
            assertMethodNotExists(proto3WrapperClass, hasMethodName,
                    "Proto3 repeated field '" + fieldName + "' should NOT have has*() method " +
                    "(CONTRACT-MATRIX.md: Repeated Fields)");
        }

        static Stream<Arguments> proto3RepeatedFields() {
            return Stream.of(
                    Arguments.of("repeated_int32", "hasRepeatedInt32"),
                    Arguments.of("repeated_string", "hasRepeatedString"),
                    Arguments.of("repeated_message", "hasRepeatedMessage")
            );
        }

        @ParameterizedTest(name = "Proto3 map {0} should NOT have has*() method")
        @MethodSource("proto3MapFields")
        @DisplayName("Proto3 map fields: NO has*() method per CONTRACT-MATRIX.md")
        void proto3Map_noHasMethod(String fieldName, String hasMethodName) {
            assertMethodNotExists(proto3WrapperClass, hasMethodName,
                    "Proto3 map field '" + fieldName + "' should NOT have has*() method " +
                    "(CONTRACT-MATRIX.md: Map Fields)");
        }

        static Stream<Arguments> proto3MapFields() {
            return Stream.of(
                    Arguments.of("string_to_int32", "hasStringToInt32"),
                    Arguments.of("string_to_message", "hasStringToMessage"),
                    Arguments.of("string_to_enum", "hasStringToEnum")
            );
        }
    }

    // ============================================================================
    // PROTO2 HAS METHOD EXISTENCE (Reflection-based)
    // ============================================================================

    @Nested
    @DisplayName("Proto2 has*() Method Existence")
    class Proto2HasMethodExistence {

        private final Class<?> proto2WrapperClass;

        Proto2HasMethodExistence() throws ClassNotFoundException {
            proto2WrapperClass = Class.forName(
                    "io.alnovis.protowrapper.golden.proto2.wrapper.api.AllFieldTypes");
        }

        @ParameterizedTest(name = "Proto2 required {0} should HAVE has*() method")
        @MethodSource("proto2RequiredFields")
        @DisplayName("Proto2 required fields: HAS has*() method per CONTRACT-MATRIX.md")
        void proto2Required_hasHasMethod(String fieldName, String hasMethodName) {
            assertMethodExists(proto2WrapperClass, hasMethodName,
                    "Proto2 required field '" + fieldName + "' should have has*() method " +
                    "(CONTRACT-MATRIX.md: Proto2 Singular Fields)");
        }

        static Stream<Arguments> proto2RequiredFields() {
            return Stream.of(
                    Arguments.of("required_int32", "hasRequiredInt32"),
                    Arguments.of("required_int64", "hasRequiredInt64"),
                    Arguments.of("required_bool", "hasRequiredBool"),
                    Arguments.of("required_string", "hasRequiredString"),
                    Arguments.of("required_message", "hasRequiredMessage"),
                    Arguments.of("required_enum", "hasRequiredEnum")
            );
        }

        @ParameterizedTest(name = "Proto2 optional {0} should HAVE has*() method")
        @MethodSource("proto2OptionalFields")
        @DisplayName("Proto2 optional fields: HAS has*() method per CONTRACT-MATRIX.md")
        void proto2Optional_hasHasMethod(String fieldName, String hasMethodName) {
            assertMethodExists(proto2WrapperClass, hasMethodName,
                    "Proto2 optional field '" + fieldName + "' should have has*() method " +
                    "(CONTRACT-MATRIX.md: Proto2 Singular Fields)");
        }

        static Stream<Arguments> proto2OptionalFields() {
            return Stream.of(
                    Arguments.of("optional_int32", "hasOptionalInt32"),
                    Arguments.of("optional_int64", "hasOptionalInt64"),
                    Arguments.of("optional_bool", "hasOptionalBool"),
                    Arguments.of("optional_string", "hasOptionalString"),
                    Arguments.of("optional_message", "hasOptionalMessage"),
                    Arguments.of("optional_enum", "hasOptionalEnum")
            );
        }

        @ParameterizedTest(name = "Proto2 repeated {0} should NOT have has*() method")
        @MethodSource("proto2RepeatedFields")
        @DisplayName("Proto2 repeated fields: NO has*() method per CONTRACT-MATRIX.md")
        void proto2Repeated_noHasMethod(String fieldName, String hasMethodName) {
            assertMethodNotExists(proto2WrapperClass, hasMethodName,
                    "Proto2 repeated field '" + fieldName + "' should NOT have has*() method " +
                    "(CONTRACT-MATRIX.md: Repeated Fields)");
        }

        static Stream<Arguments> proto2RepeatedFields() {
            return Stream.of(
                    Arguments.of("repeated_int32", "hasRepeatedInt32"),
                    Arguments.of("repeated_string", "hasRepeatedString"),
                    Arguments.of("repeated_message", "hasRepeatedMessage")
            );
        }
    }

    // ============================================================================
    // PROTO3 NULLABILITY BEHAVIOR
    // ============================================================================

    @Nested
    @DisplayName("Proto3 Nullability Behavior")
    class Proto3NullabilityBehavior {

        @Test
        @DisplayName("Proto3 implicit scalars: NOT nullable (return default values)")
        void proto3ImplicitScalars_notNullable() {
            var ctx = io.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext.forVersionId("v1");
            var msg = ctx.newAllFieldTypesBuilder().build();

            // Proto3 implicit scalars return type defaults, not null
            assertThat(msg.getSingularInt32())
                    .as("Proto3 implicit int32 returns 0, not null (CONTRACT-MATRIX.md)")
                    .isEqualTo(0);
            assertThat(msg.getSingularString())
                    .as("Proto3 implicit string returns \"\", not null (CONTRACT-MATRIX.md)")
                    .isEmpty();
            assertThat(msg.isSingularBool())
                    .as("Proto3 implicit bool returns false, not null (CONTRACT-MATRIX.md)")
                    .isFalse();
        }

        @Test
        @DisplayName("Proto3 singular message: NULLABLE (returns null when unset)")
        void proto3SingularMessage_nullable() {
            var ctx = io.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext.forVersionId("v1");
            var msg = ctx.newAllFieldTypesBuilder().build();

            assertThat(msg.getSingularMessage())
                    .as("Proto3 message field returns null when unset (CONTRACT-MATRIX.md: exception)")
                    .isNull();
            assertThat(msg.hasSingularMessage())
                    .as("Proto3 message field has*() returns false when unset")
                    .isFalse();
        }

        @Test
        @DisplayName("Proto3 explicit optional scalars: NULLABLE (return null when unset)")
        void proto3ExplicitOptionalScalars_nullable() {
            var ctx = io.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext.forVersionId("v1");
            var msg = ctx.newAllFieldTypesBuilder().build();

            assertThat(msg.getOptionalInt32())
                    .as("Proto3 explicit optional int32 returns null when unset (CONTRACT-MATRIX.md)")
                    .isNull();
            assertThat(msg.hasOptionalInt32())
                    .as("Proto3 explicit optional has*() returns false when unset")
                    .isFalse();
        }

        @Test
        @DisplayName("Proto3 explicit optional: can distinguish 0 from unset")
        void proto3ExplicitOptional_distinguishZeroFromUnset() {
            var ctx = io.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext.forVersionId("v1");

            var msgWithZero = ctx.newAllFieldTypesBuilder()
                    .setOptionalInt32(0)
                    .build();
            var msgUnset = ctx.newAllFieldTypesBuilder().build();

            assertThat(msgWithZero.getOptionalInt32())
                    .as("Explicitly set 0 returns 0")
                    .isEqualTo(0);
            assertThat(msgWithZero.hasOptionalInt32())
                    .as("Explicitly set 0 has*() returns true")
                    .isTrue();

            assertThat(msgUnset.getOptionalInt32())
                    .as("Unset returns null")
                    .isNull();
            assertThat(msgUnset.hasOptionalInt32())
                    .as("Unset has*() returns false")
                    .isFalse();
        }

        @Test
        @DisplayName("Proto3 oneof fields: NULLABLE (return null when not active)")
        void proto3Oneof_nullable() {
            var ctx = io.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext.forVersionId("v1");
            var msg = ctx.newAllFieldTypesBuilder().build();

            assertThat(msg.getOneofInt32())
                    .as("Proto3 oneof int32 returns null when not active (CONTRACT-MATRIX.md)")
                    .isNull();
            assertThat(msg.getOneofString())
                    .as("Proto3 oneof string returns null when not active")
                    .isNull();
            assertThat(msg.getOneofMessage())
                    .as("Proto3 oneof message returns null when not active")
                    .isNull();
        }

        @Test
        @DisplayName("Proto3 repeated fields: NOT nullable (return empty list)")
        void proto3Repeated_notNullable() {
            var ctx = io.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext.forVersionId("v1");
            var msg = ctx.newAllFieldTypesBuilder().build();

            assertThat(msg.getRepeatedInt32())
                    .as("Proto3 repeated int32 returns empty list, not null (CONTRACT-MATRIX.md)")
                    .isNotNull()
                    .isEmpty();
            assertThat(msg.getRepeatedString())
                    .as("Proto3 repeated string returns empty list, not null")
                    .isNotNull()
                    .isEmpty();
            assertThat(msg.getRepeatedMessage())
                    .as("Proto3 repeated message returns empty list, not null")
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        @DisplayName("Proto3 map fields: NOT nullable (return empty map)")
        void proto3Map_notNullable() {
            var ctx = io.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext.forVersionId("v1");
            var msg = ctx.newAllFieldTypesBuilder().build();

            assertThat(msg.getStringToInt32Map())
                    .as("Proto3 map returns empty map, not null (CONTRACT-MATRIX.md)")
                    .isNotNull()
                    .isEmpty();
            assertThat(msg.getStringToMessageMap())
                    .as("Proto3 map with message value returns empty map, not null")
                    .isNotNull()
                    .isEmpty();
        }
    }

    // ============================================================================
    // PROTO2 NULLABILITY BEHAVIOR
    // ============================================================================

    @Nested
    @DisplayName("Proto2 Nullability Behavior")
    class Proto2NullabilityBehavior {

        @Test
        @DisplayName("Proto2 required scalars: NOT nullable (return values)")
        void proto2RequiredScalars_notNullable() {
            var ctx = io.alnovis.protowrapper.golden.proto2.wrapper.api.VersionContext.forVersionId("v1");
            var msg = buildProto2WithRequiredFields(ctx).build();

            // Required fields should return the set value
            assertThat(msg.getRequiredInt32())
                    .as("Proto2 required int32 returns set value (CONTRACT-MATRIX.md)")
                    .isEqualTo(1);
            assertThat(msg.getRequiredString())
                    .as("Proto2 required string returns set value")
                    .isEqualTo("required");
            assertThat(msg.isRequiredBool())
                    .as("Proto2 required bool returns set value")
                    .isTrue();
        }

        @Test
        @DisplayName("Proto2 optional scalars: NULLABLE (return null when unset)")
        void proto2OptionalScalars_nullable() {
            var ctx = io.alnovis.protowrapper.golden.proto2.wrapper.api.VersionContext.forVersionId("v1");
            var msg = buildProto2WithRequiredFields(ctx).build();

            assertThat(msg.getOptionalInt32())
                    .as("Proto2 optional int32 returns null when unset (CONTRACT-MATRIX.md)")
                    .isNull();
            assertThat(msg.hasOptionalInt32())
                    .as("Proto2 optional has*() returns false when unset")
                    .isFalse();
        }

        @Test
        @DisplayName("Proto2 optional message: NULLABLE (returns null when unset)")
        void proto2OptionalMessage_nullable() {
            var ctx = io.alnovis.protowrapper.golden.proto2.wrapper.api.VersionContext.forVersionId("v1");
            var msg = buildProto2WithRequiredFields(ctx).build();

            assertThat(msg.getOptionalMessage())
                    .as("Proto2 optional message returns null when unset (CONTRACT-MATRIX.md)")
                    .isNull();
            assertThat(msg.hasOptionalMessage())
                    .as("Proto2 optional message has*() returns false when unset")
                    .isFalse();
        }

        @Test
        @DisplayName("Proto2 repeated fields: NOT nullable (return empty list)")
        void proto2Repeated_notNullable() {
            var ctx = io.alnovis.protowrapper.golden.proto2.wrapper.api.VersionContext.forVersionId("v1");
            var msg = buildProto2WithRequiredFields(ctx).build();

            assertThat(msg.getRepeatedInt32())
                    .as("Proto2 repeated int32 returns empty list, not null (CONTRACT-MATRIX.md)")
                    .isNotNull()
                    .isEmpty();
            assertThat(msg.getRepeatedString())
                    .as("Proto2 repeated string returns empty list, not null")
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        @DisplayName("Proto2 map fields: NOT nullable (return empty map)")
        void proto2Map_notNullable() {
            var ctx = io.alnovis.protowrapper.golden.proto2.wrapper.api.VersionContext.forVersionId("v1");
            var msg = buildProto2WithRequiredFields(ctx).build();

            assertThat(msg.getStringToInt32Map())
                    .as("Proto2 map returns empty map, not null (CONTRACT-MATRIX.md)")
                    .isNotNull()
                    .isEmpty();
        }
    }

    // ============================================================================
    // MAP FIELD BEHAVIOR
    // ============================================================================

    @Nested
    @DisplayName("Map Field Behavior")
    class MapFieldBehavior {

        @Test
        @DisplayName("Proto3 map: put and get works correctly")
        void proto3Map_putAndGet() {
            var ctx = io.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext.forVersionId("v1");
            var msg = ctx.newAllFieldTypesBuilder()
                    .putStringToInt32("key1", 100)
                    .putStringToInt32("key2", 200)
                    .build();

            Map<String, Integer> map = msg.getStringToInt32Map();
            assertThat(map)
                    .hasSize(2)
                    .containsEntry("key1", 100)
                    .containsEntry("key2", 200);
        }

        @Test
        @DisplayName("Proto3 map with message value: put and get works correctly")
        void proto3MapMessage_putAndGet() {
            var ctx = io.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext.forVersionId("v1");
            var nested1 = ctx.newNestedMessageBuilder().setId(1).setName("first").build();
            var nested2 = ctx.newNestedMessageBuilder().setId(2).setName("second").build();

            var msg = ctx.newAllFieldTypesBuilder()
                    .putStringToMessage("a", nested1)
                    .putStringToMessage("b", nested2)
                    .build();

            var map = msg.getStringToMessageMap();
            assertThat(map).hasSize(2);
            assertThat(map.get("a").getId()).isEqualTo(1);
            assertThat(map.get("b").getName()).isEqualTo("second");
        }

        @Test
        @DisplayName("Proto3 map: round trip preserves values")
        void proto3Map_roundTrip() throws Exception {
            var ctx = io.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext.forVersionId("v1");
            var original = ctx.newAllFieldTypesBuilder()
                    .putStringToInt32("alpha", 1)
                    .putStringToInt32("beta", 2)
                    .putInt32ToString(100, "hundred")
                    .build();

            byte[] bytes = original.toBytes();
            var restored = ctx.parseAllFieldTypesFromBytes(bytes);

            assertThat(restored.getStringToInt32Map())
                    .containsEntry("alpha", 1)
                    .containsEntry("beta", 2);
            assertThat(restored.getInt32ToStringMap())
                    .containsEntry(100, "hundred");
        }

        @Test
        @DisplayName("Proto2 map: put and get works correctly")
        void proto2Map_putAndGet() {
            var ctx = io.alnovis.protowrapper.golden.proto2.wrapper.api.VersionContext.forVersionId("v1");
            var msg = buildProto2WithRequiredFields(ctx)
                    .putStringToInt32("key1", 100)
                    .putStringToInt32("key2", 200)
                    .build();

            Map<String, Integer> map = msg.getStringToInt32Map();
            assertThat(map)
                    .hasSize(2)
                    .containsEntry("key1", 100)
                    .containsEntry("key2", 200);
        }
    }

    // ============================================================================
    // CROSS-VERSION CONTRACT CONSISTENCY
    // ============================================================================

    @Nested
    @DisplayName("Cross-Version Contract Consistency")
    class CrossVersionContractConsistency {

        @Test
        @DisplayName("Proto3: Same contract behavior across v1 and v2")
        void proto3_sameContractAcrossVersions() {
            var v1 = io.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext.forVersionId("v1");
            var v2 = io.alnovis.protowrapper.golden.proto3.wrapper.api.VersionContext.forVersionId("v2");

            var msgV1 = v1.newAllFieldTypesBuilder().build();
            var msgV2 = v2.newAllFieldTypesBuilder().build();

            // Both versions should have same nullability behavior
            assertThat(msgV1.getSingularMessage()).isNull();
            assertThat(msgV2.getSingularMessage()).isNull();

            assertThat(msgV1.getOptionalInt32()).isNull();
            assertThat(msgV2.getOptionalInt32()).isNull();

            assertThat(msgV1.getRepeatedInt32()).isEmpty();
            assertThat(msgV2.getRepeatedInt32()).isEmpty();

            assertThat(msgV1.getStringToInt32Map()).isEmpty();
            assertThat(msgV2.getStringToInt32Map()).isEmpty();
        }

        @Test
        @DisplayName("Proto2: Same contract behavior across v1 and v2")
        void proto2_sameContractAcrossVersions() {
            var v1 = io.alnovis.protowrapper.golden.proto2.wrapper.api.VersionContext.forVersionId("v1");
            var v2 = io.alnovis.protowrapper.golden.proto2.wrapper.api.VersionContext.forVersionId("v2");

            var msgV1 = buildProto2WithRequiredFields(v1).build();
            var msgV2 = buildProto2WithRequiredFields(v2).build();

            // Both versions should have same nullability behavior
            assertThat(msgV1.getOptionalInt32()).isNull();
            assertThat(msgV2.getOptionalInt32()).isNull();

            assertThat(msgV1.getOptionalMessage()).isNull();
            assertThat(msgV2.getOptionalMessage()).isNull();

            assertThat(msgV1.getRepeatedInt32()).isEmpty();
            assertThat(msgV2.getRepeatedInt32()).isEmpty();

            assertThat(msgV1.getStringToInt32Map()).isEmpty();
            assertThat(msgV2.getStringToInt32Map()).isEmpty();
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private void assertMethodExists(Class<?> clazz, String methodName, String message) {
        try {
            clazz.getMethod(methodName);
            // Method exists - test passes
        } catch (NoSuchMethodException e) {
            fail(message + " - Method '" + methodName + "' not found in " + clazz.getSimpleName());
        }
    }

    private void assertMethodNotExists(Class<?> clazz, String methodName, String message) {
        try {
            Method method = clazz.getMethod(methodName);
            fail(message + " - Method '" + methodName + "' unexpectedly exists in " + clazz.getSimpleName());
        } catch (NoSuchMethodException e) {
            // Method doesn't exist - test passes
        }
    }

    private io.alnovis.protowrapper.golden.proto2.wrapper.api.AllFieldTypes.Builder
            buildProto2WithRequiredFields(
                    io.alnovis.protowrapper.golden.proto2.wrapper.api.VersionContext ctx) {
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
                .setRequiredEnum(io.alnovis.protowrapper.golden.proto2.wrapper.api.TestEnum.ONE);
    }
}
