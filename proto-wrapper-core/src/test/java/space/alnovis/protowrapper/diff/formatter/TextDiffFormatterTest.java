package space.alnovis.protowrapper.diff.formatter;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import space.alnovis.protowrapper.diff.SchemaDiff;
import space.alnovis.protowrapper.diff.model.*;
import space.alnovis.protowrapper.model.EnumInfo;
import space.alnovis.protowrapper.model.FieldInfo;
import space.alnovis.protowrapper.model.MessageInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TextDiffFormatter.
 */
class TextDiffFormatterTest {

    private TextDiffFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new TextDiffFormatter();
    }

    @Test
    void format_includesVersionHeader() {
        SchemaDiff diff = createEmptyDiff("production", "development");

        String result = formatter.format(diff);

        assertTrue(result.contains("Schema Comparison: production -> development"));
    }

    @Test
    void format_includesSectionSeparators() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.format(diff);

        assertTrue(result.contains("=".repeat(80)));
        assertTrue(result.contains("MESSAGES"));
        assertTrue(result.contains("ENUMS"));
        assertTrue(result.contains("SUMMARY"));
    }

    @Test
    void format_showsAddedMessages() {
        MessageInfo addedMessage = createMessage("NewUser", "id", 1, "name", 2);
        MessageDiff messageDiff = MessageDiff.added(addedMessage);
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("+ ADDED: NewUser"));
        assertTrue(result.contains("Fields:"));
        assertTrue(result.contains("id"));
        assertTrue(result.contains("name"));
    }

    @Test
    void format_showsRemovedMessages() {
        MessageInfo removedMessage = createMessage("OldMessage", "data", 1);
        MessageDiff messageDiff = MessageDiff.removed(removedMessage);
        BreakingChange breaking = new BreakingChange(
            BreakingChange.Type.MESSAGE_REMOVED,
            BreakingChange.Severity.ERROR,
            "OldMessage",
            "Message removed",
            "OldMessage",
            null
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of(breaking));

        String result = formatter.format(diff);

        assertTrue(result.contains("- REMOVED: OldMessage"));
        assertTrue(result.contains("[BREAKING]"));
    }

    @Test
    void format_showsModifiedMessages() {
        MessageInfo v1 = createMessage("User", "id", 1);
        MessageInfo v2 = createMessage("User", "id", 1, "email", 2);

        FieldInfo addedField = createField("email", 2, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange fieldChange = new FieldChange(2, "email", ChangeType.ADDED, null, addedField, List.of());
        MessageDiff messageDiff = MessageDiff.compared(v1, v2, List.of(fieldChange), List.of(), List.of());

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("~ MODIFIED: User"));
        assertTrue(result.contains("+ Added field: email"));
    }

    @Test
    void format_showsRemovedFields() {
        MessageInfo v1 = createMessage("User", "id", 1, "deprecated", 2);
        MessageInfo v2 = createMessage("User", "id", 1);

        FieldInfo removedField = createField("deprecated", 2, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange fieldChange = new FieldChange(2, "deprecated", ChangeType.REMOVED, removedField, null, List.of());
        MessageDiff messageDiff = MessageDiff.compared(v1, v2, List.of(fieldChange), List.of(), List.of());

        BreakingChange breaking = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "User.deprecated",
            "Field removed",
            "string deprecated = 2",
            null
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of(breaking));

        String result = formatter.format(diff);

        assertTrue(result.contains("- Removed field: deprecated"));
        assertTrue(result.contains("[BREAKING]"));
    }

    @Test
    void format_showsAddedEnums() {
        EnumInfo addedEnum = createEnum("Status", "PENDING", 0, "ACTIVE", 1);
        EnumDiff enumDiff = EnumDiff.added(addedEnum);
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("+ ADDED: Status"));
        assertTrue(result.contains("Values:"));
        assertTrue(result.contains("PENDING(0)"));
        assertTrue(result.contains("ACTIVE(1)"));
    }

    @Test
    void format_showsRemovedEnums() {
        EnumInfo removedEnum = createEnum("OldEnum", "VALUE", 0);
        EnumDiff enumDiff = EnumDiff.removed(removedEnum);
        BreakingChange breaking = new BreakingChange(
            BreakingChange.Type.ENUM_REMOVED,
            BreakingChange.Severity.ERROR,
            "OldEnum",
            "Enum removed",
            "OldEnum",
            null
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of(breaking));

        String result = formatter.format(diff);

        assertTrue(result.contains("- REMOVED: OldEnum"));
        assertTrue(result.contains("[BREAKING]"));
    }

    @Test
    void format_showsModifiedEnums() {
        EnumInfo v1 = createEnum("Status", "PENDING", 0);
        EnumInfo v2 = createEnum("Status", "PENDING", 0, "ACTIVE", 1);

        List<EnumValueChange> changes = List.of(EnumValueChange.added("ACTIVE", 1));
        EnumDiff enumDiff = EnumDiff.modified(v1, v2, changes);

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("~ MODIFIED: Status"));
        assertTrue(result.contains("+ Added value: ACTIVE(1)"));
    }

    @Test
    void format_showsBreakingChangesSection() {
        MessageInfo removed = createMessage("Deleted", "id", 1);
        MessageDiff messageDiff = MessageDiff.removed(removed);
        BreakingChange breaking = new BreakingChange(
            BreakingChange.Type.MESSAGE_REMOVED,
            BreakingChange.Severity.ERROR,
            "Deleted",
            "Message removed",
            "message Deleted",
            null
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of(breaking));

        String result = formatter.format(diff);

        assertTrue(result.contains("BREAKING CHANGES"));
        assertTrue(result.contains("ERRORS (1):"));
        assertTrue(result.contains("[ERROR] MESSAGE_REMOVED: Deleted"));
    }

    @Test
    void format_showsWarningsInBreakingChanges() {
        BreakingChange warning = new BreakingChange(
            BreakingChange.Type.CARDINALITY_CHANGED,
            BreakingChange.Severity.WARNING,
            "User.tags",
            "Cardinality changed",
            "singular",
            "repeated"
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(warning));

        String result = formatter.format(diff);

        assertTrue(result.contains("WARNINGS (1):"));
        assertTrue(result.contains("[WARN] CARDINALITY_CHANGED: User.tags"));
    }

    @Test
    void format_showsSummary() {
        MessageInfo added = createMessage("NewMessage", "id", 1);
        MessageInfo removed = createMessage("OldMessage", "data", 1);
        MessageDiff addedDiff = MessageDiff.added(added);
        MessageDiff removedDiff = MessageDiff.removed(removed);

        BreakingChange breaking = new BreakingChange(
            BreakingChange.Type.MESSAGE_REMOVED,
            BreakingChange.Severity.ERROR,
            "OldMessage",
            "Removed",
            null,
            null
        );

        SchemaDiff diff = new SchemaDiff("v1", "v2",
            List.of(addedDiff, removedDiff), List.of(), List.of(breaking));

        String result = formatter.format(diff);

        assertTrue(result.contains("SUMMARY"));
        assertTrue(result.contains("Messages:"));
        assertTrue(result.contains("+1 added"));
        assertTrue(result.contains("-1 removed"));
        assertTrue(result.contains("Breaking:"));
        assertTrue(result.contains("1 errors"));
    }

    @Test
    void format_showsNoChangesMessage_whenEmpty() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.format(diff);

        assertTrue(result.contains("No message changes"));
        assertTrue(result.contains("No enum changes"));
    }

    @Test
    void formatBreakingOnly_returnsNoBreakingMessage_whenNoBreaking() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.formatBreakingOnly(diff);

        assertEquals("No breaking changes detected.\n", result);
    }

    @Test
    void formatBreakingOnly_showsBreakingChangesOnly() {
        BreakingChange error = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "User.email",
            "Field removed",
            "string email = 3",
            null
        );
        BreakingChange warning = new BreakingChange(
            BreakingChange.Type.CARDINALITY_CHANGED,
            BreakingChange.Severity.WARNING,
            "User.tags",
            "Changed to repeated",
            null,
            null
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(error, warning));

        String result = formatter.formatBreakingOnly(diff);

        assertTrue(result.contains("Breaking Changes: v1 -> v2"));
        assertTrue(result.contains("[ERROR] FIELD_REMOVED: User.email"));
        assertTrue(result.contains("[WARN] CARDINALITY_CHANGED: User.tags"));
        assertTrue(result.contains("Total: 1 errors, 1 warnings"));
    }

    @Test
    void formatBreakingOnly_showsValueChanges_whenHasV1AndV2() {
        BreakingChange change = new BreakingChange(
            BreakingChange.Type.FIELD_TYPE_INCOMPATIBLE,
            BreakingChange.Severity.ERROR,
            "User.status",
            "Type changed",
            "string",
            "int32"
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(change));

        String result = formatter.formatBreakingOnly(diff);

        assertTrue(result.contains("string -> int32"));
    }

    // Helper methods
    private SchemaDiff createEmptyDiff(String v1Name, String v2Name) {
        return new SchemaDiff(v1Name, v2Name, List.of(), List.of(), List.of());
    }

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

    private EnumInfo createEnum(String name, Object... valuesAndNumbers) {
        EnumDescriptorProto.Builder builder = EnumDescriptorProto.newBuilder().setName(name);

        for (int i = 0; i < valuesAndNumbers.length; i += 2) {
            String valueName = (String) valuesAndNumbers[i];
            int number = (Integer) valuesAndNumbers[i + 1];
            builder.addValue(EnumValueDescriptorProto.newBuilder()
                .setName(valueName)
                .setNumber(number)
                .build());
        }

        return new EnumInfo(builder.build());
    }
}
