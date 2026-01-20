package io.alnovis.protowrapper.diff;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import io.alnovis.protowrapper.diff.model.*;
import io.alnovis.protowrapper.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for MergedSchemaDiffAdapter.
 *
 * <p>These tests verify the logic for detecting differences between two versions
 * based on presence in MergedSchema structures. The key logic being tested:</p>
 *
 * <ul>
 *   <li>!inV1 && inV2 → ADDED (present only in v2)</li>
 *   <li>inV1 && !inV2 → REMOVED (present only in v1)</li>
 *   <li>inV1 && inV2 → check for MODIFIED (present in both)</li>
 *   <li>!inV1 && !inV2 → UNCHANGED (not in either - edge case)</li>
 * </ul>
 */
class MergedSchemaDiffAdapterTest {

    private static final String V1 = "v1";
    private static final String V2 = "v2";

    private MergedSchema schema;
    private MergedSchemaDiffAdapter adapter;

    @BeforeEach
    void setUp() {
        schema = new MergedSchema(Arrays.asList(V1, V2));
        adapter = new MergedSchemaDiffAdapter(V1, V2);
    }

    // ========== Message-Level Tests ==========

    @Nested
    @DisplayName("Message addition detection")
    class MessageAdditionTests {

        @Test
        @DisplayName("Should detect message added in v2")
        void shouldDetectMessageAddedInV2() {
            // Message exists only in v2
            MergedMessage message = new MergedMessage("NewMessage");
            message.addVersion(V2);  // Only v2
            addSimpleField(message, "id", 1, Type.TYPE_INT64, V2);
            schema.addMessage(message);

            SchemaDiff diff = adapter.adapt(schema);

            assertThat(diff.getMessageDiffs()).hasSize(1);
            MessageDiff msgDiff = diff.getMessageDiffs().get(0);
            assertThat(msgDiff.messageName()).isEqualTo("NewMessage");
            assertThat(msgDiff.changeType()).isEqualTo(ChangeType.ADDED);
            assertThat(msgDiff.v1Message()).isNull();
            assertThat(msgDiff.v2Message()).isNotNull();
        }

        @Test
        @DisplayName("Should include added message in v2Message field")
        void shouldIncludeAddedMessageDetails() {
            MergedMessage message = new MergedMessage("NewMessage");
            message.addVersion(V2);
            addSimpleField(message, "name", 1, Type.TYPE_STRING, V2);
            addSimpleField(message, "value", 2, Type.TYPE_INT32, V2);
            schema.addMessage(message);

            SchemaDiff diff = adapter.adapt(schema);

            MessageDiff msgDiff = diff.getMessageDiffs().get(0);
            assertThat(msgDiff.v2Message()).isNotNull();
            assertThat(msgDiff.v2Message().getName()).isEqualTo("NewMessage");
        }
    }

    @Nested
    @DisplayName("Message removal detection")
    class MessageRemovalTests {

        @Test
        @DisplayName("Should detect message removed in v2")
        void shouldDetectMessageRemovedInV2() {
            // Message exists only in v1
            MergedMessage message = new MergedMessage("OldMessage");
            message.addVersion(V1);  // Only v1
            addSimpleField(message, "id", 1, Type.TYPE_INT64, V1);
            schema.addMessage(message);

            SchemaDiff diff = adapter.adapt(schema);

            assertThat(diff.getMessageDiffs()).hasSize(1);
            MessageDiff msgDiff = diff.getMessageDiffs().get(0);
            assertThat(msgDiff.messageName()).isEqualTo("OldMessage");
            assertThat(msgDiff.changeType()).isEqualTo(ChangeType.REMOVED);
            assertThat(msgDiff.v1Message()).isNotNull();
            assertThat(msgDiff.v2Message()).isNull();
        }

        @Test
        @DisplayName("Should track removed message as breaking change")
        void shouldTrackRemovedMessageAsBreakingChange() {
            MergedMessage message = new MergedMessage("RemovedMessage");
            message.addVersion(V1);
            addSimpleField(message, "data", 1, Type.TYPE_STRING, V1);
            schema.addMessage(message);

            SchemaDiff diff = adapter.adapt(schema);

            assertThat(diff.getBreakingChanges()).isNotEmpty();
            assertThat(diff.getBreakingChanges()).anyMatch(
                bc -> bc.type() == BreakingChange.Type.MESSAGE_REMOVED
            );
        }
    }

    @Nested
    @DisplayName("Message modification detection")
    class MessageModificationTests {

        @Test
        @DisplayName("Should detect message present in both versions with field changes")
        void shouldDetectMessageModifiedWithFieldChanges() {
            // Message exists in both versions but with different fields
            MergedMessage message = new MergedMessage("ModifiedMessage");
            message.addVersion(V1);
            message.addVersion(V2);

            // Field present in both
            addSimpleField(message, "id", 1, Type.TYPE_INT64, V1, V2);
            // Field removed in v2
            addSimpleField(message, "oldField", 2, Type.TYPE_STRING, V1);
            // Field added in v2
            addSimpleField(message, "newField", 3, Type.TYPE_STRING, V2);

            schema.addMessage(message);

            SchemaDiff diff = adapter.adapt(schema);

            assertThat(diff.getMessageDiffs()).hasSize(1);
            MessageDiff msgDiff = diff.getMessageDiffs().get(0);
            assertThat(msgDiff.messageName()).isEqualTo("ModifiedMessage");
            assertThat(msgDiff.changeType()).isEqualTo(ChangeType.MODIFIED);
            assertThat(msgDiff.v1Message()).isNotNull();
            assertThat(msgDiff.v2Message()).isNotNull();
            assertThat(msgDiff.fieldChanges()).hasSize(2); // removed + added
        }

        @Test
        @DisplayName("Should return UNCHANGED for message with no changes")
        void shouldReturnUnchangedForIdenticalMessage() {
            MergedMessage message = new MergedMessage("UnchangedMessage");
            message.addVersion(V1);
            message.addVersion(V2);
            addSimpleField(message, "id", 1, Type.TYPE_INT64, V1, V2);
            addSimpleField(message, "name", 2, Type.TYPE_STRING, V1, V2);
            schema.addMessage(message);

            SchemaDiff diff = adapter.adapt(schema);

            // UNCHANGED messages are filtered out
            assertThat(diff.getMessageDiffs()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge case: message not in either version")
    class MessageNotInEitherVersionTests {

        @Test
        @DisplayName("Should handle message not present in compared versions")
        void shouldHandleMessageNotInEitherVersion() {
            // Create a schema with v1, v2, v3
            MergedSchema threeVersionSchema = new MergedSchema(Arrays.asList("v1", "v2", "v3"));

            // Message exists only in v3 (not in v1 or v2)
            MergedMessage message = new MergedMessage("V3OnlyMessage");
            message.addVersion("v3");
            addSimpleField(message, "id", 1, Type.TYPE_INT64, "v3");
            threeVersionSchema.addMessage(message);

            // Adapter compares v1 and v2
            MergedSchemaDiffAdapter v1v2Adapter = new MergedSchemaDiffAdapter("v1", "v2");
            SchemaDiff diff = v1v2Adapter.adapt(threeVersionSchema);

            // Message is UNCHANGED (not in either v1 or v2), so filtered out
            assertThat(diff.getMessageDiffs()).isEmpty();
        }
    }

    // ========== Field-Level Tests ==========

    @Nested
    @DisplayName("Field change detection")
    class FieldChangeTests {

        @Test
        @DisplayName("Should detect field added in v2")
        void shouldDetectFieldAddedInV2() {
            MergedMessage message = new MergedMessage("TestMessage");
            message.addVersion(V1);
            message.addVersion(V2);
            addSimpleField(message, "existing", 1, Type.TYPE_STRING, V1, V2);
            addSimpleField(message, "newField", 2, Type.TYPE_INT32, V2);
            schema.addMessage(message);

            SchemaDiff diff = adapter.adapt(schema);

            MessageDiff msgDiff = diff.getMessageDiffs().get(0);
            assertThat(msgDiff.fieldChanges()).hasSize(1);
            FieldChange fieldChange = msgDiff.fieldChanges().get(0);
            assertThat(fieldChange.fieldName()).isEqualTo("newField");
            assertThat(fieldChange.changeType()).isEqualTo(ChangeType.ADDED);
        }

        @Test
        @DisplayName("Should detect field removed in v2")
        void shouldDetectFieldRemovedInV2() {
            MergedMessage message = new MergedMessage("TestMessage");
            message.addVersion(V1);
            message.addVersion(V2);
            addSimpleField(message, "existing", 1, Type.TYPE_STRING, V1, V2);
            addSimpleField(message, "removedField", 2, Type.TYPE_INT32, V1);
            schema.addMessage(message);

            SchemaDiff diff = adapter.adapt(schema);

            MessageDiff msgDiff = diff.getMessageDiffs().get(0);
            assertThat(msgDiff.fieldChanges()).hasSize(1);
            FieldChange fieldChange = msgDiff.fieldChanges().get(0);
            assertThat(fieldChange.fieldName()).isEqualTo("removedField");
            assertThat(fieldChange.changeType()).isEqualTo(ChangeType.REMOVED);
        }

        @Test
        @DisplayName("Should detect field type change")
        void shouldDetectFieldTypeChange() {
            MergedMessage message = new MergedMessage("TestMessage");
            message.addVersion(V1);
            message.addVersion(V2);

            // Field with type change: int32 in v1 -> int64 in v2
            addFieldWithTypeChange(message, "amount", 1, Type.TYPE_INT32, Type.TYPE_INT64);

            schema.addMessage(message);

            SchemaDiff diff = adapter.adapt(schema);

            MessageDiff msgDiff = diff.getMessageDiffs().get(0);
            assertThat(msgDiff.fieldChanges()).isNotEmpty();
            // Type change is detected through conflict type in MergedField
        }

        @Test
        @DisplayName("Should detect field cardinality change (singular to repeated)")
        void shouldDetectCardinalityChange() {
            MergedMessage message = new MergedMessage("TestMessage");
            message.addVersion(V1);
            message.addVersion(V2);

            addFieldWithLabelChange(message, "items", 1, Type.TYPE_STRING,
                Label.LABEL_OPTIONAL, Label.LABEL_REPEATED);

            schema.addMessage(message);

            SchemaDiff diff = adapter.adapt(schema);

            MessageDiff msgDiff = diff.getMessageDiffs().get(0);
            assertThat(msgDiff.fieldChanges()).hasSize(1);
            FieldChange fieldChange = msgDiff.fieldChanges().get(0);
            assertThat(fieldChange.changeType()).isEqualTo(ChangeType.LABEL_CHANGED);
        }
    }

    // ========== Enum Tests ==========

    @Nested
    @DisplayName("Enum change detection")
    class EnumChangeTests {

        @Test
        @DisplayName("Should detect enum added in v2")
        void shouldDetectEnumAddedInV2() {
            MergedEnum enumType = new MergedEnum("NewStatus");
            enumType.addVersion(V2);
            addEnumValue(enumType, "PENDING", 0, V2);
            addEnumValue(enumType, "ACTIVE", 1, V2);
            schema.addEnum(enumType);

            SchemaDiff diff = adapter.adapt(schema);

            assertThat(diff.getEnumDiffs()).hasSize(1);
            EnumDiff enumDiff = diff.getEnumDiffs().get(0);
            assertThat(enumDiff.enumName()).isEqualTo("NewStatus");
            assertThat(enumDiff.changeType()).isEqualTo(ChangeType.ADDED);
        }

        @Test
        @DisplayName("Should detect enum removed in v2")
        void shouldDetectEnumRemovedInV2() {
            MergedEnum enumType = new MergedEnum("OldStatus");
            enumType.addVersion(V1);
            addEnumValue(enumType, "PENDING", 0, V1);
            schema.addEnum(enumType);

            SchemaDiff diff = adapter.adapt(schema);

            assertThat(diff.getEnumDiffs()).hasSize(1);
            EnumDiff enumDiff = diff.getEnumDiffs().get(0);
            assertThat(enumDiff.enumName()).isEqualTo("OldStatus");
            assertThat(enumDiff.changeType()).isEqualTo(ChangeType.REMOVED);
        }

        @Test
        @DisplayName("Should detect enum value added in v2")
        void shouldDetectEnumValueAddedInV2() {
            MergedEnum enumType = new MergedEnum("Status");
            enumType.addVersion(V1);
            enumType.addVersion(V2);
            addEnumValue(enumType, "PENDING", 0, V1, V2);
            addEnumValue(enumType, "NEW_VALUE", 1, V2); // Only in v2
            schema.addEnum(enumType);

            SchemaDiff diff = adapter.adapt(schema);

            assertThat(diff.getEnumDiffs()).hasSize(1);
            EnumDiff enumDiff = diff.getEnumDiffs().get(0);
            assertThat(enumDiff.valueChanges()).hasSize(1);
            assertThat(enumDiff.valueChanges().get(0).valueName()).isEqualTo("NEW_VALUE");
            assertThat(enumDiff.valueChanges().get(0).changeType()).isEqualTo(ChangeType.VALUE_ADDED);
        }

        @Test
        @DisplayName("Should detect enum value removed in v2")
        void shouldDetectEnumValueRemovedInV2() {
            MergedEnum enumType = new MergedEnum("Status");
            enumType.addVersion(V1);
            enumType.addVersion(V2);
            addEnumValue(enumType, "PENDING", 0, V1, V2);
            addEnumValue(enumType, "DEPRECATED", 1, V1); // Only in v1
            schema.addEnum(enumType);

            SchemaDiff diff = adapter.adapt(schema);

            assertThat(diff.getEnumDiffs()).hasSize(1);
            EnumDiff enumDiff = diff.getEnumDiffs().get(0);
            assertThat(enumDiff.valueChanges()).hasSize(1);
            assertThat(enumDiff.valueChanges().get(0).valueName()).isEqualTo("DEPRECATED");
            assertThat(enumDiff.valueChanges().get(0).changeType()).isEqualTo(ChangeType.VALUE_REMOVED);
        }
    }

    // ========== Breaking Changes Tests ==========

    @Nested
    @DisplayName("Breaking change collection")
    class BreakingChangeTests {

        @Test
        @DisplayName("Should collect field removal as breaking change")
        void shouldCollectFieldRemovalAsBreakingChange() {
            MergedMessage message = new MergedMessage("TestMessage");
            message.addVersion(V1);
            message.addVersion(V2);
            addSimpleField(message, "kept", 1, Type.TYPE_STRING, V1, V2);
            addSimpleField(message, "removed", 2, Type.TYPE_STRING, V1);
            schema.addMessage(message);

            SchemaDiff diff = adapter.adapt(schema);

            assertThat(diff.getBreakingChanges()).anyMatch(
                bc -> bc.type() == BreakingChange.Type.FIELD_REMOVED
                    && bc.entityPath().contains("removed")
            );
        }

        @Test
        @DisplayName("Should collect enum removal as breaking change")
        void shouldCollectEnumRemovalAsBreakingChange() {
            MergedEnum enumType = new MergedEnum("RemovedEnum");
            enumType.addVersion(V1);
            addEnumValue(enumType, "VALUE", 0, V1);
            schema.addEnum(enumType);

            SchemaDiff diff = adapter.adapt(schema);

            assertThat(diff.getBreakingChanges()).anyMatch(
                bc -> bc.type() == BreakingChange.Type.ENUM_REMOVED
            );
        }

        @Test
        @DisplayName("Should not treat additions as breaking changes")
        void shouldNotTreatAdditionsAsBreakingChanges() {
            MergedMessage message = new MergedMessage("TestMessage");
            message.addVersion(V2); // Only in v2
            addSimpleField(message, "newField", 1, Type.TYPE_STRING, V2);
            schema.addMessage(message);

            SchemaDiff diff = adapter.adapt(schema);

            // Message addition is not a breaking change
            assertThat(diff.getBreakingChanges()).noneMatch(
                bc -> bc.type() == BreakingChange.Type.MESSAGE_REMOVED
            );
        }
    }

    // ========== Nested Message Tests ==========

    @Nested
    @DisplayName("Nested message handling")
    class NestedMessageTests {

        @Test
        @DisplayName("Should detect nested message added in v2")
        void shouldDetectNestedMessageAddedInV2() {
            MergedMessage parent = new MergedMessage("Parent");
            parent.addVersion(V1);
            parent.addVersion(V2);
            addSimpleField(parent, "id", 1, Type.TYPE_INT64, V1, V2);

            MergedMessage nested = new MergedMessage("Nested");
            nested.addVersion(V2);
            addSimpleField(nested, "data", 1, Type.TYPE_STRING, V2);
            parent.addNestedMessage(nested);

            schema.addMessage(parent);

            SchemaDiff diff = adapter.adapt(schema);

            MessageDiff parentDiff = diff.getMessageDiffs().get(0);
            assertThat(parentDiff.nestedMessageChanges()).hasSize(1);
            assertThat(parentDiff.nestedMessageChanges().get(0).changeType()).isEqualTo(ChangeType.ADDED);
        }

        @Test
        @DisplayName("Should detect nested message removed in v2")
        void shouldDetectNestedMessageRemovedInV2() {
            MergedMessage parent = new MergedMessage("Parent");
            parent.addVersion(V1);
            parent.addVersion(V2);
            addSimpleField(parent, "id", 1, Type.TYPE_INT64, V1, V2);

            MergedMessage nested = new MergedMessage("OldNested");
            nested.addVersion(V1);
            addSimpleField(nested, "data", 1, Type.TYPE_STRING, V1);
            parent.addNestedMessage(nested);

            schema.addMessage(parent);

            SchemaDiff diff = adapter.adapt(schema);

            MessageDiff parentDiff = diff.getMessageDiffs().get(0);
            assertThat(parentDiff.nestedMessageChanges()).hasSize(1);
            assertThat(parentDiff.nestedMessageChanges().get(0).changeType()).isEqualTo(ChangeType.REMOVED);
        }
    }

    // ========== Static Method Test ==========

    @Nested
    @DisplayName("Static factory method")
    class StaticFactoryTests {

        @Test
        @DisplayName("toSchemaDiff should produce same result as instance method")
        void toSchemaDiffShouldProduceSameResult() {
            MergedMessage message = new MergedMessage("TestMessage");
            message.addVersion(V1);
            message.addVersion(V2);
            addSimpleField(message, "field", 1, Type.TYPE_STRING, V1, V2);
            addSimpleField(message, "newField", 2, Type.TYPE_INT32, V2);
            schema.addMessage(message);

            SchemaDiff instanceResult = adapter.adapt(schema);
            SchemaDiff staticResult = MergedSchemaDiffAdapter.toSchemaDiff(schema, V1, V2);

            assertThat(staticResult.getV1Name()).isEqualTo(instanceResult.getV1Name());
            assertThat(staticResult.getV2Name()).isEqualTo(instanceResult.getV2Name());
            assertThat(staticResult.getMessageDiffs()).hasSize(instanceResult.getMessageDiffs().size());
        }
    }

    // ========== Logical Equivalence Verification ==========

    @Nested
    @DisplayName("Logical equivalence verification (inV1 vs inV1 && inV2)")
    class LogicalEquivalenceTests {

        @Test
        @DisplayName("After filtering, inV1 implies inV2 when reaching modification check")
        void shouldVerifyLogicalEquivalence() {
            // This test verifies the optimization: after the first two conditions,
            // if we reach the third condition, inV1 == inV2

            // Case 1: Both true - should be MODIFIED
            MergedMessage bothTrue = new MergedMessage("BothTrue");
            bothTrue.addVersion(V1);
            bothTrue.addVersion(V2);
            addSimpleField(bothTrue, "f", 1, Type.TYPE_STRING, V1, V2);
            schema.addMessage(bothTrue);

            // Case 2: Only v1 - should be REMOVED
            MergedMessage onlyV1 = new MergedMessage("OnlyV1");
            onlyV1.addVersion(V1);
            addSimpleField(onlyV1, "f", 1, Type.TYPE_STRING, V1);
            schema.addMessage(onlyV1);

            // Case 3: Only v2 - should be ADDED
            MergedMessage onlyV2 = new MergedMessage("OnlyV2");
            onlyV2.addVersion(V2);
            addSimpleField(onlyV2, "f", 1, Type.TYPE_STRING, V2);
            schema.addMessage(onlyV2);

            SchemaDiff diff = adapter.adapt(schema);

            // Verify all cases are handled correctly
            Optional<MessageDiff> bothTrueDiff = diff.getMessageDiffs().stream()
                .filter(d -> d.messageName().equals("BothTrue")).findFirst();
            Optional<MessageDiff> onlyV1Diff = diff.getMessageDiffs().stream()
                .filter(d -> d.messageName().equals("OnlyV1")).findFirst();
            Optional<MessageDiff> onlyV2Diff = diff.getMessageDiffs().stream()
                .filter(d -> d.messageName().equals("OnlyV2")).findFirst();

            // BothTrue is UNCHANGED (no field changes), so filtered out
            assertThat(bothTrueDiff).isEmpty();

            // OnlyV1 is REMOVED
            assertThat(onlyV1Diff).isPresent();
            assertThat(onlyV1Diff.get().changeType()).isEqualTo(ChangeType.REMOVED);

            // OnlyV2 is ADDED
            assertThat(onlyV2Diff).isPresent();
            assertThat(onlyV2Diff.get().changeType()).isEqualTo(ChangeType.ADDED);
        }

        @Test
        @DisplayName("Three-version schema should correctly handle version pairs")
        void shouldHandleThreeVersionSchema() {
            MergedSchema threeVersionSchema = new MergedSchema(Arrays.asList("v1", "v2", "v3"));

            // Message in v1 and v3 only (not v2)
            MergedMessage skipV2 = new MergedMessage("SkipV2");
            skipV2.addVersion("v1");
            skipV2.addVersion("v3");
            addSimpleField(skipV2, "f", 1, Type.TYPE_STRING, "v1", "v3");
            threeVersionSchema.addMessage(skipV2);

            // Compare v1 vs v2
            SchemaDiff v1v2 = MergedSchemaDiffAdapter.toSchemaDiff(threeVersionSchema, "v1", "v2");
            assertThat(v1v2.getMessageDiffs()).hasSize(1);
            assertThat(v1v2.getMessageDiffs().get(0).changeType()).isEqualTo(ChangeType.REMOVED);

            // Compare v2 vs v3
            SchemaDiff v2v3 = MergedSchemaDiffAdapter.toSchemaDiff(threeVersionSchema, "v2", "v3");
            assertThat(v2v3.getMessageDiffs()).hasSize(1);
            assertThat(v2v3.getMessageDiffs().get(0).changeType()).isEqualTo(ChangeType.ADDED);

            // Compare v1 vs v3 (both have it - no changes)
            SchemaDiff v1v3 = MergedSchemaDiffAdapter.toSchemaDiff(threeVersionSchema, "v1", "v3");
            assertThat(v1v3.getMessageDiffs()).isEmpty(); // UNCHANGED filtered out
        }
    }

    // ========== Helper Methods ==========

    private void addSimpleField(MergedMessage message, String name, int number, Type type, String... versions) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(number)
            .setType(type)
            .setLabel(Label.LABEL_OPTIONAL)
            .build();
        FieldInfo fieldInfo = new FieldInfo(proto);

        MergedField.Builder builder = MergedField.builder();
        for (String version : versions) {
            builder.addVersionField(version, fieldInfo);
        }
        message.addField(builder.build());
    }

    private void addFieldWithTypeChange(MergedMessage message, String name, int number,
                                         Type v1Type, Type v2Type) {
        FieldDescriptorProto v1Proto = FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(number)
            .setType(v1Type)
            .setLabel(Label.LABEL_OPTIONAL)
            .build();
        FieldDescriptorProto v2Proto = FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(number)
            .setType(v2Type)
            .setLabel(Label.LABEL_OPTIONAL)
            .build();

        MergedField field = MergedField.builder()
            .addVersionField(V1, new FieldInfo(v1Proto))
            .addVersionField(V2, new FieldInfo(v2Proto))
            .conflictType(MergedField.ConflictType.WIDENING)
            .build();
        message.addField(field);
    }

    private void addFieldWithLabelChange(MergedMessage message, String name, int number,
                                          Type type, Label v1Label, Label v2Label) {
        FieldDescriptorProto v1Proto = FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(number)
            .setType(type)
            .setLabel(v1Label)
            .build();
        FieldDescriptorProto v2Proto = FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(number)
            .setType(type)
            .setLabel(v2Label)
            .build();

        MergedField field = MergedField.builder()
            .addVersionField(V1, new FieldInfo(v1Proto))
            .addVersionField(V2, new FieldInfo(v2Proto))
            .build();
        message.addField(field);
    }

    private void addEnumValue(MergedEnum enumType, String name, int number, String... versions) {
        EnumInfo.EnumValue value = new EnumInfo.EnumValue(name, number);
        MergedEnumValue mergedValue = new MergedEnumValue(value, versions[0]);
        for (int i = 1; i < versions.length; i++) {
            mergedValue.addVersion(versions[i]);
        }
        enumType.addValue(mergedValue);
    }
}
