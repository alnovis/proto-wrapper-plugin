package io.alnovis.protowrapper.ir.expr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BinaryExpr} and {@link BinaryOp}.
 */
class BinaryExprTest {

    @Test
    void constructor_validOperands_succeeds() {
        Expression left = new VarRefExpr("a");
        Expression right = new VarRefExpr("b");
        BinaryExpr expr = new BinaryExpr(left, BinaryOp.ADD, right);

        assertEquals(left, expr.left());
        assertEquals(BinaryOp.ADD, expr.operator());
        assertEquals(right, expr.right());
    }

    @Test
    void constructor_nullLeft_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new BinaryExpr(null, BinaryOp.ADD, new VarRefExpr("b")));
    }

    @Test
    void constructor_nullOperator_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new BinaryExpr(new VarRefExpr("a"), null, new VarRefExpr("b")));
    }

    @Test
    void constructor_nullRight_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new BinaryExpr(new VarRefExpr("a"), BinaryOp.ADD, null));
    }

    @ParameterizedTest
    @EnumSource(BinaryOp.class)
    void allOperators_canBeUsed(BinaryOp op) {
        Expression left = new VarRefExpr("x");
        Expression right = new VarRefExpr("y");
        BinaryExpr expr = new BinaryExpr(left, op, right);

        assertEquals(op, expr.operator());
    }

    @Test
    void arithmeticOperators_haveCorrectSymbols() {
        assertEquals("+", BinaryOp.ADD.symbol());
        assertEquals("-", BinaryOp.SUB.symbol());
        assertEquals("*", BinaryOp.MUL.symbol());
        assertEquals("/", BinaryOp.DIV.symbol());
        assertEquals("%", BinaryOp.MOD.symbol());
    }

    @Test
    void comparisonOperators_haveCorrectSymbols() {
        assertEquals("==", BinaryOp.EQ.symbol());
        assertEquals("!=", BinaryOp.NE.symbol());
        assertEquals("<", BinaryOp.LT.symbol());
        assertEquals("<=", BinaryOp.LE.symbol());
        assertEquals(">", BinaryOp.GT.symbol());
        assertEquals(">=", BinaryOp.GE.symbol());
    }

    @Test
    void logicalOperators_haveCorrectSymbols() {
        assertEquals("&&", BinaryOp.AND.symbol());
        assertEquals("||", BinaryOp.OR.symbol());
    }

    @Test
    void bitwiseOperators_haveCorrectSymbols() {
        assertEquals("&", BinaryOp.BIT_AND.symbol());
        assertEquals("|", BinaryOp.BIT_OR.symbol());
        assertEquals("^", BinaryOp.BIT_XOR.symbol());
        assertEquals("<<", BinaryOp.LSHIFT.symbol());
        assertEquals(">>", BinaryOp.RSHIFT.symbol());
        assertEquals(">>>", BinaryOp.URSHIFT.symbol());
    }

    @Test
    void equals_sameComponents_areEqual() {
        BinaryExpr a = new BinaryExpr(new VarRefExpr("x"), BinaryOp.ADD, new VarRefExpr("y"));
        BinaryExpr b = new BinaryExpr(new VarRefExpr("x"), BinaryOp.ADD, new VarRefExpr("y"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentLeft_areNotEqual() {
        BinaryExpr a = new BinaryExpr(new VarRefExpr("x"), BinaryOp.ADD, new VarRefExpr("y"));
        BinaryExpr b = new BinaryExpr(new VarRefExpr("z"), BinaryOp.ADD, new VarRefExpr("y"));

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentOperator_areNotEqual() {
        BinaryExpr a = new BinaryExpr(new VarRefExpr("x"), BinaryOp.ADD, new VarRefExpr("y"));
        BinaryExpr b = new BinaryExpr(new VarRefExpr("x"), BinaryOp.SUB, new VarRefExpr("y"));

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentRight_areNotEqual() {
        BinaryExpr a = new BinaryExpr(new VarRefExpr("x"), BinaryOp.ADD, new VarRefExpr("y"));
        BinaryExpr b = new BinaryExpr(new VarRefExpr("x"), BinaryOp.ADD, new VarRefExpr("z"));

        assertNotEquals(a, b);
    }

    @Test
    void implementsExpression() {
        BinaryExpr expr = new BinaryExpr(new VarRefExpr("x"), BinaryOp.ADD, new VarRefExpr("y"));

        assertInstanceOf(Expression.class, expr);
    }

    @Test
    void nestedBinaryExpr_worksCorrectly() {
        // (a + b) * c
        BinaryExpr add = new BinaryExpr(new VarRefExpr("a"), BinaryOp.ADD, new VarRefExpr("b"));
        BinaryExpr mul = new BinaryExpr(add, BinaryOp.MUL, new VarRefExpr("c"));

        assertInstanceOf(BinaryExpr.class, mul.left());
        assertEquals(BinaryOp.MUL, mul.operator());
    }
}
