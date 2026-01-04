package space.alnovis.protowrapper.diff.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EnumValueChange record.
 */
class EnumValueChangeTest {

    @Test
    void added_createsCorrectRecord() {
        EnumValueChange change = EnumValueChange.added("NEW_VALUE", 5);

        assertEquals("NEW_VALUE", change.valueName());
        assertEquals(ChangeType.VALUE_ADDED, change.changeType());
        assertNull(change.v1Number());
        assertEquals(5, change.v2Number());
    }

    @Test
    void removed_createsCorrectRecord() {
        EnumValueChange change = EnumValueChange.removed("OLD_VALUE", 3);

        assertEquals("OLD_VALUE", change.valueName());
        assertEquals(ChangeType.VALUE_REMOVED, change.changeType());
        assertEquals(3, change.v1Number());
        assertNull(change.v2Number());
    }

    @Test
    void numberChanged_createsCorrectRecord() {
        EnumValueChange change = EnumValueChange.numberChanged("RENAMED_VALUE", 1, 10);

        assertEquals("RENAMED_VALUE", change.valueName());
        assertEquals(ChangeType.VALUE_NUMBER_CHANGED, change.changeType());
        assertEquals(1, change.v1Number());
        assertEquals(10, change.v2Number());
    }

    @Test
    void isBreaking_returnsTrue_forRemovedValue() {
        EnumValueChange change = EnumValueChange.removed("DELETED", 5);

        assertTrue(change.isBreaking());
    }

    @Test
    void isBreaking_returnsTrue_forNumberChanged() {
        EnumValueChange change = EnumValueChange.numberChanged("CHANGED", 1, 2);

        assertTrue(change.isBreaking());
    }

    @Test
    void isBreaking_returnsFalse_forAddedValue() {
        EnumValueChange change = EnumValueChange.added("NEW", 10);

        assertFalse(change.isBreaking());
    }

    @Test
    void getSummary_formatsAddedValue() {
        EnumValueChange change = EnumValueChange.added("STATUS_ACTIVE", 1);

        String summary = change.getSummary();

        assertTrue(summary.contains("Added value"));
        assertTrue(summary.contains("STATUS_ACTIVE"));
        assertTrue(summary.contains("1"));
    }

    @Test
    void getSummary_formatsRemovedValue() {
        EnumValueChange change = EnumValueChange.removed("STATUS_DELETED", 99);

        String summary = change.getSummary();

        assertTrue(summary.contains("Removed value"));
        assertTrue(summary.contains("STATUS_DELETED"));
        assertTrue(summary.contains("99"));
    }

    @Test
    void getSummary_formatsNumberChanged() {
        EnumValueChange change = EnumValueChange.numberChanged("STATUS_PENDING", 2, 20);

        String summary = change.getSummary();

        assertTrue(summary.contains("Number changed"));
        assertTrue(summary.contains("STATUS_PENDING"));
        assertTrue(summary.contains("2"));
        assertTrue(summary.contains("20"));
    }

    @Test
    void equalsAndHashCode_workCorrectly() {
        EnumValueChange change1 = EnumValueChange.added("VALUE", 1);
        EnumValueChange change2 = EnumValueChange.added("VALUE", 1);
        EnumValueChange change3 = EnumValueChange.added("VALUE", 2);

        assertEquals(change1, change2);
        assertEquals(change1.hashCode(), change2.hashCode());
        assertNotEquals(change1, change3);
    }

    @Test
    void constructor_acceptsDirectValues() {
        EnumValueChange change = new EnumValueChange(
            "CUSTOM",
            ChangeType.VALUE_NUMBER_CHANGED,
            5,
            10
        );

        assertEquals("CUSTOM", change.valueName());
        assertEquals(ChangeType.VALUE_NUMBER_CHANGED, change.changeType());
        assertEquals(5, change.v1Number());
        assertEquals(10, change.v2Number());
    }

    @Test
    void getSummary_handlesUnknownChangeType() {
        // Test with a change type that's not specifically handled
        EnumValueChange change = new EnumValueChange(
            "TEST_VALUE",
            ChangeType.MODIFIED,
            1,
            2
        );

        String summary = change.getSummary();

        assertTrue(summary.contains("TEST_VALUE"));
        assertTrue(summary.contains("MODIFIED"));
    }
}
