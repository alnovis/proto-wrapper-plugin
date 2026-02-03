package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.ConstructorCallExpr;
import io.alnovis.protowrapper.ir.expr.LiteralExpr;
import io.alnovis.protowrapper.ir.expr.MethodCallExpr;
import io.alnovis.protowrapper.ir.expr.ThisExpr;
import io.alnovis.protowrapper.ir.expr.VarRefExpr;
import io.alnovis.protowrapper.ir.type.ClassType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ExpressionStmt}.
 */
class ExpressionStmtTest {

    @Test
    void constructor_validExpression_succeeds() {
        MethodCallExpr call = new MethodCallExpr(ThisExpr.INSTANCE, "process", List.of(), List.of());
        ExpressionStmt stmt = new ExpressionStmt(call);

        assertEquals(call, stmt.expression());
    }

    @Test
    void constructor_nullExpression_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new ExpressionStmt(null));
    }

    @Test
    void isMethodCall_withMethodCall_returnsTrue() {
        MethodCallExpr call = new MethodCallExpr(ThisExpr.INSTANCE, "doSomething", List.of(), List.of());
        ExpressionStmt stmt = new ExpressionStmt(call);

        assertTrue(stmt.isMethodCall());
        assertFalse(stmt.isConstructorCall());
    }

    @Test
    void isConstructorCall_withConstructorCall_returnsTrue() {
        ConstructorCallExpr call = new ConstructorCallExpr(
            ClassType.of("java.util.ArrayList"), List.of());
        ExpressionStmt stmt = new ExpressionStmt(call);

        assertTrue(stmt.isConstructorCall());
        assertFalse(stmt.isMethodCall());
    }

    @Test
    void isMethodCall_withOtherExpression_returnsFalse() {
        ExpressionStmt stmt = new ExpressionStmt(new VarRefExpr("x"));

        assertFalse(stmt.isMethodCall());
        assertFalse(stmt.isConstructorCall());
    }

    @Test
    void equals_sameExpression_areEqual() {
        MethodCallExpr call = new MethodCallExpr(ThisExpr.INSTANCE, "foo", List.of(), List.of());
        ExpressionStmt a = new ExpressionStmt(call);
        ExpressionStmt b = new ExpressionStmt(
            new MethodCallExpr(ThisExpr.INSTANCE, "foo", List.of(), List.of()));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentExpression_areNotEqual() {
        ExpressionStmt a = new ExpressionStmt(new VarRefExpr("x"));
        ExpressionStmt b = new ExpressionStmt(new VarRefExpr("y"));

        assertNotEquals(a, b);
    }

    @Test
    void implementsStatement() {
        ExpressionStmt stmt = new ExpressionStmt(new VarRefExpr("x"));

        assertInstanceOf(Statement.class, stmt);
    }
}
