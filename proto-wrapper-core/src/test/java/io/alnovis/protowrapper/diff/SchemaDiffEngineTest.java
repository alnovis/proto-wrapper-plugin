package io.alnovis.protowrapper.diff;

import com.google.protobuf.DescriptorProtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import io.alnovis.protowrapper.diff.model.*;
import io.alnovis.protowrapper.model.EnumInfo;
import io.alnovis.protowrapper.model.MessageInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SchemaDiffEngine.
 */
class SchemaDiffEngineTest {

    private SchemaDiffEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SchemaDiffEngine();
    }

    @Nested
    class MessageComparison {

        @Test
        void detectsAddedMessage() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            // Add message only to v2
            MessageInfo newMessage = createMessage("NewMessage", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING);
            v2.addMessage(newMessage);

            SchemaDiff diff = engine.compare(v1, v2);

            assertEquals(1, diff.getAddedMessages().size());
            assertEquals("NewMessage", diff.getAddedMessages().get(0).getName());
            assertEquals(0, diff.getRemovedMessages().size());
            assertEquals(0, diff.getModifiedMessages().size());
        }

        @Test
        void detectsRemovedMessage() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            // Add message only to v1
            MessageInfo oldMessage = createMessage("OldMessage", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING);
            v1.addMessage(oldMessage);

            SchemaDiff diff = engine.compare(v1, v2);

            assertEquals(0, diff.getAddedMessages().size());
            assertEquals(1, diff.getRemovedMessages().size());
            assertEquals("OldMessage", diff.getRemovedMessages().get(0).getName());
            // Message removal is now INFO level (plugin-handled), not breaking
            assertFalse(diff.hasBreakingChanges());
        }

        @Test
        void detectsAddedField() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            // v1: message with one field
            MessageInfo v1Msg = createMessage("TestMessage", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING);
            v1.addMessage(v1Msg);

            // v2: same message with additional field
            DescriptorProto.Builder msgBuilder = DescriptorProto.newBuilder()
                .setName("TestMessage")
                .addField(createFieldProto(1, "field1", FieldDescriptorProto.Type.TYPE_STRING))
                .addField(createFieldProto(2, "field2", FieldDescriptorProto.Type.TYPE_INT32));
            MessageInfo v2Msg = new MessageInfo(msgBuilder.build(), "test.package");
            v2.addMessage(v2Msg);

            SchemaDiff diff = engine.compare(v1, v2);

            assertEquals(0, diff.getAddedMessages().size());
            assertEquals(1, diff.getModifiedMessages().size());

            MessageDiff msgDiff = diff.getModifiedMessages().get(0);
            assertEquals("TestMessage", msgDiff.messageName());
            assertEquals(1, msgDiff.getAddedFields().size());
            assertEquals("field2", msgDiff.getAddedFields().get(0).fieldName());
        }

        @Test
        void detectsRemovedField() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            // v1: message with two fields
            DescriptorProto.Builder v1MsgBuilder = DescriptorProto.newBuilder()
                .setName("TestMessage")
                .addField(createFieldProto(1, "field1", FieldDescriptorProto.Type.TYPE_STRING))
                .addField(createFieldProto(2, "field2", FieldDescriptorProto.Type.TYPE_INT32));
            v1.addMessage(new MessageInfo(v1MsgBuilder.build(), "test.package"));

            // v2: same message with one field removed
            MessageInfo v2Msg = createMessage("TestMessage", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING);
            v2.addMessage(v2Msg);

            SchemaDiff diff = engine.compare(v1, v2);

            assertEquals(1, diff.getModifiedMessages().size());
            MessageDiff msgDiff = diff.getModifiedMessages().get(0);
            assertEquals(1, msgDiff.getRemovedFields().size());
            assertEquals("field2", msgDiff.getRemovedFields().get(0).fieldName());
            // Field removal is now INFO level (plugin-handled), not breaking
            assertFalse(diff.hasBreakingChanges());
        }

        @Test
        void detectsTypeChange() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            // v1: field is string
            MessageInfo v1Msg = createMessage("TestMessage", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING);
            v1.addMessage(v1Msg);

            // v2: field is int32
            MessageInfo v2Msg = createMessage("TestMessage", 1, "field1", FieldDescriptorProto.Type.TYPE_INT32);
            v2.addMessage(v2Msg);

            SchemaDiff diff = engine.compare(v1, v2);

            assertEquals(1, diff.getModifiedMessages().size());
            MessageDiff msgDiff = diff.getModifiedMessages().get(0);
            assertEquals(1, msgDiff.getModifiedFields().size());
            assertEquals(ChangeType.TYPE_CHANGED, msgDiff.getModifiedFields().get(0).changeType());
        }
    }

    @Nested
    class EnumComparison {

        @Test
        void detectsAddedEnum() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            EnumInfo newEnum = createEnum("NewEnum", "VALUE1", 0, "VALUE2", 1);
            v2.addEnum(newEnum);

            SchemaDiff diff = engine.compare(v1, v2);

            assertEquals(1, diff.getAddedEnums().size());
            assertEquals("NewEnum", diff.getAddedEnums().get(0).getName());
        }

        @Test
        void detectsRemovedEnum() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            EnumInfo oldEnum = createEnum("OldEnum", "VALUE1", 0);
            v1.addEnum(oldEnum);

            SchemaDiff diff = engine.compare(v1, v2);

            assertEquals(1, diff.getRemovedEnums().size());
            assertEquals("OldEnum", diff.getRemovedEnums().get(0).getName());
            // Enum removal is now INFO level (plugin-handled), not breaking
            assertFalse(diff.hasBreakingChanges());
        }

        @Test
        void detectsAddedEnumValue() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            EnumInfo v1Enum = createEnum("TestEnum", "VALUE1", 0);
            v1.addEnum(v1Enum);

            EnumInfo v2Enum = createEnum("TestEnum", "VALUE1", 0, "VALUE2", 1);
            v2.addEnum(v2Enum);

            SchemaDiff diff = engine.compare(v1, v2);

            assertEquals(1, diff.getModifiedEnums().size());
            EnumDiff enumDiff = diff.getModifiedEnums().get(0);
            assertEquals(1, enumDiff.getAddedValues().size());
            assertEquals("VALUE2", enumDiff.getAddedValues().get(0).valueName());
            assertFalse(enumDiff.hasBreakingChanges()); // Adding a value is not breaking
        }

        @Test
        void detectsRemovedEnumValue() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            EnumInfo v1Enum = createEnum("TestEnum", "VALUE1", 0, "VALUE2", 1);
            v1.addEnum(v1Enum);

            EnumInfo v2Enum = createEnum("TestEnum", "VALUE1", 0);
            v2.addEnum(v2Enum);

            SchemaDiff diff = engine.compare(v1, v2);

            assertEquals(1, diff.getModifiedEnums().size());
            EnumDiff enumDiff = diff.getModifiedEnums().get(0);
            assertEquals(1, enumDiff.getRemovedValues().size());
            assertEquals("VALUE2", enumDiff.getRemovedValues().get(0).valueName());
            assertTrue(enumDiff.hasBreakingChanges());
        }
    }

    @Nested
    class ChangeDetection {

        @Test
        void messageRemovalIsPluginHandled() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            v1.addMessage(createMessage("RemovedMessage", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING));

            SchemaDiff diff = engine.compare(v1, v2);

            // Message removal is INFO level (plugin-handled), not ERROR
            assertFalse(diff.hasBreakingChanges(), "Message removal should be plugin-handled, not breaking");
            assertEquals(0, diff.getErrors().size());

            // Should be in the breaking changes list but with INFO severity
            List<BreakingChange> all = diff.getBreakingChanges();
            assertEquals(1, all.size());
            assertEquals(BreakingChange.Type.MESSAGE_REMOVED, all.get(0).type());
            assertEquals(BreakingChange.Severity.INFO, all.get(0).severity());
        }

        @Test
        void fieldRemovalIsPluginHandled() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            DescriptorProto.Builder v1MsgBuilder = DescriptorProto.newBuilder()
                .setName("TestMessage")
                .addField(createFieldProto(1, "field1", FieldDescriptorProto.Type.TYPE_STRING))
                .addField(createFieldProto(2, "field2", FieldDescriptorProto.Type.TYPE_INT32));
            v1.addMessage(new MessageInfo(v1MsgBuilder.build(), "test.package"));

            v2.addMessage(createMessage("TestMessage", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING));

            SchemaDiff diff = engine.compare(v1, v2);

            // Field removal is INFO level (plugin-handled), not ERROR
            assertFalse(diff.hasBreakingChanges(), "Field removal should be plugin-handled, not breaking");

            // Should be in the breaking changes list but with INFO severity
            List<BreakingChange> all = diff.getBreakingChanges();
            assertTrue(all.stream().anyMatch(bc ->
                bc.type() == BreakingChange.Type.FIELD_REMOVED &&
                bc.severity() == BreakingChange.Severity.INFO));
        }

        @Test
        void enumValueRemovalIsPluginHandled() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            v1.addEnum(createEnum("TestEnum", "VALUE1", 0, "VALUE2", 1));
            v2.addEnum(createEnum("TestEnum", "VALUE1", 0));

            SchemaDiff diff = engine.compare(v1, v2);

            // Enum value removal is INFO level (plugin-handled), not ERROR
            assertFalse(diff.hasBreakingChanges(), "Enum value removal should be plugin-handled, not breaking");

            // Should be in the breaking changes list but with INFO severity
            assertTrue(diff.getBreakingChanges().stream()
                .anyMatch(bc -> bc.type() == BreakingChange.Type.ENUM_VALUE_REMOVED &&
                               bc.severity() == BreakingChange.Severity.INFO));
        }
    }

    @Nested
    class OutputFormatting {

        @Test
        void textFormatterProducesOutput() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            v1.addMessage(createMessage("Message1", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING));
            v2.addMessage(createMessage("Message2", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING));

            SchemaDiff diff = engine.compare(v1, v2);
            String text = diff.toText();

            assertNotNull(text);
            assertTrue(text.contains("Schema Comparison: v1 -> v2"));
            assertTrue(text.contains("ADDED: Message2"));
            assertTrue(text.contains("REMOVED: Message1"));
        }

        @Test
        void jsonFormatterProducesValidJson() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            v2.addMessage(createMessage("NewMessage", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING));

            SchemaDiff diff = engine.compare(v1, v2);
            String json = diff.toJson();

            assertNotNull(json);
            assertTrue(json.startsWith("{"));
            assertTrue(json.contains("\"v1\": \"v1\""));
            assertTrue(json.contains("\"v2\": \"v2\""));
            assertTrue(json.contains("\"addedMessages\": 1"));
        }

        @Test
        void markdownFormatterProducesOutput() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            v2.addMessage(createMessage("NewMessage", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING));

            SchemaDiff diff = engine.compare(v1, v2);
            String markdown = diff.toMarkdown();

            assertNotNull(markdown);
            assertTrue(markdown.contains("# Schema Comparison: v1 -> v2"));
            assertTrue(markdown.contains("## Summary"));
            assertTrue(markdown.contains("| Category | Added | Modified | Removed |"));
        }
    }

    @Nested
    class SummaryCalculation {

        @Test
        void summaryCountsCorrectly() {
            VersionSchema v1 = new VersionSchema("v1");
            VersionSchema v2 = new VersionSchema("v2");

            // Add 2 messages to v1, remove 1, keep 1 modified
            v1.addMessage(createMessage("Message1", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING));
            v1.addMessage(createMessage("Message2", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING));

            // v2: Message1 modified (different field type), Message2 removed, Message3 added
            v2.addMessage(createMessage("Message1", 1, "field1", FieldDescriptorProto.Type.TYPE_INT32));
            v2.addMessage(createMessage("Message3", 1, "field1", FieldDescriptorProto.Type.TYPE_STRING));

            SchemaDiff diff = engine.compare(v1, v2);
            SchemaDiff.DiffSummary summary = diff.getSummary();

            assertEquals(1, summary.addedMessages());  // Message3
            assertEquals(1, summary.removedMessages()); // Message2
            assertEquals(1, summary.modifiedMessages()); // Message1
        }
    }

    // Helper methods

    private MessageInfo createMessage(String name, int fieldNum, String fieldName, FieldDescriptorProto.Type type) {
        DescriptorProto.Builder builder = DescriptorProto.newBuilder()
            .setName(name)
            .addField(createFieldProto(fieldNum, fieldName, type));
        return new MessageInfo(builder.build(), "test.package");
    }

    private FieldDescriptorProto createFieldProto(int number, String name, FieldDescriptorProto.Type type) {
        return FieldDescriptorProto.newBuilder()
            .setNumber(number)
            .setName(name)
            .setType(type)
            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
            .build();
    }

    private EnumInfo createEnum(String name, Object... valuesAndNumbers) {
        EnumDescriptorProto.Builder builder = EnumDescriptorProto.newBuilder().setName(name);
        for (int i = 0; i < valuesAndNumbers.length; i += 2) {
            String valueName = (String) valuesAndNumbers[i];
            int valueNumber = (Integer) valuesAndNumbers[i + 1];
            builder.addValue(EnumValueDescriptorProto.newBuilder()
                .setName(valueName)
                .setNumber(valueNumber)
                .build());
        }
        return new EnumInfo(builder.build());
    }
}
