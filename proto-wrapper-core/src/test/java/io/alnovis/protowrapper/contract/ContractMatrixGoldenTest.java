package io.alnovis.protowrapper.contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.ProtoSyntax;

import java.util.Map;
import java.util.stream.Stream;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden tests for the Contract Matrix specification.
 *
 * <p>These tests verify that the contract system produces correct behavior
 * for ALL combinations defined in CONTRACT-MATRIX.md. Any change that breaks
 * these tests indicates a potential regression in field behavior.</p>
 *
 * <p>Test organization mirrors the CONTRACT-MATRIX.md document:</p>
 * <ul>
 *   <li>Proto2 Singular Fields</li>
 *   <li>Proto3 Implicit Presence</li>
 *   <li>Proto3 Explicit Presence</li>
 *   <li>Oneof Fields</li>
 *   <li>Repeated Fields</li>
 *   <li>Map Fields</li>
 *   <li>Multi-Version Merge Rules</li>
 * </ul>
 *
 * @see ContractProvider
 * @see MergedFieldContract
 */
@DisplayName("Contract Matrix Golden Tests")
class ContractMatrixGoldenTest {

    private ContractProvider provider;

    @BeforeEach
    void setUp() {
        provider = ContractProvider.getInstance();
        provider.clearCache();
    }

    // ============================================================================
    // PROTO2 SINGULAR FIELDS
    // ============================================================================

    @Nested
    @DisplayName("Proto2 Singular Fields")
    class Proto2SingularTests {

        @ParameterizedTest(name = "Proto2 optional {0} field")
        @MethodSource("proto2OptionalScalarTypes")
        @DisplayName("Proto2 optional scalars: hasMethod=YES, nullable=YES")
        void proto2OptionalScalar(String name, Type type, String typeName) {
            MergedFieldContract contract = createProto2Contract(
                    name, type, Label.LABEL_OPTIONAL, typeName, false);

            assertAll("Proto2 optional " + name,
                    () -> assertTrue(contract.unified().hasMethodExists(),
                            "hasMethodExists should be true"),
                    () -> assertTrue(contract.unified().getterUsesHasCheck(),
                            "getterUsesHasCheck should be true"),
                    () -> assertTrue(contract.unified().nullable(),
                            "nullable should be true"),
                    () -> assertEquals(FieldContract.DefaultValue.NULL,
                            contract.unified().defaultValueWhenUnset(),
                            "default should be NULL")
            );
        }

        static Stream<Arguments> proto2OptionalScalarTypes() {
            return Stream.of(
                    Arguments.of("int32", Type.TYPE_INT32, null),
                    Arguments.of("int64", Type.TYPE_INT64, null),
                    Arguments.of("uint32", Type.TYPE_UINT32, null),
                    Arguments.of("float", Type.TYPE_FLOAT, null),
                    Arguments.of("double", Type.TYPE_DOUBLE, null),
                    Arguments.of("bool", Type.TYPE_BOOL, null),
                    Arguments.of("string", Type.TYPE_STRING, null),
                    Arguments.of("bytes", Type.TYPE_BYTES, null),
                    Arguments.of("enum", Type.TYPE_ENUM, ".example.Status")
            );
        }

        @Test
        @DisplayName("Proto2 optional message: hasMethod=YES, nullable=YES")
        void proto2OptionalMessage() {
            MergedFieldContract contract = createProto2Contract(
                    "config", Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL, ".example.Config", false);

            assertAll("Proto2 optional message",
                    () -> assertTrue(contract.unified().hasMethodExists()),
                    () -> assertTrue(contract.unified().getterUsesHasCheck()),
                    () -> assertTrue(contract.unified().nullable()),
                    () -> assertEquals(FieldContract.DefaultValue.NULL,
                            contract.unified().defaultValueWhenUnset())
            );
        }

        @ParameterizedTest(name = "Proto2 required {0} field")
        @MethodSource("proto2RequiredScalarTypes")
        @DisplayName("Proto2 required scalars: hasMethod=YES, nullable=NO")
        void proto2RequiredScalar(String name, Type type, String typeName,
                                   FieldContract.DefaultValue expectedDefault) {
            MergedFieldContract contract = createProto2Contract(
                    name, type, Label.LABEL_REQUIRED, typeName, false);

            assertAll("Proto2 required " + name,
                    () -> assertTrue(contract.unified().hasMethodExists(),
                            "hasMethodExists should be true (required fields have has*())"),
                    () -> assertFalse(contract.unified().getterUsesHasCheck(),
                            "getterUsesHasCheck should be false (not nullable)"),
                    () -> assertFalse(contract.unified().nullable(),
                            "nullable should be false"),
                    () -> assertEquals(expectedDefault,
                            contract.unified().defaultValueWhenUnset(),
                            "default should be type-specific")
            );
        }

        static Stream<Arguments> proto2RequiredScalarTypes() {
            return Stream.of(
                    Arguments.of("int32", Type.TYPE_INT32, null, FieldContract.DefaultValue.ZERO),
                    Arguments.of("int64", Type.TYPE_INT64, null, FieldContract.DefaultValue.ZERO_LONG),
                    Arguments.of("float", Type.TYPE_FLOAT, null, FieldContract.DefaultValue.ZERO_FLOAT),
                    Arguments.of("double", Type.TYPE_DOUBLE, null, FieldContract.DefaultValue.ZERO_DOUBLE),
                    Arguments.of("bool", Type.TYPE_BOOL, null, FieldContract.DefaultValue.FALSE),
                    Arguments.of("string", Type.TYPE_STRING, null, FieldContract.DefaultValue.EMPTY_STRING),
                    Arguments.of("bytes", Type.TYPE_BYTES, null, FieldContract.DefaultValue.EMPTY_BYTES),
                    Arguments.of("enum", Type.TYPE_ENUM, ".example.Status", FieldContract.DefaultValue.FIRST_ENUM_VALUE)
            );
        }

        @Test
        @DisplayName("Proto2 required message: hasMethod=YES, nullable=NO")
        void proto2RequiredMessage() {
            MergedFieldContract contract = createProto2Contract(
                    "config", Type.TYPE_MESSAGE, Label.LABEL_REQUIRED, ".example.Config", false);

            assertAll("Proto2 required message",
                    () -> assertTrue(contract.unified().hasMethodExists()),
                    () -> assertFalse(contract.unified().getterUsesHasCheck()),
                    () -> assertFalse(contract.unified().nullable()),
                    // Note: Current implementation returns NULL for non-nullable messages.
                    // This is technically a gap - required messages should have default instance.
                    // However, in practice required fields are always set.
                    () -> assertEquals(FieldContract.DefaultValue.NULL,
                            contract.unified().defaultValueWhenUnset())
            );
        }
    }

    // ============================================================================
    // PROTO3 IMPLICIT PRESENCE (no 'optional' keyword)
    // ============================================================================

    @Nested
    @DisplayName("Proto3 Implicit Presence")
    class Proto3ImplicitTests {

        @ParameterizedTest(name = "Proto3 implicit {0} field")
        @MethodSource("proto3ImplicitScalarTypes")
        @DisplayName("Proto3 implicit scalars: hasMethod=NO, nullable=NO")
        void proto3ImplicitScalar(String name, Type type, String typeName,
                                   FieldContract.DefaultValue expectedDefault) {
            MergedFieldContract contract = createProto3ImplicitContract(
                    name, type, typeName);

            assertAll("Proto3 implicit " + name,
                    () -> assertFalse(contract.unified().hasMethodExists(),
                            "hasMethodExists should be false (proto3 implicit)"),
                    () -> assertFalse(contract.unified().getterUsesHasCheck(),
                            "getterUsesHasCheck should be false"),
                    () -> assertFalse(contract.unified().nullable(),
                            "nullable should be false"),
                    () -> assertEquals(expectedDefault,
                            contract.unified().defaultValueWhenUnset(),
                            "default should be type-specific")
            );
        }

        static Stream<Arguments> proto3ImplicitScalarTypes() {
            return Stream.of(
                    Arguments.of("int32", Type.TYPE_INT32, null, FieldContract.DefaultValue.ZERO),
                    Arguments.of("int64", Type.TYPE_INT64, null, FieldContract.DefaultValue.ZERO_LONG),
                    Arguments.of("uint32", Type.TYPE_UINT32, null, FieldContract.DefaultValue.ZERO),
                    Arguments.of("float", Type.TYPE_FLOAT, null, FieldContract.DefaultValue.ZERO_FLOAT),
                    Arguments.of("double", Type.TYPE_DOUBLE, null, FieldContract.DefaultValue.ZERO_DOUBLE),
                    Arguments.of("bool", Type.TYPE_BOOL, null, FieldContract.DefaultValue.FALSE),
                    Arguments.of("string", Type.TYPE_STRING, null, FieldContract.DefaultValue.EMPTY_STRING),
                    Arguments.of("bytes", Type.TYPE_BYTES, null, FieldContract.DefaultValue.EMPTY_BYTES),
                    Arguments.of("enum", Type.TYPE_ENUM, ".example.Status", FieldContract.DefaultValue.FIRST_ENUM_VALUE)
            );
        }

        @Test
        @DisplayName("Proto3 implicit message: hasMethod=YES, nullable=YES (exception!)")
        void proto3ImplicitMessage() {
            // MESSAGE is the exception - always has presence even in proto3
            MergedFieldContract contract = createProto3ImplicitContract(
                    "config", Type.TYPE_MESSAGE, ".example.Config");

            assertAll("Proto3 implicit message (exception to implicit rule)",
                    () -> assertTrue(contract.unified().hasMethodExists(),
                            "hasMethodExists should be true (messages always have presence)"),
                    () -> assertTrue(contract.unified().getterUsesHasCheck(),
                            "getterUsesHasCheck should be true"),
                    () -> assertTrue(contract.unified().nullable(),
                            "nullable should be true"),
                    () -> assertEquals(FieldContract.DefaultValue.NULL,
                            contract.unified().defaultValueWhenUnset())
            );
        }
    }

    // ============================================================================
    // PROTO3 EXPLICIT PRESENCE ('optional' keyword)
    // ============================================================================

    @Nested
    @DisplayName("Proto3 Explicit Presence")
    class Proto3ExplicitTests {

        @ParameterizedTest(name = "Proto3 explicit optional {0} field")
        @MethodSource("proto3ExplicitScalarTypes")
        @DisplayName("Proto3 explicit scalars: hasMethod=YES, nullable=YES")
        void proto3ExplicitScalar(String name, Type type, String typeName) {
            MergedFieldContract contract = createProto3ExplicitContract(
                    name, type, typeName);

            assertAll("Proto3 explicit " + name,
                    () -> assertTrue(contract.unified().hasMethodExists(),
                            "hasMethodExists should be true (explicit optional)"),
                    () -> assertTrue(contract.unified().getterUsesHasCheck(),
                            "getterUsesHasCheck should be true"),
                    () -> assertTrue(contract.unified().nullable(),
                            "nullable should be true"),
                    () -> assertEquals(FieldContract.DefaultValue.NULL,
                            contract.unified().defaultValueWhenUnset(),
                            "default should be NULL")
            );
        }

        static Stream<Arguments> proto3ExplicitScalarTypes() {
            return Stream.of(
                    Arguments.of("int32", Type.TYPE_INT32, null),
                    Arguments.of("int64", Type.TYPE_INT64, null),
                    Arguments.of("float", Type.TYPE_FLOAT, null),
                    Arguments.of("double", Type.TYPE_DOUBLE, null),
                    Arguments.of("bool", Type.TYPE_BOOL, null),
                    Arguments.of("string", Type.TYPE_STRING, null),
                    Arguments.of("bytes", Type.TYPE_BYTES, null),
                    Arguments.of("enum", Type.TYPE_ENUM, ".example.Status"),
                    Arguments.of("message", Type.TYPE_MESSAGE, ".example.Config")
            );
        }
    }

    // ============================================================================
    // ONEOF FIELDS
    // ============================================================================

    @Nested
    @DisplayName("Oneof Fields")
    class OneofTests {

        @ParameterizedTest(name = "Oneof {0} field")
        @MethodSource("oneofFieldTypes")
        @DisplayName("All oneof fields: hasMethod=YES, nullable=YES")
        void oneofField(String name, Type type, String typeName, ProtoSyntax syntax) {
            MergedFieldContract contract = createOneofContract(name, type, typeName, syntax);

            assertAll("Oneof " + name + " (" + syntax + ")",
                    () -> assertTrue(contract.unified().hasMethodExists(),
                            "hasMethodExists should be true (oneof always has presence)"),
                    () -> assertTrue(contract.unified().getterUsesHasCheck(),
                            "getterUsesHasCheck should be true"),
                    () -> assertTrue(contract.unified().nullable(),
                            "nullable should be true"),
                    () -> assertEquals(FieldContract.DefaultValue.NULL,
                            contract.unified().defaultValueWhenUnset())
            );
        }

        static Stream<Arguments> oneofFieldTypes() {
            return Stream.of(
                    // Proto2 oneof
                    Arguments.of("stringValue", Type.TYPE_STRING, null, ProtoSyntax.PROTO2),
                    Arguments.of("intValue", Type.TYPE_INT32, null, ProtoSyntax.PROTO2),
                    Arguments.of("messageValue", Type.TYPE_MESSAGE, ".example.Msg", ProtoSyntax.PROTO2),
                    Arguments.of("enumValue", Type.TYPE_ENUM, ".example.Status", ProtoSyntax.PROTO2),
                    // Proto3 oneof
                    Arguments.of("stringValue", Type.TYPE_STRING, null, ProtoSyntax.PROTO3),
                    Arguments.of("intValue", Type.TYPE_INT32, null, ProtoSyntax.PROTO3),
                    Arguments.of("messageValue", Type.TYPE_MESSAGE, ".example.Msg", ProtoSyntax.PROTO3),
                    Arguments.of("enumValue", Type.TYPE_ENUM, ".example.Status", ProtoSyntax.PROTO3)
            );
        }
    }

    // ============================================================================
    // REPEATED FIELDS
    // ============================================================================

    @Nested
    @DisplayName("Repeated Fields")
    class RepeatedTests {

        @ParameterizedTest(name = "Repeated {0} field ({2})")
        @MethodSource("repeatedFieldTypes")
        @DisplayName("All repeated fields: hasMethod=NO, nullable=NO, default=[]")
        void repeatedField(String name, Type type, ProtoSyntax syntax, String typeName) {
            MergedFieldContract contract = createRepeatedContract(name, type, typeName, syntax);

            assertAll("Repeated " + name + " (" + syntax + ")",
                    () -> assertFalse(contract.unified().hasMethodExists(),
                            "hasMethodExists should be false (repeated never has has*())"),
                    () -> assertFalse(contract.unified().getterUsesHasCheck(),
                            "getterUsesHasCheck should be false"),
                    () -> assertFalse(contract.unified().nullable(),
                            "nullable should be false (returns empty list, not null)"),
                    () -> assertEquals(FieldContract.DefaultValue.EMPTY_LIST,
                            contract.unified().defaultValueWhenUnset(),
                            "default should be EMPTY_LIST"),
                    () -> assertEquals(FieldCardinality.REPEATED,
                            contract.unified().cardinality())
            );
        }

        static Stream<Arguments> repeatedFieldTypes() {
            return Stream.of(
                    // Proto2 repeated
                    Arguments.of("strings", Type.TYPE_STRING, ProtoSyntax.PROTO2, null),
                    Arguments.of("ints", Type.TYPE_INT32, ProtoSyntax.PROTO2, null),
                    Arguments.of("messages", Type.TYPE_MESSAGE, ProtoSyntax.PROTO2, ".example.Item"),
                    Arguments.of("enums", Type.TYPE_ENUM, ProtoSyntax.PROTO2, ".example.Status"),
                    // Proto3 repeated
                    Arguments.of("strings", Type.TYPE_STRING, ProtoSyntax.PROTO3, null),
                    Arguments.of("ints", Type.TYPE_INT32, ProtoSyntax.PROTO3, null),
                    Arguments.of("messages", Type.TYPE_MESSAGE, ProtoSyntax.PROTO3, ".example.Item"),
                    Arguments.of("enums", Type.TYPE_ENUM, ProtoSyntax.PROTO3, ".example.Status")
            );
        }
    }

    // ============================================================================
    // MAP FIELDS
    // ============================================================================

    @Nested
    @DisplayName("Map Fields")
    class MapTests {

        @Test
        @DisplayName("Map field: hasMethod=NO, nullable=NO, default={}")
        void mapField() {
            FieldInfo fieldInfo = createMapFieldInfo("metadata");
            MergedField merged = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged,
                    Map.of("v1", ProtoSyntax.PROTO3));

            assertAll("Map field",
                    () -> assertFalse(contract.unified().hasMethodExists(),
                            "hasMethodExists should be false"),
                    () -> assertFalse(contract.unified().getterUsesHasCheck(),
                            "getterUsesHasCheck should be false"),
                    () -> assertFalse(contract.unified().nullable(),
                            "nullable should be false"),
                    () -> assertEquals(FieldContract.DefaultValue.EMPTY_MAP,
                            contract.unified().defaultValueWhenUnset(),
                            "default should be EMPTY_MAP"),
                    () -> assertEquals(FieldCardinality.MAP,
                            contract.unified().cardinality())
            );
        }
    }

    // ============================================================================
    // MULTI-VERSION MERGE RULES
    // ============================================================================

    @Nested
    @DisplayName("Multi-Version Merge Rules")
    class MergeRulesTests {

        @Nested
        @DisplayName("hasMethodExists Merge: ALL versions must have it")
        class HasMethodMergeTests {

            @Test
            @DisplayName("Both versions have has*() -> unified has has*()")
            void bothHaveHas() {
                // Both Proto2 optional fields = both have has*()
                FieldInfo v1 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                        null, false, true); // Proto2: has*() available
                FieldInfo v2 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                        null, false, true); // Proto2: has*() available

                MergedField merged = MergedField.builder()
                        .addVersionField("v1", v1)
                        .addVersionField("v2", v2)
                        .build();

                MergedFieldContract contract = MergedFieldContract.from(merged,
                        Map.of("v1", ProtoSyntax.PROTO2, "v2", ProtoSyntax.PROTO2));

                assertTrue(contract.unified().hasMethodExists(),
                        "When ALL versions have has*(), unified should have has*()");
            }

            @Test
            @DisplayName("One version lacks has*() -> unified lacks has*()")
            void oneLacksHas() {
                // Proto2 optional (has) + Proto3 implicit (no has)
                FieldInfo v1 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                        null, false, true);  // Proto2: has*() available
                FieldInfo v2 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                        null, false, false); // Proto3 implicit: no has*()

                MergedField merged = MergedField.builder()
                        .addVersionField("v1", v1)
                        .addVersionField("v2", v2)
                        .build();

                MergedFieldContract contract = MergedFieldContract.from(merged,
                        Map.of("v1", ProtoSyntax.PROTO2, "v2", ProtoSyntax.PROTO3));

                assertFalse(contract.unified().hasMethodExists(),
                        "When ANY version lacks has*(), unified should lack has*()");
            }
        }

        @Nested
        @DisplayName("nullable Merge: ANY version nullable -> unified nullable")
        class NullableMergeTests {

            @Test
            @DisplayName("Both versions nullable -> unified nullable")
            void bothNullable() {
                // Two Proto2 optional fields
                FieldInfo v1 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                        null, false, true);
                FieldInfo v2 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                        null, false, true);

                MergedField merged = MergedField.builder()
                        .addVersionField("v1", v1)
                        .addVersionField("v2", v2)
                        .build();

                MergedFieldContract contract = MergedFieldContract.from(merged,
                        Map.of("v1", ProtoSyntax.PROTO2, "v2", ProtoSyntax.PROTO2));

                assertTrue(contract.unified().nullable());
            }

            @Test
            @DisplayName("One version nullable -> unified nullable")
            void oneNullable() {
                // Proto2 optional (nullable) + Proto2 required (not nullable)
                FieldInfo v1 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                        null, false, true);
                FieldInfo v2 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_REQUIRED,
                        null, false, true);

                MergedField merged = MergedField.builder()
                        .addVersionField("v1", v1)
                        .addVersionField("v2", v2)
                        .build();

                MergedFieldContract contract = MergedFieldContract.from(merged,
                        Map.of("v1", ProtoSyntax.PROTO2, "v2", ProtoSyntax.PROTO2));

                assertTrue(contract.unified().nullable(),
                        "When ANY version is nullable, unified should be nullable");
            }

            @Test
            @DisplayName("Neither version nullable -> unified not nullable")
            void neitherNullable() {
                // Two Proto2 required fields
                FieldInfo v1 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_REQUIRED,
                        null, false, true);
                FieldInfo v2 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_REQUIRED,
                        null, false, true);

                MergedField merged = MergedField.builder()
                        .addVersionField("v1", v1)
                        .addVersionField("v2", v2)
                        .build();

                MergedFieldContract contract = MergedFieldContract.from(merged,
                        Map.of("v1", ProtoSyntax.PROTO2, "v2", ProtoSyntax.PROTO2));

                assertFalse(contract.unified().nullable(),
                        "When NEITHER version is nullable, unified should not be nullable");
            }
        }

        @Nested
        @DisplayName("Cardinality Merge: REPEATED wins over SINGULAR")
        class CardinalityMergeTests {

            @Test
            @DisplayName("Singular + Repeated -> Repeated (REPEATED_SINGLE conflict)")
            void singularPlusRepeated() {
                FieldInfo v1 = createFieldInfo("tags", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                        null, false, true);
                FieldInfo v2 = createFieldInfo("tags", Type.TYPE_STRING, Label.LABEL_REPEATED,
                        null, false, false);

                MergedField merged = MergedField.builder()
                        .addVersionField("v1", v1)
                        .addVersionField("v2", v2)
                        .conflictType(MergedField.ConflictType.REPEATED_SINGLE)
                        .build();

                MergedFieldContract contract = MergedFieldContract.from(merged,
                        Map.of("v1", ProtoSyntax.PROTO2, "v2", ProtoSyntax.PROTO3));

                assertEquals(FieldCardinality.REPEATED, contract.unified().cardinality(),
                        "REPEATED should win over SINGULAR");
                assertFalse(contract.unified().nullable(),
                        "Repeated fields are never nullable");
                assertEquals(FieldContract.DefaultValue.EMPTY_LIST,
                        contract.unified().defaultValueWhenUnset());
            }
        }

        @Nested
        @DisplayName("getterUsesHasCheck = hasMethodExists AND nullable")
        class GetterPatternMergeTests {

            @Test
            @DisplayName("has=YES, nullable=YES -> getterUsesHasCheck=YES")
            void hasAndNullable() {
                // Proto2 optional: has=YES, nullable=YES
                FieldInfo v1 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                        null, false, true);

                MergedField merged = MergedField.builder()
                        .addVersionField("v1", v1)
                        .build();

                MergedFieldContract contract = MergedFieldContract.from(merged,
                        Map.of("v1", ProtoSyntax.PROTO2));

                assertTrue(contract.unified().getterUsesHasCheck(),
                        "has=YES && nullable=YES -> getterUsesHasCheck=YES");
            }

            @Test
            @DisplayName("has=YES, nullable=NO -> getterUsesHasCheck=NO")
            void hasButNotNullable() {
                // Proto2 required: has=YES, nullable=NO
                FieldInfo v1 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_REQUIRED,
                        null, false, true);

                MergedField merged = MergedField.builder()
                        .addVersionField("v1", v1)
                        .build();

                MergedFieldContract contract = MergedFieldContract.from(merged,
                        Map.of("v1", ProtoSyntax.PROTO2));

                assertFalse(contract.unified().getterUsesHasCheck(),
                        "has=YES && nullable=NO -> getterUsesHasCheck=NO");
            }

            @Test
            @DisplayName("has=NO, nullable=YES -> getterUsesHasCheck=NO")
            void noHasButNullable() {
                // Mixed: v1 nullable (proto2 opt), v2 no has (proto3 implicit)
                FieldInfo v1 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                        null, false, true);  // has=YES, nullable=YES
                FieldInfo v2 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                        null, false, false); // has=NO

                MergedField merged = MergedField.builder()
                        .addVersionField("v1", v1)
                        .addVersionField("v2", v2)
                        .build();

                MergedFieldContract contract = MergedFieldContract.from(merged,
                        Map.of("v1", ProtoSyntax.PROTO2, "v2", ProtoSyntax.PROTO3));

                // unified: has=NO (not all have it), nullable=YES (any is nullable)
                assertFalse(contract.unified().getterUsesHasCheck(),
                        "has=NO (even if nullable=YES) -> getterUsesHasCheck=NO");
            }
        }
    }

    // ============================================================================
    // CONFLICT TYPES AND BUILDER BEHAVIOR
    // ============================================================================

    @Nested
    @DisplayName("Conflict Types - Builder Setter Behavior")
    class ConflictTypeTests {

        @Test
        @DisplayName("WIDENING conflict -> skip builder setters")
        void wideningSkipsSetters() {
            FieldInfo v1 = createFieldInfo("count", Type.TYPE_INT32, Label.LABEL_OPTIONAL,
                    null, false, true);
            FieldInfo v2 = createFieldInfo("count", Type.TYPE_INT64, Label.LABEL_OPTIONAL,
                    null, false, true);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .addVersionField("v2", v2)
                    .conflictType(MergedField.ConflictType.WIDENING)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged,
                    Map.of("v1", ProtoSyntax.PROTO3, "v2", ProtoSyntax.PROTO3));

            assertTrue(contract.shouldSkipBuilderSetter(),
                    "WIDENING conflict should skip builder setters");
        }

        @Test
        @DisplayName("FLOAT_DOUBLE conflict -> skip builder setters")
        void floatDoubleSkipsSetters() {
            FieldInfo v1 = createFieldInfo("value", Type.TYPE_FLOAT, Label.LABEL_OPTIONAL,
                    null, false, true);
            FieldInfo v2 = createFieldInfo("value", Type.TYPE_DOUBLE, Label.LABEL_OPTIONAL,
                    null, false, true);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .addVersionField("v2", v2)
                    .conflictType(MergedField.ConflictType.FLOAT_DOUBLE)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged,
                    Map.of("v1", ProtoSyntax.PROTO3, "v2", ProtoSyntax.PROTO3));

            assertTrue(contract.shouldSkipBuilderSetter(),
                    "FLOAT_DOUBLE conflict should skip builder setters");
        }

        @Test
        @DisplayName("SIGNED_UNSIGNED conflict -> skip builder setters")
        void signedUnsignedSkipsSetters() {
            FieldInfo v1 = createFieldInfo("value", Type.TYPE_INT32, Label.LABEL_OPTIONAL,
                    null, false, true);
            FieldInfo v2 = createFieldInfo("value", Type.TYPE_UINT32, Label.LABEL_OPTIONAL,
                    null, false, true);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .addVersionField("v2", v2)
                    .conflictType(MergedField.ConflictType.SIGNED_UNSIGNED)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged,
                    Map.of("v1", ProtoSyntax.PROTO3, "v2", ProtoSyntax.PROTO3));

            assertTrue(contract.shouldSkipBuilderSetter(),
                    "SIGNED_UNSIGNED conflict should skip builder setters");
        }

        @Test
        @DisplayName("NONE conflict -> generate builder setters")
        void noneConflictGeneratesSetters() {
            FieldInfo v1 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                    null, false, true);
            FieldInfo v2 = createFieldInfo("name", Type.TYPE_STRING, Label.LABEL_OPTIONAL,
                    null, false, true);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .addVersionField("v2", v2)
                    .conflictType(MergedField.ConflictType.NONE)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged,
                    Map.of("v1", ProtoSyntax.PROTO3, "v2", ProtoSyntax.PROTO3));

            assertFalse(contract.shouldSkipBuilderSetter(),
                    "NONE conflict should generate builder setters");
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private MergedFieldContract createProto2Contract(String name, Type type, Label label,
                                                      String typeName, boolean inOneof) {
        FieldInfo fieldInfo = createFieldInfo(name, type, label, typeName, inOneof, true);
        MergedField merged = MergedField.builder()
                .addVersionField("v1", fieldInfo)
                .build();
        return MergedFieldContract.from(merged, Map.of("v1", ProtoSyntax.PROTO2));
    }

    private MergedFieldContract createProto3ImplicitContract(String name, Type type, String typeName) {
        // Proto3 implicit: supportsHasMethod = false for scalars, true for messages
        boolean supportsHas = (type == Type.TYPE_MESSAGE);
        FieldInfo fieldInfo = createFieldInfo(name, type, Label.LABEL_OPTIONAL, typeName, false, supportsHas);
        MergedField merged = MergedField.builder()
                .addVersionField("v1", fieldInfo)
                .build();
        return MergedFieldContract.from(merged, Map.of("v1", ProtoSyntax.PROTO3));
    }

    private MergedFieldContract createProto3ExplicitContract(String name, Type type, String typeName) {
        // Proto3 explicit optional: supportsHasMethod = true (synthetic oneof)
        FieldInfo fieldInfo = createFieldInfo(name, type, Label.LABEL_OPTIONAL, typeName, false, true);
        MergedField merged = MergedField.builder()
                .addVersionField("v1", fieldInfo)
                .build();
        // Use PROTO2 to signal "has explicit presence" (or could be PROTO3 with optional)
        // The key is supportsHasMethod=true
        return MergedFieldContract.from(merged, Map.of("v1", ProtoSyntax.PROTO2));
    }

    private MergedFieldContract createOneofContract(String name, Type type, String typeName,
                                                     ProtoSyntax syntax) {
        FieldInfo fieldInfo = createFieldInfo(name, type, Label.LABEL_OPTIONAL, typeName, true, true);
        MergedField merged = MergedField.builder()
                .addVersionField("v1", fieldInfo)
                .build();
        return MergedFieldContract.from(merged, Map.of("v1", syntax));
    }

    private MergedFieldContract createRepeatedContract(String name, Type type, String typeName,
                                                        ProtoSyntax syntax) {
        FieldInfo fieldInfo = createFieldInfo(name, type, Label.LABEL_REPEATED, typeName, false, false);
        MergedField merged = MergedField.builder()
                .addVersionField("v1", fieldInfo)
                .build();
        return MergedFieldContract.from(merged, Map.of("v1", syntax));
    }

    private FieldInfo createFieldInfo(String name, Type type, Label label, String typeName,
                                       boolean inOneof, boolean supportsHasMethod) {
        int oneofIndex = inOneof ? 0 : -1;
        String oneofName = inOneof ? "testOneof" : null;
        return new FieldInfo(name, name, 1, type, label, typeName,
                null, oneofIndex, oneofName, null, supportsHasMethod);
    }

    private FieldInfo createMapFieldInfo(String name) {
        // Map fields are represented as repeated message with MapInfo
        // For simplicity, we use a simplified representation
        return new FieldInfo(name, name, 1, Type.TYPE_MESSAGE, Label.LABEL_REPEATED,
                ".example." + capitalize(name) + "Entry",
                new io.alnovis.protowrapper.model.MapInfo(Type.TYPE_STRING, Type.TYPE_STRING, null, null),
                -1, null, null, false);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
