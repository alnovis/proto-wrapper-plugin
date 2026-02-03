package io.alnovis.protowrapper.ir.type;

import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ArrayType}.
 */
class ArrayTypeTest {

    @Test
    void of_primitiveComponent_createsArrayType() {
        PrimitiveType intType = PrimitiveType.of(PrimitiveKind.INT);
        ArrayType arrayType = ArrayType.of(intType);

        assertNotNull(arrayType);
        assertEquals(intType, arrayType.componentType());
    }

    @Test
    void of_classComponent_createsArrayType() {
        ClassType stringType = ClassType.of("java.lang.String");
        ArrayType arrayType = ArrayType.of(stringType);

        assertEquals(stringType, arrayType.componentType());
    }

    @Test
    void of_nullComponent_throwsException() {
        assertThrows(NullPointerException.class, () -> ArrayType.of(null));
    }

    @Test
    void componentType_returnsComponentType() {
        TypeRef component = ClassType.of("java.lang.Object");
        ArrayType arrayType = ArrayType.of(component);

        assertSame(component, arrayType.componentType());
    }

    @Test
    void nestedArrayType_createsMultiDimensionalArray() {
        PrimitiveType intType = PrimitiveType.of(PrimitiveKind.INT);
        ArrayType intArray = ArrayType.of(intType);
        ArrayType int2DArray = ArrayType.of(intArray);

        // int[][]
        assertInstanceOf(ArrayType.class, int2DArray.componentType());
        assertEquals(intArray, int2DArray.componentType());
        assertEquals(intType, ((ArrayType) int2DArray.componentType()).componentType());
    }

    @Test
    void dimensions_singleDimensional_returns1() {
        ArrayType array = ArrayType.of(PrimitiveType.of(PrimitiveKind.INT));

        assertEquals(1, array.dimensions());
    }

    @Test
    void dimensions_twoDimensional_returns2() {
        ArrayType intArray = ArrayType.of(PrimitiveType.of(PrimitiveKind.INT));
        ArrayType int2D = ArrayType.of(intArray);

        assertEquals(2, int2D.dimensions());
    }

    @Test
    void dimensions_threeDimensional_returns3() {
        ArrayType intArray = ArrayType.of(PrimitiveType.of(PrimitiveKind.INT));
        ArrayType int2D = ArrayType.of(intArray);
        ArrayType int3D = ArrayType.of(int2D);

        assertEquals(3, int3D.dimensions());
    }

    @Test
    void elementType_singleDimensional_returnsComponent() {
        PrimitiveType intType = PrimitiveType.of(PrimitiveKind.INT);
        ArrayType array = ArrayType.of(intType);

        assertEquals(intType, array.elementType());
    }

    @Test
    void elementType_multiDimensional_returnsInnermost() {
        PrimitiveType intType = PrimitiveType.of(PrimitiveKind.INT);
        ArrayType int3D = ArrayType.of(ArrayType.of(ArrayType.of(intType)));

        assertEquals(intType, int3D.elementType());
    }

    @Test
    void equals_sameComponent_areEqual() {
        ArrayType a = ArrayType.of(PrimitiveType.of(PrimitiveKind.INT));
        ArrayType b = ArrayType.of(PrimitiveType.of(PrimitiveKind.INT));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentComponent_areNotEqual() {
        ArrayType intArray = ArrayType.of(PrimitiveType.of(PrimitiveKind.INT));
        ArrayType longArray = ArrayType.of(PrimitiveType.of(PrimitiveKind.LONG));

        assertNotEquals(intArray, longArray);
    }

    @Test
    void equals_differentDimensions_areNotEqual() {
        ArrayType int1D = ArrayType.of(PrimitiveType.of(PrimitiveKind.INT));
        ArrayType int2D = ArrayType.of(int1D);

        assertNotEquals(int1D, int2D);
    }

    @Test
    void implementsTypeRef() {
        ArrayType type = ArrayType.of(PrimitiveType.of(PrimitiveKind.INT));

        assertInstanceOf(TypeRef.class, type);
    }
}
