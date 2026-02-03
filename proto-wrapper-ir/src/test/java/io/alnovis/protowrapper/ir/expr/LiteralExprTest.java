package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.ClassType;
import io.alnovis.protowrapper.ir.type.PrimitiveType;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import io.alnovis.protowrapper.ir.type.TypeRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LiteralExpr}.
 */
class LiteralExprTest {

    @Test
    void constructor_nullValue_nullType_succeeds() {
        // null literal
        LiteralExpr nullLit = new LiteralExpr(null, null);

        assertNull(nullLit.value());
        assertNull(nullLit.type());
    }

    @Test
    void constructor_intValue_intType_succeeds() {
        TypeRef intType = PrimitiveType.of(PrimitiveKind.INT);
        LiteralExpr literal = new LiteralExpr(42, intType);

        assertEquals(42, literal.value());
        assertEquals(intType, literal.type());
    }

    @Test
    void constructor_stringValue_stringType_succeeds() {
        TypeRef stringType = ClassType.of("java.lang.String");
        LiteralExpr literal = new LiteralExpr("hello", stringType);

        assertEquals("hello", literal.value());
        assertEquals(stringType, literal.type());
    }

    @Test
    void constructor_booleanValue_booleanType_succeeds() {
        TypeRef boolType = PrimitiveType.of(PrimitiveKind.BOOLEAN);
        LiteralExpr trueLit = new LiteralExpr(true, boolType);
        LiteralExpr falseLit = new LiteralExpr(false, boolType);

        assertEquals(true, trueLit.value());
        assertEquals(false, falseLit.value());
    }

    @Test
    void constructor_longValue_longType_succeeds() {
        TypeRef longType = PrimitiveType.of(PrimitiveKind.LONG);
        LiteralExpr literal = new LiteralExpr(123456789012345L, longType);

        assertEquals(123456789012345L, literal.value());
        assertEquals(longType, literal.type());
    }

    @Test
    void constructor_doubleValue_doubleType_succeeds() {
        TypeRef doubleType = PrimitiveType.of(PrimitiveKind.DOUBLE);
        LiteralExpr literal = new LiteralExpr(3.14159, doubleType);

        assertEquals(3.14159, literal.value());
        assertEquals(doubleType, literal.type());
    }

    @Test
    void constructor_charValue_charType_succeeds() {
        TypeRef charType = PrimitiveType.of(PrimitiveKind.CHAR);
        LiteralExpr literal = new LiteralExpr('A', charType);

        assertEquals('A', literal.value());
        assertEquals(charType, literal.type());
    }

    @Test
    void isNull_nullValue_returnsTrue() {
        LiteralExpr nullLit = new LiteralExpr(null, ClassType.of("java.lang.Object"));

        assertTrue(nullLit.isNull());
    }

    @Test
    void isNull_nonNullValue_returnsFalse() {
        LiteralExpr literal = new LiteralExpr(42, PrimitiveType.of(PrimitiveKind.INT));

        assertFalse(literal.isNull());
    }

    @Test
    void isClass_classTypeValue_returnsTrue() {
        TypeRef classLiteralType = ClassType.of("java.lang.Class");
        ClassType stringType = ClassType.of("java.lang.String");
        LiteralExpr classLit = new LiteralExpr(stringType, classLiteralType);

        assertTrue(classLit.isClass());
        assertEquals(stringType, classLit.value());
    }

    @Test
    void isClass_nonClassTypeValue_returnsFalse() {
        LiteralExpr literal = new LiteralExpr(42, PrimitiveType.of(PrimitiveKind.INT));

        assertFalse(literal.isClass());
    }

    @Test
    void isString_stringValue_returnsTrue() {
        LiteralExpr literal = new LiteralExpr("hello", ClassType.of("java.lang.String"));

        assertTrue(literal.isString());
    }

    @Test
    void isString_nonStringValue_returnsFalse() {
        LiteralExpr literal = new LiteralExpr(42, PrimitiveType.of(PrimitiveKind.INT));

        assertFalse(literal.isString());
    }

    @Test
    void isNumeric_intValue_returnsTrue() {
        LiteralExpr literal = new LiteralExpr(42, PrimitiveType.of(PrimitiveKind.INT));

        assertTrue(literal.isNumeric());
    }

    @Test
    void isNumeric_doubleValue_returnsTrue() {
        LiteralExpr literal = new LiteralExpr(3.14, PrimitiveType.of(PrimitiveKind.DOUBLE));

        assertTrue(literal.isNumeric());
    }

    @Test
    void isNumeric_stringValue_returnsFalse() {
        LiteralExpr literal = new LiteralExpr("hello", ClassType.of("java.lang.String"));

        assertFalse(literal.isNumeric());
    }

    @Test
    void isBoolean_trueValue_returnsTrue() {
        LiteralExpr literal = new LiteralExpr(true, PrimitiveType.of(PrimitiveKind.BOOLEAN));

        assertTrue(literal.isBoolean());
    }

    @Test
    void isBoolean_intValue_returnsFalse() {
        LiteralExpr literal = new LiteralExpr(42, PrimitiveType.of(PrimitiveKind.INT));

        assertFalse(literal.isBoolean());
    }

    @Test
    void equals_sameValueAndType_areEqual() {
        TypeRef intType = PrimitiveType.of(PrimitiveKind.INT);
        LiteralExpr a = new LiteralExpr(42, intType);
        LiteralExpr b = new LiteralExpr(42, intType);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentValue_areNotEqual() {
        TypeRef intType = PrimitiveType.of(PrimitiveKind.INT);
        LiteralExpr a = new LiteralExpr(42, intType);
        LiteralExpr b = new LiteralExpr(43, intType);

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentType_areNotEqual() {
        LiteralExpr a = new LiteralExpr(42, PrimitiveType.of(PrimitiveKind.INT));
        LiteralExpr b = new LiteralExpr(42, PrimitiveType.of(PrimitiveKind.LONG));

        assertNotEquals(a, b);
    }

    @Test
    void implementsExpression() {
        LiteralExpr literal = new LiteralExpr(42, PrimitiveType.of(PrimitiveKind.INT));

        assertInstanceOf(Expression.class, literal);
    }
}
