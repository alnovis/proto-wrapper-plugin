package io.alnovis.protowrapper.ir.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link VoidType}.
 */
class VoidTypeTest {

    @Test
    void instance_isSingleton() {
        VoidType a = VoidType.INSTANCE;
        VoidType b = VoidType.INSTANCE;

        assertSame(a, b);
    }

    @Test
    void equals_sameInstance_areEqual() {
        VoidType a = VoidType.INSTANCE;
        VoidType b = VoidType.INSTANCE;

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void implementsTypeRef() {
        assertInstanceOf(TypeRef.class, VoidType.INSTANCE);
    }

    @Test
    void notNull() {
        assertNotNull(VoidType.INSTANCE);
    }
}
