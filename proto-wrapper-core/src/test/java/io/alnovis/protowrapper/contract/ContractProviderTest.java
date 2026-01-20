package io.alnovis.protowrapper.contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.MergedMessage;

import java.util.Map;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContractProvider}.
 */
class ContractProviderTest {

    private ContractProvider provider;

    @BeforeEach
    void setUp() {
        provider = ContractProvider.getInstance();
        provider.clearCache();
    }

    // ==================== Basic Contract Tests ====================

    @Nested
    @DisplayName("Basic Contract Creation")
    class BasicContractTests {

        @Test
        @DisplayName("Get contract for simple string field")
        void getContract_simpleStringField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            MergedFieldContract contract = provider.getContract(field);

            assertNotNull(contract);
            assertEquals(1, contract.versionCount());
            assertTrue(contract.isPresentIn("v1"));
            assertEquals(FieldCardinality.SINGULAR, contract.unified().cardinality());
            assertEquals(FieldTypeCategory.SCALAR_STRING, contract.unified().typeCategory());
        }

        @Test
        @DisplayName("Get contract for message field")
        void getContract_messageField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "config", "config", 1, Type.TYPE_MESSAGE,
                    Label.LABEL_OPTIONAL, ".example.Config");

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            MergedFieldContract contract = provider.getContract(field);

            assertNotNull(contract);
            assertEquals(FieldTypeCategory.MESSAGE, contract.unified().typeCategory());
            assertTrue(contract.unified().hasMethodExists());
            assertTrue(contract.unified().nullable());
        }

        @Test
        @DisplayName("Get contract for repeated field")
        void getContract_repeatedField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "tags", "tags", 1, Type.TYPE_STRING,
                    Label.LABEL_REPEATED, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            MergedFieldContract contract = provider.getContract(field);

            assertNotNull(contract);
            assertEquals(FieldCardinality.REPEATED, contract.unified().cardinality());
            assertFalse(contract.unified().hasMethodExists());
            assertFalse(contract.unified().nullable());
        }

        @Test
        @DisplayName("Get contract for int field")
        void getContract_intField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "count", "count", 1, Type.TYPE_INT32,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            MergedFieldContract contract = provider.getContract(field);

            assertNotNull(contract);
            assertEquals(FieldCardinality.SINGULAR, contract.unified().cardinality());
            assertEquals(FieldTypeCategory.SCALAR_NUMERIC, contract.unified().typeCategory());
        }
    }

    // ==================== Multi-Version Tests ====================

    @Nested
    @DisplayName("Multi-Version Contracts")
    class MultiVersionTests {

        @Test
        @DisplayName("Contract for field present in two versions")
        void getContract_twoVersions() {
            FieldInfo v1Field = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);
            FieldInfo v2Field = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .build();

            MergedFieldContract contract = provider.getContract(field);

            assertEquals(2, contract.versionCount());
            assertTrue(contract.isPresentIn("v1"));
            assertTrue(contract.isPresentIn("v2"));
            assertFalse(contract.hasConflict());
        }

        @Test
        @DisplayName("Contract for widening conflict")
        void getContract_wideningConflict() {
            FieldInfo v1Field = new FieldInfo(
                    "count", "count", 1, Type.TYPE_INT32,
                    Label.LABEL_OPTIONAL, null);
            FieldInfo v2Field = new FieldInfo(
                    "count", "count", 1, Type.TYPE_INT64,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .conflictType(MergedField.ConflictType.WIDENING)
                    .build();

            MergedFieldContract contract = provider.getContract(field);

            assertTrue(contract.hasConflict());
            assertEquals(MergedField.ConflictType.WIDENING, contract.conflictType());
            assertTrue(contract.shouldSkipBuilderSetter());
        }
    }

    // ==================== Caching Tests ====================

    @Nested
    @DisplayName("Contract Caching")
    class CachingTests {

        @Test
        @DisplayName("Contracts are cached per message")
        void contractsCachedPerMessage() {
            FieldInfo fieldInfo = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            MergedMessage message = new MergedMessage("TestMessage");
            message.addField(field);

            // First call creates cache
            Map<MergedField, MergedFieldContract> contracts1 = provider.getContracts(message);
            // Second call returns same cache
            Map<MergedField, MergedFieldContract> contracts2 = provider.getContracts(message);

            assertSame(contracts1, contracts2);
        }

        @Test
        @DisplayName("Clear cache removes cached contracts")
        void clearCache_removesContracts() {
            FieldInfo fieldInfo = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            MergedMessage message = new MergedMessage("TestMessage");
            message.addField(field);

            Map<MergedField, MergedFieldContract> contracts1 = provider.getContracts(message);
            provider.clearCache();
            Map<MergedField, MergedFieldContract> contracts2 = provider.getContracts(message);

            assertNotSame(contracts1, contracts2);
        }

        @Test
        @DisplayName("Clear cache for specific message")
        void clearCacheForMessage() {
            FieldInfo fieldInfo = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            MergedMessage message1 = new MergedMessage("TestMessage1");
            message1.addField(field);

            MergedMessage message2 = new MergedMessage("TestMessage2");
            message2.addField(field);

            Map<MergedField, MergedFieldContract> contracts1 = provider.getContracts(message1);
            Map<MergedField, MergedFieldContract> contracts2 = provider.getContracts(message2);

            provider.clearCache(message1);

            Map<MergedField, MergedFieldContract> contracts1After = provider.getContracts(message1);
            Map<MergedField, MergedFieldContract> contracts2After = provider.getContracts(message2);

            assertNotSame(contracts1, contracts1After);
            assertSame(contracts2, contracts2After);
        }
    }

    // ==================== Utility Method Tests ====================

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethodTests {

        @Test
        @DisplayName("getMethodNames returns correct names")
        void getMethodNames_correctNames() {
            FieldInfo fieldInfo = new FieldInfo(
                    "user_name", "userName", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            FieldMethodNames names = provider.getMethodNames(field);

            assertEquals("getUserName", names.getterName());
            assertEquals("setUserName", names.setterName());
            assertEquals("hasUserName", names.hasMethodName());
        }

        @Test
        @DisplayName("shouldGenerateHasMethod for message field")
        void shouldGenerateHasMethod_messageField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "config", "config", 1, Type.TYPE_MESSAGE,
                    Label.LABEL_OPTIONAL, ".example.Config");

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            assertTrue(provider.shouldGenerateHasMethod(field));
        }

        @Test
        @DisplayName("shouldGenerateHasMethod for repeated field")
        void shouldGenerateHasMethod_repeatedField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "tags", "tags", 1, Type.TYPE_STRING,
                    Label.LABEL_REPEATED, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            assertFalse(provider.shouldGenerateHasMethod(field));
        }

        @Test
        @DisplayName("isNullable for message field")
        void isNullable_messageField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "config", "config", 1, Type.TYPE_MESSAGE,
                    Label.LABEL_OPTIONAL, ".example.Config");

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            assertTrue(provider.isNullable(field));
        }

        @Test
        @DisplayName("isNullable for repeated field")
        void isNullable_repeatedField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "tags", "tags", 1, Type.TYPE_STRING,
                    Label.LABEL_REPEATED, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            assertFalse(provider.isNullable(field));
        }

        @Test
        @DisplayName("shouldSkipBuilderSetter for widening conflict")
        void shouldSkipBuilderSetter_wideningConflict() {
            FieldInfo v1Field = new FieldInfo(
                    "count", "count", 1, Type.TYPE_INT32,
                    Label.LABEL_OPTIONAL, null);
            FieldInfo v2Field = new FieldInfo(
                    "count", "count", 1, Type.TYPE_INT64,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .conflictType(MergedField.ConflictType.WIDENING)
                    .build();

            assertTrue(provider.shouldSkipBuilderSetter(field));
        }

        @Test
        @DisplayName("shouldSkipBuilderSetter for normal field")
        void shouldSkipBuilderSetter_normalField() {
            FieldInfo fieldInfo = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField field = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            assertFalse(provider.shouldSkipBuilderSetter(field));
        }
    }

    // ==================== Singleton Tests ====================

    @Nested
    @DisplayName("Singleton")
    class SingletonTests {

        @Test
        @DisplayName("getInstance returns same instance")
        void getInstance_sameInstance() {
            ContractProvider instance1 = ContractProvider.getInstance();
            ContractProvider instance2 = ContractProvider.getInstance();

            assertSame(instance1, instance2);
        }
    }
}
