package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.ConstructorCallExpr;
import io.alnovis.protowrapper.ir.expr.LiteralExpr;
import io.alnovis.protowrapper.ir.expr.VarRefExpr;
import io.alnovis.protowrapper.ir.type.ClassType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ThrowStmt}.
 */
class ThrowStmtTest {

    @Test
    void constructor_validException_succeeds() {
        VarRefExpr exception = new VarRefExpr("e");
        ThrowStmt stmt = new ThrowStmt(exception);

        assertEquals(exception, stmt.exception());
    }

    @Test
    void constructor_nullException_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new ThrowStmt(null));
    }

    @Test
    void throwsNewException_withConstructorCall_returnsTrue() {
        ConstructorCallExpr newEx = new ConstructorCallExpr(
            ClassType.of("java.lang.RuntimeException"),
            List.of(new LiteralExpr("Error message", null)));
        ThrowStmt stmt = new ThrowStmt(newEx);

        assertTrue(stmt.throwsNewException());
    }

    @Test
    void throwsNewException_withVarRef_returnsFalse() {
        ThrowStmt stmt = new ThrowStmt(new VarRefExpr("e"));

        assertFalse(stmt.throwsNewException());
    }

    @Test
    void equals_sameException_areEqual() {
        ThrowStmt a = new ThrowStmt(new VarRefExpr("e"));
        ThrowStmt b = new ThrowStmt(new VarRefExpr("e"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentException_areNotEqual() {
        ThrowStmt a = new ThrowStmt(new VarRefExpr("e"));
        ThrowStmt b = new ThrowStmt(new VarRefExpr("ex"));

        assertNotEquals(a, b);
    }

    @Test
    void implementsStatement() {
        ThrowStmt stmt = new ThrowStmt(new VarRefExpr("e"));

        assertInstanceOf(Statement.class, stmt);
    }
}
