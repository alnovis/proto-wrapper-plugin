package io.alnovis.protowrapper.ir.stmt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContinueStmt}.
 */
class ContinueStmtTest {

    @Test
    void constructor_nullLabel_succeeds() {
        ContinueStmt stmt = new ContinueStmt(null);

        assertNull(stmt.label());
        assertTrue(stmt.isUnlabeled());
        assertFalse(stmt.isLabeled());
    }

    @Test
    void constructor_withLabel_succeeds() {
        ContinueStmt stmt = new ContinueStmt("outer");

        assertEquals("outer", stmt.label());
        assertTrue(stmt.isLabeled());
        assertFalse(stmt.isUnlabeled());
    }

    @Test
    void instance_isUnlabeled() {
        assertNull(ContinueStmt.INSTANCE.label());
        assertTrue(ContinueStmt.INSTANCE.isUnlabeled());
    }

    @Test
    void isLabeled_withLabel_returnsTrue() {
        ContinueStmt stmt = new ContinueStmt("loop");

        assertTrue(stmt.isLabeled());
    }

    @Test
    void isLabeled_withoutLabel_returnsFalse() {
        ContinueStmt stmt = new ContinueStmt(null);

        assertFalse(stmt.isLabeled());
    }

    @Test
    void isUnlabeled_withoutLabel_returnsTrue() {
        ContinueStmt stmt = new ContinueStmt(null);

        assertTrue(stmt.isUnlabeled());
    }

    @Test
    void isUnlabeled_withLabel_returnsFalse() {
        ContinueStmt stmt = new ContinueStmt("outer");

        assertFalse(stmt.isUnlabeled());
    }

    @Test
    void equals_bothUnlabeled_areEqual() {
        ContinueStmt a = new ContinueStmt(null);
        ContinueStmt b = new ContinueStmt(null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_sameLabel_areEqual() {
        ContinueStmt a = new ContinueStmt("outer");
        ContinueStmt b = new ContinueStmt("outer");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentLabel_areNotEqual() {
        ContinueStmt a = new ContinueStmt("outer");
        ContinueStmt b = new ContinueStmt("inner");

        assertNotEquals(a, b);
    }

    @Test
    void equals_labeledVsUnlabeled_areNotEqual() {
        ContinueStmt a = new ContinueStmt("outer");
        ContinueStmt b = new ContinueStmt(null);

        assertNotEquals(a, b);
    }

    @Test
    void equals_instanceEqualsNewNull() {
        assertEquals(ContinueStmt.INSTANCE, new ContinueStmt(null));
    }

    @Test
    void implementsStatement() {
        ContinueStmt stmt = new ContinueStmt(null);

        assertInstanceOf(Statement.class, stmt);
    }
}
