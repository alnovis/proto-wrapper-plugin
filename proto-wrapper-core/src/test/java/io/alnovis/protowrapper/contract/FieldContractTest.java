package io.alnovis.protowrapper.contract;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import io.alnovis.protowrapper.contract.FieldContract.DefaultValue;
import io.alnovis.protowrapper.model.ProtoSyntax;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for {@link FieldContract} contract matrix.
 *
 * <p>These tests verify that all combinations in the contract matrix
 * produce the expected behavior. Each test case corresponds to a cell
 * in the matrix from FIELD_CONTRACT_MATRIX.md.</p>
 */
@DisplayName("FieldContract - Contract Matrix Tests")
class FieldContractTest {

    // ==================== Proto2 Singular Tests ====================

    @Nested
    @DisplayName("Proto2 Singular Fields")
    class Proto2SingularFields {

        @Nested
        @DisplayName("Optional Fields")
        class OptionalFields {

            @Test
            @DisplayName("optional int32 - has exists, nullable, null when unset")
            void optionalInt32() {
                FieldContract contract = createContract(
                        ProtoSyntax.PROTO2, Label.LABEL_OPTIONAL, Type.TYPE_INT32, false);

                assertThat(contract.cardinality()).isEqualTo(FieldCardinality.SINGULAR);
                assertThat(contract.typeCategory()).isEqualTo(FieldTypeCategory.SCALAR_NUMERIC);
                assertThat(contract.presence()).isEqualTo(FieldPresence.PROTO2_OPTIONAL);
                assertThat(contract.hasMethodExists()).isTrue();
                assertThat(contract.getterUsesHasCheck()).isTrue();
                assertThat(contract.nullable()).isTrue();
                assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.NULL);
            }

            @Test
            @DisplayName("optional string - has exists, nullable, null when unset")
            void optionalString() {
                FieldContract contract = createContract(
                        ProtoSyntax.PROTO2, Label.LABEL_OPTIONAL, Type.TYPE_STRING, false);

                assertThat(contract.hasMethodExists()).isTrue();
                assertThat(contract.getterUsesHasCheck()).isTrue();
                assertThat(contract.nullable()).isTrue();
                assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.NULL);
            }

            @Test
            @DisplayName("optional bytes - has exists, nullable, null when unset")
            void optionalBytes() {
                FieldContract contract = createContract(
                        ProtoSyntax.PROTO2, Label.LABEL_OPTIONAL, Type.TYPE_BYTES, false);

                assertThat(contract.hasMethodExists()).isTrue();
                assertThat(contract.nullable()).isTrue();
                assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.NULL);
            }

            @Test
            @DisplayName("optional message - has exists, returns default instance when unset")
            void optionalMessage() {
                FieldContract contract = createContract(
                        ProtoSyntax.PROTO2, Label.LABEL_OPTIONAL, Type.TYPE_MESSAGE, false);

                assertThat(contract.typeCategory()).isEqualTo(FieldTypeCategory.MESSAGE);
                assertThat(contract.hasMethodExists()).isTrue();
                assertThat(contract.getterUsesHasCheck()).isFalse();
                assertThat(contract.nullable()).isFalse();
                assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.DEFAULT_INSTANCE);
            }

            @Test
            @DisplayName("optional enum - has exists, nullable, null when unset")
            void optionalEnum() {
                FieldContract contract = createContract(
                        ProtoSyntax.PROTO2, Label.LABEL_OPTIONAL, Type.TYPE_ENUM, false);

                assertThat(contract.typeCategory()).isEqualTo(FieldTypeCategory.ENUM);
                assertThat(contract.hasMethodExists()).isTrue();
                assertThat(contract.nullable()).isTrue();
                assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.NULL);
            }
        }

        @Nested
        @DisplayName("Required Fields")
        class RequiredFields {

            @Test
            @DisplayName("required int32 - has exists, NOT nullable, returns value")
            void requiredInt32() {
                FieldContract contract = createContract(
                        ProtoSyntax.PROTO2, Label.LABEL_REQUIRED, Type.TYPE_INT32, false);

                assertThat(contract.presence()).isEqualTo(FieldPresence.PROTO2_REQUIRED);
                assertThat(contract.hasMethodExists()).isTrue();
                assertThat(contract.getterUsesHasCheck()).isFalse(); // Required always has value
                assertThat(contract.nullable()).isFalse();
                assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO);
            }

            @Test
            @DisplayName("required int64 - NOT nullable, default 0L")
            void requiredInt64() {
                FieldContract contract = createContract(
                        ProtoSyntax.PROTO2, Label.LABEL_REQUIRED, Type.TYPE_INT64, false);

                assertThat(contract.nullable()).isFalse();
                assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO_LONG);
            }

            @Test
            @DisplayName("required float - NOT nullable, default 0.0f")
            void requiredFloat() {
                FieldContract contract = createContract(
                        ProtoSyntax.PROTO2, Label.LABEL_REQUIRED, Type.TYPE_FLOAT, false);

                assertThat(contract.nullable()).isFalse();
                assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO_FLOAT);
            }

            @Test
            @DisplayName("required double - NOT nullable, default 0.0")
            void requiredDouble() {
                FieldContract contract = createContract(
                        ProtoSyntax.PROTO2, Label.LABEL_REQUIRED, Type.TYPE_DOUBLE, false);

                assertThat(contract.nullable()).isFalse();
                assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO_DOUBLE);
            }

            @Test
            @DisplayName("required bool - NOT nullable, default false")
            void requiredBool() {
                FieldContract contract = createContract(
                        ProtoSyntax.PROTO2, Label.LABEL_REQUIRED, Type.TYPE_BOOL, false);

                assertThat(contract.nullable()).isFalse();
                assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.FALSE);
            }

            @Test
            @DisplayName("required string - NOT nullable, default empty string")
            void requiredString() {
                FieldContract contract = createContract(
                        ProtoSyntax.PROTO2, Label.LABEL_REQUIRED, Type.TYPE_STRING, false);

                assertThat(contract.nullable()).isFalse();
                assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.EMPTY_STRING);
            }

            @Test
            @DisplayName("required message - has exists, NOT nullable (required)")
            void requiredMessage() {
                FieldContract contract = createContract(
                        ProtoSyntax.PROTO2, Label.LABEL_REQUIRED, Type.TYPE_MESSAGE, false);

                assertThat(contract.hasMethodExists()).isTrue();
                assertThat(contract.getterUsesHasCheck()).isFalse();
                assertThat(contract.nullable()).isFalse();
            }
        }
    }

    // ==================== Proto3 Singular Tests ====================

    @Nested
    @DisplayName("Proto3 Singular Fields (No 'optional' keyword)")
    class Proto3SingularImplicit {

        @Test
        @DisplayName("implicit int32 - NO has, NOT nullable, default 0")
        void implicitInt32() {
            FieldContract contract = createContract(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_INT32, false);

            assertThat(contract.presence()).isEqualTo(FieldPresence.PROTO3_IMPLICIT);
            assertThat(contract.hasMethodExists()).isFalse();
            assertThat(contract.getterUsesHasCheck()).isFalse();
            assertThat(contract.nullable()).isFalse();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO);
        }

        @Test
        @DisplayName("implicit bool - NO has, NOT nullable, default false")
        void implicitBool() {
            FieldContract contract = createContract(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_BOOL, false);

            assertThat(contract.hasMethodExists()).isFalse();
            assertThat(contract.nullable()).isFalse();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.FALSE);
        }

        @Test
        @DisplayName("implicit string - NO has, NOT nullable, default empty")
        void implicitString() {
            FieldContract contract = createContract(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_STRING, false);

            assertThat(contract.hasMethodExists()).isFalse();
            assertThat(contract.nullable()).isFalse();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.EMPTY_STRING);
        }

        @Test
        @DisplayName("implicit bytes - NO has, NOT nullable, default empty")
        void implicitBytes() {
            FieldContract contract = createContract(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_BYTES, false);

            assertThat(contract.hasMethodExists()).isFalse();
            assertThat(contract.nullable()).isFalse();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.EMPTY_BYTES);
        }

        @Test
        @DisplayName("implicit message - HAS exists (always for messages), returns default instance when unset")
        void implicitMessage() {
            FieldContract contract = createContract(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_MESSAGE, false);

            // Message types ALWAYS have has*() even in proto3 implicit
            assertThat(contract.hasMethodExists()).isTrue();
            assertThat(contract.getterUsesHasCheck()).isFalse();
            assertThat(contract.nullable()).isFalse();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.DEFAULT_INSTANCE);
        }

        @Test
        @DisplayName("implicit enum - NO has, NOT nullable, default first value")
        void implicitEnum() {
            FieldContract contract = createContract(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_ENUM, false);

            assertThat(contract.hasMethodExists()).isFalse();
            assertThat(contract.nullable()).isFalse();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.FIRST_ENUM_VALUE);
        }
    }

    @Nested
    @DisplayName("Proto3 Singular Fields (With 'optional' keyword)")
    class Proto3SingularExplicit {

        @Test
        @DisplayName("explicit optional int32 - has exists, nullable, null when unset")
        void explicitOptionalInt32() {
            // Proto3 optional is represented as synthetic oneof
            FieldContract contract = createContractWithOneof(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_INT32, 0);

            assertThat(contract.presence()).isEqualTo(FieldPresence.PROTO3_EXPLICIT_OPTIONAL);
            assertThat(contract.hasMethodExists()).isTrue();
            assertThat(contract.getterUsesHasCheck()).isTrue();
            assertThat(contract.nullable()).isTrue();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.NULL);
        }

        @Test
        @DisplayName("explicit optional string - has exists, nullable")
        void explicitOptionalString() {
            FieldContract contract = createContractWithOneof(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_STRING, 0);

            assertThat(contract.hasMethodExists()).isTrue();
            assertThat(contract.nullable()).isTrue();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.NULL);
        }

        @Test
        @DisplayName("explicit optional message - has exists, nullable")
        void explicitOptionalMessage() {
            FieldContract contract = createContractWithOneof(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_MESSAGE, 0);

            assertThat(contract.hasMethodExists()).isTrue();
            assertThat(contract.nullable()).isTrue();
        }

        @Test
        @DisplayName("explicit optional enum - has exists, nullable")
        void explicitOptionalEnum() {
            FieldContract contract = createContractWithOneof(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_ENUM, 0);

            assertThat(contract.hasMethodExists()).isTrue();
            assertThat(contract.nullable()).isTrue();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.NULL);
        }
    }

    // ==================== Repeated Fields Tests ====================

    @Nested
    @DisplayName("Repeated Fields (Both Proto2 and Proto3)")
    class RepeatedFields {

        @ParameterizedTest
        @EnumSource(ProtoSyntax.class)
        @DisplayName("repeated int32 - NO has, NOT nullable, empty list when unset")
        void repeatedInt32(ProtoSyntax syntax) {
            if (syntax == ProtoSyntax.AUTO) return;

            FieldContract contract = createContract(
                    syntax, Label.LABEL_REPEATED, Type.TYPE_INT32, false);

            assertThat(contract.cardinality()).isEqualTo(FieldCardinality.REPEATED);
            assertThat(contract.hasMethodExists()).isFalse();
            assertThat(contract.getterUsesHasCheck()).isFalse();
            assertThat(contract.nullable()).isFalse();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.EMPTY_LIST);
        }

        @ParameterizedTest
        @EnumSource(ProtoSyntax.class)
        @DisplayName("repeated message - NO has, NOT nullable, empty list when unset")
        void repeatedMessage(ProtoSyntax syntax) {
            if (syntax == ProtoSyntax.AUTO) return;

            FieldContract contract = createContract(
                    syntax, Label.LABEL_REPEATED, Type.TYPE_MESSAGE, false);

            // Even message types have NO has*() when repeated
            assertThat(contract.hasMethodExists()).isFalse();
            assertThat(contract.nullable()).isFalse();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.EMPTY_LIST);
        }

        @ParameterizedTest
        @EnumSource(ProtoSyntax.class)
        @DisplayName("repeated string - NO has, NOT nullable, empty list when unset")
        void repeatedString(ProtoSyntax syntax) {
            if (syntax == ProtoSyntax.AUTO) return;

            FieldContract contract = createContract(
                    syntax, Label.LABEL_REPEATED, Type.TYPE_STRING, false);

            assertThat(contract.hasMethodExists()).isFalse();
            assertThat(contract.nullable()).isFalse();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.EMPTY_LIST);
        }

        @ParameterizedTest
        @EnumSource(ProtoSyntax.class)
        @DisplayName("repeated enum - NO has, NOT nullable, empty list when unset")
        void repeatedEnum(ProtoSyntax syntax) {
            if (syntax == ProtoSyntax.AUTO) return;

            FieldContract contract = createContract(
                    syntax, Label.LABEL_REPEATED, Type.TYPE_ENUM, false);

            assertThat(contract.hasMethodExists()).isFalse();
            assertThat(contract.nullable()).isFalse();
            assertThat(contract.defaultValueWhenUnset()).isEqualTo(DefaultValue.EMPTY_LIST);
        }
    }

    // ==================== Oneof Fields Tests ====================

    @Nested
    @DisplayName("Oneof Fields")
    class OneofFields {

        @Test
        @DisplayName("proto2 oneof int32 - has exists, nullable")
        void proto2OneofInt32() {
            FieldContract contract = createContractWithOneof(
                    ProtoSyntax.PROTO2, Label.LABEL_OPTIONAL, Type.TYPE_INT32, 0);

            assertThat(contract.inOneof()).isTrue();
            assertThat(contract.hasMethodExists()).isTrue();
            assertThat(contract.getterUsesHasCheck()).isTrue();
            assertThat(contract.nullable()).isTrue();
        }

        @Test
        @DisplayName("proto3 oneof message - has exists, nullable")
        void proto3OneofMessage() {
            FieldContract contract = createContractWithOneof(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_MESSAGE, 0);

            assertThat(contract.inOneof()).isTrue();
            assertThat(contract.hasMethodExists()).isTrue();
            assertThat(contract.nullable()).isTrue();
        }

        @Test
        @DisplayName("proto3 oneof string - has exists, nullable")
        void proto3OneofString() {
            FieldContract contract = createContractWithOneof(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_STRING, 0);

            assertThat(contract.inOneof()).isTrue();
            assertThat(contract.hasMethodExists()).isTrue();
            assertThat(contract.nullable()).isTrue();
        }
    }

    // ==================== Convenience Methods Tests ====================

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethods {

        @Test
        @DisplayName("isSingular() returns true for singular fields")
        void isSingular() {
            FieldContract contract = createContract(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_INT32, false);
            assertThat(contract.isSingular()).isTrue();
            assertThat(contract.isRepeated()).isFalse();
            assertThat(contract.isMap()).isFalse();
        }

        @Test
        @DisplayName("isRepeated() returns true for repeated fields")
        void isRepeated() {
            FieldContract contract = createContract(
                    ProtoSyntax.PROTO3, Label.LABEL_REPEATED, Type.TYPE_INT32, false);
            assertThat(contract.isSingular()).isFalse();
            assertThat(contract.isRepeated()).isTrue();
        }

        @Test
        @DisplayName("isMessage() returns true for message types")
        void isMessage() {
            FieldContract contract = createContract(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_MESSAGE, false);
            assertThat(contract.isMessage()).isTrue();
            assertThat(contract.isEnum()).isFalse();
            assertThat(contract.isScalar()).isFalse();
        }

        @Test
        @DisplayName("isEnum() returns true for enum types")
        void isEnum() {
            FieldContract contract = createContract(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_ENUM, false);
            assertThat(contract.isMessage()).isFalse();
            assertThat(contract.isEnum()).isTrue();
            assertThat(contract.isScalar()).isFalse();
        }

        @Test
        @DisplayName("isScalar() returns true for scalar types")
        void isScalar() {
            FieldContract contract = createContract(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_INT32, false);
            assertThat(contract.isMessage()).isFalse();
            assertThat(contract.isEnum()).isFalse();
            assertThat(contract.isScalar()).isTrue();
        }

        @Test
        @DisplayName("describe() returns human-readable description")
        void describe() {
            FieldContract contract = createContract(
                    ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_INT32, false);
            String description = contract.describe();

            assertThat(description).contains("PROTO3_IMPLICIT");
            assertThat(description).contains("SINGULAR");
            assertThat(description).contains("SCALAR_NUMERIC");
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("all numeric types have correct defaults")
        void numericDefaults() {
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_INT32, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO);
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_INT64, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO_LONG);
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_UINT32, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO);
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_UINT64, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO_LONG);
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_FLOAT, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO_FLOAT);
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_DOUBLE, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO_DOUBLE);
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_BOOL, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.FALSE);
        }

        @Test
        @DisplayName("sint types have correct defaults")
        void sintDefaults() {
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_SINT32, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO);
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_SINT64, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO_LONG);
        }

        @Test
        @DisplayName("fixed types have correct defaults")
        void fixedDefaults() {
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_FIXED32, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO);
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_FIXED64, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO_LONG);
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_SFIXED32, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO);
            assertThat(createContract(ProtoSyntax.PROTO3, Label.LABEL_OPTIONAL, Type.TYPE_SFIXED64, false)
                    .defaultValueWhenUnset()).isEqualTo(DefaultValue.ZERO_LONG);
        }
    }

    // ==================== Helper Methods ====================

    private static FieldContract createContract(ProtoSyntax syntax, Label label, Type type, boolean inOneof) {
        return createContractWithOneof(syntax, label, type, inOneof ? 0 : -1);
    }

    private static FieldContract createContractWithOneof(ProtoSyntax syntax, Label label, Type type, int oneofIndex) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
                .setName("test_field")
                .setNumber(1)
                .setLabel(label)
                .setType(type)
                .build();

        return FieldContract.fromDescriptor(proto, syntax, oneofIndex);
    }
}
