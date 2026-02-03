package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.expr.LiteralExpr;
import io.alnovis.protowrapper.ir.type.ClassType;
import io.alnovis.protowrapper.ir.type.PrimitiveType;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import io.alnovis.protowrapper.ir.type.TypeRef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FieldDecl}.
 */
class FieldDeclTest {

    @Test
    void constructor_validArguments_succeeds() {
        TypeRef type = ClassType.of("java.lang.String");
        FieldDecl field = new FieldDecl("name", type, null,
            Set.of(Modifier.PRIVATE), List.of(), null);

        assertEquals("name", field.name());
        assertEquals(type, field.type());
        assertNull(field.initializer());
        assertTrue(field.modifiers().contains(Modifier.PRIVATE));
    }

    @Test
    void constructor_nullName_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new FieldDecl(null, PrimitiveType.of(PrimitiveKind.INT), null,
                Set.of(), List.of(), null));
    }

    @Test
    void constructor_emptyName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new FieldDecl("", PrimitiveType.of(PrimitiveKind.INT), null,
                Set.of(), List.of(), null));
    }

    @Test
    void constructor_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new FieldDecl("   ", PrimitiveType.of(PrimitiveKind.INT), null,
                Set.of(), List.of(), null));
    }

    @Test
    void constructor_nullType_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new FieldDecl("field", null, null, Set.of(), List.of(), null));
    }

    @Test
    void constructor_nullModifiers_treatedAsEmpty() {
        FieldDecl field = new FieldDecl("field", PrimitiveType.of(PrimitiveKind.INT),
            null, null, List.of(), null);

        assertNotNull(field.modifiers());
        assertTrue(field.modifiers().isEmpty());
    }

    @Test
    void constructor_nullAnnotations_treatedAsEmpty() {
        FieldDecl field = new FieldDecl("field", PrimitiveType.of(PrimitiveKind.INT),
            null, Set.of(), null, null);

        assertNotNull(field.annotations());
        assertTrue(field.annotations().isEmpty());
    }

    @Test
    void modifiers_isImmutable() {
        Set<Modifier> modifiers = new HashSet<>();
        modifiers.add(Modifier.PRIVATE);
        FieldDecl field = new FieldDecl("field", PrimitiveType.of(PrimitiveKind.INT),
            null, modifiers, List.of(), null);

        assertThrows(UnsupportedOperationException.class,
            () -> field.modifiers().add(Modifier.FINAL));
    }

    @Test
    void annotations_isImmutable() {
        List<AnnotationSpec> annotations = new ArrayList<>();
        annotations.add(AnnotationSpec.of(ClassType.of("javax.annotation.Nonnull")));
        FieldDecl field = new FieldDecl("field", PrimitiveType.of(PrimitiveKind.INT),
            null, Set.of(), annotations, null);

        assertThrows(UnsupportedOperationException.class,
            () -> field.annotations().add(AnnotationSpec.of(ClassType.of("Deprecated"))));
    }

    @Test
    void hasInitializer_withInitializer_returnsTrue() {
        FieldDecl field = new FieldDecl("count", PrimitiveType.of(PrimitiveKind.INT),
            new LiteralExpr(0, null), Set.of(), List.of(), null);

        assertTrue(field.hasInitializer());
    }

    @Test
    void hasInitializer_withoutInitializer_returnsFalse() {
        FieldDecl field = new FieldDecl("count", PrimitiveType.of(PrimitiveKind.INT),
            null, Set.of(), List.of(), null);

        assertFalse(field.hasInitializer());
    }

    @Test
    void hasJavadoc_withJavadoc_returnsTrue() {
        FieldDecl field = new FieldDecl("count", PrimitiveType.of(PrimitiveKind.INT),
            null, Set.of(), List.of(), "The count.");

        assertTrue(field.hasJavadoc());
    }

    @Test
    void hasJavadoc_withoutJavadoc_returnsFalse() {
        FieldDecl field = new FieldDecl("count", PrimitiveType.of(PrimitiveKind.INT),
            null, Set.of(), List.of(), null);

        assertFalse(field.hasJavadoc());
    }

    @Test
    void isStatic_withStatic_returnsTrue() {
        FieldDecl field = new FieldDecl("INSTANCE", ClassType.of("MyClass"),
            null, Set.of(Modifier.STATIC), List.of(), null);

        assertTrue(field.isStatic());
    }

    @Test
    void isStatic_withoutStatic_returnsFalse() {
        FieldDecl field = new FieldDecl("name", ClassType.of("java.lang.String"),
            null, Set.of(Modifier.PRIVATE), List.of(), null);

        assertFalse(field.isStatic());
    }

    @Test
    void isFinal_withFinal_returnsTrue() {
        FieldDecl field = new FieldDecl("name", ClassType.of("java.lang.String"),
            null, Set.of(Modifier.FINAL), List.of(), null);

        assertTrue(field.isFinal());
    }

    @Test
    void isPrivate_withPrivate_returnsTrue() {
        FieldDecl field = new FieldDecl("name", ClassType.of("java.lang.String"),
            null, Set.of(Modifier.PRIVATE), List.of(), null);

        assertTrue(field.isPrivate());
    }

    @Test
    void isPublic_withPublic_returnsTrue() {
        FieldDecl field = new FieldDecl("name", ClassType.of("java.lang.String"),
            null, Set.of(Modifier.PUBLIC), List.of(), null);

        assertTrue(field.isPublic());
    }

    @Test
    void isConstant_publicStaticFinal_returnsTrue() {
        FieldDecl field = new FieldDecl("MAX_SIZE", PrimitiveType.of(PrimitiveKind.INT),
            new LiteralExpr(100, null),
            Set.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL),
            List.of(), null);

        assertTrue(field.isConstant());
    }

    @Test
    void isConstant_missingModifier_returnsFalse() {
        FieldDecl field = new FieldDecl("MAX_SIZE", PrimitiveType.of(PrimitiveKind.INT),
            null, Set.of(Modifier.PUBLIC, Modifier.FINAL), List.of(), null);

        assertFalse(field.isConstant());
    }

    @Test
    void equals_sameComponents_areEqual() {
        TypeRef type = PrimitiveType.of(PrimitiveKind.INT);
        FieldDecl a = new FieldDecl("x", type, null, Set.of(), List.of(), null);
        FieldDecl b = new FieldDecl("x", type, null, Set.of(), List.of(), null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentName_areNotEqual() {
        TypeRef type = PrimitiveType.of(PrimitiveKind.INT);
        FieldDecl a = new FieldDecl("x", type, null, Set.of(), List.of(), null);
        FieldDecl b = new FieldDecl("y", type, null, Set.of(), List.of(), null);

        assertNotEquals(a, b);
    }

    @Test
    void implementsMemberDeclaration() {
        FieldDecl field = new FieldDecl("x", PrimitiveType.of(PrimitiveKind.INT),
            null, Set.of(), List.of(), null);

        assertInstanceOf(MemberDeclaration.class, field);
    }
}
