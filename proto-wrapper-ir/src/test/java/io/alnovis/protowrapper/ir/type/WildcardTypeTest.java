package io.alnovis.protowrapper.ir.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WildcardType}.
 */
class WildcardTypeTest {

    @Test
    void unbounded_createsUnboundedWildcard() {
        WildcardType wildcard = WildcardType.unbounded();

        assertNotNull(wildcard);
        assertTrue(wildcard.isUnbounded());
        assertNull(wildcard.upperBound());
        assertNull(wildcard.lowerBound());
    }

    @Test
    void extendsType_createsUpperBoundedWildcard() {
        ClassType numberType = ClassType.of("java.lang.Number");
        WildcardType wildcard = WildcardType.extendsType(numberType);

        assertNotNull(wildcard);
        assertFalse(wildcard.isUnbounded());
        assertEquals(numberType, wildcard.upperBound());
        assertNull(wildcard.lowerBound());
    }

    @Test
    void extendsType_nullBound_throwsException() {
        assertThrows(NullPointerException.class, () -> WildcardType.extendsType(null));
    }

    @Test
    void superType_createsLowerBoundedWildcard() {
        ClassType integerType = ClassType.of("java.lang.Integer");
        WildcardType wildcard = WildcardType.superType(integerType);

        assertNotNull(wildcard);
        assertFalse(wildcard.isUnbounded());
        assertNull(wildcard.upperBound());
        assertEquals(integerType, wildcard.lowerBound());
    }

    @Test
    void superType_nullBound_throwsException() {
        assertThrows(NullPointerException.class, () -> WildcardType.superType(null));
    }

    @Test
    void isUnbounded_unbounded_returnsTrue() {
        WildcardType wildcard = WildcardType.unbounded();

        assertTrue(wildcard.isUnbounded());
    }

    @Test
    void isUnbounded_upperBounded_returnsFalse() {
        WildcardType wildcard = WildcardType.extendsType(ClassType.of("java.lang.Number"));

        assertFalse(wildcard.isUnbounded());
    }

    @Test
    void isUnbounded_lowerBounded_returnsFalse() {
        WildcardType wildcard = WildcardType.superType(ClassType.of("java.lang.Integer"));

        assertFalse(wildcard.isUnbounded());
    }

    @Test
    void hasUpperBound_withUpperBound_returnsTrue() {
        WildcardType wildcard = WildcardType.extendsType(ClassType.of("java.lang.Number"));

        assertTrue(wildcard.hasUpperBound());
        assertFalse(wildcard.hasLowerBound());
    }

    @Test
    void hasLowerBound_withLowerBound_returnsTrue() {
        WildcardType wildcard = WildcardType.superType(ClassType.of("java.lang.Integer"));

        assertTrue(wildcard.hasLowerBound());
        assertFalse(wildcard.hasUpperBound());
    }

    @Test
    void equals_bothUnbounded_areEqual() {
        WildcardType a = WildcardType.unbounded();
        WildcardType b = WildcardType.unbounded();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_sameUpperBound_areEqual() {
        ClassType bound = ClassType.of("java.lang.Number");
        WildcardType a = WildcardType.extendsType(bound);
        WildcardType b = WildcardType.extendsType(ClassType.of("java.lang.Number"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentUpperBound_areNotEqual() {
        WildcardType a = WildcardType.extendsType(ClassType.of("java.lang.Number"));
        WildcardType b = WildcardType.extendsType(ClassType.of("java.lang.Comparable"));

        assertNotEquals(a, b);
    }

    @Test
    void equals_sameLowerBound_areEqual() {
        WildcardType a = WildcardType.superType(ClassType.of("java.lang.Integer"));
        WildcardType b = WildcardType.superType(ClassType.of("java.lang.Integer"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_upperVsLowerBound_areNotEqual() {
        ClassType type = ClassType.of("java.lang.Number");
        WildcardType upper = WildcardType.extendsType(type);
        WildcardType lower = WildcardType.superType(type);

        assertNotEquals(upper, lower);
    }

    @Test
    void equals_unboundedVsBounded_areNotEqual() {
        WildcardType unbounded = WildcardType.unbounded();
        WildcardType bounded = WildcardType.extendsType(ClassType.of("java.lang.Object"));

        assertNotEquals(unbounded, bounded);
    }

    @Test
    void implementsTypeRef() {
        WildcardType type = WildcardType.unbounded();

        assertInstanceOf(TypeRef.class, type);
    }
}
