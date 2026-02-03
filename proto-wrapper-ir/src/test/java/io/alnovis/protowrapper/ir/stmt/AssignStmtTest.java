package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.FieldAccessExpr;
import io.alnovis.protowrapper.ir.expr.LiteralExpr;
import io.alnovis.protowrapper.ir.expr.ThisExpr;
import io.alnovis.protowrapper.ir.expr.VarRefExpr;
import io.alnovis.protowrapper.ir.type.PrimitiveType;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AssignStmt}.
 */
class AssignStmtTest {

    @Test
    void constructor_validArguments_succeeds() {
        VarRefExpr target = new VarRefExpr("count");
        LiteralExpr value = new LiteralExpr(0, PrimitiveType.of(PrimitiveKind.INT));
        AssignStmt stmt = new AssignStmt(target, value);

        assertEquals(target, stmt.target());
        assertEquals(value, stmt.value());
    }

    @Test
    void constructor_nullTarget_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new AssignStmt(null, new LiteralExpr(0, null)));
    }

    @Test
    void constructor_nullValue_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new AssignStmt(new VarRefExpr("x"), null));
    }

    @Test
    void isVariableAssignment_withVarRef_returnsTrue() {
        AssignStmt stmt = new AssignStmt(new VarRefExpr("count"), new LiteralExpr(0, null));

        assertTrue(stmt.isVariableAssignment());
        assertFalse(stmt.isFieldAssignment());
    }

    @Test
    void isFieldAssignment_withFieldAccess_returnsTrue() {
        FieldAccessExpr field = new FieldAccessExpr(ThisExpr.INSTANCE, "value");
        AssignStmt stmt = new AssignStmt(field, new LiteralExpr(0, null));

        assertTrue(stmt.isFieldAssignment());
        assertFalse(stmt.isVariableAssignment());
    }

    @Test
    void equals_sameComponents_areEqual() {
        AssignStmt a = new AssignStmt(new VarRefExpr("x"), new LiteralExpr(1, null));
        AssignStmt b = new AssignStmt(new VarRefExpr("x"), new LiteralExpr(1, null));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentTarget_areNotEqual() {
        AssignStmt a = new AssignStmt(new VarRefExpr("x"), new LiteralExpr(1, null));
        AssignStmt b = new AssignStmt(new VarRefExpr("y"), new LiteralExpr(1, null));

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentValue_areNotEqual() {
        AssignStmt a = new AssignStmt(new VarRefExpr("x"), new LiteralExpr(1, null));
        AssignStmt b = new AssignStmt(new VarRefExpr("x"), new LiteralExpr(2, null));

        assertNotEquals(a, b);
    }

    @Test
    void implementsStatement() {
        AssignStmt stmt = new AssignStmt(new VarRefExpr("x"), new LiteralExpr(1, null));

        assertInstanceOf(Statement.class, stmt);
    }
}
