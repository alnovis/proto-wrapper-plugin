package io.alnovis.protowrapper.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MergedField;
import io.alnovis.protowrapper.model.ProtoSyntax;

import java.util.Map;
import java.util.Set;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MergedFieldContract}.
 */
class MergedFieldContractTest {

    // ==================== Single Version Tests ====================

    @Nested
    @DisplayName("Single Version")
    class SingleVersionTests {

        @Test
        @DisplayName("Single version - unified equals version contract")
        void singleVersion_unifiedEqualsVersionContract() {
            FieldInfo fieldInfo = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", fieldInfo)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            assertEquals(1, contract.versionCount());
            assertTrue(contract.isPresentIn("v1"));
            assertFalse(contract.isPresentIn("v2"));

            // Unified should match v1 contract
            FieldContract unified = contract.unified();
            FieldContract v1Contract = contract.contractForVersion("v1").orElseThrow();
            assertEquals(v1Contract.cardinality(), unified.cardinality());
            assertEquals(v1Contract.typeCategory(), unified.typeCategory());
        }
    }

    // ==================== Multi-Version Merge Tests ====================

    @Nested
    @DisplayName("Multi-Version Merging")
    class MultiVersionMergeTests {

        @Test
        @DisplayName("Two versions same type - contracts merged correctly")
        void twoVersionsSameType() {
            FieldInfo v1Field = new FieldInfo(
                    "count", "count", 1, Type.TYPE_INT32,
                    Label.LABEL_OPTIONAL, null);
            FieldInfo v2Field = new FieldInfo(
                    "count", "count", 1, Type.TYPE_INT32,
                    Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            assertEquals(2, contract.versionCount());
            assertTrue(contract.isPresentIn("v1"));
            assertTrue(contract.isPresentIn("v2"));
            assertFalse(contract.hasConflict());
            assertEquals(MergedField.ConflictType.NONE, contract.conflictType());
        }

        @Test
        @DisplayName("Field present in only one version")
        void fieldPresentInOneVersion() {
            FieldInfo v2Field = new FieldInfo(
                    "newField", "newField", 5, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v2", v2Field)
                    .build();

            Map<String, ProtoSyntax> syntaxMap = Map.of(
                    "v1", ProtoSyntax.PROTO3,
                    "v2", ProtoSyntax.PROTO3
            );

            MergedFieldContract contract = MergedFieldContract.from(merged, syntaxMap);

            assertEquals(1, contract.versionCount());
            assertFalse(contract.isPresentIn("v1"));
            assertTrue(contract.isPresentIn("v2"));
            assertFalse(contract.isUniversal(Set.of("v1", "v2")));
            assertTrue(contract.isUniversal(Set.of("v2")));
        }
    }

    // ==================== Has Method Merge Rules ====================

    @Nested
    @DisplayName("Has Method Merge Rules")
    class HasMethodMergeTests {

        @Test
        @DisplayName("Proto3 message - has method in unified")
        void proto3Message_hasMethodInUnified() {
            FieldInfo v1Field = new FieldInfo(
                    "nested", "nested", 1, Type.TYPE_MESSAGE,
                    Label.LABEL_OPTIONAL, ".example.Nested");
            FieldInfo v2Field = new FieldInfo(
                    "nested", "nested", 1, Type.TYPE_MESSAGE,
                    Label.LABEL_OPTIONAL, ".example.Nested");

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            // Messages always have has method
            assertTrue(contract.unified().hasMethodExists());
            assertTrue(contract.hasMethodAvailableIn("v1"));
            assertTrue(contract.hasMethodAvailableIn("v2"));
        }

        @Test
        @DisplayName("Proto3 implicit scalar - no has method in unified")
        void proto3ImplicitScalar_noHasMethodInUnified() {
            // Proto3 scalars without 'optional' don't have has*()
            FieldInfo v1Field = new FieldInfo(
                    "count", "count", 1, Type.TYPE_INT32,
                    Label.LABEL_OPTIONAL, null);
            FieldInfo v2Field = new FieldInfo(
                    "count", "count", 1, Type.TYPE_INT32,
                    Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .build();

            Map<String, ProtoSyntax> syntaxMap = Map.of(
                    "v1", ProtoSyntax.PROTO3,
                    "v2", ProtoSyntax.PROTO3
            );

            MergedFieldContract contract = MergedFieldContract.from(merged, syntaxMap);

            // Proto3 implicit scalars don't have has method
            // Note: This depends on FieldInfo.supportsHasMethod() and FieldContract logic
            assertFalse(contract.unified().hasMethodExists());
        }

        @Test
        @DisplayName("Mixed proto2/proto3 - has method only if all support")
        void mixedProto2Proto3() {
            // v1 is proto2 (has has*())
            FieldInfo v1Field = new FieldInfo(
                    "value", "value", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            // v2 is proto3 implicit (no has*())
            FieldInfo v2Field = new FieldInfo(
                    "value", "value", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .build();

            Map<String, ProtoSyntax> syntaxMap = Map.of(
                    "v1", ProtoSyntax.PROTO2,
                    "v2", ProtoSyntax.PROTO3
            );

            MergedFieldContract contract = MergedFieldContract.from(merged, syntaxMap);

            // v1 (proto2) has has*(), but v2 (proto3 implicit) doesn't
            // hasMethodAvailableIn checks individual version contracts
            assertTrue(contract.hasMethodAvailableIn("v1"));
            assertFalse(contract.hasMethodAvailableIn("v2"));

            // Unified: has method only if ALL versions support it
            // Since v2 doesn't, unified should be false
            assertFalse(contract.unified().hasMethodExists());
        }
    }

    // ==================== Nullable Merge Rules ====================

    @Nested
    @DisplayName("Nullable Merge Rules")
    class NullableMergeTests {

        @Test
        @DisplayName("Any nullable - unified is nullable")
        void anyNullable_unifiedIsNullable() {
            // v1: proto2 optional (nullable)
            FieldInfo v1Field = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            // v2: proto3 implicit (not nullable for scalars)
            FieldInfo v2Field = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .build();

            Map<String, ProtoSyntax> syntaxMap = Map.of(
                    "v1", ProtoSyntax.PROTO2,
                    "v2", ProtoSyntax.PROTO3
            );

            MergedFieldContract contract = MergedFieldContract.from(merged, syntaxMap);

            // v1 is nullable (proto2 optional)
            assertTrue(contract.contractForVersion("v1").get().nullable());

            // Unified: nullable if ANY version is nullable
            assertTrue(contract.unified().nullable());
        }

        @Test
        @DisplayName("Message field - returns default instance, not nullable")
        void messageField_returnsDefaultInstance() {
            FieldInfo v1Field = new FieldInfo(
                    "config", "config", 1, Type.TYPE_MESSAGE,
                    Label.LABEL_OPTIONAL, ".example.Config");

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            assertFalse(contract.unified().nullable());
            assertFalse(contract.unified().getterUsesHasCheck());
        }
    }

    // ==================== Cardinality Merge Rules ====================

    @Nested
    @DisplayName("Cardinality Merge Rules")
    class CardinalityMergeTests {

        @Test
        @DisplayName("REPEATED_SINGLE conflict - unified is REPEATED")
        void repeatedSingleConflict_unifiedIsRepeated() {
            // v1: singular
            FieldInfo v1Field = new FieldInfo(
                    "items", "items", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            // v2: repeated
            FieldInfo v2Field = new FieldInfo(
                    "items", "items", 1, Type.TYPE_STRING,
                    Label.LABEL_REPEATED, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .conflictType(MergedField.ConflictType.REPEATED_SINGLE)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            assertEquals(FieldCardinality.REPEATED, contract.unified().cardinality());
            assertTrue(contract.hasConflict());
            assertEquals(MergedField.ConflictType.REPEATED_SINGLE, contract.conflictType());
        }

        @Test
        @DisplayName("Repeated field - no has method, not nullable")
        void repeatedField_noHasNotNullable() {
            FieldInfo v1Field = new FieldInfo(
                    "tags", "tags", 1, Type.TYPE_STRING,
                    Label.LABEL_REPEATED, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            assertEquals(FieldCardinality.REPEATED, contract.unified().cardinality());
            assertFalse(contract.unified().hasMethodExists());
            assertFalse(contract.unified().nullable());
            assertFalse(contract.unified().getterUsesHasCheck());
            assertEquals(FieldContract.DefaultValue.EMPTY_LIST, contract.unified().defaultValueWhenUnset());
        }
    }

    // ==================== Conflict Type Tests ====================

    @Nested
    @DisplayName("Conflict Type Handling")
    class ConflictTypeTests {

        @Test
        @DisplayName("No conflict - shouldSkipBuilderSetter false")
        void noConflict_shouldNotSkipSetter() {
            FieldInfo v1Field = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            assertFalse(contract.hasConflict());
            assertFalse(contract.shouldSkipBuilderSetter());
        }

        @Test
        @DisplayName("WIDENING conflict - shouldSkipBuilderSetter true")
        void wideningConflict_shouldSkipSetter() {
            FieldInfo v1Field = new FieldInfo(
                    "count", "count", 1, Type.TYPE_INT32,
                    Label.LABEL_OPTIONAL, null);
            FieldInfo v2Field = new FieldInfo(
                    "count", "count", 1, Type.TYPE_INT64,
                    Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .conflictType(MergedField.ConflictType.WIDENING)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            assertTrue(contract.hasConflict());
            assertTrue(contract.shouldSkipBuilderSetter());
        }

        @Test
        @DisplayName("OPTIONAL_REQUIRED conflict - shouldSkipBuilderSetter false")
        void optionalRequiredConflict_shouldNotSkipSetter() {
            FieldInfo v1Field = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);
            FieldInfo v2Field = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_REQUIRED, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .conflictType(MergedField.ConflictType.OPTIONAL_REQUIRED)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            assertTrue(contract.hasConflict());
            assertFalse(contract.shouldSkipBuilderSetter());
        }
    }

    // ==================== Describe Tests ====================

    @Nested
    @DisplayName("Describe Method")
    class DescribeTests {

        @Test
        @DisplayName("Describe includes all relevant info")
        void describe_includesAllInfo() {
            FieldInfo v1Field = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);
            FieldInfo v2Field = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            String description = contract.describe();

            assertTrue(description.contains("MergedFieldContract"));
            assertTrue(description.contains("unified"));
            assertTrue(description.contains("v1"));
            assertTrue(description.contains("v2"));
        }

        @Test
        @DisplayName("Describe shows conflict type when present")
        void describe_showsConflict() {
            FieldInfo v1Field = new FieldInfo(
                    "value", "value", 1, Type.TYPE_INT32,
                    Label.LABEL_OPTIONAL, null);
            FieldInfo v2Field = new FieldInfo(
                    "value", "value", 1, Type.TYPE_INT64,
                    Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .addVersionField("v2", v2Field)
                    .conflictType(MergedField.ConflictType.WIDENING)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            String description = contract.describe();

            assertTrue(description.contains("WIDENING"));
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Oneof field - inOneof in unified")
        void oneofField_inOneofInUnified() {
            FieldInfo v1Field = new FieldInfo(
                    "value", "value", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null, 0, "choice");

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            assertTrue(contract.unified().inOneof());
        }

        @Test
        @DisplayName("contractForVersion returns empty for missing version")
        void contractForVersion_emptyForMissing() {
            FieldInfo v1Field = new FieldInfo(
                    "name", "name", 1, Type.TYPE_STRING,
                    Label.LABEL_OPTIONAL, null);

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            assertTrue(contract.contractForVersion("v1").isPresent());
            assertTrue(contract.contractForVersion("v2").isEmpty());
        }

        @Test
        @DisplayName("hasMethodAvailableIn returns false for missing version")
        void hasMethodAvailableIn_falseForMissing() {
            FieldInfo v1Field = new FieldInfo(
                    "nested", "nested", 1, Type.TYPE_MESSAGE,
                    Label.LABEL_OPTIONAL, ".example.Nested");

            MergedField merged = MergedField.builder()
                    .addVersionField("v1", v1Field)
                    .build();

            MergedFieldContract contract = MergedFieldContract.from(merged);

            assertTrue(contract.hasMethodAvailableIn("v1"));
            assertFalse(contract.hasMethodAvailableIn("v2"));
        }
    }
}
