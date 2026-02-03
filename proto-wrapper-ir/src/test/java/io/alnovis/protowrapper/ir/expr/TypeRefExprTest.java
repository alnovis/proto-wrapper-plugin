package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.ClassType;
import io.alnovis.protowrapper.ir.type.TypeRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TypeRefExpr}.
 */
class TypeRefExprTest {

    @Test
    void constructor_validType_succeeds() {
        TypeRef type = ClassType.of("java.lang.Math");
        TypeRefExpr typeRef = new TypeRefExpr(type);

        assertEquals(type, typeRef.type());
    }

    @Test
    void constructor_nullType_throwsException() {
        assertThrows(NullPointerException.class, () -> new TypeRefExpr(null));
    }

    @Test
    void of_factoryMethod_createsTypeRefExpr() {
        TypeRef type = ClassType.of("java.lang.System");
        TypeRefExpr typeRef = TypeRefExpr.of(type);

        assertNotNull(typeRef);
        assertEquals(type, typeRef.type());
    }

    @Test
    void equals_sameType_areEqual() {
        TypeRef type = ClassType.of("java.lang.Math");
        TypeRefExpr a = new TypeRefExpr(type);
        TypeRefExpr b = new TypeRefExpr(ClassType.of("java.lang.Math"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentType_areNotEqual() {
        TypeRefExpr a = new TypeRefExpr(ClassType.of("java.lang.Math"));
        TypeRefExpr b = new TypeRefExpr(ClassType.of("java.lang.System"));

        assertNotEquals(a, b);
    }

    @Test
    void implementsExpression() {
        TypeRefExpr typeRef = new TypeRefExpr(ClassType.of("java.lang.Math"));

        assertInstanceOf(Expression.class, typeRef);
    }
}
