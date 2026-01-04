package space.alnovis.protowrapper.diff.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BreakingChange record.
 */
class BreakingChangeTest {

    @Test
    void isError_returnsTrue_forErrorSeverity() {
        BreakingChange change = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "User.email",
            "Field removed",
            "string email = 3",
            null
        );

        assertTrue(change.isError());
        assertFalse(change.isWarning());
    }

    @Test
    void isWarning_returnsTrue_forWarningSeverity() {
        BreakingChange change = new BreakingChange(
            BreakingChange.Type.CARDINALITY_CHANGED,
            BreakingChange.Severity.WARNING,
            "User.tags",
            "Cardinality changed",
            "singular",
            "repeated"
        );

        assertFalse(change.isError());
        assertTrue(change.isWarning());
    }

    @Test
    void toDisplayString_formatsFullChange() {
        BreakingChange change = new BreakingChange(
            BreakingChange.Type.FIELD_TYPE_INCOMPATIBLE,
            BreakingChange.Severity.ERROR,
            "Order.status",
            "Type changed",
            "string",
            "int32"
        );

        String display = change.toDisplayString();

        assertTrue(display.contains("[ERROR]"));
        assertTrue(display.contains("FIELD_TYPE_INCOMPATIBLE"));
        assertTrue(display.contains("Order.status"));
        assertTrue(display.contains("Type changed"));
        assertTrue(display.contains("string -> int32"));
    }

    @Test
    void toDisplayString_formatsRemovedChange() {
        BreakingChange change = new BreakingChange(
            BreakingChange.Type.MESSAGE_REMOVED,
            BreakingChange.Severity.ERROR,
            "DeprecatedMessage",
            "Message removed",
            "message DeprecatedMessage",
            null
        );

        String display = change.toDisplayString();

        assertTrue(display.contains("MESSAGE_REMOVED"));
        assertTrue(display.contains("DeprecatedMessage"));
        assertTrue(display.contains("was: message DeprecatedMessage"));
        assertFalse(display.contains("->"));
    }

    @Test
    void toDisplayString_formatsAddedChange() {
        BreakingChange change = new BreakingChange(
            BreakingChange.Type.REQUIRED_FIELD_ADDED,
            BreakingChange.Severity.WARNING,
            "User.id",
            "Required field added",
            null,
            "required string id = 1"
        );

        String display = change.toDisplayString();

        assertTrue(display.contains("[WARNING]"));
        assertTrue(display.contains("now: required string id = 1"));
    }

    @Test
    void toDisplayString_handlesNullDescription() {
        BreakingChange change = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "User.name",
            null,
            "string name",
            null
        );

        String display = change.toDisplayString();

        assertTrue(display.contains("User.name"));
        assertFalse(display.contains(" - "));
    }

    @Test
    void toDisplayString_handlesEmptyDescription() {
        BreakingChange change = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "User.name",
            "",
            "string name",
            null
        );

        String display = change.toDisplayString();

        assertFalse(display.contains(" - "));
    }

    @ParameterizedTest
    @EnumSource(BreakingChange.Type.class)
    void allTypesHaveValidName(BreakingChange.Type type) {
        assertNotNull(type.name());
        assertFalse(type.name().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(BreakingChange.Severity.class)
    void allSeveritiesHaveValidName(BreakingChange.Severity severity) {
        assertNotNull(severity.name());
        assertFalse(severity.name().isEmpty());
    }

    @Test
    void recordComponentsAreAccessible() {
        BreakingChange change = new BreakingChange(
            BreakingChange.Type.ENUM_REMOVED,
            BreakingChange.Severity.ERROR,
            "PaymentType",
            "Enum removed",
            "enum PaymentType",
            null
        );

        assertEquals(BreakingChange.Type.ENUM_REMOVED, change.type());
        assertEquals(BreakingChange.Severity.ERROR, change.severity());
        assertEquals("PaymentType", change.entityPath());
        assertEquals("Enum removed", change.description());
        assertEquals("enum PaymentType", change.v1Value());
        assertNull(change.v2Value());
    }

    @Test
    void equalsAndHashCode_workCorrectly() {
        BreakingChange change1 = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "User.email",
            "Field removed",
            "string",
            null
        );

        BreakingChange change2 = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.ERROR,
            "User.email",
            "Field removed",
            "string",
            null
        );

        BreakingChange change3 = new BreakingChange(
            BreakingChange.Type.FIELD_REMOVED,
            BreakingChange.Severity.WARNING,
            "User.email",
            "Field removed",
            "string",
            null
        );

        assertEquals(change1, change2);
        assertEquals(change1.hashCode(), change2.hashCode());
        assertNotEquals(change1, change3);
    }
}
