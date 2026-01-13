package space.alnovis.protowrapper.contract;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContractBasedFieldGenerator}.
 *
 * <p>These tests verify that the generator produces correct method specs
 * based on the field contract.</p>
 */
class ContractBasedFieldGeneratorTest {

    private static final TypeName PROTO_TYPE = ClassName.get("com.example", "MyProto");
    private static final TypeName STRING_TYPE = ClassName.get(String.class);
    private static final TypeName INT_TYPE = TypeName.INT.box();
    private static final TypeName MESSAGE_TYPE = ClassName.get("com.example", "NestedMessage");
    private static final TypeName LIST_STRING = ParameterizedTypeName.get(
            ClassName.get(List.class), STRING_TYPE);
    private static final TypeName BUILDER_TYPE = ClassName.get("com.example", "MyBuilder");

    // ==================== Getter Tests ====================

    @Nested
    @DisplayName("Getter Generation")
    class GetterTests {

        @Test
        @DisplayName("Proto3 implicit scalar - getter without has-check")
        void proto3ImplicitScalar_getterWithoutHasCheck() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_IMPLICIT,
                    false,  // inOneof
                    false,  // hasMethodExists
                    false,  // getterUsesHasCheck
                    false,  // nullable
                    FieldContract.DefaultValue.EMPTY_STRING
            );

            FieldMethodNames names = FieldMethodNames.from("userName");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            MethodSpec getter = generator.generateGetter();

            assertEquals("getUserName", getter.name);
            assertTrue(getter.modifiers.contains(Modifier.PUBLIC));
            assertTrue(getter.modifiers.contains(Modifier.FINAL));
            assertEquals(STRING_TYPE, getter.returnType);
            assertTrue(getter.toString().contains("return extractUserName(proto)"));
            assertFalse(getter.toString().contains("extractHas"));
        }

        @Test
        @DisplayName("Proto2 optional message - getter with has-check")
        void proto2OptionalMessage_getterWithHasCheck() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.MESSAGE,
                    FieldPresence.PROTO2_OPTIONAL,
                    false,  // inOneof
                    true,   // hasMethodExists
                    true,   // getterUsesHasCheck
                    true,   // nullable
                    FieldContract.DefaultValue.NULL
            );

            FieldMethodNames names = FieldMethodNames.from("address");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, MESSAGE_TYPE, PROTO_TYPE);

            MethodSpec getter = generator.generateGetter();

            assertEquals("getAddress", getter.name);
            assertTrue(getter.toString().contains("extractHasAddress(proto)"));
            assertTrue(getter.toString().contains("extractAddress(proto)"));
            assertTrue(getter.toString().contains("null"));
        }

        @Test
        @DisplayName("Proto3 explicit optional scalar - getter with has-check")
        void proto3ExplicitOptionalScalar_getterWithHasCheck() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_NUMERIC,
                    FieldPresence.PROTO3_EXPLICIT_OPTIONAL,
                    false,  // inOneof
                    true,   // hasMethodExists
                    true,   // getterUsesHasCheck
                    true,   // nullable
                    FieldContract.DefaultValue.NULL
            );

            FieldMethodNames names = FieldMethodNames.from("optionalId");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, INT_TYPE, PROTO_TYPE);

            MethodSpec getter = generator.generateGetter();

            assertEquals("getOptionalId", getter.name);
            assertTrue(getter.toString().contains("extractHasOptionalId(proto)"));
            assertTrue(getter.toString().contains("extractOptionalId(proto)"));
        }

        @Test
        @DisplayName("Proto2 required - getter without has-check")
        void proto2Required_getterWithoutHasCheck() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO2_REQUIRED,
                    false,  // inOneof
                    true,   // hasMethodExists (proto2 has it)
                    false,  // getterUsesHasCheck (required = always set)
                    false,  // nullable
                    FieldContract.DefaultValue.EMPTY_STRING
            );

            FieldMethodNames names = FieldMethodNames.from("requiredName");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            MethodSpec getter = generator.generateGetter();

            assertEquals("getRequiredName", getter.name);
            assertTrue(getter.toString().contains("return extractRequiredName(proto)"));
            assertFalse(getter.toString().contains("extractHas"));
        }
    }

    // ==================== Has Method Tests ====================

    @Nested
    @DisplayName("Has Method Generation")
    class HasMethodTests {

        @Test
        @DisplayName("Proto3 implicit scalar - no has method")
        void proto3ImplicitScalar_noHasMethod() {
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

            Optional<MethodSpec> hasMethod = generator.generateHasMethod();

            assertTrue(hasMethod.isEmpty());
        }

        @Test
        @DisplayName("Proto2 optional - has method exists")
        void proto2Optional_hasMethodExists() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO2_OPTIONAL,
                    false,  // inOneof
                    true,   // hasMethodExists
                    true,   // getterUsesHasCheck
                    true,   // nullable
                    FieldContract.DefaultValue.NULL
            );

            FieldMethodNames names = FieldMethodNames.from("optionalField");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            Optional<MethodSpec> hasMethod = generator.generateHasMethod();

            assertTrue(hasMethod.isPresent());
            MethodSpec method = hasMethod.get();
            assertEquals("hasOptionalField", method.name);
            assertEquals(TypeName.BOOLEAN, method.returnType);
            assertTrue(method.toString().contains("extractHasOptionalField(proto)"));
        }

        @Test
        @DisplayName("Message type - has method exists")
        void messageType_hasMethodExists() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.MESSAGE,
                    FieldPresence.PROTO3_IMPLICIT,
                    false,  // inOneof
                    true,   // hasMethodExists (messages always have has)
                    true,   // getterUsesHasCheck
                    true,   // nullable
                    FieldContract.DefaultValue.NULL
            );

            FieldMethodNames names = FieldMethodNames.from("nestedMessage");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, MESSAGE_TYPE, PROTO_TYPE);

            Optional<MethodSpec> hasMethod = generator.generateHasMethod();

            assertTrue(hasMethod.isPresent());
            assertEquals("hasNestedMessage", hasMethod.get().name);
        }

        @Test
        @DisplayName("Repeated field - no has method")
        void repeatedField_noHasMethod() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.REPEATED,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_IMPLICIT,
                    false, false, false, false,
                    FieldContract.DefaultValue.EMPTY_LIST
            );

            FieldMethodNames names = FieldMethodNames.from("items");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, LIST_STRING, PROTO_TYPE, STRING_TYPE);

            Optional<MethodSpec> hasMethod = generator.generateHasMethod();

            assertTrue(hasMethod.isEmpty());
        }
    }

    // ==================== Abstract Extract Methods Tests ====================

    @Nested
    @DisplayName("Abstract Extract Method Generation")
    class AbstractExtractTests {

        @Test
        @DisplayName("Extract method - correct signature")
        void extractMethod_correctSignature() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_IMPLICIT,
                    false, false, false, false,
                    FieldContract.DefaultValue.EMPTY_STRING
            );

            FieldMethodNames names = FieldMethodNames.from("title");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            MethodSpec extractMethod = generator.generateAbstractExtractMethod();

            assertEquals("extractTitle", extractMethod.name);
            assertTrue(extractMethod.modifiers.contains(Modifier.PROTECTED));
            assertTrue(extractMethod.modifiers.contains(Modifier.ABSTRACT));
            assertEquals(STRING_TYPE, extractMethod.returnType);
            assertEquals(1, extractMethod.parameters.size());
            assertEquals(PROTO_TYPE, extractMethod.parameters.get(0).type);
            assertEquals("proto", extractMethod.parameters.get(0).name);
        }

        @Test
        @DisplayName("ExtractHas method - generated when has exists")
        void extractHasMethod_generatedWhenHasExists() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.MESSAGE,
                    FieldPresence.PROTO2_OPTIONAL,
                    false, true, true, true,
                    FieldContract.DefaultValue.NULL
            );

            FieldMethodNames names = FieldMethodNames.from("config");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, MESSAGE_TYPE, PROTO_TYPE);

            Optional<MethodSpec> extractHas = generator.generateAbstractExtractHasMethod();

            assertTrue(extractHas.isPresent());
            MethodSpec method = extractHas.get();
            assertEquals("extractHasConfig", method.name);
            assertTrue(method.modifiers.contains(Modifier.PROTECTED));
            assertTrue(method.modifiers.contains(Modifier.ABSTRACT));
            assertEquals(TypeName.BOOLEAN, method.returnType);
        }

        @Test
        @DisplayName("ExtractHas method - not generated when has doesn't exist")
        void extractHasMethod_notGeneratedWhenNoHas() {
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

            Optional<MethodSpec> extractHas = generator.generateAbstractExtractHasMethod();

            assertTrue(extractHas.isEmpty());
        }
    }

    // ==================== Builder Methods Tests ====================

    @Nested
    @DisplayName("Builder Method Generation")
    class BuilderMethodTests {

        @Test
        @DisplayName("Singular non-nullable - only set method")
        void singularNonNullable_onlySetMethod() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_IMPLICIT,
                    false, false, false, false,
                    FieldContract.DefaultValue.EMPTY_STRING
            );

            FieldMethodNames names = FieldMethodNames.from("name");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            List<MethodSpec> methods = generator.generateBuilderMethods(BUILDER_TYPE);

            assertEquals(1, methods.size());
            MethodSpec setMethod = methods.get(0);
            assertEquals("setName", setMethod.name);
            assertEquals(BUILDER_TYPE, setMethod.returnType);
            assertTrue(setMethod.toString().contains("doSetName(name)"));
            assertTrue(setMethod.toString().contains("return this"));
        }

        @Test
        @DisplayName("Singular nullable - set and clear methods")
        void singularNullable_setAndClearMethods() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.MESSAGE,
                    FieldPresence.PROTO2_OPTIONAL,
                    false, true, true, true,
                    FieldContract.DefaultValue.NULL
            );

            FieldMethodNames names = FieldMethodNames.from("config");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, MESSAGE_TYPE, PROTO_TYPE);

            List<MethodSpec> methods = generator.generateBuilderMethods(BUILDER_TYPE);

            assertEquals(2, methods.size());

            MethodSpec setMethod = methods.stream()
                    .filter(m -> m.name.equals("setConfig"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(setMethod.toString().contains("doSetConfig(config)"));

            MethodSpec clearMethod = methods.stream()
                    .filter(m -> m.name.equals("clearConfig"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(clearMethod.toString().contains("doClearConfig()"));
        }

        @Test
        @DisplayName("Repeated field - add, addAll, set, clear methods")
        void repeatedField_allMethods() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.REPEATED,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_IMPLICIT,
                    false, false, false, false,
                    FieldContract.DefaultValue.EMPTY_LIST
            );

            FieldMethodNames names = FieldMethodNames.from("tags");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, LIST_STRING, PROTO_TYPE, STRING_TYPE);

            List<MethodSpec> methods = generator.generateBuilderMethods(BUILDER_TYPE);

            assertEquals(4, methods.size());

            // Check method names
            List<String> methodNames = methods.stream()
                    .map(m -> m.name)
                    .toList();
            assertTrue(methodNames.contains("addTags"));
            assertTrue(methodNames.contains("addAllTags"));
            assertTrue(methodNames.contains("setTags"));
            assertTrue(methodNames.contains("clearTags"));

            // Check add method signature
            MethodSpec addMethod = methods.stream()
                    .filter(m -> m.name.equals("addTags"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(1, addMethod.parameters.size());
            assertEquals(STRING_TYPE, addMethod.parameters.get(0).type);

            // Check addAll method signature
            MethodSpec addAllMethod = methods.stream()
                    .filter(m -> m.name.equals("addAllTags"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(LIST_STRING, addAllMethod.parameters.get(0).type);
        }
    }

    // ==================== Abstract Builder Methods Tests ====================

    @Nested
    @DisplayName("Abstract Builder Method Generation")
    class AbstractBuilderMethodTests {

        @Test
        @DisplayName("Singular non-nullable - only doSet method")
        void singularNonNullable_onlyDoSetMethod() {
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

            List<MethodSpec> methods = generator.generateAbstractBuilderMethods();

            assertEquals(1, methods.size());
            MethodSpec doSet = methods.get(0);
            assertEquals("doSetCount", doSet.name);
            assertTrue(doSet.modifiers.contains(Modifier.PROTECTED));
            assertTrue(doSet.modifiers.contains(Modifier.ABSTRACT));
        }

        @Test
        @DisplayName("Singular nullable - doSet and doClear methods")
        void singularNullable_doSetAndDoClearMethods() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO2_OPTIONAL,
                    false, true, true, true,
                    FieldContract.DefaultValue.NULL
            );

            FieldMethodNames names = FieldMethodNames.from("label");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            List<MethodSpec> methods = generator.generateAbstractBuilderMethods();

            assertEquals(2, methods.size());
            List<String> methodNames = methods.stream().map(m -> m.name).toList();
            assertTrue(methodNames.contains("doSetLabel"));
            assertTrue(methodNames.contains("doClearLabel"));
        }

        @Test
        @DisplayName("Repeated field - doAdd, doAddAll, doSet, doClear methods")
        void repeatedField_allDoMethods() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.REPEATED,
                    FieldTypeCategory.MESSAGE,
                    FieldPresence.PROTO3_IMPLICIT,
                    false, false, false, false,
                    FieldContract.DefaultValue.EMPTY_LIST
            );

            TypeName listMessage = ParameterizedTypeName.get(
                    ClassName.get(List.class), MESSAGE_TYPE);
            FieldMethodNames names = FieldMethodNames.from("items");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, listMessage, PROTO_TYPE, MESSAGE_TYPE);

            List<MethodSpec> methods = generator.generateAbstractBuilderMethods();

            assertEquals(4, methods.size());
            List<String> methodNames = methods.stream().map(m -> m.name).toList();
            assertTrue(methodNames.contains("doAddItems"));
            assertTrue(methodNames.contains("doAddAllItems"));
            assertTrue(methodNames.contains("doSetItems"));
            assertTrue(methodNames.contains("doClearItems"));

            // Check doAdd parameter type
            MethodSpec doAdd = methods.stream()
                    .filter(m -> m.name.equals("doAddItems"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(MESSAGE_TYPE, doAdd.parameters.get(0).type);
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Oneof field - has method and nullable")
        void oneofField_hasMethodAndNullable() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_IMPLICIT,
                    true,   // inOneof
                    true,   // hasMethodExists (oneof always has)
                    true,   // getterUsesHasCheck
                    true,   // nullable (oneof always nullable)
                    FieldContract.DefaultValue.NULL
            );

            FieldMethodNames names = FieldMethodNames.from("oneofValue");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            // Should have has method
            assertTrue(generator.generateHasMethod().isPresent());

            // Getter should use has-check
            MethodSpec getter = generator.generateGetter();
            assertTrue(getter.toString().contains("extractHasOneofValue"));

            // Should have clear method (nullable)
            List<MethodSpec> builderMethods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertTrue(builderMethods.stream().anyMatch(m -> m.name.equals("clearOneofValue")));
        }

        @Test
        @DisplayName("Map field - treated as repeated")
        void mapField_treatedAsRepeated() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.MAP,
                    FieldTypeCategory.MESSAGE,
                    FieldPresence.PROTO3_IMPLICIT,
                    false, false, false, false,
                    FieldContract.DefaultValue.EMPTY_MAP
            );

            TypeName mapType = ParameterizedTypeName.get(
                    ClassName.get(java.util.Map.class),
                    STRING_TYPE, MESSAGE_TYPE);
            FieldMethodNames names = FieldMethodNames.from("metadata");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, mapType, PROTO_TYPE);

            // No has method for maps
            assertTrue(generator.generateHasMethod().isEmpty());

            // Should have all repeated-style methods
            List<MethodSpec> methods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertEquals(4, methods.size());
        }

        @Test
        @DisplayName("Accessor methods - contract and names")
        void accessorMethods_contractAndNames() {
            FieldContract contract = new FieldContract(
                    FieldCardinality.SINGULAR,
                    FieldTypeCategory.SCALAR_STRING,
                    FieldPresence.PROTO3_IMPLICIT,
                    false, false, false, false,
                    FieldContract.DefaultValue.EMPTY_STRING
            );

            FieldMethodNames names = FieldMethodNames.from("field");
            ContractBasedFieldGenerator generator = new ContractBasedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            assertSame(contract, generator.contract());
            assertSame(names, generator.names());
        }
    }
}
