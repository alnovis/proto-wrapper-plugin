package io.alnovis.protowrapper.contract;

import com.squareup.javapoet.TypeVariableName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.generator.GenerationContext;
import io.alnovis.protowrapper.generator.GeneratorConfig;
import io.alnovis.protowrapper.generator.conflict.ProcessingContext;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;
import io.alnovis.protowrapper.model.MergedSchema;

import java.nio.file.Path;
import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for contract support in {@link ProcessingContext}.
 */
class ProcessingContextContractIntegrationTest {

    private MergedSchema schema;
    private GeneratorConfig config;
    private GenerationContext genCtx;
    private MergedMessage message;

    @BeforeEach
    void setUp() {
        schema = new MergedSchema(List.of("v1", "v2"));
        config = GeneratorConfig.builder()
                .outputDirectory(Path.of("target/test-output"))
                .protoPackagePattern("com.example.proto.{version}")
                .apiPackage("com.example.api")
                .implPackagePattern("com.example.impl.{version}")
                .build();
        genCtx = GenerationContext.create(schema, config);
        message = new MergedMessage("TestMessage");
        schema.addMessage(message);
    }

    private ProcessingContext createContext() {
        TypeVariableName protoType = TypeVariableName.get("P");
        return ProcessingContext.forAbstract(message, protoType, genCtx, config);
    }

    // ==================== Contract Provider Access ====================

    @Nested
    @DisplayName("Contract Provider Access")
    class ContractProviderAccessTests {

        @Test
        @DisplayName("contractProvider returns singleton")
        void contractProvider_returnsSingleton() {
            ProcessingContext ctx = createContext();

            ContractProvider provider1 = ctx.contractProvider();
            ContractProvider provider2 = ctx.contractProvider();

            assertSame(provider1, provider2);
            assertSame(ContractProvider.getInstance(), provider1);
        }
    }

    // ==================== Contract Retrieval ====================

    @Nested
    @DisplayName("Contract Retrieval")
    class ContractRetrievalTests {

        @Test
        @DisplayName("getContractFor returns valid contract")
        void getContractFor_returnsValidContract() {
            FieldInfo fieldInfo = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();
            message.addField(field);

            ProcessingContext ctx = createContext();
            MergedFieldContract contract = ctx.getContractFor(field);

            assertNotNull(contract);
            assertEquals(1, contract.versionCount());
            assertTrue(contract.isPresentIn("v1"));
        }

        @Test
        @DisplayName("getContractFor returns cached contract")
        void getContractFor_returnsCachedContract() {
            FieldInfo fieldInfo = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();
            message.addField(field);

            ProcessingContext ctx = createContext();
            MergedFieldContract contract1 = ctx.getContractFor(field);
            MergedFieldContract contract2 = ctx.getContractFor(field);

            assertSame(contract1, contract2);
        }
    }

    // ==================== Field Names ====================

    @Nested
    @DisplayName("Field Names")
    class FieldNamesTests {

        @Test
        @DisplayName("getFieldNames returns correct names")
        void getFieldNames_correctNames() {
            FieldInfo fieldInfo = new FieldInfo(
                    "user_name", "userName", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();
            message.addField(field);

            ProcessingContext ctx = createContext();
            FieldMethodNames names = ctx.getFieldNames(field);

            assertEquals("getUserName", names.getterName());
            assertEquals("setUserName", names.setterName());
            assertEquals("hasUserName", names.hasMethodName());
            assertEquals("clearUserName", names.clearMethodName());
        }
    }

    // ==================== Has Method Generation ====================

    @Nested
    @DisplayName("Has Method Generation")
    class HasMethodGenerationTests {

        @Test
        @DisplayName("shouldGenerateHasMethod true for message field")
        void shouldGenerateHasMethod_messageField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "config", "config", 1, Type.TYPE_MESSAGE,
                    Label.LABEL_OPTIONAL, ".example.Config");

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();
            message.addField(field);

            ProcessingContext ctx = createContext();

            assertTrue(ctx.shouldGenerateHasMethod(field));
        }

        @Test
        @DisplayName("shouldGenerateHasMethod false for repeated field")
        void shouldGenerateHasMethod_repeatedField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "tags", "tags", 1, Type.TYPE_STRING,
                    Label.LABEL_REPEATED, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();
            message.addField(field);

            ProcessingContext ctx = createContext();

            assertFalse(ctx.shouldGenerateHasMethod(field));
        }
    }

    // ==================== Has Check Pattern ====================

    @Nested
    @DisplayName("Has Check Pattern")
    class HasCheckPatternTests {

        @Test
        @DisplayName("shouldUseHasCheckInGetter false for message field (returns default instance)")
        void shouldUseHasCheckInGetter_messageField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "config", "config", 1, Type.TYPE_MESSAGE,
                    Label.LABEL_OPTIONAL, ".example.Config");

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();
            message.addField(field);

            ProcessingContext ctx = createContext();

            assertFalse(ctx.shouldUseHasCheckInGetter(field));
        }

        @Test
        @DisplayName("shouldUseHasCheckInGetter false for repeated field")
        void shouldUseHasCheckInGetter_repeatedField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "tags", "tags", 1, Type.TYPE_STRING,
                    Label.LABEL_REPEATED, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();
            message.addField(field);

            ProcessingContext ctx = createContext();

            assertFalse(ctx.shouldUseHasCheckInGetter(field));
        }
    }

    // ==================== Contract Content Verification ====================

    @Nested
    @DisplayName("Contract Content Verification")
    class ContractContentTests {

        @Test
        @DisplayName("Contract has correct cardinality for singular field")
        void contract_singularCardinality() {
            FieldInfo fieldInfo = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();
            message.addField(field);

            ProcessingContext ctx = createContext();
            MergedFieldContract contract = ctx.getContractFor(field);

            assertEquals(FieldCardinality.SINGULAR, contract.unified().cardinality());
        }

        @Test
        @DisplayName("Contract has correct cardinality for repeated field")
        void contract_repeatedCardinality() {
            FieldInfo fieldInfo = new FieldInfo(
                    "tags", "tags", 1, Type.TYPE_STRING,
                    Label.LABEL_REPEATED, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();
            message.addField(field);

            ProcessingContext ctx = createContext();
            MergedFieldContract contract = ctx.getContractFor(field);

            assertEquals(FieldCardinality.REPEATED, contract.unified().cardinality());
        }

        @Test
        @DisplayName("Contract has correct type category for string")
        void contract_stringTypeCategory() {
            FieldInfo fieldInfo = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();
            message.addField(field);

            ProcessingContext ctx = createContext();
            MergedFieldContract contract = ctx.getContractFor(field);

            assertEquals(FieldTypeCategory.SCALAR_STRING, contract.unified().typeCategory());
        }

        @Test
        @DisplayName("Contract has correct type category for message")
        void contract_messageTypeCategory() {
            FieldInfo fieldInfo = new FieldInfo(
                    "config", "config", 1, Type.TYPE_MESSAGE,
                    Label.LABEL_OPTIONAL, ".example.Config");

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();
            message.addField(field);

            ProcessingContext ctx = createContext();
            MergedFieldContract contract = ctx.getContractFor(field);

            assertEquals(FieldTypeCategory.MESSAGE, contract.unified().typeCategory());
        }

        @Test
        @DisplayName("Contract has correct type category for enum")
        void contract_enumTypeCategory() {
            FieldInfo fieldInfo = new FieldInfo(
                    "status", "status", 1, Type.TYPE_ENUM,
                    Label.LABEL_OPTIONAL, ".example.Status");

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();
            message.addField(field);

            ProcessingContext ctx = createContext();
            MergedFieldContract contract = ctx.getContractFor(field);

            assertEquals(FieldTypeCategory.ENUM, contract.unified().typeCategory());
        }
    }
}
