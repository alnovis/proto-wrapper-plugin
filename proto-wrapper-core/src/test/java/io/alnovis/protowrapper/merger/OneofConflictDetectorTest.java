package io.alnovis.protowrapper.merger;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.model.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OneofConflictDetector.
 */
@DisplayName("OneofConflictDetector")
class OneofConflictDetectorTest {

    private OneofConflictDetector detector;

    @BeforeEach
    void setup() {
        detector = new OneofConflictDetector();
    }

    @Nested
    @DisplayName("Partial Existence Detection")
    class PartialExistenceTests {

        @Test
        @DisplayName("Detects oneof missing in some versions")
        void detectsPartialExistence() {
            OneofInfo v1Oneof = new OneofInfo("method", 0, List.of(10, 11));
            // v2 doesn't have this oneof

            MergedOneof.Builder builder = MergedOneof.builder("method")
                    .addVersionOneof("v1", v1Oneof);

            Set<String> allVersions = Set.of("v1", "v2");

            List<OneofConflictInfo> conflicts = detector.detectConflicts(builder, "Payment", allVersions);

            assertThat(conflicts).hasSize(1);
            assertThat(conflicts.get(0).getType()).isEqualTo(OneofConflictType.PARTIAL_EXISTENCE);
            assertThat(conflicts.get(0).getAffectedVersions()).contains("v2");
        }

        @Test
        @DisplayName("No conflict when oneof in all versions")
        void noConflictWhenUniversal() {
            OneofInfo v1Oneof = new OneofInfo("method", 0, List.of(10, 11));
            OneofInfo v2Oneof = new OneofInfo("method", 0, List.of(10, 11));

            MergedOneof.Builder builder = MergedOneof.builder("method")
                    .addVersionOneof("v1", v1Oneof)
                    .addVersionOneof("v2", v2Oneof);

            Set<String> allVersions = Set.of("v1", "v2");

            List<OneofConflictInfo> conflicts = detector.detectConflicts(builder, "Payment", allVersions);

            assertThat(conflicts.stream()
                    .filter(c -> c.getType() == OneofConflictType.PARTIAL_EXISTENCE)
                    .toList()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Field Set Difference Detection")
    class FieldSetDifferenceTests {

        @Test
        @DisplayName("Detects different fields across versions")
        void detectsFieldSetDifference() {
            OneofInfo v1Oneof = new OneofInfo("method", 0, List.of(10, 11));
            OneofInfo v2Oneof = new OneofInfo("method", 0, List.of(10, 11, 12)); // adds field 12

            MergedOneof.Builder builder = MergedOneof.builder("method")
                    .addVersionOneof("v1", v1Oneof)
                    .addVersionOneof("v2", v2Oneof);

            Set<String> allVersions = Set.of("v1", "v2");

            List<OneofConflictInfo> conflicts = detector.detectConflicts(builder, "Payment", allVersions);

            List<OneofConflictInfo> fieldDiffs = conflicts.stream()
                    .filter(c -> c.getType() == OneofConflictType.FIELD_SET_DIFFERENCE)
                    .toList();

            assertThat(fieldDiffs).hasSize(1);
            assertThat(fieldDiffs.get(0).getDescription()).contains("v2");
        }

        @Test
        @DisplayName("No conflict when same fields")
        void noConflictWhenSameFields() {
            OneofInfo v1Oneof = new OneofInfo("method", 0, List.of(10, 11));
            OneofInfo v2Oneof = new OneofInfo("method", 0, List.of(10, 11));

            MergedOneof.Builder builder = MergedOneof.builder("method")
                    .addVersionOneof("v1", v1Oneof)
                    .addVersionOneof("v2", v2Oneof);

            Set<String> allVersions = Set.of("v1", "v2");

            List<OneofConflictInfo> conflicts = detector.detectConflicts(builder, "Payment", allVersions);

            assertThat(conflicts.stream()
                    .filter(c -> c.getType() == OneofConflictType.FIELD_SET_DIFFERENCE)
                    .toList()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Renamed Oneof Detection")
    class RenamedOneofTests {

        @Test
        @DisplayName("Detects renamed oneof by same field numbers")
        void detectsRenamedOneof() {
            OneofInfo v1Oneof = new OneofInfo("payment_method", 0, List.of(10, 11));
            OneofInfo v2Oneof = new OneofInfo("method", 0, List.of(10, 11)); // Same fields, different name

            Map<String, List<OneofInfo>> oneofsByVersion = new LinkedHashMap<>();
            oneofsByVersion.put("v1", List.of(v1Oneof));
            oneofsByVersion.put("v2", List.of(v2Oneof));

            List<OneofConflictDetector.RenamedOneofGroup> renamedGroups =
                    detector.detectRenamedOneofs(oneofsByVersion, "Payment");

            assertThat(renamedGroups).hasSize(1);
            assertThat(renamedGroups.get(0).getAllNames()).containsExactlyInAnyOrder("payment_method", "method");
            assertThat(renamedGroups.get(0).fieldNumbers()).containsExactlyInAnyOrder(10, 11);
        }

        @Test
        @DisplayName("No rename detected for different field sets")
        void noRenameForDifferentFields() {
            OneofInfo v1Oneof = new OneofInfo("payment_method", 0, List.of(10, 11));
            OneofInfo v2Oneof = new OneofInfo("method", 0, List.of(20, 21)); // Different fields

            Map<String, List<OneofInfo>> oneofsByVersion = new LinkedHashMap<>();
            oneofsByVersion.put("v1", List.of(v1Oneof));
            oneofsByVersion.put("v2", List.of(v2Oneof));

            List<OneofConflictDetector.RenamedOneofGroup> renamedGroups =
                    detector.detectRenamedOneofs(oneofsByVersion, "Payment");

            assertThat(renamedGroups).isEmpty();
        }
    }

    @Nested
    @DisplayName("Field Membership Change Detection")
    class FieldMembershipChangeTests {

        @Test
        @DisplayName("Detects field moved into oneof")
        void detectsFieldMovedIntoOneof() {
            // v1: credit_card=10 is a regular field
            // v2: credit_card=10 is in oneof
            FieldInfo v1Field = createFieldInfo("credit_card", 10, false);
            FieldInfo v2Field = createFieldInfo("credit_card", 10, true);

            Map<Integer, Map<String, FieldInfo>> allFields = new LinkedHashMap<>();
            allFields.put(10, Map.of("v1", v1Field, "v2", v2Field));

            OneofInfo v2Oneof = new OneofInfo("method", 0, List.of(10));
            Map<String, List<OneofInfo>> oneofsByVersion = new LinkedHashMap<>();
            oneofsByVersion.put("v1", List.of()); // No oneofs in v1
            oneofsByVersion.put("v2", List.of(v2Oneof));

            Set<String> allVersions = Set.of("v1", "v2");

            List<OneofConflictInfo> conflicts = detector.detectFieldMembershipChanges(
                    allFields, oneofsByVersion, "Payment", allVersions);

            assertThat(conflicts).hasSize(1);
            assertThat(conflicts.get(0).getType()).isEqualTo(OneofConflictType.FIELD_MEMBERSHIP_CHANGE);
            assertThat(conflicts.get(0).getDescription()).contains("credit_card");
        }

        @Test
        @DisplayName("Detects regular field becoming part of oneof with new alternative - Order to oneof{Order, Invoice}")
        void detectsRegularFieldBecomingOneofWithAlternative() {
            // v1: order=10 is a regular field (Document has only Order)
            // v2: order=10 is in oneof content {Order, Invoice}
            FieldInfo v1OrderField = createFieldInfo("order", 10, false);
            FieldInfo v2OrderField = createFieldInfo("order", 10, true);
            FieldInfo v2InvoiceField = createFieldInfo("invoice", 11, true);

            Map<Integer, Map<String, FieldInfo>> allFields = new LinkedHashMap<>();
            allFields.put(10, new LinkedHashMap<>(Map.of("v1", v1OrderField, "v2", v2OrderField)));
            allFields.put(11, new LinkedHashMap<>(Map.of("v2", v2InvoiceField))); // invoice only in v2

            // v1 has no oneof, v2 has oneof with both fields
            OneofInfo v2ContentOneof = new OneofInfo("content", 0, List.of(10, 11));
            Map<String, List<OneofInfo>> oneofsByVersion = new LinkedHashMap<>();
            oneofsByVersion.put("v1", List.of());
            oneofsByVersion.put("v2", List.of(v2ContentOneof));

            Set<String> allVersions = Set.of("v1", "v2");

            List<OneofConflictInfo> conflicts = detector.detectFieldMembershipChanges(
                    allFields, oneofsByVersion, "Document", allVersions);

            // Should detect that 'order' field moved into oneof
            // 'invoice' is new field, only in v2, always in oneof - no membership change
            assertThat(conflicts).hasSize(1);

            OneofConflictInfo orderConflict = conflicts.get(0);
            assertThat(orderConflict.getType()).isEqualTo(OneofConflictType.FIELD_MEMBERSHIP_CHANGE);
            assertThat(orderConflict.getMessageName()).isEqualTo("Document");
            assertThat(orderConflict.getDescription()).contains("order");
            assertThat(orderConflict.getDescription()).contains("v2"); // in oneof in v2
            assertThat(orderConflict.getDescription()).contains("v1"); // regular in v1

            // Check details
            @SuppressWarnings("unchecked")
            Set<String> inOneofVersions = (Set<String>) orderConflict.getDetails()
                    .get(OneofConflictInfo.DETAIL_IN_ONEOF_VERSIONS);
            @SuppressWarnings("unchecked")
            Set<String> regularVersions = (Set<String>) orderConflict.getDetails()
                    .get(OneofConflictInfo.DETAIL_REGULAR_VERSIONS);

            assertThat(inOneofVersions).containsExactly("v2");
            assertThat(regularVersions).containsExactly("v1");
        }

        @Test
        @DisplayName("Detects field moved out of oneof")
        void detectsFieldMovedOutOfOneof() {
            // v1: order=10 is in oneof
            // v2: order=10 is a regular field (oneof removed)
            FieldInfo v1OrderField = createFieldInfo("order", 10, true);
            FieldInfo v2OrderField = createFieldInfo("order", 10, false);

            Map<Integer, Map<String, FieldInfo>> allFields = new LinkedHashMap<>();
            allFields.put(10, new LinkedHashMap<>(Map.of("v1", v1OrderField, "v2", v2OrderField)));

            OneofInfo v1ContentOneof = new OneofInfo("content", 0, List.of(10));
            Map<String, List<OneofInfo>> oneofsByVersion = new LinkedHashMap<>();
            oneofsByVersion.put("v1", List.of(v1ContentOneof));
            oneofsByVersion.put("v2", List.of()); // No oneof in v2

            Set<String> allVersions = Set.of("v1", "v2");

            List<OneofConflictInfo> conflicts = detector.detectFieldMembershipChanges(
                    allFields, oneofsByVersion, "Document", allVersions);

            assertThat(conflicts).hasSize(1);
            OneofConflictInfo conflict = conflicts.get(0);
            assertThat(conflict.getType()).isEqualTo(OneofConflictType.FIELD_MEMBERSHIP_CHANGE);
            assertThat(conflict.getDescription()).contains("order");

            @SuppressWarnings("unchecked")
            Set<String> inOneofVersions = (Set<String>) conflict.getDetails()
                    .get(OneofConflictInfo.DETAIL_IN_ONEOF_VERSIONS);
            @SuppressWarnings("unchecked")
            Set<String> regularVersions = (Set<String>) conflict.getDetails()
                    .get(OneofConflictInfo.DETAIL_REGULAR_VERSIONS);

            assertThat(inOneofVersions).containsExactly("v1");
            assertThat(regularVersions).containsExactly("v2");
        }

        @Test
        @DisplayName("No conflict when field is always in oneof")
        void noConflictWhenAlwaysInOneof() {
            // Both versions have field in oneof
            FieldInfo v1Field = createFieldInfo("order", 10, true);
            FieldInfo v2Field = createFieldInfo("order", 10, true);

            Map<Integer, Map<String, FieldInfo>> allFields = new LinkedHashMap<>();
            allFields.put(10, new LinkedHashMap<>(Map.of("v1", v1Field, "v2", v2Field)));

            OneofInfo v1Oneof = new OneofInfo("content", 0, List.of(10));
            OneofInfo v2Oneof = new OneofInfo("content", 0, List.of(10));
            Map<String, List<OneofInfo>> oneofsByVersion = new LinkedHashMap<>();
            oneofsByVersion.put("v1", List.of(v1Oneof));
            oneofsByVersion.put("v2", List.of(v2Oneof));

            Set<String> allVersions = Set.of("v1", "v2");

            List<OneofConflictInfo> conflicts = detector.detectFieldMembershipChanges(
                    allFields, oneofsByVersion, "Document", allVersions);

            assertThat(conflicts).isEmpty();
        }

        @Test
        @DisplayName("No conflict when field is always regular")
        void noConflictWhenAlwaysRegular() {
            // Both versions have field as regular (not in oneof)
            FieldInfo v1Field = createFieldInfo("order", 10, false);
            FieldInfo v2Field = createFieldInfo("order", 10, false);

            Map<Integer, Map<String, FieldInfo>> allFields = new LinkedHashMap<>();
            allFields.put(10, new LinkedHashMap<>(Map.of("v1", v1Field, "v2", v2Field)));

            Map<String, List<OneofInfo>> oneofsByVersion = new LinkedHashMap<>();
            oneofsByVersion.put("v1", List.of());
            oneofsByVersion.put("v2", List.of());

            Set<String> allVersions = Set.of("v1", "v2");

            List<OneofConflictInfo> conflicts = detector.detectFieldMembershipChanges(
                    allFields, oneofsByVersion, "Document", allVersions);

            assertThat(conflicts).isEmpty();
        }

        @Test
        @DisplayName("Detects membership change across three versions")
        void detectsMembershipChangeAcrossThreeVersions() {
            // v1: order=10 is regular
            // v2: order=10 is in oneof
            // v3: order=10 is in oneof
            FieldInfo v1Field = createFieldInfo("order", 10, false);
            FieldInfo v2Field = createFieldInfo("order", 10, true);
            FieldInfo v3Field = createFieldInfo("order", 10, true);

            Map<Integer, Map<String, FieldInfo>> allFields = new LinkedHashMap<>();
            Map<String, FieldInfo> fieldVersions = new LinkedHashMap<>();
            fieldVersions.put("v1", v1Field);
            fieldVersions.put("v2", v2Field);
            fieldVersions.put("v3", v3Field);
            allFields.put(10, fieldVersions);

            OneofInfo v2Oneof = new OneofInfo("content", 0, List.of(10));
            OneofInfo v3Oneof = new OneofInfo("content", 0, List.of(10));
            Map<String, List<OneofInfo>> oneofsByVersion = new LinkedHashMap<>();
            oneofsByVersion.put("v1", List.of());
            oneofsByVersion.put("v2", List.of(v2Oneof));
            oneofsByVersion.put("v3", List.of(v3Oneof));

            Set<String> allVersions = new LinkedHashSet<>(List.of("v1", "v2", "v3"));

            List<OneofConflictInfo> conflicts = detector.detectFieldMembershipChanges(
                    allFields, oneofsByVersion, "Document", allVersions);

            assertThat(conflicts).hasSize(1);
            OneofConflictInfo conflict = conflicts.get(0);

            @SuppressWarnings("unchecked")
            Set<String> inOneofVersions = (Set<String>) conflict.getDetails()
                    .get(OneofConflictInfo.DETAIL_IN_ONEOF_VERSIONS);
            @SuppressWarnings("unchecked")
            Set<String> regularVersions = (Set<String>) conflict.getDetails()
                    .get(OneofConflictInfo.DETAIL_REGULAR_VERSIONS);

            assertThat(inOneofVersions).containsExactlyInAnyOrder("v2", "v3");
            assertThat(regularVersions).containsExactly("v1");
        }
    }

    @Nested
    @DisplayName("Field Type Conflict Detection")
    class FieldTypeConflictTests {

        @Test
        @DisplayName("Detects type conflict in oneof field")
        void detectsTypeConflictInOneofField() {
            OneofInfo v1Oneof = new OneofInfo("content", 0, List.of(1, 2));
            OneofInfo v2Oneof = new OneofInfo("content", 0, List.of(1, 2));

            // Create merged field with conflict
            MergedField conflictedField = MergedField.builder()
                    .addVersionField("v1", createFieldInfo("text", 1, true))
                    .addVersionField("v2", createFieldInfo("text", 1, true))
                    .conflictType(MergedField.ConflictType.STRING_BYTES)
                    .build();

            MergedOneof.Builder builder = MergedOneof.builder("content")
                    .addVersionOneof("v1", v1Oneof)
                    .addVersionOneof("v2", v2Oneof)
                    .addField(conflictedField);

            Set<String> allVersions = Set.of("v1", "v2");

            List<OneofConflictInfo> conflicts = detector.detectConflicts(builder, "Container", allVersions);

            List<OneofConflictInfo> typeConflicts = conflicts.stream()
                    .filter(c -> c.getType() == OneofConflictType.FIELD_TYPE_CONFLICT)
                    .toList();

            assertThat(typeConflicts).hasSize(1);
            assertThat(typeConflicts.get(0).getDescription()).contains("text");
            assertThat(typeConflicts.get(0).getDescription()).contains("STRING_BYTES");
        }
    }

    @Nested
    @DisplayName("Field Removal Detection")
    class FieldRemovalTests {

        @Test
        @DisplayName("Detects field removed in newer version")
        void detectsFieldRemoval() {
            OneofInfo v1Oneof = new OneofInfo("method", 0, List.of(10, 11, 12)); // has cash=12
            OneofInfo v2Oneof = new OneofInfo("method", 0, List.of(10, 11)); // cash removed

            MergedOneof.Builder builder = MergedOneof.builder("method")
                    .addVersionOneof("v1", v1Oneof)
                    .addVersionOneof("v2", v2Oneof);

            Set<String> allVersions = Set.of("v1", "v2");

            List<OneofConflictInfo> conflicts = detector.detectConflicts(builder, "Payment", allVersions);

            List<OneofConflictInfo> removedConflicts = conflicts.stream()
                    .filter(c -> c.getType() == OneofConflictType.FIELD_REMOVED)
                    .toList();

            assertThat(removedConflicts).hasSize(1);
            assertThat(removedConflicts.get(0).getDescription()).contains("removed");
        }
    }

    @Nested
    @DisplayName("Field Number Change Detection")
    class FieldNumberChangeTests {

        @Test
        @DisplayName("Detects field number change for same field name")
        void detectsFieldNumberChange() {
            // v1: credit_card=10, v2: credit_card=15
            FieldInfo v1Field = createFieldInfo("credit_card", 10, true);
            FieldInfo v2Field = createFieldInfo("credit_card", 15, true);

            OneofInfo v1Oneof = new OneofInfo("method", 0, List.of(10), List.of(v1Field));
            OneofInfo v2Oneof = new OneofInfo("method", 0, List.of(15), List.of(v2Field));

            MergedOneof.Builder builder = MergedOneof.builder("method")
                    .addVersionOneof("v1", v1Oneof)
                    .addVersionOneof("v2", v2Oneof);

            List<OneofConflictInfo> conflicts = detector.detectFieldNumberChanges(builder, "Payment");

            assertThat(conflicts).hasSize(1);
            assertThat(conflicts.get(0).getType()).isEqualTo(OneofConflictType.FIELD_NUMBER_CHANGE);
            assertThat(conflicts.get(0).getDescription()).contains("credit_card");
        }

        @Test
        @DisplayName("No conflict when field numbers are same")
        void noConflictWhenSameNumbers() {
            FieldInfo v1Field = createFieldInfo("credit_card", 10, true);
            FieldInfo v2Field = createFieldInfo("credit_card", 10, true);

            OneofInfo v1Oneof = new OneofInfo("method", 0, List.of(10), List.of(v1Field));
            OneofInfo v2Oneof = new OneofInfo("method", 0, List.of(10), List.of(v2Field));

            MergedOneof.Builder builder = MergedOneof.builder("method")
                    .addVersionOneof("v1", v1Oneof)
                    .addVersionOneof("v2", v2Oneof);

            List<OneofConflictInfo> conflicts = detector.detectFieldNumberChanges(builder, "Payment");

            assertThat(conflicts).isEmpty();
        }
    }

    @Nested
    @DisplayName("MergedOneof Conflict Tracking")
    class MergedOneofConflictTrackingTests {

        @Test
        @DisplayName("MergedOneof stores and returns conflicts")
        void mergedOneofStoresConflicts() {
            OneofInfo v1Oneof = new OneofInfo("method", 0, List.of(10, 11));

            OneofConflictInfo conflict = OneofConflictInfo.builder(OneofConflictType.PARTIAL_EXISTENCE)
                    .oneofName("method")
                    .messageName("Payment")
                    .affectedVersion("v2")
                    .build();

            MergedOneof mergedOneof = MergedOneof.builder("method")
                    .addVersionOneof("v1", v1Oneof)
                    .addConflict(conflict)
                    .build();

            assertThat(mergedOneof.hasConflicts()).isTrue();
            assertThat(mergedOneof.getConflicts()).hasSize(1);
            assertThat(mergedOneof.hasConflictOfType(OneofConflictType.PARTIAL_EXISTENCE)).isTrue();
            assertThat(mergedOneof.hasConflictOfType(OneofConflictType.RENAMED)).isFalse();
        }

        @Test
        @DisplayName("MergedOneof tracks merged names for renamed oneofs")
        void mergedOneofTracksMergedNames() {
            OneofInfo v1Oneof = new OneofInfo("payment_method", 0, List.of(10, 11));
            OneofInfo v2Oneof = new OneofInfo("method", 0, List.of(10, 11));

            MergedOneof mergedOneof = MergedOneof.builder("method")
                    .addVersionOneof("v1", v1Oneof)
                    .addVersionOneof("v2", v2Oneof)
                    .addMergedFromName("payment_method")
                    .addMergedFromName("method")
                    .build();

            assertThat(mergedOneof.wasMergedFromMultipleNames()).isTrue();
            assertThat(mergedOneof.getMergedFromNames()).containsExactlyInAnyOrder("payment_method", "method");
        }
    }

    // Helper methods

    private FieldInfo createFieldInfo(String name, int number, boolean inOneof) {
        // Create a minimal FieldInfo for testing
        return new FieldInfo(
                name,                    // protoName
                toCamelCase(name),       // javaName
                number,                  // number
                Type.TYPE_STRING,        // type
                Label.LABEL_OPTIONAL,    // label
                "",                      // typeName
                inOneof ? 0 : -1,        // oneofIndex
                inOneof ? "method" : null // oneofName
        );
    }

    private String toCamelCase(String snakeCase) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return result.toString();
    }
}
