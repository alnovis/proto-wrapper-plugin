package io.alnovis.protowrapper.ir.stmt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BreakStmt}.
 */
class BreakStmtTest {

    @Test
    void constructor_nullLabel_succeeds() {
        BreakStmt stmt = new BreakStmt(null);

        assertNull(stmt.label());
        assertTrue(stmt.isUnlabeled());
        assertFalse(stmt.isLabeled());
    }

    @Test
    void constructor_withLabel_succeeds() {
        BreakStmt stmt = new BreakStmt("outer");

        assertEquals("outer", stmt.label());
        assertTrue(stmt.isLabeled());
        assertFalse(stmt.isUnlabeled());
    }

    @Test
    void instance_isUnlabeled() {
        assertNull(BreakStmt.INSTANCE.label());
        assertTrue(BreakStmt.INSTANCE.isUnlabeled());
    }

    @Test
    void isLabeled_withLabel_returnsTrue() {
        BreakStmt stmt = new BreakStmt("loop");

        assertTrue(stmt.isLabeled());
    }

    @Test
    void isLabeled_withoutLabel_returnsFalse() {
        BreakStmt stmt = new BreakStmt(null);

        assertFalse(stmt.isLabeled());
    }

    @Test
    void isUnlabeled_withoutLabel_returnsTrue() {
        BreakStmt stmt = new BreakStmt(null);

        assertTrue(stmt.isUnlabeled());
    }

    @Test
    void isUnlabeled_withLabel_returnsFalse() {
        BreakStmt stmt = new BreakStmt("outer");

        assertFalse(stmt.isUnlabeled());
    }

    @Test
    void equals_bothUnlabeled_areEqual() {
        BreakStmt a = new BreakStmt(null);
        BreakStmt b = new BreakStmt(null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_sameLabel_areEqual() {
        BreakStmt a = new BreakStmt("outer");
        BreakStmt b = new BreakStmt("outer");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentLabel_areNotEqual() {
        BreakStmt a = new BreakStmt("outer");
        BreakStmt b = new BreakStmt("inner");

        assertNotEquals(a, b);
    }

    @Test
    void equals_labeledVsUnlabeled_areNotEqual() {
        BreakStmt a = new BreakStmt("outer");
        BreakStmt b = new BreakStmt(null);

        assertNotEquals(a, b);
    }

    @Test
    void equals_instanceEqualsNewNull() {
        assertEquals(BreakStmt.INSTANCE, new BreakStmt(null));
    }

    @Test
    void implementsStatement() {
        BreakStmt stmt = new BreakStmt(null);

        assertInstanceOf(Statement.class, stmt);
    }
}
