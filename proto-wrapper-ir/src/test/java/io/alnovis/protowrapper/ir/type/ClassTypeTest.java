package io.alnovis.protowrapper.ir.type;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ClassType}.
 */
class ClassTypeTest {

    @Test
    void of_simpleClassName_createsType() {
        ClassType type = ClassType.of("java.lang.String");

        assertEquals("java.lang.String", type.qualifiedName());
        assertTrue(type.typeArguments().isEmpty());
    }

    @Test
    void of_nullName_throwsException() {
        assertThrows(NullPointerException.class, () -> ClassType.of(null));
    }

    @Test
    void of_emptyName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> ClassType.of(""));
    }

    @Test
    void of_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> ClassType.of("   "));
    }

    @Test
    void qualifiedName_returnsFullyQualifiedName() {
        ClassType type = ClassType.of("com.example.MyClass");

        assertEquals("com.example.MyClass", type.qualifiedName());
    }

    @Test
    void simpleName_returnsOnlyClassName() {
        ClassType type = ClassType.of("com.example.MyClass");

        assertEquals("MyClass", type.simpleName());
    }

    @Test
    void simpleName_noPackage_returnsClassName() {
        ClassType type = ClassType.of("MyClass");

        assertEquals("MyClass", type.simpleName());
    }

    @Test
    void packageName_returnsPackage() {
        ClassType type = ClassType.of("com.example.MyClass");

        assertEquals("com.example", type.packageName());
    }

    @Test
    void packageName_noPackage_returnsEmpty() {
        ClassType type = ClassType.of("MyClass");

        assertEquals("", type.packageName());
    }

    @Test
    void typeArguments_defaultEmpty() {
        ClassType type = ClassType.of("java.util.List");

        assertNotNull(type.typeArguments());
        assertTrue(type.typeArguments().isEmpty());
    }

    @Test
    void withTypeArguments_varargs_addsTypeArguments() {
        ClassType listType = ClassType.of("java.util.List");
        ClassType stringType = ClassType.of("java.lang.String");

        ClassType parameterizedList = listType.withTypeArguments(stringType);

        assertEquals(1, parameterizedList.typeArguments().size());
        assertEquals(stringType, parameterizedList.typeArguments().get(0));
    }

    @Test
    void withTypeArguments_list_addsTypeArguments() {
        ClassType mapType = ClassType.of("java.util.Map");
        ClassType keyType = ClassType.of("java.lang.String");
        ClassType valueType = ClassType.of("java.lang.Integer");

        ClassType parameterizedMap = mapType.withTypeArguments(List.of(keyType, valueType));

        assertEquals(2, parameterizedMap.typeArguments().size());
        assertEquals(keyType, parameterizedMap.typeArguments().get(0));
        assertEquals(valueType, parameterizedMap.typeArguments().get(1));
    }

    @Test
    void withTypeArguments_immutableList() {
        ClassType listType = ClassType.of("java.util.List");
        ClassType stringType = ClassType.of("java.lang.String");

        ClassType parameterizedList = listType.withTypeArguments(stringType);

        assertThrows(UnsupportedOperationException.class,
            () -> parameterizedList.typeArguments().add(ClassType.of("java.lang.Object")));
    }

    @Test
    void withTypeArguments_preservesQualifiedName() {
        ClassType listType = ClassType.of("java.util.List");
        ClassType parameterized = listType.withTypeArguments(ClassType.of("java.lang.String"));

        assertEquals("java.util.List", parameterized.qualifiedName());
    }

    @Test
    void equals_sameNameNoArgs_areEqual() {
        ClassType a = ClassType.of("java.lang.String");
        ClassType b = ClassType.of("java.lang.String");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentName_areNotEqual() {
        ClassType a = ClassType.of("java.lang.String");
        ClassType b = ClassType.of("java.lang.Integer");

        assertNotEquals(a, b);
    }

    @Test
    void equals_sameNameDifferentArgs_areNotEqual() {
        ClassType listOfString = ClassType.of("java.util.List")
            .withTypeArguments(ClassType.of("java.lang.String"));
        ClassType listOfInteger = ClassType.of("java.util.List")
            .withTypeArguments(ClassType.of("java.lang.Integer"));

        assertNotEquals(listOfString, listOfInteger);
    }

    @Test
    void equals_sameNameSameArgs_areEqual() {
        ClassType list1 = ClassType.of("java.util.List")
            .withTypeArguments(ClassType.of("java.lang.String"));
        ClassType list2 = ClassType.of("java.util.List")
            .withTypeArguments(ClassType.of("java.lang.String"));

        assertEquals(list1, list2);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    void implementsTypeRef() {
        ClassType type = ClassType.of("java.lang.String");

        assertInstanceOf(TypeRef.class, type);
    }

    @Test
    void isParameterized_noArgs_returnsFalse() {
        ClassType type = ClassType.of("java.util.List");

        assertFalse(type.isParameterized());
    }

    @Test
    void isParameterized_withArgs_returnsTrue() {
        ClassType type = ClassType.of("java.util.List")
            .withTypeArguments(ClassType.of("java.lang.String"));

        assertTrue(type.isParameterized());
    }
}
