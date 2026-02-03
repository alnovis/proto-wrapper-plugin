package io.alnovis.protowrapper.ir.type;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TypeVariable}.
 */
class TypeVariableTest {

    @Test
    void of_simpleName_createsUnboundedTypeVariable() {
        TypeVariable typeVar = TypeVariable.of("T");

        assertEquals("T", typeVar.name());
        assertTrue(typeVar.bounds().isEmpty());
    }

    @Test
    void of_nullName_throwsException() {
        assertThrows(NullPointerException.class, () -> TypeVariable.of(null));
    }

    @Test
    void of_emptyName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> TypeVariable.of(""));
    }

    @Test
    void of_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> TypeVariable.of("   "));
    }

    @Test
    void constructor_withBounds_createsBoundedTypeVariable() {
        ClassType comparableBound = ClassType.of("java.lang.Comparable");
        TypeVariable typeVar = new TypeVariable("T", List.of(comparableBound));

        assertEquals("T", typeVar.name());
        assertEquals(1, typeVar.bounds().size());
        assertEquals(comparableBound, typeVar.bounds().get(0));
    }

    @Test
    void constructor_nullBounds_treatedAsEmpty() {
        TypeVariable typeVar = new TypeVariable("T", null);

        assertNotNull(typeVar.bounds());
        assertTrue(typeVar.bounds().isEmpty());
    }

    @Test
    void constructor_multipleBounds_preservesAll() {
        ClassType bound1 = ClassType.of("java.lang.Comparable");
        ClassType bound2 = ClassType.of("java.io.Serializable");
        TypeVariable typeVar = new TypeVariable("T", List.of(bound1, bound2));

        assertEquals(2, typeVar.bounds().size());
        assertEquals(bound1, typeVar.bounds().get(0));
        assertEquals(bound2, typeVar.bounds().get(1));
    }

    @Test
    void bounds_areImmutable() {
        TypeVariable typeVar = new TypeVariable("T", List.of(ClassType.of("java.lang.Number")));

        assertThrows(UnsupportedOperationException.class,
            () -> typeVar.bounds().add(ClassType.of("java.io.Serializable")));
    }

    @Test
    void isUnbounded_noBounds_returnsTrue() {
        TypeVariable typeVar = TypeVariable.of("T");

        assertTrue(typeVar.isUnbounded());
    }

    @Test
    void isUnbounded_withBounds_returnsFalse() {
        TypeVariable typeVar = new TypeVariable("T", List.of(ClassType.of("java.lang.Number")));

        assertFalse(typeVar.isUnbounded());
    }

    @Test
    void equals_sameName_noBounds_areEqual() {
        TypeVariable a = TypeVariable.of("T");
        TypeVariable b = TypeVariable.of("T");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentName_areNotEqual() {
        TypeVariable t = TypeVariable.of("T");
        TypeVariable e = TypeVariable.of("E");

        assertNotEquals(t, e);
    }

    @Test
    void equals_sameNameSameBounds_areEqual() {
        TypeVariable a = new TypeVariable("T", List.of(ClassType.of("java.lang.Number")));
        TypeVariable b = new TypeVariable("T", List.of(ClassType.of("java.lang.Number")));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_sameNameDifferentBounds_areNotEqual() {
        TypeVariable a = new TypeVariable("T", List.of(ClassType.of("java.lang.Number")));
        TypeVariable b = new TypeVariable("T", List.of(ClassType.of("java.lang.Comparable")));

        assertNotEquals(a, b);
    }

    @Test
    void equals_withVsWithoutBounds_areNotEqual() {
        TypeVariable unbounded = TypeVariable.of("T");
        TypeVariable bounded = new TypeVariable("T", List.of(ClassType.of("java.lang.Number")));

        assertNotEquals(unbounded, bounded);
    }

    @Test
    void implementsTypeRef() {
        TypeVariable type = TypeVariable.of("T");

        assertInstanceOf(TypeRef.class, type);
    }
}
