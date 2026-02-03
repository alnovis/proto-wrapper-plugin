package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.LiteralExpr;
import io.alnovis.protowrapper.ir.expr.VarRefExpr;
import io.alnovis.protowrapper.ir.type.PrimitiveType;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ReturnStmt}.
 */
class ReturnStmtTest {

    @Test
    void constructor_nullValue_succeeds() {
        // void return
        ReturnStmt stmt = new ReturnStmt(null);

        assertNull(stmt.value());
    }

    @Test
    void constructor_withValue_succeeds() {
        LiteralExpr value = new LiteralExpr(42, PrimitiveType.of(PrimitiveKind.INT));
        ReturnStmt stmt = new ReturnStmt(value);

        assertEquals(value, stmt.value());
    }

    @Test
    void hasValue_withValue_returnsTrue() {
        ReturnStmt stmt = new ReturnStmt(new VarRefExpr("x"));

        assertTrue(stmt.hasValue());
    }

    @Test
    void hasValue_nullValue_returnsFalse() {
        ReturnStmt stmt = new ReturnStmt(null);

        assertFalse(stmt.hasValue());
    }

    @Test
    void equals_bothNullValue_areEqual() {
        ReturnStmt a = new ReturnStmt(null);
        ReturnStmt b = new ReturnStmt(null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_sameValue_areEqual() {
        ReturnStmt a = new ReturnStmt(new VarRefExpr("x"));
        ReturnStmt b = new ReturnStmt(new VarRefExpr("x"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentValue_areNotEqual() {
        ReturnStmt a = new ReturnStmt(new VarRefExpr("x"));
        ReturnStmt b = new ReturnStmt(new VarRefExpr("y"));

        assertNotEquals(a, b);
    }

    @Test
    void equals_valueVsNull_areNotEqual() {
        ReturnStmt a = new ReturnStmt(new VarRefExpr("x"));
        ReturnStmt b = new ReturnStmt(null);

        assertNotEquals(a, b);
    }

    @Test
    void implementsStatement() {
        ReturnStmt stmt = new ReturnStmt(null);

        assertInstanceOf(Statement.class, stmt);
    }
}
