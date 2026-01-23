package io.alnovis.protowrapper.diff;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import io.alnovis.protowrapper.analyzer.ProtoAnalyzer.VersionSchema;
import io.alnovis.protowrapper.diff.model.*;
import io.alnovis.protowrapper.model.FieldMapping;
import io.alnovis.protowrapper.model.MessageInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SchemaDiff with field renumbering support.
 * Tests the full flow through VersionMerger with field mappings and suspected renumber detection.
 */
class SchemaDiffRenumberTest {

    @Nested
    @DisplayName("compare() with fieldMappings - mapped renumbers")
    class MappedRenumberTests {

        @Test
        @DisplayName("Should detect NUMBER_CHANGED for name-based field mapping")
        void shouldDetectNumberChangedForNameMapping() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("parent_ref", 3, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("parent_ref", 5, Type.TYPE_INT64)));

            // Name-based mapping: match by field name "parent_ref"
            FieldMapping mapping = new FieldMapping("Order", "parent_ref");

            SchemaDiff diff = SchemaDiff.compare(v1, v2, List.of(mapping));

            assertThat(diff.getModifiedMessages()).hasSize(1);
            MessageDiff msgDiff = diff.getModifiedMessages().get(0);
            assertThat(msgDiff.fieldChanges()).hasSize(1);

            FieldChange fc = msgDiff.fieldChanges().get(0);
            assertThat(fc.changeType()).isEqualTo(ChangeType.NUMBER_CHANGED);
            assertThat(fc.isRenumberedByMapping()).isTrue();
            assertThat(fc.isBreaking()).isFalse();
        }

        @Test
        @DisplayName("Should detect NUMBER_CHANGED for explicit number mapping")
        void shouldDetectNumberChangedForExplicitMapping() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("amount", 4, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("amount", 6, Type.TYPE_INT64)));

            // Explicit mapping: v1=4, v2=6
            FieldMapping mapping = new FieldMapping("Order", "amount",
                Map.of("v1", 4, "v2", 6));

            SchemaDiff diff = SchemaDiff.compare(v1, v2, List.of(mapping));

            assertThat(diff.getModifiedMessages()).hasSize(1);
            FieldChange fc = diff.getModifiedMessages().get(0).fieldChanges().get(0);
            assertThat(fc.changeType()).isEqualTo(ChangeType.NUMBER_CHANGED);
            assertThat(fc.fieldNumber()).isEqualTo(4); // v1 number used
            assertThat(fc.isRenumberedByMapping()).isTrue();
        }

        @Test
        @DisplayName("Should report mapped renumber as INFO-level breaking change")
        void shouldReportMappedRenumberAsInfo() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("ref", 3, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("ref", 5, Type.TYPE_INT64)));

            FieldMapping mapping = new FieldMapping("Order", "ref");

            SchemaDiff diff = SchemaDiff.compare(v1, v2, List.of(mapping));

            assertThat(diff.hasBreakingChanges()).isFalse(); // No ERROR-level
            assertThat(diff.getBreakingChanges()).hasSize(1);
            BreakingChange bc = diff.getBreakingChanges().get(0);
            assertThat(bc.severity()).isEqualTo(BreakingChange.Severity.INFO);
            assertThat(bc.type()).isEqualTo(BreakingChange.Type.FIELD_NUMBER_CHANGED);
            assertThat(bc.description()).contains("handled by field mapping");
        }

        @Test
        @DisplayName("Should count mapped renumbers in summary")
        void shouldCountMappedRenumbersInSummary() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("ref", 3, Type.TYPE_INT64),
                    field("amount", 4, Type.TYPE_INT32)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("ref", 5, Type.TYPE_INT64),
                    field("amount", 6, Type.TYPE_INT32)));

            List<FieldMapping> mappings = List.of(
                new FieldMapping("Order", "ref"),
                new FieldMapping("Order", "amount")
            );

            SchemaDiff diff = SchemaDiff.compare(v1, v2, mappings);

            assertThat(diff.getMappedRenumberCount()).isEqualTo(2);
            assertThat(diff.getSummary().mappedRenumbers()).isEqualTo(2);
            assertThat(diff.getSummary().hasRenumbers()).isTrue();
        }

        @Test
        @DisplayName("Should detect number change alongside type conflict")
        void shouldDetectNumberChangeWithTypeConflict() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("amount", 4, Type.TYPE_INT32)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("amount", 6, Type.TYPE_INT64)));

            FieldMapping mapping = new FieldMapping("Order", "amount");

            SchemaDiff diff = SchemaDiff.compare(v1, v2, List.of(mapping));

            FieldChange fc = diff.getModifiedMessages().get(0).fieldChanges().get(0);
            assertThat(fc.changeType()).isEqualTo(ChangeType.NUMBER_CHANGED);
            // Should also report the type change in the changes list
            assertThat(fc.changes()).anyMatch(c -> c.contains("Number:"));
            assertThat(fc.changes()).anyMatch(c -> c.contains("Type:"));
        }

        @Test
        @DisplayName("Should handle multiple field mappings in same message")
        void shouldHandleMultipleMappingsInSameMessage() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("ref", 3, Type.TYPE_INT64),
                    field("code", 7, Type.TYPE_STRING)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("ref", 5, Type.TYPE_INT64),
                    field("code", 8, Type.TYPE_STRING)));

            List<FieldMapping> mappings = List.of(
                new FieldMapping("Order", "ref"),
                new FieldMapping("Order", "code")
            );

            SchemaDiff diff = SchemaDiff.compare(v1, v2, mappings);

            assertThat(diff.getModifiedMessages()).hasSize(1);
            List<FieldChange> changes = diff.getModifiedMessages().get(0).fieldChanges();
            assertThat(changes).hasSize(2);
            assertThat(changes).allMatch(fc -> fc.changeType() == ChangeType.NUMBER_CHANGED);
            assertThat(changes).allMatch(FieldChange::isRenumberedByMapping);
        }
    }

    @Nested
    @DisplayName("compare() without fieldMappings - suspected renumbers")
    class SuspectedRenumberTests {

        @Test
        @DisplayName("Should detect suspected renumber for field with same name and type")
        void shouldDetectSuspectedRenumber() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("parent_ref", 3, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("parent_ref", 5, Type.TYPE_INT64)));

            // No field mappings — should detect as suspected
            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            assertThat(diff.hasSuspectedRenumbers()).isTrue();
            assertThat(diff.getSuspectedRenumbers()).hasSize(1);

            SuspectedRenumber sr = diff.getSuspectedRenumbers().get(0);
            assertThat(sr.messageName()).isEqualTo("Order");
            assertThat(sr.fieldName()).isEqualTo("parent_ref");
            assertThat(sr.v1Number()).isEqualTo(3);
            assertThat(sr.v2Number()).isEqualTo(5);
            assertThat(sr.confidence()).isEqualTo(SuspectedRenumber.Confidence.HIGH);
        }

        @Test
        @DisplayName("Should detect MEDIUM confidence for compatible type change")
        void shouldDetectMediumConfidenceForTypeChange() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("amount", 4, Type.TYPE_INT32)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("amount", 6, Type.TYPE_INT64)));

            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            assertThat(diff.hasSuspectedRenumbers()).isTrue();
            assertThat(diff.getSuspectedRenumbers().get(0).confidence())
                .isEqualTo(SuspectedRenumber.Confidence.MEDIUM);
        }

        @Test
        @DisplayName("Should count suspected renumbers in summary")
        void shouldCountSuspectedRenumbersInSummary() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("ref", 3, Type.TYPE_INT64),
                    field("code", 7, Type.TYPE_STRING)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("ref", 5, Type.TYPE_INT64),
                    field("code", 8, Type.TYPE_STRING)));

            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            assertThat(diff.getSummary().suspectedRenumbers()).isEqualTo(2);
            assertThat(diff.getSummary().hasRenumbers()).isTrue();
        }

        @Test
        @DisplayName("Should not detect suspected renumber for incompatible types")
        void shouldNotDetectForIncompatibleTypes() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("status", 3, Type.TYPE_STRING)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("status", 5, Type.TYPE_BOOL)));

            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            assertThat(diff.hasSuspectedRenumbers()).isFalse();
        }

        @Test
        @DisplayName("Should detect suspected renumber for displaced field (field takes position of removed field)")
        void shouldDetectDisplacedFieldRenumber() {
            // Scenario: ref_id moved from #7 to #5, displacing old_counter
            // Merger sees: #5 NAME_CHANGED (old_counter -> ref_id), #7 REMOVED (ref_id)
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Transaction",
                    field("id", 1, Type.TYPE_INT64),
                    field("old_counter", 5, Type.TYPE_UINT32),
                    field("ref_id", 7, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Transaction",
                    field("id", 1, Type.TYPE_INT64),
                    field("ref_id", 5, Type.TYPE_INT64)));

            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            assertThat(diff.hasSuspectedRenumbers()).isTrue();
            List<SuspectedRenumber> suspected = diff.getSuspectedRenumbers();
            assertThat(suspected).anyMatch(sr ->
                sr.fieldName().equals("ref_id") &&
                sr.v1Number() == 7 &&
                sr.v2Number() == 5 &&
                sr.confidence() == SuspectedRenumber.Confidence.HIGH);
        }

        @Test
        @DisplayName("Should detect displaced field with compatible type change as MEDIUM confidence")
        void shouldDetectDisplacedFieldWithTypeChange() {
            // amount moved from #10 to #5, type widened int32 -> int64
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("old_field", 5, Type.TYPE_STRING),
                    field("amount", 10, Type.TYPE_INT32)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("amount", 5, Type.TYPE_INT64)));

            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            assertThat(diff.hasSuspectedRenumbers()).isTrue();
            List<SuspectedRenumber> suspected = diff.getSuspectedRenumbers();
            assertThat(suspected).anyMatch(sr ->
                sr.fieldName().equals("amount") &&
                sr.v1Number() == 10 &&
                sr.v2Number() == 5 &&
                sr.confidence() == SuspectedRenumber.Confidence.MEDIUM);
        }

        @Test
        @DisplayName("Should NOT detect displaced field with incompatible types")
        void shouldNotDetectDisplacedFieldWithIncompatibleTypes() {
            // field moved from #10 to #5, but type changed incompatibly (string -> bool)
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("old_field", 5, Type.TYPE_INT32),
                    field("flag", 10, Type.TYPE_STRING)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("flag", 5, Type.TYPE_BOOL)));

            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            // "flag" should not be detected (string -> bool is incompatible)
            assertThat(diff.getSuspectedRenumbers()).noneMatch(sr ->
                sr.fieldName().equals("flag"));
        }

        @Test
        @DisplayName("Should suggest mapping configuration via toSuggestedMapping")
        void shouldSuggestMappingConfiguration() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("parent_ref", 3, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("parent_ref", 5, Type.TYPE_INT64)));

            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            SuspectedRenumber sr = diff.getSuspectedRenumbers().get(0);
            FieldMapping mapping = sr.toSuggestedMapping("v1", "v2");

            assertThat(mapping.getMessage()).isEqualTo("Order");
            assertThat(mapping.getFieldName()).isEqualTo("parent_ref");
            assertThat(mapping.getVersionNumbers()).containsEntry("v1", 3);
            assertThat(mapping.getVersionNumbers()).containsEntry("v2", 5);
        }
    }

    @Nested
    @DisplayName("compare() with fieldMappings suppresses suspected renumbers")
    class MappedSuppressesSuspectedTests {

        @Test
        @DisplayName("Mapped fields should not appear as suspected renumbers")
        void mappedFieldsShouldNotAppearAsSuspected() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("parent_ref", 3, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("parent_ref", 5, Type.TYPE_INT64)));

            FieldMapping mapping = new FieldMapping("Order", "parent_ref");

            SchemaDiff diff = SchemaDiff.compare(v1, v2, List.of(mapping));

            // When mapped, there are no REMOVED+ADDED pairs, so no suspected renumbers
            assertThat(diff.hasSuspectedRenumbers()).isFalse();
            assertThat(diff.getMappedRenumberCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Summary and DiffSummary record")
    class SummaryTests {

        @Test
        @DisplayName("Summary should include both mapped and suspected counts")
        void summaryShouldIncludeBothCounts() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("ref", 3, Type.TYPE_INT64),
                    field("code", 7, Type.TYPE_STRING)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("ref", 5, Type.TYPE_INT64),
                    field("code", 8, Type.TYPE_STRING)));

            // Map only "ref", leave "code" as suspected
            FieldMapping mapping = new FieldMapping("Order", "ref");

            SchemaDiff diff = SchemaDiff.compare(v1, v2, List.of(mapping));

            SchemaDiff.DiffSummary summary = diff.getSummary();
            assertThat(summary.mappedRenumbers()).isEqualTo(1);
            assertThat(summary.suspectedRenumbers()).isEqualTo(1);
            assertThat(summary.hasRenumbers()).isTrue();
        }

        @Test
        @DisplayName("hasRenumbers() should return false when no renumbers")
        void hasRenumbersShouldReturnFalseWhenNone() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("id", 1, Type.TYPE_INT64),
                    field("name", 2, Type.TYPE_STRING)));

            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            assertThat(diff.getSummary().hasRenumbers()).isFalse();
        }

        @Test
        @DisplayName("toString() should include renumber info when present")
        void toStringShouldIncludeRenumberInfo() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("ref", 3, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("ref", 5, Type.TYPE_INT64)));

            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            String summaryStr = diff.getSummary().toString();
            assertThat(summaryStr).contains("Renumbers:");
            assertThat(summaryStr).contains("suspected");
        }
    }

    @Nested
    @DisplayName("Output format integration")
    class OutputFormatTests {

        @Test
        @DisplayName("toText() should include renumber section for suspected renumbers")
        void toTextShouldIncludeRenumberSection() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("ref", 3, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("ref", 5, Type.TYPE_INT64)));

            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            String text = diff.toText();
            assertThat(text).contains("SUSPECTED RENUMBERED FIELDS");
            assertThat(text).contains("Order.ref");
        }

        @Test
        @DisplayName("toJson() should include suspected renumbers array")
        void toJsonShouldIncludeSuspectedRenumbers() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("ref", 3, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("ref", 5, Type.TYPE_INT64)));

            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            String json = diff.toJson();
            assertThat(json).contains("\"suspectedRenumbers\"");
            assertThat(json).contains("\"fieldName\": \"ref\"");
        }

        @Test
        @DisplayName("toMarkdown() should include suspected renumbers section")
        void toMarkdownShouldIncludeSuspectedRenumbers() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("ref", 3, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("ref", 5, Type.TYPE_INT64)));

            SchemaDiff diff = SchemaDiff.compare(v1, v2);

            String markdown = diff.toMarkdown();
            assertThat(markdown).contains("## Suspected Renumbered Fields");
            assertThat(markdown).contains("Order.ref");
        }

        @Test
        @DisplayName("toText() should include mapped renumber for mapped fields")
        void toTextShouldIncludeMappedRenumber() {
            VersionSchema v1 = createVersionSchema("v1",
                createMessage("Order",
                    field("ref", 3, Type.TYPE_INT64)));

            VersionSchema v2 = createVersionSchema("v2",
                createMessage("Order",
                    field("ref", 5, Type.TYPE_INT64)));

            FieldMapping mapping = new FieldMapping("Order", "ref");
            SchemaDiff diff = SchemaDiff.compare(v1, v2, List.of(mapping));

            String text = diff.toText();
            assertThat(text).contains("Renumbered field: ref");
            assertThat(text).contains("[MAPPED]");
        }
    }

    // ========== Helper Methods ==========

    private VersionSchema createVersionSchema(String version, MessageInfo... messages) {
        VersionSchema schema = new VersionSchema(version);
        for (MessageInfo msg : messages) {
            schema.addMessage(msg);
        }
        return schema;
    }

    private MessageInfo createMessage(String name, FieldDescriptorProto... fields) {
        DescriptorProto.Builder builder = DescriptorProto.newBuilder().setName(name);
        for (FieldDescriptorProto field : fields) {
            builder.addField(field);
        }
        return new MessageInfo(builder.build(), "test.package");
    }

    private FieldDescriptorProto field(String name, int number, Type type) {
        return FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(number)
            .setType(type)
            .setLabel(Label.LABEL_OPTIONAL)
            .build();
    }
}
