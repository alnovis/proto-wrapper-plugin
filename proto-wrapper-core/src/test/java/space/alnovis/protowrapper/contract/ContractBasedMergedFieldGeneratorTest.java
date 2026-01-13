package space.alnovis.protowrapper.contract;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MergedField;
import space.alnovis.protowrapper.model.ProtoSyntax;

import java.util.List;
import java.util.Map;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContractBasedMergedFieldGenerator}.
 */
class ContractBasedMergedFieldGeneratorTest {

    private static final TypeName PROTO_TYPE = ClassName.get("com.example", "MyProto");
    private static final TypeName BUILDER_TYPE = ClassName.get("com.example", "MyBuilder");
    private static final TypeName STRING_TYPE = ClassName.get(String.class);
    private static final TypeName MESSAGE_TYPE = ClassName.get("com.example", "Nested");

    // ==================== Basic Generation Tests ====================

    @Nested
    @DisplayName("Basic Generation")
    class BasicGenerationTests {

        @Test
        @DisplayName("Single version - generates methods correctly")
        void singleVersion_generatesCorrectly() {
            FieldInfo fieldInfo = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);
            FieldMethodNames names = FieldMethodNames.from("name");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            // Getter
            MethodSpec getter = generator.generateGetter();
            assertEquals("getName", getter.name);

            // Extract method
            MethodSpec extract = generator.generateAbstractExtractMethod();
            assertEquals("extractName", extract.name);
            assertTrue(extract.modifiers.contains(javax.lang.model.element.Modifier.ABSTRACT));

            // Builder methods
            List<MethodSpec> builderMethods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertFalse(builderMethods.isEmpty());
        }

        @Test
        @DisplayName("Two versions same type - unified contract used")
        void twoVersionsSameType() {
            FieldInfo v1 = new FieldInfo("count", "count", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL, null);
            FieldInfo v2 = new FieldInfo("count", "count", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .addVersionField("v2", v2)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);
            FieldMethodNames names = FieldMethodNames.from("count");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, TypeName.INT, PROTO_TYPE);

            // Should work with unified contract
            assertNotNull(generator.generateGetter());
            assertNotNull(generator.generateAbstractExtractMethod());
        }
    }

    // ==================== Has Method Tests ====================

    @Nested
    @DisplayName("Has Method Generation")
    class HasMethodTests {

        @Test
        @DisplayName("Message field - has method generated")
        void messageField_hasMethodGenerated() {
            FieldInfo v1 = new FieldInfo("config", "config", 1, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL, ".example.Config");
            FieldInfo v2 = new FieldInfo("config", "config", 1, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL, ".example.Config");

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .addVersionField("v2", v2)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);
            FieldMethodNames names = FieldMethodNames.from("config");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, MESSAGE_TYPE, PROTO_TYPE);

            assertTrue(generator.generateHasMethod().isPresent());
            assertEquals("hasConfig", generator.generateHasMethod().get().name);

            assertTrue(generator.generateAbstractExtractHasMethod().isPresent());
            assertEquals("extractHasConfig", generator.generateAbstractExtractHasMethod().get().name);
        }

        @Test
        @DisplayName("Proto3 implicit scalar - no has method")
        void proto3ImplicitScalar_noHasMethod() {
            FieldInfo v1 = new FieldInfo("count", "count", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .build();

            Map<String, ProtoSyntax> syntax = Map.of("v1", ProtoSyntax.PROTO3);
            MergedFieldContract contract = MergedFieldContract.from(merged, syntax);
            FieldMethodNames names = FieldMethodNames.from("count");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, TypeName.INT, PROTO_TYPE);

            // Proto3 scalar without optional - no has method
            assertTrue(generator.generateHasMethod().isEmpty());
            assertTrue(generator.generateAbstractExtractHasMethod().isEmpty());
        }
    }

    // ==================== Getter Pattern Tests ====================

    @Nested
    @DisplayName("Getter Pattern")
    class GetterPatternTests {

        @Test
        @DisplayName("Nullable field - getter uses has-check")
        void nullableField_getterUsesHasCheck() {
            FieldInfo v1 = new FieldInfo("config", "config", 1, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL, ".example.Config");

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);
            FieldMethodNames names = FieldMethodNames.from("config");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, MESSAGE_TYPE, PROTO_TYPE);

            MethodSpec getter = generator.generateGetter();
            String code = getter.toString();

            assertTrue(code.contains("extractHasConfig"));
            assertTrue(code.contains("extractConfig"));
            assertTrue(code.contains("null"));
        }

        @Test
        @DisplayName("Non-nullable field - getter returns directly")
        void nonNullableField_getterReturnsDirect() {
            FieldInfo v1 = new FieldInfo("count", "count", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .build();

            Map<String, ProtoSyntax> syntax = Map.of("v1", ProtoSyntax.PROTO3);
            MergedFieldContract contract = MergedFieldContract.from(merged, syntax);
            FieldMethodNames names = FieldMethodNames.from("count");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, TypeName.INT, PROTO_TYPE);

            MethodSpec getter = generator.generateGetter();
            String code = getter.toString();

            assertTrue(code.contains("return extractCount(proto)"));
            assertFalse(code.contains("extractHas"));
        }
    }

    // ==================== Builder Methods Tests ====================

    @Nested
    @DisplayName("Builder Methods")
    class BuilderMethodTests {

        @Test
        @DisplayName("No conflict - builder methods generated")
        void noConflict_builderMethodsGenerated() {
            FieldInfo v1 = new FieldInfo("name", "name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);
            FieldMethodNames names = FieldMethodNames.from("name");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            List<MethodSpec> methods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertFalse(methods.isEmpty());

            List<String> methodNames = methods.stream().map(m -> m.name).toList();
            assertTrue(methodNames.contains("setName"));
        }

        @Test
        @DisplayName("WIDENING conflict - no builder methods")
        void wideningConflict_noBuilderMethods() {
            FieldInfo v1 = new FieldInfo("count", "count", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL, null);
            FieldInfo v2 = new FieldInfo("count", "count", 1, Type.TYPE_INT64, Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .addVersionField("v2", v2)
                    .conflictType(MergedField.ConflictType.WIDENING)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);
            FieldMethodNames names = FieldMethodNames.from("count");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, TypeName.LONG, PROTO_TYPE);

            List<MethodSpec> methods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertTrue(methods.isEmpty(), "WIDENING conflict should skip builder setters");

            List<MethodSpec> abstractMethods = generator.generateAbstractBuilderMethods();
            assertTrue(abstractMethods.isEmpty(), "WIDENING conflict should skip abstract builder methods");
        }

        @Test
        @DisplayName("Repeated field - all 4 builder methods")
        void repeatedField_allBuilderMethods() {
            FieldInfo v1 = new FieldInfo("tags", "tags", 1, Type.TYPE_STRING, Label.LABEL_REPEATED, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);
            FieldMethodNames names = FieldMethodNames.from("tags");
            TypeName listString = ParameterizedTypeName.get(ClassName.get(List.class), STRING_TYPE);

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, listString, PROTO_TYPE, STRING_TYPE);

            List<MethodSpec> methods = generator.generateBuilderMethods(BUILDER_TYPE);
            assertEquals(4, methods.size());

            List<String> methodNames = methods.stream().map(m -> m.name).toList();
            assertTrue(methodNames.contains("addTags"));
            assertTrue(methodNames.contains("addAllTags"));
            assertTrue(methodNames.contains("setTags"));
            assertTrue(methodNames.contains("clearTags"));
        }
    }

    // ==================== Version Awareness Tests ====================

    @Nested
    @DisplayName("Version Awareness")
    class VersionAwarenessTests {

        @Test
        @DisplayName("isPresentIn - checks field presence")
        void isPresentIn_checksPresence() {
            FieldInfo v1 = new FieldInfo("name", "name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);
            FieldMethodNames names = FieldMethodNames.from("name");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            assertTrue(generator.isPresentIn("v1"));
            assertFalse(generator.isPresentIn("v2"));
        }

        @Test
        @DisplayName("hasMethodAvailableIn - version-specific has support")
        void hasMethodAvailableIn_versionSpecific() {
            // v1: proto2 (has has*())
            FieldInfo v1 = new FieldInfo("name", "name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL, null);
            // v2: proto3 implicit (no has*())
            FieldInfo v2 = new FieldInfo("name", "name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .addVersionField("v2", v2)
                    .build();

            Map<String, ProtoSyntax> syntax = Map.of(
                    "v1", ProtoSyntax.PROTO2,
                    "v2", ProtoSyntax.PROTO3
            );
            MergedFieldContract contract = MergedFieldContract.from(merged, syntax);
            FieldMethodNames names = FieldMethodNames.from("name");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            assertTrue(generator.hasMethodAvailableIn("v1"), "Proto2 should have has*()");
            assertFalse(generator.hasMethodAvailableIn("v2"), "Proto3 implicit scalar should not have has*()");
        }

        @Test
        @DisplayName("presentInVersions - returns all versions")
        void presentInVersions_returnsAll() {
            FieldInfo v1 = new FieldInfo("name", "name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL, null);
            FieldInfo v2 = new FieldInfo("name", "name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .addVersionField("v2", v2)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);
            FieldMethodNames names = FieldMethodNames.from("name");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            assertEquals(2, generator.presentInVersions().size());
            assertTrue(generator.presentInVersions().contains("v1"));
            assertTrue(generator.presentInVersions().contains("v2"));
        }
    }

    // ==================== Accessor Methods Tests ====================

    @Nested
    @DisplayName("Accessor Methods")
    class AccessorMethodTests {

        @Test
        @DisplayName("contract() returns merged contract")
        void contract_returnsMergedContract() {
            FieldInfo v1 = new FieldInfo("name", "name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);
            FieldMethodNames names = FieldMethodNames.from("name");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            assertSame(contract, generator.contract());
        }

        @Test
        @DisplayName("unifiedContract() returns unified contract")
        void unifiedContract_returnsUnified() {
            FieldInfo v1 = new FieldInfo("name", "name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);
            FieldMethodNames names = FieldMethodNames.from("name");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            assertSame(contract.unified(), generator.unifiedContract());
        }

        @Test
        @DisplayName("names() returns method names")
        void names_returnsMethodNames() {
            FieldInfo v1 = new FieldInfo("name", "name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);
            FieldMethodNames names = FieldMethodNames.from("name");

            ContractBasedMergedFieldGenerator generator = new ContractBasedMergedFieldGenerator(
                    contract, names, STRING_TYPE, PROTO_TYPE);

            assertSame(names, generator.names());
        }
    }
}
