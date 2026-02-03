package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.ClassType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FieldAccessExpr}.
 */
class FieldAccessExprTest {

    @Test
    void constructor_validTargetAndFieldName_succeeds() {
        Expression target = new VarRefExpr("obj");
        FieldAccessExpr fieldAccess = new FieldAccessExpr(target, "field");

        assertEquals(target, fieldAccess.target());
        assertEquals("field", fieldAccess.fieldName());
    }

    @Test
    void constructor_nullTarget_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new FieldAccessExpr(null, "field"));
    }

    @Test
    void constructor_nullFieldName_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new FieldAccessExpr(new VarRefExpr("obj"), null));
    }

    @Test
    void constructor_emptyFieldName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new FieldAccessExpr(new VarRefExpr("obj"), ""));
    }

    @Test
    void constructor_blankFieldName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new FieldAccessExpr(new VarRefExpr("obj"), "   "));
    }

    @Test
    void target_thisExpr_accessesThisField() {
        FieldAccessExpr fieldAccess = new FieldAccessExpr(ThisExpr.INSTANCE, "name");

        assertSame(ThisExpr.INSTANCE, fieldAccess.target());
        assertEquals("name", fieldAccess.fieldName());
    }

    @Test
    void target_typeRefExpr_accessesStaticField() {
        TypeRefExpr typeRef = new TypeRefExpr(ClassType.of("java.lang.Math"));
        FieldAccessExpr staticField = new FieldAccessExpr(typeRef, "PI");

        assertInstanceOf(TypeRefExpr.class, staticField.target());
        assertEquals("PI", staticField.fieldName());
    }

    @Test
    void equals_sameTargetAndFieldName_areEqual() {
        FieldAccessExpr a = new FieldAccessExpr(new VarRefExpr("obj"), "field");
        FieldAccessExpr b = new FieldAccessExpr(new VarRefExpr("obj"), "field");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentTarget_areNotEqual() {
        FieldAccessExpr a = new FieldAccessExpr(new VarRefExpr("obj1"), "field");
        FieldAccessExpr b = new FieldAccessExpr(new VarRefExpr("obj2"), "field");

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentFieldName_areNotEqual() {
        Expression target = new VarRefExpr("obj");
        FieldAccessExpr a = new FieldAccessExpr(target, "field1");
        FieldAccessExpr b = new FieldAccessExpr(target, "field2");

        assertNotEquals(a, b);
    }

    @Test
    void implementsExpression() {
        FieldAccessExpr fieldAccess = new FieldAccessExpr(new VarRefExpr("obj"), "field");

        assertInstanceOf(Expression.class, fieldAccess);
    }
}
