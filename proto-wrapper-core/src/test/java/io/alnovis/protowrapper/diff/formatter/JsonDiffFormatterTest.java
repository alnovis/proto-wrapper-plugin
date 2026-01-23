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
 * Unit tests for JsonDiffFormatter.
 */
class JsonDiffFormatterTest {

    private JsonDiffFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new JsonDiffFormatter();
    }

    @Test
    void format_producesValidJsonStructure() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.format(diff);

        assertTrue(result.startsWith("{"));
        assertTrue(result.trim().endsWith("}"));
        assertTrue(result.contains("\"v1\": \"v1\""));
        assertTrue(result.contains("\"v2\": \"v2\""));
    }

    @Test
    void format_includesSummarySection() {
        MessageInfo added = createMessage("NewMessage", "id", 1);
        MessageDiff messageDiff = MessageDiff.added(added);
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("\"summary\":"));
        assertTrue(result.contains("\"addedMessages\": 1"));
        assertTrue(result.contains("\"removedMessages\": 0"));
        assertTrue(result.contains("\"modifiedMessages\": 0"));
    }

    @Test
    void format_includesMessagesSection() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.format(diff);

        assertTrue(result.contains("\"messages\":"));
        assertTrue(result.contains("\"added\":"));
        assertTrue(result.contains("\"removed\":"));
        assertTrue(result.contains("\"modified\":"));
    }

    @Test
    void format_includesEnumsSection() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.format(diff);

        assertTrue(result.contains("\"enums\":"));
    }

    @Test
    void format_includesBreakingChangesSection() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.format(diff);

        assertTrue(result.contains("\"breakingChanges\":"));
    }

    @Test
    void format_formatsAddedMessages() {
        MessageInfo addedMessage = createMessage("User", "id", 1, "name", 2);
        MessageDiff messageDiff = MessageDiff.added(addedMessage);
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("\"name\": \"User\""));
        assertTrue(result.contains("\"fields\":"));
        assertTrue(result.contains("\"name\": \"id\""));
        assertTrue(result.contains("\"name\": \"name\""));
        assertTrue(result.contains("\"type\": \"string\""));
    }

    @Test
    void format_formatsRemovedMessages() {
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

        assertTrue(result.contains("\"removed\":"));
        assertTrue(result.contains("\"name\": \"OldMessage\""));
    }

    @Test
    void format_formatsModifiedMessages() {
        MessageInfo v1 = createMessage("User", "id", 1);
        MessageInfo v2 = createMessage("User", "id", 1, "email", 2);

        FieldInfo addedField = createField("email", 2, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange fieldChange = new FieldChange(2, "email", ChangeType.ADDED, null, addedField, List.of());
        MessageDiff messageDiff = MessageDiff.compared(v1, v2, List.of(fieldChange), List.of(), List.of());

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("\"modified\":"));
        assertTrue(result.contains("\"fieldChanges\":"));
        assertTrue(result.contains("\"fieldNumber\": 2"));
        assertTrue(result.contains("\"fieldName\": \"email\""));
        assertTrue(result.contains("\"changeType\": \"ADDED\""));
    }

    @Test
    void format_includesBreakingFlag_inFieldChanges() {
        MessageInfo v1 = createMessage("User", "id", 1, "deprecated", 2);
        MessageInfo v2 = createMessage("User", "id", 1);

        FieldInfo removed = createField("deprecated", 2, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange fieldChange = new FieldChange(2, "deprecated", ChangeType.REMOVED, removed, null, List.of());
        MessageDiff messageDiff = MessageDiff.compared(v1, v2, List.of(fieldChange), List.of(), List.of());

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("\"breaking\": true"));
    }

    @Test
    void format_formatsAddedEnums() {
        EnumInfo addedEnum = createEnum("Status", "PENDING", 0, "ACTIVE", 1);
        EnumDiff enumDiff = EnumDiff.added(addedEnum);
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("\"name\": \"Status\""));
        assertTrue(result.contains("\"values\":"));
        assertTrue(result.contains("\"name\": \"PENDING\""));
        assertTrue(result.contains("\"number\": 0"));
        assertTrue(result.contains("\"name\": \"ACTIVE\""));
        assertTrue(result.contains("\"number\": 1"));
    }

    @Test
    void format_formatsRemovedEnums() {
        EnumInfo removedEnum = createEnum("OldEnum", "VALUE", 0);
        EnumDiff enumDiff = EnumDiff.removed(removedEnum);
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("\"name\": \"OldEnum\""));
    }

    @Test
    void format_formatsModifiedEnums() {
        EnumInfo v1 = createEnum("Status", "PENDING", 0);
        EnumInfo v2 = createEnum("Status", "PENDING", 0, "ACTIVE", 1);

        List<EnumValueChange> changes = List.of(EnumValueChange.added("ACTIVE", 1));
        EnumDiff enumDiff = EnumDiff.modified(v1, v2, changes);

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(enumDiff), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("\"addedValues\":"));
        assertTrue(result.contains("\"name\": \"ACTIVE\""));
    }

    @Test
    void format_formatsBreakingChanges() {
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

        assertTrue(result.contains("\"type\": \"FIELD_REMOVED\""));
        assertTrue(result.contains("\"severity\": \"ERROR\""));
        assertTrue(result.contains("\"entityPath\": \"User.email\""));
        assertTrue(result.contains("\"description\": \"Field removed\""));
        assertTrue(result.contains("\"v1Value\": \"string email = 3\""));
    }

    @Test
    void format_includesV2Value_whenPresent() {
        BreakingChange breaking = new BreakingChange(
            BreakingChange.Type.FIELD_TYPE_INCOMPATIBLE,
            BreakingChange.Severity.ERROR,
            "User.status",
            "Type changed",
            "string",
            "int32"
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(breaking));

        String result = formatter.format(diff);

        assertTrue(result.contains("\"v1Value\": \"string\""));
        assertTrue(result.contains("\"v2Value\": \"int32\""));
    }

    @Test
    void format_escapesJsonStrings() {
        // Create a message with a field that has special characters
        DescriptorProto proto = DescriptorProto.newBuilder()
            .setName("Test")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("field_with_special")
                .setNumber(1)
                .setType(Type.TYPE_STRING)
                .setLabel(Label.LABEL_OPTIONAL)
                .build())
            .build();
        MessageInfo message = new MessageInfo(proto, "test.package");
        MessageDiff messageDiff = MessageDiff.added(message);

        BreakingChange breaking = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "Test",
            "Description with \"quotes\" and \\ backslash",
            null,
            null
        );

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of(breaking));

        String result = formatter.format(diff);

        assertTrue(result.contains("\\\"quotes\\\""));
        assertTrue(result.contains("\\\\"));
    }

    @Test
    void formatBreakingOnly_producesValidJsonStructure() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.formatBreakingOnly(diff);

        assertTrue(result.startsWith("{"));
        assertTrue(result.trim().endsWith("}"));
    }

    @Test
    void formatBreakingOnly_includesVersionInfo() {
        SchemaDiff diff = createEmptyDiff("production", "development");

        String result = formatter.formatBreakingOnly(diff);

        assertTrue(result.contains("\"v1\": \"production\""));
        assertTrue(result.contains("\"v2\": \"development\""));
    }

    @Test
    void formatBreakingOnly_includesBreakingChanges() {
        BreakingChange error = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "User.email",
            "Field removed",
            "string email = 3",
            null
        );
        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(error));

        String result = formatter.formatBreakingOnly(diff);

        assertTrue(result.contains("\"breakingChanges\":"));
        assertTrue(result.contains("\"type\": \"FIELD_REMOVED\""));
    }

    @Test
    void formatBreakingOnly_includesErrorAndWarningCounts() {
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

        assertTrue(result.contains("\"errorCount\": 1"));
        assertTrue(result.contains("\"warningCount\": 1"));
    }

    @Test
    void format_includesRepeatedFlag_forRepeatedFields() {
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

        assertTrue(result.contains("\"repeated\": true"));
    }

    // ========== Renumber Tests ==========

    @Test
    void format_showsMappedRenumberedFieldWithNumbers() {
        MessageInfo v1 = createMessage("Order", "id", 1);
        MessageInfo v2 = createMessage("Order", "id", 1);

        FieldInfo v1Field = createField("parent_ref", 3, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
        FieldInfo v2Field = createField("parent_ref", 5, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
        FieldChange fieldChange = new FieldChange(3, "parentRef", ChangeType.NUMBER_CHANGED,
            v1Field, v2Field, List.of("Number: #3 -> #5 (mapped)"));
        MessageDiff messageDiff = MessageDiff.compared(v1, v2, List.of(fieldChange), List.of(), List.of());

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of());

        String result = formatter.format(diff);

        assertTrue(result.contains("\"changeType\": \"NUMBER_CHANGED\""));
        assertTrue(result.contains("\"v1Number\": 3"));
        assertTrue(result.contains("\"v2Number\": 5"));
        assertTrue(result.contains("\"mapped\": true"));
    }

    @Test
    void format_showsSuspectedRenumbersArray() {
        FieldInfo v1Field = createField("parent_ref", 3, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
        FieldInfo v2Field = createField("parent_ref", 5, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

        SuspectedRenumber sr = new SuspectedRenumber(
            "Order", "parent_ref", 3, 5, v1Field, v2Field,
            SuspectedRenumber.Confidence.HIGH
        );

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(), List.of(sr));

        String result = formatter.format(diff);

        assertTrue(result.contains("\"suspectedRenumbers\""));
        assertTrue(result.contains("\"messageName\": \"Order\""));
        assertTrue(result.contains("\"fieldName\": \"parent_ref\""));
        assertTrue(result.contains("\"v1Number\": 3"));
        assertTrue(result.contains("\"v2Number\": 5"));
        assertTrue(result.contains("\"confidence\": \"HIGH\""));
        assertTrue(result.contains("\"type\": \"int64\""));
        assertTrue(result.contains("\"suggestedMapping\""));
    }

    @Test
    void format_showsMediumConfidenceSuspectedRenumber() {
        FieldInfo v1Field = createField("amount", 4, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldInfo v2Field = createField("amount", 6, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

        SuspectedRenumber sr = new SuspectedRenumber(
            "Order", "amount", 4, 6, v1Field, v2Field,
            SuspectedRenumber.Confidence.MEDIUM
        );

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(), List.of(sr));

        String result = formatter.format(diff);

        assertTrue(result.contains("\"confidence\": \"MEDIUM\""));
        assertTrue(result.contains("\"type\": \"int32\""));
    }

    @Test
    void format_showsRenumberCountsInSummary() {
        MessageInfo v1 = createMessage("Order", "id", 1);
        MessageInfo v2 = createMessage("Order", "id", 1);

        FieldInfo v1Field = createField("ref", 3, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
        FieldInfo v2Field = createField("ref", 5, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
        FieldChange fieldChange = new FieldChange(3, "ref", ChangeType.NUMBER_CHANGED,
            v1Field, v2Field, List.of("Number: #3 -> #5 (mapped)"));
        MessageDiff messageDiff = MessageDiff.compared(v1, v2, List.of(fieldChange), List.of(), List.of());

        FieldInfo v1Sr = createField("amount", 4, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldInfo v2Sr = createField("amount", 6, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
        SuspectedRenumber sr = new SuspectedRenumber("Order", "amount", 4, 6, v1Sr, v2Sr,
            SuspectedRenumber.Confidence.MEDIUM);

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(messageDiff), List.of(), List.of(), List.of(sr));

        String result = formatter.format(diff);

        assertTrue(result.contains("\"mappedRenumbers\": 1"));
        assertTrue(result.contains("\"suspectedRenumbers\": 1"));
    }

    @Test
    void format_noSuspectedRenumbersArraySection_whenEmpty() {
        SchemaDiff diff = createEmptyDiff("v1", "v2");

        String result = formatter.format(diff);

        // The summary always has "suspectedRenumbers": 0, but the array section should not appear
        assertFalse(result.contains("\"messageName\""));
        assertFalse(result.contains("\"suggestedMapping\""));
        // Summary should show zero
        assertTrue(result.contains("\"suspectedRenumbers\": 0"));
    }

    @Test
    void format_showsSuggestedMappingForSuspectedRenumber() {
        FieldInfo v1Field = createField("ref", 10, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
        FieldInfo v2Field = createField("ref", 15, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

        SuspectedRenumber sr = new SuspectedRenumber(
            "Payment", "ref", 10, 15, v1Field, v2Field,
            SuspectedRenumber.Confidence.HIGH
        );

        SchemaDiff diff = new SchemaDiff("v1", "v2", List.of(), List.of(), List.of(), List.of(sr));

        String result = formatter.format(diff);

        assertTrue(result.contains("<fieldMapping><message>Payment</message><fieldName>ref</fieldName></fieldMapping>"));
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
