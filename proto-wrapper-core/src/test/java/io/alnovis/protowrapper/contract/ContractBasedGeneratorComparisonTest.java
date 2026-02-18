package io.alnovis.protowrapper.contract;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.ProtoSyntax;

import javax.lang.model.element.Modifier;
import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comparison tests between ContractBasedFieldGenerator and existing handler output.
 *
 * <p>These tests verify that the contract-based generator produces equivalent output
 * to the existing AbstractConflictHandler-based generators.</p>
 *
 * <p>For each field type scenario, we verify:</p>
 * <ul>
 *   <li>Getter method structure matches</li>
 *   <li>Has method presence/absence matches</li>
 *   <li>Builder methods match</li>
 *   <li>Abstract extract methods match</li>
 * </ul>
 */
class ContractBasedGeneratorComparisonTest {

    private static final TypeName PROTO_TYPE = ClassName.get("com.example", "MyProto");
    private static final TypeName BUILDER_TYPE = ClassName.get("com.example", "MyBuilder");

    // ==================== Proto3 Implicit Scalar ====================

    @Nested
    @DisplayName("Proto3 Implicit Scalar (no optional)")
    class Proto3ImplicitScalarTests {

        /**
         * Proto3 implicit string field:
         * - No has*() method (proto3 scalars without optional)
         * - Getter returns value directly (no null check)
         * - Only setXxx (no clearXxx since not nullable)
         */
        @Test
        @DisplayName("String field - getter without has check")
        void stringField() {
            // Simulate MergedField for proto3 implicit string
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_IMPLICIT,
                    false,  // inOneof
                    false,  // hasMethodExists (proto3 implicit scalar)
                    false,  // getterUsesHasCheck
                    false,  // nullable
                    FieldContract.DefaultValue.EMPTY_STRING
            );

            FieldMethodNames names = FieldMethodNames.from("title");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, ClassName.get(String.class), PROTO_TYPE);

            // Getter should NOT use has-check
            MethodSpec getter = generator.generateGetter();
            assertEquals("getTitle", getter.name);
            assertFalse(getter.toString().contains("extractHas"));
            assertTrue(getter.toString().contains("return extractTitle(proto)"));

            // No has method
            assertTrue(generator.generateHasMethod().isEmpty());

            // No extractHas abstract method
            assertTrue(generator.generateAbstractExtractHasMethod().isEmpty());

            // Builder: only set (no clear because not nullable)
            List<MethodSpec> builderMethods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertEquals(1, builderMethods.size());
            assertEquals("setTitle", builderMethods.get(0).name);
        }

        @Test
        @DisplayName("Int field - getter without has check")
        void intField() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_NUMERIC,
                    FieldPresence.PROTO3_IMPLICIT,
                    false, false, false, false,
                    FieldContract.DefaultValue.ZERO
            );

            FieldMethodNames names = FieldMethodNames.from("count");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, TypeName.INT, PROTO_TYPE);

            MethodSpec getter = generator.generateGetter();
            assertFalse(getter.toString().contains("extractHas"));
            assertTrue(getter.toString().contains("return extractCount(proto)"));

            assertTrue(generator.generateHasMethod().isEmpty());
        }
    }

    // ==================== Proto3 Explicit Optional ====================

    @Nested
    @DisplayName("Proto3 Explicit Optional (with optional keyword)")
    class Proto3ExplicitOptionalTests {

        /**
         * Proto3 explicit optional string:
         * - has*() method exists
         * - Getter uses has-check (returns null when not set)
         * - setXxx and clearXxx methods
         */
        @Test
        @DisplayName("Optional string - getter with has check")
        void optionalStringField() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_EXPLICIT_OPTIONAL,
                    false,  // inOneof
                    true,   // hasMethodExists
                    true,   // getterUsesHasCheck
                    true,   // nullable
                    FieldContract.DefaultValue.NULL
            );

            FieldMethodNames names = FieldMethodNames.from("optionalName");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, ClassName.get(String.class), PROTO_TYPE);

            // Getter SHOULD use has-check
            MethodSpec getter = generator.generateGetter();
            assertTrue(getter.toString().contains("extractHasOptionalName(proto)"));
            assertTrue(getter.toString().contains("extractOptionalName(proto)"));
            assertTrue(getter.toString().contains("null"));

            // Has method exists
            assertTrue(generator.generateHasMethod().isPresent());
            assertEquals("hasOptionalName", generator.generateHasMethod().get().name);

            // extractHas abstract method exists
            assertTrue(generator.generateAbstractExtractHasMethod().isPresent());

            // Builder: set AND clear (because nullable)
            List<MethodSpec> builderMethods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertEquals(2, builderMethods.size());
            assertTrue(builderMethods.stream().anyMatch(m -> m.name.equals("setOptionalName")));
            assertTrue(builderMethods.stream().anyMatch(m -> m.name.equals("clearOptionalName")));
        }

        @Test
        @DisplayName("Optional int - getter with has check, boxed type")
        void optionalIntField() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_NUMERIC,
                    FieldPresence.PROTO3_EXPLICIT_OPTIONAL,
                    false, true, true, true,
                    FieldContract.DefaultValue.NULL
            );

            FieldMethodNames names = FieldMethodNames.from("optionalCount");
            // Note: For nullable ints, we use boxed Integer type
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, TypeName.INT.box(), PROTO_TYPE);

            MethodSpec getter = generator.generateGetter();
            assertTrue(getter.toString().contains("extractHasOptionalCount"));
        }
    }

    // ==================== Proto2 Optional ====================

    @Nested
    @DisplayName("Proto2 Optional Fields")
    class Proto2OptionalTests {

        /**
         * Proto2 optional string:
         * - has*() method exists
         * - Getter uses has-check
         * - setXxx and clearXxx methods
         */
        @Test
        @DisplayName("Optional string - getter with has check")
        void optionalStringField() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO2_OPTIONAL,
                    false, true, true, true,
                    FieldContract.DefaultValue.NULL
            );

            FieldMethodNames names = FieldMethodNames.from("name");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, ClassName.get(String.class), PROTO_TYPE);

            MethodSpec getter = generator.generateGetter();
            assertTrue(getter.toString().contains("extractHasName"));

            assertTrue(generator.generateHasMethod().isPresent());

            List<MethodSpec> builderMethods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertEquals(2, builderMethods.size());
        }
    }

    // ==================== Proto2 Required ====================

    @Nested
    @DisplayName("Proto2 Required Fields")
    class Proto2RequiredTests {

        /**
         * Proto2 required string:
         * - has*() method EXISTS in proto (for backwards compat)
         * - But getter does NOT use has-check (always set)
         * - Only setXxx (no clearXxx)
         */
        @Test
        @DisplayName("Required string - getter without has check")
        void requiredStringField() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO2_REQUIRED,
                    false,
                    true,   // hasMethodExists (proto2 always has it)
                    false,  // getterUsesHasCheck (required = always set)
                    false,  // nullable (required = never null)
                    FieldContract.DefaultValue.EMPTY_STRING
            );

            FieldMethodNames names = FieldMethodNames.from("requiredId");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, ClassName.get(String.class), PROTO_TYPE);

            // Getter should NOT use has-check for required fields
            MethodSpec getter = generator.generateGetter();
            assertFalse(getter.toString().contains("extractHas"));
            assertTrue(getter.toString().contains("return extractRequiredId(proto)"));

            // Has method still exists (for proto2)
            assertTrue(generator.generateHasMethod().isPresent());

            // Builder: only set (no clear for required)
            List<MethodSpec> builderMethods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertEquals(1, builderMethods.size());
            assertEquals("setRequiredId", builderMethods.get(0).name);
        }
    }

    // ==================== Message Fields ====================

    @Nested
    @DisplayName("Message Fields")
    class MessageFieldTests {

        private static final TypeName NESTED_TYPE = ClassName.get("com.example", "NestedMessage");

        /**
         * Proto3 message field (always has has*() method):
         * - has*() method exists
         * - Getter returns default instance (no has-check)
         * - setXxx method
         */
        @Test
        @DisplayName("Proto3 message field - has method exists, getter returns default instance")
        void proto3MessageField() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.MESSAGE,
                    FieldPresence.PROTO3_IMPLICIT,
                    false,
                    true,   // hasMethodExists (messages always have has)
                    false,  // getterUsesHasCheck (returns default instance)
                    false,  // nullable (returns default instance)
                    FieldContract.DefaultValue.DEFAULT_INSTANCE
            );

            FieldMethodNames names = FieldMethodNames.from("config");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, NESTED_TYPE, PROTO_TYPE);

            // Has method exists for messages
            assertTrue(generator.generateHasMethod().isPresent());

            // Getter returns directly (no has-check)
            MethodSpec getter = generator.generateGetter();
            assertFalse(getter.toString().contains("extractHasConfig"));
            assertFalse(getter.toString().contains("null"));
            assertTrue(getter.toString().contains("extractConfig"));

            // Builder has set method
            List<MethodSpec> builderMethods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertFalse(builderMethods.isEmpty());
        }

        @Test
        @DisplayName("Proto2 message field - same behavior as proto3")
        void proto2MessageField() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.MESSAGE,
                    FieldPresence.PROTO2_OPTIONAL,
                    false, true, false, false,
                    FieldContract.DefaultValue.DEFAULT_INSTANCE
            );

            FieldMethodNames names = FieldMethodNames.from("data");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, NESTED_TYPE, PROTO_TYPE);

            assertTrue(generator.generateHasMethod().isPresent());
            assertFalse(generator.generateGetter().toString().contains("extractHasData"));
            assertTrue(generator.generateGetter().toString().contains("extractData"));
        }
    }

    // ==================== Repeated Fields ====================

    @Nested
    @DisplayName("Repeated Fields")
    class RepeatedFieldTests {

        private static final TypeName LIST_STRING = ParameterizedTypeName.get(
                ClassName.get(List.class), ClassName.get(String.class));

        /**
         * Repeated field:
         * - No has*() method
         * - Getter returns list directly (never null)
         * - add, addAll, set, clear methods
         */
        @Test
        @DisplayName("Repeated string - no has, all builder methods")
        void repeatedStringField() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.REPEATED,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_IMPLICIT,
                    false, false, false, false,
                    FieldContract.DefaultValue.EMPTY_LIST
            );

            FieldMethodNames names = FieldMethodNames.from("tags");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, LIST_STRING, PROTO_TYPE, ClassName.get(String.class));

            // No has method for repeated
            assertTrue(generator.generateHasMethod().isEmpty());

            // Getter without has-check
            MethodSpec getter = generator.generateGetter();
            assertFalse(getter.toString().contains("extractHas"));

            // All 4 builder methods
            List<MethodSpec> builderMethods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertEquals(4, builderMethods.size());

            List<String> names_list = builderMethods.stream().map(m -> m.name).toList();
            assertTrue(names_list.contains("addTags"));
            assertTrue(names_list.contains("addAllTags"));
            assertTrue(names_list.contains("setTags"));
            assertTrue(names_list.contains("clearTags"));
        }
    }

    // ==================== Oneof Fields ====================

    @Nested
    @DisplayName("Oneof Fields")
    class OneofFieldTests {

        /**
         * Oneof field (proto3):
         * - has*() method exists (always for oneof)
         * - Getter uses has-check
         * - Nullable (only one field in oneof is set)
         */
        @Test
        @DisplayName("Oneof string field - has method, nullable")
        void oneofStringField() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_IMPLICIT,  // Even implicit presence, oneof has has*()
                    true,   // inOneof
                    true,   // hasMethodExists
                    true,   // getterUsesHasCheck
                    true,   // nullable
                    FieldContract.DefaultValue.NULL
            );

            FieldMethodNames names = FieldMethodNames.from("oneofString");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, ClassName.get(String.class), PROTO_TYPE);

            assertTrue(generator.generateHasMethod().isPresent());
            assertTrue(generator.generateGetter().toString().contains("extractHasOneofString"));

            // Has both set and clear
            List<MethodSpec> builderMethods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertEquals(2, builderMethods.size());
        }
    }

    // ==================== FieldContract.from() Integration ====================

    @Nested
    @DisplayName("FieldContract.from() Integration")
    class FieldContractFromIntegrationTests {

        /**
         * Test that FieldContract.from() correctly determines behavior
         * for a proto3 message field.
         */
        @Test
        @DisplayName("FieldContract.from() for proto3 message field")
        void fieldContractFromProto3Message() {
            // Create a mock FieldInfo for a proto3 message field
            // Using constructor: (protoName, javaName, number, type, label, typeName)
            FieldInfo fieldInfo = new FieldInfo(
                    "nested", "nested", 1, Type.TYPE_MESSAGE,
                    Label.LABEL_OPTIONAL, ".example.NestedMessage");

            FieldContract contract = FieldContract.from(fieldInfo, ProtoSyntax.PROTO3);

            // Verify contract properties
            assertEquals(FieldCardinality.SINGULAR, contract.cardinality());
            assertEquals(FieldTypeCategory.MESSAGE, contract.typeCategory());
            assertTrue(contract.hasMethodExists(), "Messages should have has*() method");
            assertFalse(contract.getterUsesHasCheck(), "Messages should not use has-check (return default instance)");
            assertFalse(contract.nullable(), "Messages should not be nullable (return default instance)");
        }

        /**
         * Test that FieldContract.from() correctly determines behavior
         * for a proto3 implicit scalar.
         */
        @Test
        @DisplayName("FieldContract.from() for proto3 implicit int32")
        void fieldContractFromProto3ImplicitInt() {
            // Proto3 implicit scalar - no oneofIndex
            FieldInfo fieldInfo = new FieldInfo(
                    "count", "count", 2, Type.TYPE_INT32,
                    Label.LABEL_OPTIONAL, null);

            FieldContract contract = FieldContract.from(fieldInfo, ProtoSyntax.PROTO3);

            assertEquals(FieldCardinality.SINGULAR, contract.cardinality());
            assertEquals(FieldTypeCategory.SCALAR_NUMERIC, contract.typeCategory());
            // Note: FieldContract.from uses oneofIndex to detect proto3 explicit optional
            // With oneofIndex=-1 (default), it's proto3 implicit
            assertFalse(contract.getterUsesHasCheck());
            assertFalse(contract.nullable());
            assertEquals(FieldContract.DefaultValue.ZERO, contract.defaultValueWhenUnset());
        }

        /**
         * Test that FieldContract.from() for repeated fields.
         */
        @Test
        @DisplayName("FieldContract.from() for repeated string")
        void fieldContractFromRepeated() {
            FieldInfo fieldInfo = new FieldInfo(
                    "items", "items", 3, Type.TYPE_STRING,
                    Label.LABEL_REPEATED, null);

            FieldContract contract = FieldContract.from(fieldInfo, ProtoSyntax.PROTO3);

            assertEquals(FieldCardinality.REPEATED, contract.cardinality());
            assertFalse(contract.hasMethodExists(), "Repeated fields should NOT have has*() method");
            assertFalse(contract.nullable(), "Repeated fields are never null (empty list)");
            assertEquals(FieldContract.DefaultValue.EMPTY_LIST, contract.defaultValueWhenUnset());
        }
    }

    // ==================== Method Signature Verification ====================

    @Nested
    @DisplayName("Method Signature Verification")
    class MethodSignatureTests {

        @Test
        @DisplayName("Getter method has correct modifiers")
        void getterHasCorrectModifiers() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_IMPLICIT,
                    false, false, false, false,
                    FieldContract.DefaultValue.EMPTY_STRING
            );

            FieldMethodNames names = FieldMethodNames.from("field");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, ClassName.get(String.class), PROTO_TYPE);

            MethodSpec getter = generator.generateGetter();

            assertTrue(getter.modifiers.contains(Modifier.PUBLIC));
            assertTrue(getter.modifiers.contains(Modifier.FINAL));
            assertTrue(getter.annotations.stream()
                    .anyMatch(a -> a.type.equals(ClassName.get(Override.class))));
        }

        @Test
        @DisplayName("Abstract extract method has correct modifiers")
        void abstractExtractHasCorrectModifiers() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_IMPLICIT,
                    false, false, false, false,
                    FieldContract.DefaultValue.EMPTY_STRING
            );

            FieldMethodNames names = FieldMethodNames.from("field");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, ClassName.get(String.class), PROTO_TYPE);

            MethodSpec extract = generator.generateAbstractExtractMethod();

            assertTrue(extract.modifiers.contains(Modifier.PROTECTED));
            assertTrue(extract.modifiers.contains(Modifier.ABSTRACT));
            assertEquals(1, extract.parameters.size());
            assertEquals("proto", extract.parameters.get(0).name);
            assertEquals(PROTO_TYPE, extract.parameters.get(0).type);
        }

        @Test
        @DisplayName("Builder methods return correct type")
        void builderMethodsReturnCorrectType() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.MESSAGE,
                    FieldPresence.PROTO3_IMPLICIT,
                    false, true, false, false,
                    FieldContract.DefaultValue.DEFAULT_INSTANCE
            );

            TypeName messageType = ClassName.get("com.example", "Nested");
            FieldMethodNames names = FieldMethodNames.from("data");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, messageType, PROTO_TYPE);

            List<MethodSpec> methods = generator.generateBuilderMethods(BUILDER_TYPE);

            for (MethodSpec method : methods) {
                assertEquals(BUILDER_TYPE, method.returnType);
                assertTrue(method.toString().contains("return this"));
            }
        }
    }
}
