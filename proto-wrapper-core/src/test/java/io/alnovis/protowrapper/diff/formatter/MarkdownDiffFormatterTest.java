package io.alnovis.protowrapper.diff.formatter;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.diff.SchemaDiff;
import io.alnovis.protowrapper.diff.model.*;
import io.alnovis.protowrapper.model.EnumInfo;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MessageInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MarkdownDiffFormatter.
 */
class MarkdownDiffFormatterTest {

    private MarkdownDiffFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new MarkdownDiffFormatter();
    }

    @Test
    void format_includesTitle() {
        SchemaDiff diff = createEmptyDiff("production", "development");

        String result = formatter.format(diff);

        assertTrue(result.contains("# Schema Comparison: production -> development"));
    }

    @Test
    void format_includesSummarySection() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.format(diff);

        assertTrue(result.contains("## Summary"));
    }

    @Test
    void format_includesSummaryTable() {
        MessageInfo added = createMessage("NewMessage", "id", 1);
        MessageDiff addedDiff = MessageDiff.added(added);
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(addedDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("| Category | Added | Modified | Removed |"));
        assertTrue(result.contains("|----------|-------|----------|--------|"));
        assertTrue(result.contains("| Messages |"));
        assertTrue(result.contains("| Enums |"));
    }

    @Test
    void format_showsBreakingChangesCount_inSummary() {
        BreakingChange error = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "User.email",
            "Field removed",
            null,
            null
        );
        BreakingChange warning = new BreakingChange(
            BreakingChange.Type.CARDINALITY_CHANGED,
            BreakingChange.Severity.WARNING,
            "User.tags",
            "Changed",
            null,
            null
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(error, warning));

        String result = formatter.format(diff);

        assertTrue(result.contains("**Breaking Changes:** 1 errors, 1 warnings"));
    }

    @Test
    void format_includesMessagesSection() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.format(diff);

        assertTrue(result.contains("## Messages"));
    }

    @Test
    void format_showsAddedMessages() {
        MessageInfo addedMessage = createMessage("User", "id", 1, "name", 2);
        MessageDiff messageDiff = MessageDiff.added(addedMessage);
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("### Added Messages"));
        assertTrue(result.contains("#### User"));
        assertTrue(result.contains("| Field | Type | Number |"));
        assertTrue(result.contains("| id |"));
        assertTrue(result.contains("| name |"));
    }

    @Test
    void format_showsMessageSourceFile() {
        DescriptorProto proto = DescriptorProto.newBuilder()
            .setName("User")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("id")
                .setNumber(1)
                .setType(Type.TYPE_INT64)
                .setLabel(Label.LABEL_OPTIONAL)
                .build())
            .build();
        MessageInfo message = new MessageInfo(proto, "test.package", "user.proto");
        MessageDiff messageDiff = MessageDiff.added(message);
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("*Source: user.proto*"));
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
            null,
            null
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of(breaking));

        String result = formatter.format(diff);

        assertTrue(result.contains("### Removed Messages"));
        assertTrue(result.contains("- **OldMessage** - **BREAKING**"));
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

        assertTrue(result.contains("### Modified Messages"));
        assertTrue(result.contains("#### User"));
        assertTrue(result.contains("| Change | Field | Details |"));
        assertTrue(result.contains("| + Added |"));
        assertTrue(result.contains("email"));
    }

    @Test
    void format_showsRemovedFields_withBreakingFlag() {
        MessageInfo v1 = createMessage("User", "id", 1, "deprecated", 2);
        MessageInfo v2 = createMessage("User", "id", 1);

        FieldInfo removedField = createField("deprecated", 2, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange fieldChange = new FieldChange(2, "deprecated", ChangeType.REMOVED, removedField, null, List.of());
        MessageDiff messageDiff = MessageDiff.compared(v1, v2, List.of(fieldChange), List.of(), List.of());

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("| - Removed |"));
        assertTrue(result.contains("deprecated"));
        assertTrue(result.contains("**BREAKING**"));
    }

    @Test
    void format_includesEnumsSection() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.format(diff);

        assertTrue(result.contains("## Enums"));
    }

    @Test
    void format_showsAddedEnums() {
        EnumInfo addedEnum = createEnum("Status", "PENDING", 0, "ACTIVE", 1);
        EnumDiff enumDiff = EnumDiff.added(addedEnum);
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("### Added Enums"));
        assertTrue(result.contains("#### Status"));
        assertTrue(result.contains("| Name | Number |"));
        assertTrue(result.contains("| PENDING | 0 |"));
        assertTrue(result.contains("| ACTIVE | 1 |"));
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
            null,
            null
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of(breaking));

        String result = formatter.format(diff);

        assertTrue(result.contains("### Removed Enums"));
        assertTrue(result.contains("- **OldEnum** - **BREAKING**"));
    }

    @Test
    void format_showsModifiedEnums() {
        EnumInfo v1 = createEnum("Status", "PENDING", 0);
        EnumInfo v2 = createEnum("Status", "PENDING", 0, "ACTIVE", 1, "INACTIVE", 2);

        List<EnumValueChange> changes = List.of(
            EnumValueChange.added("ACTIVE", 1),
            EnumValueChange.added("INACTIVE", 2)
        );
        EnumDiff enumDiff = EnumDiff.modified(v1, v2, changes);

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("### Modified Enums"));
        assertTrue(result.contains("#### Status"));
        assertTrue(result.contains("- + Added: `ACTIVE` (1)"));
        assertTrue(result.contains("- + Added: `INACTIVE` (2)"));
    }

    @Test
    void format_showsRemovedEnumValues_withBreakingFlag() {
        EnumInfo v1 = createEnum("Status", "PENDING", 0, "DEPRECATED", 99);
        EnumInfo v2 = createEnum("Status", "PENDING", 0);

        List<EnumValueChange> changes = List.of(EnumValueChange.removed("DEPRECATED", 99));
        EnumDiff enumDiff = EnumDiff.modified(v1, v2, changes);

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("- - Removed: `DEPRECATED` (99) **BREAKING**"));
    }

    @Test
    void format_showsChangedEnumValues() {
        EnumInfo v1 = createEnum("Status", "PENDING", 0);
        EnumInfo v2 = createEnum("Status", "PENDING", 0);

        List<EnumValueChange> changes = List.of(
            EnumValueChange.numberChanged("RENUMBERED", 1, 10)
        );
        EnumDiff enumDiff = new EnumDiff("Status", ChangeType.MODIFIED, v1, v2, changes);

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("- ~ Changed: `RENUMBERED` (1 -> 10) **BREAKING**"));
    }

    @Test
    void format_includesBreakingChangesSection_whenPresent() {
        BreakingChange breaking = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "User.email",
            "Field removed",
            "string email = 3",
            null
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(breaking));

        String result = formatter.format(diff);

        assertTrue(result.contains("## Breaking Changes"));
        assertTrue(result.contains("| Severity | Type | Entity | Description |"));
    }

    @Test
    void format_formatsBreakingChangesTable() {
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
            "Cardinality changed",
            "singular",
            "repeated"
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(error, warning));

        String result = formatter.format(diff);

        assertTrue(result.contains("| ERROR | Field Removed | User.email |"));
        assertTrue(result.contains("| WARNING | Cardinality Changed | User.tags |"));
    }

    @Test
    void format_includesValueChanges_inBreakingChangesTable() {
        BreakingChange change = new BreakingChange(
            BreakingChange.Type.FIELD_TYPE_INCOMPATIBLE,
            BreakingChange.Severity.ERROR,
            "User.status",
            "Type changed",
            "string",
            "int32"
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(change));

        String result = formatter.format(diff);

        assertTrue(result.contains("`string` -> `int32`"));
    }

    @Test
    void format_showsNoMessageChanges_whenEmpty() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.format(diff);

        assertTrue(result.contains("No message changes"));
    }

    @Test
    void format_showsNoEnumChanges_whenEmpty() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.format(diff);

        assertTrue(result.contains("No enum changes"));
    }

    @Test
    void format_formatsRepeatedFieldType() {
        DescriptorProto proto = DescriptorProto.newBuilder()
            .setName("User")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("tags")
                .setNumber(1)
                .setType(Type.TYPE_STRING)
                .setLabel(Label.LABEL_REPEATED)
                .build())
            .build();
        MessageInfo message = new MessageInfo(proto, "test.package");
        MessageDiff messageDiff = MessageDiff.added(message);
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("`repeated string`"));
    }

    @Test
    void formatBreakingOnly_returnsNoBreakingMessage_whenNoBreaking() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.formatBreakingOnly(diff);

        assertTrue(result.contains("# Breaking Changes: v1 -> v2"));
        assertTrue(result.contains("No breaking changes detected"));
    }

    @Test
    void formatBreakingOnly_includesTitle() {
        BreakingChange breaking = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "User.email",
            "Field removed",
            null,
            null
        );
        SchemaDiff diff = new SchemaDiff("prod", "dev", List.of(), List.of(), List.of(breaking));

        String result = formatter.formatBreakingOnly(diff);

        assertTrue(result.contains("# Breaking Changes: prod -> dev"));
    }

    @Test
    void formatBreakingOnly_includesTotalCount() {
        BreakingChange error = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "User.email",
            "Field removed",
            null,
            null
        );
        BreakingChange warning = new BreakingChange(
            BreakingChange.Type.CARDINALITY_CHANGED,
            BreakingChange.Severity.WARNING,
            "User.tags",
            "Changed",
            null,
            null
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(error, warning));

        String result = formatter.formatBreakingOnly(diff);

        assertTrue(result.contains("**Total:** 1 errors, 1 warnings"));
    }

    @Test
    void formatBreakingOnly_includesBreakingChangesTable() {
        BreakingChange breaking = new BreakingChange(
            BreakingChange.Type.MESSAGE_REMOVED,
            BreakingChange.Severity.ERROR,
            "OldMessage",
            "Message removed",
            "message OldMessage",
            null
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(breaking));

        String result = formatter.formatBreakingOnly(diff);

        assertTrue(result.contains("| Severity | Type | Entity | Description |"));
        assertTrue(result.contains("| ERROR | Message Removed | OldMessage |"));
    }

    @Test
    void format_formatsNestedMessageChanges() {
        MessageInfo nested = createMessage("Address", "street", 1);
        MessageDiff nestedDiff = MessageDiff.added(nested);

        MessageInfo v1 = createMessage("User", "id", 1);
        MessageInfo v2 = createMessage("User", "id", 1);
        MessageDiff parentDiff = new MessageDiff(
            "User", ChangeType.MODIFIED, v1, v2,
            List.of(), List.of(nestedDiff), List.of()
        );

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(parentDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("**Nested message changes:**"));
        assertTrue(result.contains("added: Address"));
    }

    @Test
    void format_formatsNestedEnumChanges() {
        EnumInfo nestedEnum = createEnum("Status", "ACTIVE", 1);
        EnumDiff nestedEnumDiff = EnumDiff.added(nestedEnum);

        MessageInfo v1 = createMessage("User", "id", 1);
        MessageInfo v2 = createMessage("User", "id", 1);
        MessageDiff parentDiff = new MessageDiff(
            "User", ChangeType.MODIFIED, v1, v2,
            List.of(), List.of(), List.of(nestedEnumDiff)
        );

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(parentDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("**Nested enum changes:**"));
        assertTrue(result.contains("added: Status"));
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
