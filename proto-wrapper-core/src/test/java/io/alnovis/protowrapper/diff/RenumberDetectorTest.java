package io.alnovis.protowrapper.diff;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import io.alnovis.protowrapper.diff.model.*;
import io.alnovis.protowrapper.model.FieldInfo;
import io.alnovis.protowrapper.model.MessageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RenumberDetector heuristic detection.
 */
class RenumberDetectorTest {

    private RenumberDetector detector;

    @BeforeEach
    void setUp() {
        detector = new RenumberDetector();
    }

    @Nested
    @DisplayName("HIGH confidence detection (same name + same type)")
    class HighConfidenceTests {

        @Test
        @DisplayName("Should detect field with same name and same type as HIGH confidence")
        void shouldDetectSameNameSameType() {
            FieldInfo v1Field = createField("parent_ref", 3, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("parent_ref", 5, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Order", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            SuspectedRenumber sr = result.get(0);
            assertThat(sr.messageName()).isEqualTo("Order");
            assertThat(sr.fieldName()).isEqualTo("parent_ref");
            assertThat(sr.v1Number()).isEqualTo(3);
            assertThat(sr.v2Number()).isEqualTo(5);
            assertThat(sr.confidence()).isEqualTo(SuspectedRenumber.Confidence.HIGH);
        }

        @Test
        @DisplayName("Should detect string field renumber as HIGH confidence")
        void shouldDetectStringFieldRenumber() {
            FieldInfo v1Field = createField("description", 7, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("description", 8, Type.TYPE_STRING, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Order", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).confidence()).isEqualTo(SuspectedRenumber.Confidence.HIGH);
        }

        @Test
        @DisplayName("Should detect bool field renumber as HIGH confidence")
        void shouldDetectBoolFieldRenumber() {
            FieldInfo v1Field = createField("active", 5, Type.TYPE_BOOL, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("active", 9, Type.TYPE_BOOL, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Order", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).confidence()).isEqualTo(SuspectedRenumber.Confidence.HIGH);
        }

        @Test
        @DisplayName("Should detect repeated field renumber as HIGH confidence")
        void shouldDetectRepeatedFieldRenumber() {
            FieldInfo v1Field = createField("items", 3, Type.TYPE_STRING, Label.LABEL_REPEATED);
            FieldInfo v2Field = createField("items", 7, Type.TYPE_STRING, Label.LABEL_REPEATED);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Order", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).confidence()).isEqualTo(SuspectedRenumber.Confidence.HIGH);
        }

        @Test
        @DisplayName("Should detect multiple renumbered fields in same message")
        void shouldDetectMultipleRenumbers() {
            FieldInfo v1Field1 = createField("parent_ref", 3, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v2Field1 = createField("parent_ref", 5, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v1Field2 = createField("description", 7, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v2Field2 = createField("description", 8, Type.TYPE_STRING, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(3, "parent_ref", ChangeType.REMOVED, v1Field1, null, List.of()),
                new FieldChange(7, "description", ChangeType.REMOVED, v1Field2, null, List.of()),
                new FieldChange(5, "parent_ref", ChangeType.ADDED, null, v2Field1, List.of()),
                new FieldChange(8, "description", ChangeType.ADDED, null, v2Field2, List.of())
            );

            MessageDiff msgDiff = createModifiedMessage("Order", fieldChanges);
            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(SuspectedRenumber::fieldName)
                .containsExactlyInAnyOrder("parent_ref", "description");
        }
    }

    @Nested
    @DisplayName("MEDIUM confidence detection (same name + compatible type)")
    class MediumConfidenceTests {

        @Test
        @DisplayName("Should detect int32 to int64 widening as MEDIUM confidence")
        void shouldDetectInt32ToInt64Widening() {
            FieldInfo v1Field = createField("amount", 4, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("amount", 6, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Order", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).confidence()).isEqualTo(SuspectedRenumber.Confidence.MEDIUM);
        }

        @Test
        @DisplayName("Should detect uint32 to uint64 widening as MEDIUM confidence")
        void shouldDetectUint32ToUint64Widening() {
            FieldInfo v1Field = createField("count", 2, Type.TYPE_UINT32, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("count", 5, Type.TYPE_UINT64, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Metrics", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).confidence()).isEqualTo(SuspectedRenumber.Confidence.MEDIUM);
        }

        @Test
        @DisplayName("Should detect float to double widening as MEDIUM confidence")
        void shouldDetectFloatToDoubleWidening() {
            FieldInfo v1Field = createField("rate", 3, Type.TYPE_FLOAT, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("rate", 7, Type.TYPE_DOUBLE, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Metrics", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).confidence()).isEqualTo(SuspectedRenumber.Confidence.MEDIUM);
        }

        @Test
        @DisplayName("Should detect int32 to enum conversion as MEDIUM confidence")
        void shouldDetectIntToEnumConversion() {
            FieldInfo v1Field = createField("status", 2, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("status", 5, Type.TYPE_ENUM, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Order", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).confidence()).isEqualTo(SuspectedRenumber.Confidence.MEDIUM);
        }

        @Test
        @DisplayName("Should detect enum to int32 conversion as MEDIUM confidence")
        void shouldDetectEnumToIntConversion() {
            FieldInfo v1Field = createField("priority", 3, Type.TYPE_ENUM, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("priority", 8, Type.TYPE_INT32, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Order", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).confidence()).isEqualTo(SuspectedRenumber.Confidence.MEDIUM);
        }

        @Test
        @DisplayName("Should detect string to bytes conversion as MEDIUM confidence")
        void shouldDetectStringToBytesConversion() {
            FieldInfo v1Field = createField("payload", 4, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("payload", 9, Type.TYPE_BYTES, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Request", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).confidence()).isEqualTo(SuspectedRenumber.Confidence.MEDIUM);
        }

        @Test
        @DisplayName("Should detect fixed32 to fixed64 widening as MEDIUM confidence")
        void shouldDetectFixed32ToFixed64Widening() {
            FieldInfo v1Field = createField("hash", 2, Type.TYPE_FIXED32, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("hash", 6, Type.TYPE_FIXED64, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Data", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).confidence()).isEqualTo(SuspectedRenumber.Confidence.MEDIUM);
        }
    }

    @Nested
    @DisplayName("Non-detection cases (incompatible types or mismatched names)")
    class NonDetectionTests {

        @Test
        @DisplayName("Should NOT detect field with different names")
        void shouldNotDetectDifferentNames() {
            FieldInfo v1Field = createField("old_name", 3, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("new_name", 5, Type.TYPE_STRING, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Order", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should NOT detect field with incompatible types (string to int32)")
        void shouldNotDetectIncompatibleStringToInt() {
            FieldInfo v1Field = createField("total", 3, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("total", 5, Type.TYPE_INT32, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Payment", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should NOT detect field with incompatible types (int32 to bool)")
        void shouldNotDetectIncompatibleIntToBool() {
            FieldInfo v1Field = createField("flag", 2, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("flag", 7, Type.TYPE_BOOL, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Settings", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should NOT detect field with cardinality mismatch (singular to repeated)")
        void shouldNotDetectCardinalityMismatch() {
            FieldInfo v1Field = createField("items", 3, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("items", 5, Type.TYPE_STRING, Label.LABEL_REPEATED);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Order", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should NOT detect when only REMOVED fields exist (no ADDED)")
        void shouldNotDetectOnlyRemoved() {
            FieldInfo v1Field = createField("old_field", 3, Type.TYPE_STRING, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(3, "old_field", ChangeType.REMOVED, v1Field, null, List.of())
            );
            MessageDiff msgDiff = createModifiedMessage("Order", fieldChanges);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should NOT detect when only ADDED fields exist (no REMOVED)")
        void shouldNotDetectOnlyAdded() {
            FieldInfo v2Field = createField("new_field", 5, Type.TYPE_STRING, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(5, "new_field", ChangeType.ADDED, null, v2Field, List.of())
            );
            MessageDiff msgDiff = createModifiedMessage("Order", fieldChanges);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should NOT detect for non-MODIFIED messages")
        void shouldNotDetectForNonModifiedMessages() {
            MessageInfo msgInfo = createMessageInfo("NewMessage");
            MessageDiff addedDiff = MessageDiff.added(msgInfo);

            List<SuspectedRenumber> result = detector.detect(List.of(addedDiff));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should NOT detect field with incompatible types (int32 to float)")
        void shouldNotDetectIntToFloat() {
            FieldInfo v1Field = createField("total", 3, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("total", 5, Type.TYPE_FLOAT, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Payment", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should NOT detect field with incompatible types (bool to string)")
        void shouldNotDetectBoolToString() {
            FieldInfo v1Field = createField("active", 2, Type.TYPE_BOOL, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("active", 8, Type.TYPE_STRING, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Settings", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Nested message detection")
    class NestedMessageTests {

        @Test
        @DisplayName("Should detect renumbered fields in nested messages")
        void shouldDetectInNestedMessages() {
            FieldInfo v1Field = createField("ref_id", 2, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("ref_id", 5, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            MessageDiff nestedDiff = createModifiedMessageWithRemovedAndAdded(
                "NestedItem", v1Field, v2Field);

            // Create parent with nested message changes
            MessageInfo v1Parent = createMessageInfo("Parent");
            MessageInfo v2Parent = createMessageInfo("Parent");
            MessageDiff parentDiff = MessageDiff.compared(v1Parent, v2Parent,
                List.of(), List.of(nestedDiff), List.of());

            List<SuspectedRenumber> result = detector.detect(List.of(parentDiff));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).messageName()).isEqualTo("NestedItem");
            assertThat(result.get(0).fieldName()).isEqualTo("ref_id");
        }

        @Test
        @DisplayName("Should NOT recurse into non-MODIFIED nested messages")
        void shouldNotRecurseIntoNonModifiedNested() {
            MessageInfo nestedInfo = createMessageInfo("AddedNested");
            MessageDiff addedNestedDiff = MessageDiff.added(nestedInfo);

            MessageInfo v1Parent = createMessageInfo("Parent");
            MessageInfo v2Parent = createMessageInfo("Parent");
            MessageDiff parentDiff = MessageDiff.compared(v1Parent, v2Parent,
                List.of(), List.of(addedNestedDiff), List.of());

            List<SuspectedRenumber> result = detector.detect(List.of(parentDiff));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Multiple messages detection")
    class MultipleMessageTests {

        @Test
        @DisplayName("Should detect renumbers across multiple messages")
        void shouldDetectAcrossMultipleMessages() {
            FieldInfo v1Field1 = createField("ref", 2, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v2Field1 = createField("ref", 5, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            FieldInfo v1Field2 = createField("code", 3, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v2Field2 = createField("code", 7, Type.TYPE_STRING, Label.LABEL_OPTIONAL);

            MessageDiff msg1Diff = createModifiedMessageWithRemovedAndAdded("Order", v1Field1, v2Field1);
            MessageDiff msg2Diff = createModifiedMessageWithRemovedAndAdded("Payment", v1Field2, v2Field2);

            List<SuspectedRenumber> result = detector.detect(List.of(msg1Diff, msg2Diff));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(SuspectedRenumber::messageName)
                .containsExactlyInAnyOrder("Order", "Payment");
        }

        @Test
        @DisplayName("Should return empty list for empty input")
        void shouldReturnEmptyForEmptyInput() {
            List<SuspectedRenumber> result = detector.detect(List.of());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return unmodifiable list")
        void shouldReturnUnmodifiableList() {
            List<SuspectedRenumber> result = detector.detect(List.of());
            assertThat(result).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("Displaced field detection (REMOVED + NAME_CHANGED)")
    class DisplacedFieldTests {

        @Test
        @DisplayName("Should detect renumber when field displaces another field at its new number")
        void shouldDetectDisplacedFieldRenumber() {
            // Scenario: parent_ticket moved from #17 to #15, displacing shift_document_number
            // Merger sees: #15 NAME_CHANGED (shift_document_number -> parent_ticket), #17 REMOVED (parent_ticket)
            FieldInfo v1ParentTicket = createField("parent_ticket", 17, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL);
            FieldInfo v1ShiftDocNum = createField("shift_document_number", 15, Type.TYPE_UINT32, Label.LABEL_OPTIONAL);
            FieldInfo v2ParentTicket = createField("parent_ticket", 15, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(17, "parent_ticket", ChangeType.REMOVED, v1ParentTicket, null, List.of()),
                new FieldChange(15, "parent_ticket", ChangeType.NAME_CHANGED, v1ShiftDocNum, v2ParentTicket,
                    List.of("Name: shift_document_number -> parent_ticket"))
            );

            MessageDiff msgDiff = createModifiedMessage("TicketRequest", fieldChanges);
            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            SuspectedRenumber sr = result.get(0);
            assertThat(sr.messageName()).isEqualTo("TicketRequest");
            assertThat(sr.fieldName()).isEqualTo("parent_ticket");
            assertThat(sr.v1Number()).isEqualTo(17);
            assertThat(sr.v2Number()).isEqualTo(15);
            assertThat(sr.v1Field()).isEqualTo(v1ParentTicket);
            assertThat(sr.v2Field()).isEqualTo(v2ParentTicket);
            assertThat(sr.confidence()).isEqualTo(SuspectedRenumber.Confidence.HIGH);
        }

        @Test
        @DisplayName("Should detect displaced field with compatible type as MEDIUM confidence")
        void shouldDetectDisplacedFieldWithCompatibleType() {
            // amount (int32) moved from #10 to #5, displacing old_count (uint64)
            FieldInfo v1Amount = createField("amount", 10, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
            FieldInfo v1OldCount = createField("old_count", 5, Type.TYPE_UINT64, Label.LABEL_OPTIONAL);
            FieldInfo v2Amount = createField("amount", 5, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(10, "amount", ChangeType.REMOVED, v1Amount, null, List.of()),
                new FieldChange(5, "amount", ChangeType.NAME_CHANGED, v1OldCount, v2Amount,
                    List.of("Name: old_count -> amount"))
            );

            MessageDiff msgDiff = createModifiedMessage("Payment", fieldChanges);
            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            SuspectedRenumber sr = result.get(0);
            assertThat(sr.fieldName()).isEqualTo("amount");
            assertThat(sr.v1Number()).isEqualTo(10);
            assertThat(sr.v2Number()).isEqualTo(5);
            assertThat(sr.confidence()).isEqualTo(SuspectedRenumber.Confidence.MEDIUM);
        }

        @Test
        @DisplayName("Should NOT detect displaced field with incompatible types")
        void shouldNotDetectDisplacedFieldWithIncompatibleTypes() {
            // field moved from #10 to #5, but types are incompatible (string vs bool)
            FieldInfo v1Field = createField("flag", 10, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v1Other = createField("other", 5, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("flag", 5, Type.TYPE_BOOL, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(10, "flag", ChangeType.REMOVED, v1Field, null, List.of()),
                new FieldChange(5, "flag", ChangeType.NAME_CHANGED, v1Other, v2Field,
                    List.of("Name: other -> flag"))
            );

            MessageDiff msgDiff = createModifiedMessage("Settings", fieldChanges);
            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should NOT detect displaced field with cardinality mismatch")
        void shouldNotDetectDisplacedFieldWithCardinalityMismatch() {
            FieldInfo v1Field = createField("items", 10, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v1Other = createField("other", 5, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("items", 5, Type.TYPE_STRING, Label.LABEL_REPEATED);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(10, "items", ChangeType.REMOVED, v1Field, null, List.of()),
                new FieldChange(5, "items", ChangeType.NAME_CHANGED, v1Other, v2Field,
                    List.of("Name: other -> items"))
            );

            MessageDiff msgDiff = createModifiedMessage("Order", fieldChanges);
            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should prefer strategy 1 (REMOVED+ADDED) over strategy 2 (displaced)")
        void shouldPreferRemovedAddedOverDisplaced() {
            // If a field matches both strategies, strategy 1 should win
            FieldInfo v1Field = createField("ref", 10, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v2FieldAdded = createField("ref", 8, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v1Other = createField("other", 5, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v2FieldRenamed = createField("ref", 5, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(10, "ref", ChangeType.REMOVED, v1Field, null, List.of()),
                new FieldChange(8, "ref", ChangeType.ADDED, null, v2FieldAdded, List.of()),
                new FieldChange(5, "ref", ChangeType.NAME_CHANGED, v1Other, v2FieldRenamed,
                    List.of("Name: other -> ref"))
            );

            MessageDiff msgDiff = createModifiedMessage("Order", fieldChanges);
            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            // Should match strategy 1 (#10 -> #8), not strategy 2 (#10 -> #5)
            assertThat(result).hasSize(1);
            assertThat(result.get(0).v1Number()).isEqualTo(10);
            assertThat(result.get(0).v2Number()).isEqualTo(8);
        }

        @Test
        @DisplayName("Should detect multiple displaced fields")
        void shouldDetectMultipleDisplacedFields() {
            FieldInfo v1FieldA = createField("field_a", 10, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v1FieldB = createField("field_b", 12, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v1Other1 = createField("old1", 5, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
            FieldInfo v1Other2 = createField("old2", 7, Type.TYPE_UINT32, Label.LABEL_OPTIONAL);
            FieldInfo v2FieldA = createField("field_a", 5, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v2FieldB = createField("field_b", 7, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(10, "field_a", ChangeType.REMOVED, v1FieldA, null, List.of()),
                new FieldChange(12, "field_b", ChangeType.REMOVED, v1FieldB, null, List.of()),
                new FieldChange(5, "field_a", ChangeType.NAME_CHANGED, v1Other1, v2FieldA,
                    List.of("Name: old1 -> field_a")),
                new FieldChange(7, "field_b", ChangeType.NAME_CHANGED, v1Other2, v2FieldB,
                    List.of("Name: old2 -> field_b"))
            );

            MessageDiff msgDiff = createModifiedMessage("Order", fieldChanges);
            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(SuspectedRenumber::fieldName)
                .containsExactlyInAnyOrder("field_a", "field_b");
        }

        @Test
        @DisplayName("Should detect displaced field in nested messages")
        void shouldDetectDisplacedFieldInNestedMessages() {
            FieldInfo v1Field = createField("nested_ref", 8, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
            FieldInfo v1Other = createField("old_field", 3, Type.TYPE_UINT32, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("nested_ref", 3, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(8, "nested_ref", ChangeType.REMOVED, v1Field, null, List.of()),
                new FieldChange(3, "nested_ref", ChangeType.NAME_CHANGED, v1Other, v2Field,
                    List.of("Name: old_field -> nested_ref"))
            );

            MessageDiff nestedDiff = createModifiedMessage("NestedItem", fieldChanges);

            MessageInfo v1Parent = createMessageInfo("Parent");
            MessageInfo v2Parent = createMessageInfo("Parent");
            MessageDiff parentDiff = MessageDiff.compared(v1Parent, v2Parent,
                List.of(), List.of(nestedDiff), List.of());

            List<SuspectedRenumber> result = detector.detect(List.of(parentDiff));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).messageName()).isEqualTo("NestedItem");
            assertThat(result.get(0).fieldName()).isEqualTo("nested_ref");
            assertThat(result.get(0).v1Number()).isEqualTo(8);
            assertThat(result.get(0).v2Number()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should detect displaced field even when primary changeType is TYPE_CHANGED")
        void shouldDetectDisplacedFieldWithTypeChangedPrimary() {
            // Real-world scenario: parent_ticket (MESSAGE) moved from #17 to #15,
            // displacing shift_document_number (uint32). The primary changeType is TYPE_CHANGED.
            FieldInfo v1ParentTicket = createField("parent_ticket", 17, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL);
            FieldInfo v1ShiftDocNum = createField("shift_document_number", 15, Type.TYPE_UINT32, Label.LABEL_OPTIONAL);
            FieldInfo v2ParentTicket = createField("parent_ticket", 15, Type.TYPE_MESSAGE, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(17, "parent_ticket", ChangeType.REMOVED, v1ParentTicket, null, List.of()),
                // Primary changeType is TYPE_CHANGED (not NAME_CHANGED), which is what
                // MergedSchemaDiffAdapter produces when type conflict takes priority
                new FieldChange(15, "parent_ticket", ChangeType.TYPE_CHANGED, v1ShiftDocNum, v2ParentTicket,
                    List.of("Type: uint32 -> ParentTicket", "Name: shift_document_number -> parent_ticket"))
            );

            MessageDiff msgDiff = createModifiedMessage("TicketRequest", fieldChanges);
            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            SuspectedRenumber sr = result.get(0);
            assertThat(sr.messageName()).isEqualTo("TicketRequest");
            assertThat(sr.fieldName()).isEqualTo("parent_ticket");
            assertThat(sr.v1Number()).isEqualTo(17);
            assertThat(sr.v2Number()).isEqualTo(15);
            assertThat(sr.confidence()).isEqualTo(SuspectedRenumber.Confidence.HIGH);
        }

        @Test
        @DisplayName("Should NOT detect when NAME_CHANGED v2 name doesn't match any REMOVED field")
        void shouldNotDetectWhenNoNameMatch() {
            FieldInfo v1Field = createField("unrelated", 10, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v1Other = createField("old_name", 5, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("new_name", 5, Type.TYPE_STRING, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(10, "unrelated", ChangeType.REMOVED, v1Field, null, List.of()),
                new FieldChange(5, "new_name", ChangeType.NAME_CHANGED, v1Other, v2Field,
                    List.of("Name: old_name -> new_name"))
            );

            MessageDiff msgDiff = createModifiedMessage("Order", fieldChanges);
            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should not match same ADDED field to multiple REMOVED fields")
        void shouldNotDoubleMatchAdded() {
            // Two removed fields with same name shouldn't happen, but test one-to-one matching
            FieldInfo v1Field = createField("field", 2, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("field", 5, Type.TYPE_STRING, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Test", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            // Should only produce one match
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should handle null v1Field in REMOVED field change gracefully")
        void shouldHandleNullV1Field() {
            FieldInfo v2Field = createField("field", 5, Type.TYPE_STRING, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(3, "field", ChangeType.REMOVED, null, null, List.of()),
                new FieldChange(5, "field", ChangeType.ADDED, null, v2Field, List.of())
            );
            MessageDiff msgDiff = createModifiedMessage("Test", fieldChanges);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle null v2Field in ADDED field change gracefully")
        void shouldHandleNullV2Field() {
            FieldInfo v1Field = createField("field", 3, Type.TYPE_STRING, Label.LABEL_OPTIONAL);

            List<FieldChange> fieldChanges = List.of(
                new FieldChange(3, "field", ChangeType.REMOVED, v1Field, null, List.of()),
                new FieldChange(5, "field", ChangeType.ADDED, null, null, List.of())
            );
            MessageDiff msgDiff = createModifiedMessage("Test", fieldChanges);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should populate v1Field and v2Field in SuspectedRenumber")
        void shouldPopulateFieldInfos() {
            FieldInfo v1Field = createField("amount", 4, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
            FieldInfo v2Field = createField("amount", 6, Type.TYPE_INT64, Label.LABEL_OPTIONAL);

            MessageDiff msgDiff = createModifiedMessageWithRemovedAndAdded(
                "Order", v1Field, v2Field);

            List<SuspectedRenumber> result = detector.detect(List.of(msgDiff));

            assertThat(result).hasSize(1);
            SuspectedRenumber sr = result.get(0);
            assertThat(sr.v1Field()).isEqualTo(v1Field);
            assertThat(sr.v2Field()).isEqualTo(v2Field);
        }
    }

    // ========== Helper Methods ==========

    private FieldInfo createField(String name, int number, Type type, Label label) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(number)
            .setType(type)
            .setLabel(label)
            .build();
        return new FieldInfo(proto);
    }

    private MessageInfo createMessageInfo(String name) {
        return new MessageInfo(name, name, null, List.of(), List.of(), List.of());
    }

    private MessageDiff createModifiedMessageWithRemovedAndAdded(
            String messageName, FieldInfo v1Field, FieldInfo v2Field) {
        List<FieldChange> fieldChanges = List.of(
            new FieldChange(v1Field.getNumber(), v1Field.getProtoName(),
                ChangeType.REMOVED, v1Field, null, List.of()),
            new FieldChange(v2Field.getNumber(), v2Field.getProtoName(),
                ChangeType.ADDED, null, v2Field, List.of())
        );
        return createModifiedMessage(messageName, fieldChanges);
    }

    private MessageDiff createModifiedMessage(String messageName, List<FieldChange> fieldChanges) {
        MessageInfo v1Info = createMessageInfo(messageName);
        MessageInfo v2Info = createMessageInfo(messageName);
        return MessageDiff.compared(v1Info, v2Info, fieldChanges, List.of(), List.of());
    }
}
