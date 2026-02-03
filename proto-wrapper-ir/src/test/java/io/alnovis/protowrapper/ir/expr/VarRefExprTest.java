package io.alnovis.protowrapper.ir.expr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link VarRefExpr}.
 */
class VarRefExprTest {

    @Test
    void constructor_validName_succeeds() {
        VarRefExpr varRef = new VarRefExpr("myVariable");

        assertEquals("myVariable", varRef.name());
    }

    @Test
    void constructor_nullName_throwsException() {
        assertThrows(NullPointerException.class, () -> new VarRefExpr(null));
    }

    @Test
    void constructor_emptyName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new VarRefExpr(""));
    }

    @Test
    void constructor_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new VarRefExpr("   "));
    }

    @Test
    void name_returnsName() {
        VarRefExpr varRef = new VarRefExpr("count");

        assertEquals("count", varRef.name());
    }

    @Test
    void equals_sameName_areEqual() {
        VarRefExpr a = new VarRefExpr("x");
        VarRefExpr b = new VarRefExpr("x");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentName_areNotEqual() {
        VarRefExpr a = new VarRefExpr("x");
        VarRefExpr b = new VarRefExpr("y");

        assertNotEquals(a, b);
    }

    @Test
    void implementsExpression() {
        VarRefExpr varRef = new VarRefExpr("x");

        assertInstanceOf(Expression.class, varRef);
    }
}
