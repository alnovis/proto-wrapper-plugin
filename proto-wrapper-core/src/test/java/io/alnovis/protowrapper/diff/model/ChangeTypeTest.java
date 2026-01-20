package io.alnovis.protowrapper.diff.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChangeType enum.
 */
class ChangeTypeTest {

    @Test
    void isAddition_returnsTrue_forAddedTypes() {
        assertTrue(ChangeType.ADDED.isAddition());
        assertTrue(ChangeType.VALUE_ADDED.isAddition());
    }

    @Test
    void isAddition_returnsFalse_forNonAddedTypes() {
        assertFalse(ChangeType.REMOVED.isAddition());
        assertFalse(ChangeType.MODIFIED.isAddition());
        assertFalse(ChangeType.UNCHANGED.isAddition());
        assertFalse(ChangeType.TYPE_CHANGED.isAddition());
    }

    @Test
    void isRemoval_returnsTrue_forRemovedTypes() {
        assertTrue(ChangeType.REMOVED.isRemoval());
        assertTrue(ChangeType.VALUE_REMOVED.isRemoval());
    }

    @Test
    void isRemoval_returnsFalse_forNonRemovedTypes() {
        assertFalse(ChangeType.ADDED.isRemoval());
        assertFalse(ChangeType.MODIFIED.isRemoval());
        assertFalse(ChangeType.UNCHANGED.isRemoval());
    }

    @Test
    void isModification_returnsTrue_forModificationTypes() {
        assertTrue(ChangeType.MODIFIED.isModification());
        assertTrue(ChangeType.TYPE_CHANGED.isModification());
        assertTrue(ChangeType.LABEL_CHANGED.isModification());
        assertTrue(ChangeType.NUMBER_CHANGED.isModification());
        assertTrue(ChangeType.NAME_CHANGED.isModification());
        assertTrue(ChangeType.DEFAULT_CHANGED.isModification());
        assertTrue(ChangeType.VALUE_NUMBER_CHANGED.isModification());
    }

    @Test
    void isModification_returnsFalse_forNonModificationTypes() {
        assertFalse(ChangeType.ADDED.isModification());
        assertFalse(ChangeType.REMOVED.isModification());
        assertFalse(ChangeType.UNCHANGED.isModification());
        assertFalse(ChangeType.VALUE_ADDED.isModification());
    }

    @Test
    void isPotentiallyBreaking_returnsTrue_forBreakingTypes() {
        assertTrue(ChangeType.REMOVED.isPotentiallyBreaking());
        assertTrue(ChangeType.TYPE_CHANGED.isPotentiallyBreaking());
        assertTrue(ChangeType.LABEL_CHANGED.isPotentiallyBreaking());
        assertTrue(ChangeType.NUMBER_CHANGED.isPotentiallyBreaking());
        assertTrue(ChangeType.VALUE_REMOVED.isPotentiallyBreaking());
        assertTrue(ChangeType.VALUE_NUMBER_CHANGED.isPotentiallyBreaking());
    }

    @Test
    void isPotentiallyBreaking_returnsFalse_forNonBreakingTypes() {
        assertFalse(ChangeType.ADDED.isPotentiallyBreaking());
        assertFalse(ChangeType.UNCHANGED.isPotentiallyBreaking());
        assertFalse(ChangeType.VALUE_ADDED.isPotentiallyBreaking());
        assertFalse(ChangeType.MODIFIED.isPotentiallyBreaking());
        assertFalse(ChangeType.NAME_CHANGED.isPotentiallyBreaking());
    }

    @ParameterizedTest
    @EnumSource(ChangeType.class)
    void allValuesHaveConsistentClassification(ChangeType type) {
        // Each type should belong to at most one primary category
        int count = 0;
        if (type.isAddition()) count++;
        if (type.isRemoval()) count++;
        // Note: modification can overlap with breaking, so we don't count it here

        assertTrue(count <= 1, "Type " + type + " should belong to at most one primary category");
    }
}
