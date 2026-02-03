package io.alnovis.protowrapper.ir.type;

import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PrimitiveType}.
 */
class PrimitiveTypeTest {

    @ParameterizedTest
    @EnumSource(PrimitiveKind.class)
    void of_createsCorrectType(PrimitiveKind kind) {
        PrimitiveType type = PrimitiveType.of(kind);

        assertNotNull(type);
        assertEquals(kind, type.kind());
    }

    @Test
    void of_nullKind_throwsException() {
        assertThrows(NullPointerException.class, () -> PrimitiveType.of(null));
    }

    @Test
    void of_sameKind_returnsCachedInstance() {
        // PrimitiveType should cache instances for each kind
        PrimitiveType int1 = PrimitiveType.of(PrimitiveKind.INT);
        PrimitiveType int2 = PrimitiveType.of(PrimitiveKind.INT);

        assertSame(int1, int2, "PrimitiveType instances should be cached");
    }

    @Test
    void kind_returnsCorrectKind() {
        assertEquals(PrimitiveKind.BOOLEAN, PrimitiveType.of(PrimitiveKind.BOOLEAN).kind());
        assertEquals(PrimitiveKind.BYTE, PrimitiveType.of(PrimitiveKind.BYTE).kind());
        assertEquals(PrimitiveKind.CHAR, PrimitiveType.of(PrimitiveKind.CHAR).kind());
        assertEquals(PrimitiveKind.SHORT, PrimitiveType.of(PrimitiveKind.SHORT).kind());
        assertEquals(PrimitiveKind.INT, PrimitiveType.of(PrimitiveKind.INT).kind());
        assertEquals(PrimitiveKind.LONG, PrimitiveType.of(PrimitiveKind.LONG).kind());
        assertEquals(PrimitiveKind.FLOAT, PrimitiveType.of(PrimitiveKind.FLOAT).kind());
        assertEquals(PrimitiveKind.DOUBLE, PrimitiveType.of(PrimitiveKind.DOUBLE).kind());
    }

    @Test
    void equals_sameKind_areEqual() {
        PrimitiveType a = PrimitiveType.of(PrimitiveKind.INT);
        PrimitiveType b = PrimitiveType.of(PrimitiveKind.INT);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentKind_areNotEqual() {
        PrimitiveType intType = PrimitiveType.of(PrimitiveKind.INT);
        PrimitiveType longType = PrimitiveType.of(PrimitiveKind.LONG);

        assertNotEquals(intType, longType);
    }

    @Test
    void implementsTypeRef() {
        PrimitiveType type = PrimitiveType.of(PrimitiveKind.INT);

        assertInstanceOf(TypeRef.class, type);
    }

    @ParameterizedTest
    @EnumSource(PrimitiveKind.class)
    void primitiveKind_keyword_returnsCorrectKeyword(PrimitiveKind kind) {
        String keyword = kind.keyword();

        assertNotNull(keyword);
        assertFalse(keyword.isEmpty());
        assertEquals(kind.name().toLowerCase(), keyword);
    }
}
