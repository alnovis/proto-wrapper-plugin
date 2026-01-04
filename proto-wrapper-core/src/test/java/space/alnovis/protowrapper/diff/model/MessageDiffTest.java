package space.alnovis.protowrapper.diff.model;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import org.junit.jupiter.api.Test;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MessageInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MessageDiff record.
 */
class MessageDiffTest {

    @Test
    void added_createsCorrectDiff() {
        MessageInfo message = createMessage("User", "name", 1, "email", 2);

        MessageDiff diff = MessageDiff.added(message);

        assertEquals("User", diff.messageName());
        assertEquals(ChangeType.ADDED, diff.changeType());
        assertNull(diff.v1Message());
        assertEquals(message, diff.v2Message());
        assertTrue(diff.fieldChanges().isEmpty());
        assertTrue(diff.nestedMessageChanges().isEmpty());
        assertTrue(diff.nestedEnumChanges().isEmpty());
    }

    @Test
    void removed_createsCorrectDiff() {
        MessageInfo message = createMessage("OldMessage", "id", 1);

        MessageDiff diff = MessageDiff.removed(message);

        assertEquals("OldMessage", diff.messageName());
        assertEquals(ChangeType.REMOVED, diff.changeType());
        assertEquals(message, diff.v1Message());
        assertNull(diff.v2Message());
        assertTrue(diff.fieldChanges().isEmpty());
    }

    @Test
    void compared_withChanges_createsModifiedDiff() {
        MessageInfo v1 = createMessage("User", "name", 1);
        MessageInfo v2 = createMessage("User", "name", 1, "email", 2);

        FieldInfo addedField = createField("email", 2, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        List<FieldChange> fieldChanges = List.of(
            new FieldChange(2, "email", ChangeType.ADDED, null, addedField, List.of())
        );

        MessageDiff diff = MessageDiff.compared(v1, v2, fieldChanges, List.of(), List.of());

        assertEquals("User", diff.messageName());
        assertEquals(ChangeType.MODIFIED, diff.changeType());
        assertEquals(v1, diff.v1Message());
        assertEquals(v2, diff.v2Message());
        assertEquals(1, diff.fieldChanges().size());
    }

    @Test
    void compared_withoutChanges_createsUnchangedDiff() {
        MessageInfo v1 = createMessage("User", "name", 1);
        MessageInfo v2 = createMessage("User", "name", 1);

        MessageDiff diff = MessageDiff.compared(v1, v2, List.of(), List.of(), List.of());

        assertEquals(ChangeType.UNCHANGED, diff.changeType());
    }

    @Test
    void compared_withNestedMessageChanges_createsModifiedDiff() {
        MessageInfo v1 = createMessage("User", "name", 1);
        MessageInfo v2 = createMessage("User", "name", 1);
        MessageInfo nested = createMessage("Address", "street", 1);

        MessageDiff diff = MessageDiff.compared(
            v1, v2,
            List.of(),
            List.of(MessageDiff.added(nested)),
            List.of()
        );

        assertEquals(ChangeType.MODIFIED, diff.changeType());
        assertEquals(1, diff.nestedMessageChanges().size());
    }

    @Test
    void getAddedFields_returnsOnlyAdded() {
        FieldInfo added1 = createField("field1", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo added2 = createField("field2", 2, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldInfo removed = createField("old", 3, Type.TYPE_BOOL, Label.LABEL_OPTIONAL);

        List<FieldChange> changes = List.of(
            new FieldChange(1, "field1", ChangeType.ADDED, null, added1, List.of()),
            new FieldChange(2, "field2", ChangeType.ADDED, null, added2, List.of()),
            new FieldChange(3, "old", ChangeType.REMOVED, removed, null, List.of())
        );

        MessageDiff diff = new MessageDiff(
            "Test", ChangeType.MODIFIED, null, null, changes, List.of(), List.of()
        );

        List<FieldChange> addedFields = diff.getAddedFields();

        assertEquals(2, addedFields.size());
        assertTrue(addedFields.stream().allMatch(fc -> fc.changeType() == ChangeType.ADDED));
    }

    @Test
    void getRemovedFields_returnsOnlyRemoved() {
        FieldInfo removed1 = createField("field1", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo removed2 = createField("field2", 2, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldInfo added = createField("new", 3, Type.TYPE_BOOL, Label.LABEL_OPTIONAL);

        List<FieldChange> changes = List.of(
            new FieldChange(1, "field1", ChangeType.REMOVED, removed1, null, List.of()),
            new FieldChange(2, "field2", ChangeType.REMOVED, removed2, null, List.of()),
            new FieldChange(3, "new", ChangeType.ADDED, null, added, List.of())
        );

        MessageDiff diff = new MessageDiff(
            "Test", ChangeType.MODIFIED, null, null, changes, List.of(), List.of()
        );

        List<FieldChange> removedFields = diff.getRemovedFields();

        assertEquals(2, removedFields.size());
        assertTrue(removedFields.stream().allMatch(fc -> fc.changeType() == ChangeType.REMOVED));
    }

    @Test
    void getModifiedFields_returnsTypeAndLabelChanges() {
        FieldInfo f1v1 = createField("field1", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldInfo f1v2 = createField("field1", 1, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
        FieldInfo f2v1 = createField("field2", 2, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo f2v2 = createField("field2", 2, Type.TYPE_STRING, Label.LABEL_REPEATED);
        FieldInfo added = createField("new", 3, Type.TYPE_BOOL, Label.LABEL_OPTIONAL);

        List<FieldChange> changes = List.of(
            new FieldChange(1, "field1", ChangeType.TYPE_CHANGED, f1v1, f1v2, List.of()),
            new FieldChange(2, "field2", ChangeType.LABEL_CHANGED, f2v1, f2v2, List.of()),
            new FieldChange(3, "new", ChangeType.ADDED, null, added, List.of())
        );

        MessageDiff diff = new MessageDiff(
            "Test", ChangeType.MODIFIED, null, null, changes, List.of(), List.of()
        );

        List<FieldChange> modifiedFields = diff.getModifiedFields();

        assertEquals(2, modifiedFields.size());
    }

    @Test
    void hasBreakingChanges_returnsTrue_whenMessageRemoved() {
        MessageInfo message = createMessage("OldMessage", "id", 1);
        MessageDiff diff = MessageDiff.removed(message);

        assertTrue(diff.hasBreakingChanges());
    }

    @Test
    void hasBreakingChanges_returnsTrue_whenFieldRemoved() {
        FieldInfo removed = createField("deleted", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        List<FieldChange> changes = List.of(
            new FieldChange(1, "deleted", ChangeType.REMOVED, removed, null, List.of())
        );

        MessageDiff diff = new MessageDiff(
            "Test", ChangeType.MODIFIED, null, null, changes, List.of(), List.of()
        );

        assertTrue(diff.hasBreakingChanges());
    }

    @Test
    void hasBreakingChanges_returnsTrue_whenNestedMessageHasBreakingChanges() {
        MessageInfo nestedMessage = createMessage("NestedRemoved", "id", 1);
        MessageDiff nestedDiff = MessageDiff.removed(nestedMessage);

        MessageDiff diff = new MessageDiff(
            "Parent", ChangeType.MODIFIED, null, null,
            List.of(), List.of(nestedDiff), List.of()
        );

        assertTrue(diff.hasBreakingChanges());
    }

    @Test
    void hasBreakingChanges_returnsTrue_whenNestedEnumHasBreakingChanges() {
        EnumDiff enumDiff = new EnumDiff(
            "RemovedEnum", ChangeType.REMOVED, null, null, List.of()
        );

        MessageDiff diff = new MessageDiff(
            "Parent", ChangeType.MODIFIED, null, null,
            List.of(), List.of(), List.of(enumDiff)
        );

        assertTrue(diff.hasBreakingChanges());
    }

    @Test
    void hasBreakingChanges_returnsFalse_whenOnlyAdditions() {
        MessageInfo message = createMessage("NewMessage", "id", 1);
        MessageDiff diff = MessageDiff.added(message);

        assertFalse(diff.hasBreakingChanges());
    }

    @Test
    void hasBreakingChanges_returnsFalse_whenOnlyFieldsAdded() {
        FieldInfo added = createField("new_field", 5, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        List<FieldChange> changes = List.of(
            new FieldChange(5, "new_field", ChangeType.ADDED, null, added, List.of())
        );

        MessageDiff diff = new MessageDiff(
            "Test", ChangeType.MODIFIED, null, null, changes, List.of(), List.of()
        );

        assertFalse(diff.hasBreakingChanges());
    }

    @Test
    void countBreakingChanges_countsMessageRemoval() {
        MessageInfo message = createMessage("Removed", "id", 1);
        MessageDiff diff = MessageDiff.removed(message);

        assertEquals(1, diff.countBreakingChanges());
    }

    @Test
    void countBreakingChanges_countsBreakingFieldChanges() {
        FieldInfo removed1 = createField("f1", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo removed2 = createField("f2", 2, Type.TYPE_INT32, Label.LABEL_OPTIONAL);

        List<FieldChange> changes = List.of(
            new FieldChange(1, "f1", ChangeType.REMOVED, removed1, null, List.of()),
            new FieldChange(2, "f2", ChangeType.REMOVED, removed2, null, List.of())
        );

        MessageDiff diff = new MessageDiff(
            "Test", ChangeType.MODIFIED, null, null, changes, List.of(), List.of()
        );

        assertEquals(2, diff.countBreakingChanges());
    }

    @Test
    void countBreakingChanges_sumsNestedChanges() {
        // Parent message with 1 removed field
        FieldInfo removed = createField("f1", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        List<FieldChange> parentChanges = List.of(
            new FieldChange(1, "f1", ChangeType.REMOVED, removed, null, List.of())
        );

        // Nested message that was removed
        MessageInfo nestedMessage = createMessage("Nested", "id", 1);
        MessageDiff nestedDiff = MessageDiff.removed(nestedMessage);

        MessageDiff diff = new MessageDiff(
            "Parent", ChangeType.MODIFIED, null, null,
            parentChanges, List.of(nestedDiff), List.of()
        );

        assertEquals(2, diff.countBreakingChanges());
    }

    @Test
    void getSummary_formatsAddedMessage() {
        MessageInfo message = createMessage("NewUser", "id", 1);
        MessageDiff diff = MessageDiff.added(message);

        String summary = diff.getSummary();

        assertTrue(summary.contains("Added message"));
        assertTrue(summary.contains("NewUser"));
    }

    @Test
    void getSummary_formatsRemovedMessage() {
        MessageInfo message = createMessage("OldUser", "id", 1);
        MessageDiff diff = MessageDiff.removed(message);

        String summary = diff.getSummary();

        assertTrue(summary.contains("Removed message"));
        assertTrue(summary.contains("OldUser"));
    }

    @Test
    void getSummary_formatsModifiedMessage() {
        FieldInfo added = createField("new", 5, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo removed = createField("old", 3, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldInfo v1 = createField("modified", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("modified", 1, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

        List<FieldChange> changes = List.of(
            new FieldChange(5, "new", ChangeType.ADDED, null, added, List.of()),
            new FieldChange(3, "old", ChangeType.REMOVED, removed, null, List.of()),
            new FieldChange(1, "modified", ChangeType.TYPE_CHANGED, v1, v2, List.of())
        );

        MessageDiff diff = new MessageDiff(
            "User", ChangeType.MODIFIED, null, null, changes, List.of(), List.of()
        );

        String summary = diff.getSummary();

        assertTrue(summary.contains("Modified message"));
        assertTrue(summary.contains("User"));
        assertTrue(summary.contains("+1 fields"));
        assertTrue(summary.contains("-1 fields"));
        assertTrue(summary.contains("~1 changed"));
    }

    @Test
    void getSummary_formatsUnchangedMessage() {
        MessageInfo v1 = createMessage("User", "id", 1);
        MessageInfo v2 = createMessage("User", "id", 1);
        MessageDiff diff = MessageDiff.compared(v1, v2, List.of(), List.of(), List.of());

        String summary = diff.getSummary();

        assertTrue(summary.contains("User"));
        assertTrue(summary.contains("UNCHANGED"));
    }

    @Test
    void getSourceFileName_returnsV2FileName_whenAvailable() {
        DescriptorProto proto = DescriptorProto.newBuilder()
            .setName("Test")
            .build();
        MessageInfo v1 = new MessageInfo(proto, "test.package", "old.proto");
        MessageInfo v2 = new MessageInfo(proto, "test.package", "new.proto");

        MessageDiff diff = MessageDiff.compared(v1, v2, List.of(), List.of(), List.of());

        assertEquals("new.proto", diff.getSourceFileName());
    }

    @Test
    void getSourceFileName_returnsV1FileName_whenV2IsNull() {
        DescriptorProto proto = DescriptorProto.newBuilder()
            .setName("Removed")
            .build();
        MessageInfo v1 = new MessageInfo(proto, "test.package", "removed.proto");

        MessageDiff diff = MessageDiff.removed(v1);

        assertEquals("removed.proto", diff.getSourceFileName());
    }

    @Test
    void getSourceFileName_returnsNull_whenBothMessagesHaveNoFileName() {
        MessageInfo v1 = createMessage("Test", "id", 1);
        MessageInfo v2 = createMessage("Test", "id", 1);

        MessageDiff diff = MessageDiff.compared(v1, v2, List.of(), List.of(), List.of());

        assertNull(diff.getSourceFileName());
    }

    @Test
    void equalsAndHashCode_workCorrectly() {
        // Use the same MessageInfo instance since MessageInfo doesn't override equals()
        MessageInfo msg = createMessage("User", "id", 1);

        // Two diffs with same MessageInfo should be equal
        MessageDiff diff1 = MessageDiff.added(msg);
        MessageDiff diff2 = MessageDiff.added(msg);
        MessageDiff diff3 = MessageDiff.removed(msg);

        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());
        assertNotEquals(diff1, diff3);
    }

    // Helper methods
    private MessageInfo createMessage(String name, Object... fieldsAndNumbers) {
        DescriptorProto.Builder builder = DescriptorProto.newBuilder().setName(name);

        for (int i = 0; i < fieldsAndNumbers.length; i += 2) {
            String fieldName = (String) fieldsAndNumbers[i];
            int fieldNumber = (Integer) fieldsAndNumbers[i + 1];
            builder.addField(FieldDescriptorProto.newBuilder()
                .setName(fieldName)
                .setNumber(fieldNumber)
                .setType(Type.TYPE_STRING)
                .setLabel(Label.LABEL_OPTIONAL)
                .build());
        }

        return new MessageInfo(builder.build(), "test.package");
    }

    private FieldInfo createField(String name, int number, Type type, Label label) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(number)
            .setType(type)
            .setLabel(label)
            .build();
        return new FieldInfo(proto);
    }
}
