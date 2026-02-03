package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Types}.
 */
class TypesTest {

    // ========================================================================
    // Primitive Type Constants
    // ========================================================================

    @Test
    void primitiveConstants_areCorrectTypes() {
        assertEquals(PrimitiveKind.BOOLEAN, Types.BOOLEAN.kind());
        assertEquals(PrimitiveKind.BYTE, Types.BYTE.kind());
        assertEquals(PrimitiveKind.CHAR, Types.CHAR.kind());
        assertEquals(PrimitiveKind.SHORT, Types.SHORT.kind());
        assertEquals(PrimitiveKind.INT, Types.INT.kind());
        assertEquals(PrimitiveKind.LONG, Types.LONG.kind());
        assertEquals(PrimitiveKind.FLOAT, Types.FLOAT.kind());
        assertEquals(PrimitiveKind.DOUBLE, Types.DOUBLE.kind());
    }

    @Test
    void void_isVoidType() {
        assertSame(VoidType.INSTANCE, Types.VOID);
    }

    // ========================================================================
    // Common Class Type Constants
    // ========================================================================

    @Test
    void object_isCorrectType() {
        assertEquals("java.lang", Types.OBJECT.packageName());
        assertEquals("Object", Types.OBJECT.simpleName());
    }

    @Test
    void string_isCorrectType() {
        assertEquals("java.lang", Types.STRING.packageName());
        assertEquals("String", Types.STRING.simpleName());
    }

    @Test
    void boxedTypes_areCorrect() {
        assertEquals("java.lang.Boolean", Types.BOOLEAN_BOXED.qualifiedName());
        assertEquals("java.lang.Byte", Types.BYTE_BOXED.qualifiedName());
        assertEquals("java.lang.Character", Types.CHAR_BOXED.qualifiedName());
        assertEquals("java.lang.Short", Types.SHORT_BOXED.qualifiedName());
        assertEquals("java.lang.Integer", Types.INT_BOXED.qualifiedName());
        assertEquals("java.lang.Long", Types.LONG_BOXED.qualifiedName());
        assertEquals("java.lang.Float", Types.FLOAT_BOXED.qualifiedName());
        assertEquals("java.lang.Double", Types.DOUBLE_BOXED.qualifiedName());
    }

    // ========================================================================
    // Class Type Factory Methods
    // ========================================================================

    @Test
    void type_simpleClass_createsClassType() {
        ClassType type = Types.type("java.util.ArrayList");

        assertEquals("java.util", type.packageName());
        assertEquals("ArrayList", type.simpleName());
        assertFalse(type.isParameterized());
    }

    @Test
    void type_withTypeArguments_createsParameterizedType() {
        ClassType type = Types.type("java.util.List", Types.STRING);

        assertEquals("List", type.simpleName());
        assertTrue(type.isParameterized());
        assertEquals(1, type.typeArguments().size());
        assertEquals(Types.STRING, type.typeArguments().get(0));
    }

    @Test
    void type_withMultipleTypeArguments_createsParameterizedType() {
        ClassType type = Types.type("java.util.Map", Types.STRING, Types.INT_BOXED);

        assertTrue(type.isParameterized());
        assertEquals(2, type.typeArguments().size());
    }

    @Test
    void type_withListTypeArguments_createsParameterizedType() {
        ClassType type = Types.type("java.util.List", List.of(Types.STRING));

        assertTrue(type.isParameterized());
        assertEquals(1, type.typeArguments().size());
    }

    // ========================================================================
    // Array Type Factory Methods
    // ========================================================================

    @Test
    void array_singleDimension_createsArrayType() {
        ArrayType type = Types.array(Types.INT);

        assertEquals(Types.INT, type.componentType());
        assertEquals(1, type.dimensions());
    }

    @Test
    void array_multiDimension_createsNestedArrayType() {
        ArrayType type = Types.array(Types.INT, 3);

        assertEquals(3, type.dimensions());
    }

    @Test
    void array_zeroDimension_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> Types.array(Types.INT, 0));
    }

    @Test
    void array_negativeDimension_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> Types.array(Types.INT, -1));
    }

    // ========================================================================
    // Wildcard Type Factory Methods
    // ========================================================================

    @Test
    void wildcard_unbounded_createsUnboundedWildcard() {
        WildcardType type = Types.wildcard();

        assertTrue(type.isUnbounded());
    }

    @Test
    void wildcardExtends_createsUpperBoundedWildcard() {
        WildcardType type = Types.wildcardExtends(Types.type("java.lang.Number"));

        assertTrue(type.hasUpperBound());
        assertFalse(type.hasLowerBound());
    }

    @Test
    void wildcardSuper_createsLowerBoundedWildcard() {
        WildcardType type = Types.wildcardSuper(Types.INT_BOXED);

        assertTrue(type.hasLowerBound());
        assertFalse(type.hasUpperBound());
    }

    // ========================================================================
    // Type Variable Factory Methods
    // ========================================================================

    @Test
    void typeVar_simple_createsUnboundedTypeVariable() {
        TypeVariable type = Types.typeVar("T");

        assertEquals("T", type.name());
        assertTrue(type.bounds().isEmpty());
    }

    @Test
    void typeVar_withBounds_createsBoundedTypeVariable() {
        TypeVariable type = Types.typeVar("T", Types.type("java.lang.Number"));

        assertEquals("T", type.name());
        assertEquals(1, type.bounds().size());
    }

    @Test
    void typeVar_withListBounds_createsBoundedTypeVariable() {
        TypeVariable type = Types.typeVar("T", List.of(Types.type("java.lang.Number")));

        assertEquals(1, type.bounds().size());
    }

    // ========================================================================
    // Convenience Methods for Common Generic Types
    // ========================================================================

    @Test
    void list_createsListType() {
        ClassType type = Types.list(Types.STRING);

        assertEquals("java.util.List", type.qualifiedName());
        assertTrue(type.isParameterized());
        assertEquals(Types.STRING, type.typeArguments().get(0));
    }

    @Test
    void list_withPrimitive_boxesAutomatically() {
        ClassType type = Types.list(Types.INT);

        assertEquals(Types.INT_BOXED, type.typeArguments().get(0));
    }

    @Test
    void set_createsSetType() {
        ClassType type = Types.set(Types.STRING);

        assertEquals("java.util.Set", type.qualifiedName());
    }

    @Test
    void map_createsMapType() {
        ClassType type = Types.map(Types.STRING, Types.INT);

        assertEquals("java.util.Map", type.qualifiedName());
        assertEquals(2, type.typeArguments().size());
        assertEquals(Types.STRING, type.typeArguments().get(0));
        assertEquals(Types.INT_BOXED, type.typeArguments().get(1));
    }

    @Test
    void optional_createsOptionalType() {
        ClassType type = Types.optional(Types.STRING);

        assertEquals("java.util.Optional", type.qualifiedName());
    }

    @Test
    void classOf_createsClassType() {
        ClassType type = Types.classOf(Types.STRING);

        assertEquals("java.lang.Class", type.qualifiedName());
        assertEquals(Types.STRING, type.typeArguments().get(0));
    }

    @Test
    void collection_createsCollectionType() {
        ClassType type = Types.collection(Types.STRING);

        assertEquals("java.util.Collection", type.qualifiedName());
    }

    @Test
    void iterable_createsIterableType() {
        ClassType type = Types.iterable(Types.STRING);

        assertEquals("java.lang.Iterable", type.qualifiedName());
    }

    @Test
    void supplier_createsSupplierType() {
        ClassType type = Types.supplier(Types.STRING);

        assertEquals("java.util.function.Supplier", type.qualifiedName());
    }

    @Test
    void consumer_createsConsumerType() {
        ClassType type = Types.consumer(Types.STRING);

        assertEquals("java.util.function.Consumer", type.qualifiedName());
    }

    @Test
    void function_createsFunctionType() {
        ClassType type = Types.function(Types.STRING, Types.INT);

        assertEquals("java.util.function.Function", type.qualifiedName());
        assertEquals(2, type.typeArguments().size());
    }

    @Test
    void predicate_createsPredicateType() {
        ClassType type = Types.predicate(Types.STRING);

        assertEquals("java.util.function.Predicate", type.qualifiedName());
    }

    // ========================================================================
    // Boxing Utilities
    // ========================================================================

    @Test
    void boxIfPrimitive_primitive_returnsBoxed() {
        assertEquals(Types.INT_BOXED, Types.boxIfPrimitive(Types.INT));
        assertEquals(Types.BOOLEAN_BOXED, Types.boxIfPrimitive(Types.BOOLEAN));
    }

    @Test
    void boxIfPrimitive_nonPrimitive_returnsOriginal() {
        assertSame(Types.STRING, Types.boxIfPrimitive(Types.STRING));
    }

    @Test
    void box_allPrimitives_returnCorrectBoxed() {
        assertEquals(Types.BOOLEAN_BOXED, Types.box(Types.BOOLEAN));
        assertEquals(Types.BYTE_BOXED, Types.box(Types.BYTE));
        assertEquals(Types.CHAR_BOXED, Types.box(Types.CHAR));
        assertEquals(Types.SHORT_BOXED, Types.box(Types.SHORT));
        assertEquals(Types.INT_BOXED, Types.box(Types.INT));
        assertEquals(Types.LONG_BOXED, Types.box(Types.LONG));
        assertEquals(Types.FLOAT_BOXED, Types.box(Types.FLOAT));
        assertEquals(Types.DOUBLE_BOXED, Types.box(Types.DOUBLE));
    }

    // ========================================================================
    // Type Checking Utilities
    // ========================================================================

    @Test
    void isPrimitive_primitive_returnsTrue() {
        assertTrue(Types.isPrimitive(Types.INT));
        assertTrue(Types.isPrimitive(Types.BOOLEAN));
    }

    @Test
    void isPrimitive_nonPrimitive_returnsFalse() {
        assertFalse(Types.isPrimitive(Types.STRING));
        assertFalse(Types.isPrimitive(Types.VOID));
    }

    @Test
    void isVoid_void_returnsTrue() {
        assertTrue(Types.isVoid(Types.VOID));
    }

    @Test
    void isVoid_nonVoid_returnsFalse() {
        assertFalse(Types.isVoid(Types.INT));
        assertFalse(Types.isVoid(Types.STRING));
    }

    @Test
    void isArray_array_returnsTrue() {
        assertTrue(Types.isArray(Types.array(Types.INT)));
    }

    @Test
    void isArray_nonArray_returnsFalse() {
        assertFalse(Types.isArray(Types.INT));
        assertFalse(Types.isArray(Types.STRING));
    }
}
