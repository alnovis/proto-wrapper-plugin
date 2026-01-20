package io.alnovis.protowrapper.diff.model;

import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import org.junit.jupiter.api.Test;
import io.alnovis.protowrapper.model.EnumInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EnumDiff record.
 */
class EnumDiffTest {

    @Test
    void added_createsCorrectDiff() {
        EnumInfo enumInfo = createEnum("Status", "PENDING", 0, "ACTIVE", 1);

        EnumDiff diff = EnumDiff.added(enumInfo);

        assertEquals("Status", diff.enumName());
        assertEquals(ChangeType.ADDED, diff.changeType());
        assertNull(diff.v1Enum());
        assertEquals(enumInfo, diff.v2Enum());
        assertTrue(diff.valueChanges().isEmpty());
    }

    @Test
    void removed_createsCorrectDiff() {
        EnumInfo enumInfo = createEnum("OldStatus", "UNKNOWN", 0);

        EnumDiff diff = EnumDiff.removed(enumInfo);

        assertEquals("OldStatus", diff.enumName());
        assertEquals(ChangeType.REMOVED, diff.changeType());
        assertEquals(enumInfo, diff.v1Enum());
        assertNull(diff.v2Enum());
        assertTrue(diff.valueChanges().isEmpty());
    }

    @Test
    void modified_withChanges_createsModifiedDiff() {
        EnumInfo v1 = createEnum("Status", "PENDING", 0, "ACTIVE", 1);
        EnumInfo v2 = createEnum("Status", "PENDING", 0, "ACTIVE", 1, "INACTIVE", 2);

        List<EnumValueChange> changes = List.of(
            EnumValueChange.added("INACTIVE", 2)
        );

        EnumDiff diff = EnumDiff.modified(v1, v2, changes);

        assertEquals("Status", diff.enumName());
        assertEquals(ChangeType.MODIFIED, diff.changeType());
        assertEquals(1, diff.valueChanges().size());
    }

    @Test
    void modified_withoutChanges_createsUnchangedDiff() {
        EnumInfo v1 = createEnum("Status", "PENDING", 0);
        EnumInfo v2 = createEnum("Status", "PENDING", 0);

        EnumDiff diff = EnumDiff.modified(v1, v2, List.of());

        assertEquals(ChangeType.UNCHANGED, diff.changeType());
    }

    @Test
    void getAddedValues_returnsOnlyAdded() {
        List<EnumValueChange> changes = List.of(
            EnumValueChange.added("NEW1", 5),
            EnumValueChange.removed("OLD1", 2),
            EnumValueChange.added("NEW2", 6),
            EnumValueChange.numberChanged("CHANGED", 1, 10)
        );

        EnumDiff diff = new EnumDiff("TestEnum", ChangeType.MODIFIED, null, null, changes);

        List<EnumValueChange> added = diff.getAddedValues();

        assertEquals(2, added.size());
        assertTrue(added.stream().allMatch(vc -> vc.changeType() == ChangeType.VALUE_ADDED));
    }

    @Test
    void getRemovedValues_returnsOnlyRemoved() {
        List<EnumValueChange> changes = List.of(
            EnumValueChange.added("NEW1", 5),
            EnumValueChange.removed("OLD1", 2),
            EnumValueChange.removed("OLD2", 3)
        );

        EnumDiff diff = new EnumDiff("TestEnum", ChangeType.MODIFIED, null, null, changes);

        List<EnumValueChange> removed = diff.getRemovedValues();

        assertEquals(2, removed.size());
        assertTrue(removed.stream().allMatch(vc -> vc.changeType() == ChangeType.VALUE_REMOVED));
    }

    @Test
    void getChangedValues_returnsOnlyNumberChanged() {
        List<EnumValueChange> changes = List.of(
            EnumValueChange.added("NEW", 5),
            EnumValueChange.numberChanged("CHANGED1", 1, 10),
            EnumValueChange.numberChanged("CHANGED2", 2, 20)
        );

        EnumDiff diff = new EnumDiff("TestEnum", ChangeType.MODIFIED, null, null, changes);

        List<EnumValueChange> changed = diff.getChangedValues();

        assertEquals(2, changed.size());
        assertTrue(changed.stream().allMatch(vc -> vc.changeType() == ChangeType.VALUE_NUMBER_CHANGED));
    }

    @Test
    void hasBreakingChanges_returnsTrue_whenRemoved() {
        EnumInfo enumInfo = createEnum("OldEnum", "VALUE", 0);
        EnumDiff diff = EnumDiff.removed(enumInfo);

        assertTrue(diff.hasBreakingChanges());
    }

    @Test
    void hasBreakingChanges_returnsTrue_whenValueRemoved() {
        List<EnumValueChange> changes = List.of(
            EnumValueChange.added("NEW", 5),
            EnumValueChange.removed("DELETED", 2)
        );

        EnumDiff diff = new EnumDiff("TestEnum", ChangeType.MODIFIED, null, null, changes);

        assertTrue(diff.hasBreakingChanges());
    }

    @Test
    void hasBreakingChanges_returnsTrue_whenValueNumberChanged() {
        List<EnumValueChange> changes = List.of(
            EnumValueChange.numberChanged("RENUMBERED", 1, 100)
        );

        EnumDiff diff = new EnumDiff("TestEnum", ChangeType.MODIFIED, null, null, changes);

        assertTrue(diff.hasBreakingChanges());
    }

    @Test
    void hasBreakingChanges_returnsFalse_whenOnlyAdded() {
        EnumInfo enumInfo = createEnum("NewEnum", "VALUE", 0);
        EnumDiff diff = EnumDiff.added(enumInfo);

        assertFalse(diff.hasBreakingChanges());
    }

    @Test
    void hasBreakingChanges_returnsFalse_whenOnlyValuesAdded() {
        List<EnumValueChange> changes = List.of(
            EnumValueChange.added("NEW1", 5),
            EnumValueChange.added("NEW2", 6)
        );

        EnumDiff diff = new EnumDiff("TestEnum", ChangeType.MODIFIED, null, null, changes);

        assertFalse(diff.hasBreakingChanges());
    }

    @Test
    void getSummary_formatsAddedEnum() {
        EnumInfo enumInfo = createEnum("NewStatus", "PENDING", 0);
        EnumDiff diff = EnumDiff.added(enumInfo);

        String summary = diff.getSummary();

        assertTrue(summary.contains("Added enum"));
        assertTrue(summary.contains("NewStatus"));
    }

    @Test
    void getSummary_formatsRemovedEnum() {
        EnumInfo enumInfo = createEnum("OldStatus", "DEPRECATED", 0);
        EnumDiff diff = EnumDiff.removed(enumInfo);

        String summary = diff.getSummary();

        assertTrue(summary.contains("Removed enum"));
        assertTrue(summary.contains("OldStatus"));
    }

    @Test
    void getSummary_formatsModifiedEnum() {
        List<EnumValueChange> changes = List.of(
            EnumValueChange.added("NEW1", 5),
            EnumValueChange.added("NEW2", 6),
            EnumValueChange.removed("OLD", 2),
            EnumValueChange.numberChanged("CHANGED", 1, 10)
        );

        EnumDiff diff = new EnumDiff("Status", ChangeType.MODIFIED, null, null, changes);

        String summary = diff.getSummary();

        assertTrue(summary.contains("Modified enum"));
        assertTrue(summary.contains("Status"));
        assertTrue(summary.contains("+2 values"));  // 2 added
        assertTrue(summary.contains("-1 values"));  // 1 removed
        assertTrue(summary.contains("~1 changed")); // 1 number changed
    }

    // Helper method
    private EnumInfo createEnum(String name, Object... valuesAndNumbers) {
        EnumDescriptorProto.Builder builder = EnumDescriptorProto.newBuilder()
            .setName(name);

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
