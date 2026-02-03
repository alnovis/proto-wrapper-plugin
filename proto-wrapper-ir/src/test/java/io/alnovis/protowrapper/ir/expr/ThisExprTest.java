package io.alnovis.protowrapper.ir.expr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ThisExpr}.
 */
class ThisExprTest {

    @Test
    void instance_isSingleton() {
        ThisExpr a = ThisExpr.INSTANCE;
        ThisExpr b = ThisExpr.INSTANCE;

        assertSame(a, b);
    }

    @Test
    void equals_sameInstance_areEqual() {
        ThisExpr a = ThisExpr.INSTANCE;
        ThisExpr b = ThisExpr.INSTANCE;

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void implementsExpression() {
        assertInstanceOf(Expression.class, ThisExpr.INSTANCE);
    }

    @Test
    void notNull() {
        assertNotNull(ThisExpr.INSTANCE);
    }
}
