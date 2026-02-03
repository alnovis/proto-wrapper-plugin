package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.stmt.ReturnStmt;
import io.alnovis.protowrapper.ir.stmt.Statement;
import io.alnovis.protowrapper.ir.type.ClassType;
import io.alnovis.protowrapper.ir.type.PrimitiveType;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ConstructorDecl}.
 */
class ConstructorDeclTest {

    @Test
    void constructor_validArguments_succeeds() {
        List<ParameterDecl> params = List.of(
            ParameterDecl.of("name", ClassType.of("java.lang.String"))
        );

        ConstructorDecl ctor = new ConstructorDecl(
            params,
            List.of(),
            Set.of(Modifier.PUBLIC),
            List.of(),
            "Creates a new instance.",
            List.of()
        );

        assertEquals(1, ctor.parameters().size());
        assertTrue(ctor.isPublic());
        assertTrue(ctor.hasJavadoc());
    }

    @Test
    void constructor_nullBody_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new ConstructorDecl(List.of(), null, Set.of(), List.of(), null, List.of()));
    }

    @Test
    void constructor_nullParameters_treatedAsEmpty() {
        ConstructorDecl ctor = new ConstructorDecl(null, List.of(), Set.of(), List.of(), null, List.of());

        assertNotNull(ctor.parameters());
        assertTrue(ctor.parameters().isEmpty());
        assertTrue(ctor.isNoArg());
    }

    @Test
    void constructor_nullModifiers_treatedAsEmpty() {
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(), null, List.of(), null, List.of());

        assertNotNull(ctor.modifiers());
        assertTrue(ctor.modifiers().isEmpty());
    }

    @Test
    void constructor_nullAnnotations_treatedAsEmpty() {
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(), Set.of(), null, null, List.of());

        assertNotNull(ctor.annotations());
        assertTrue(ctor.annotations().isEmpty());
    }

    @Test
    void constructor_nullThrowsTypes_treatedAsEmpty() {
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(), Set.of(), List.of(), null, null);

        assertNotNull(ctor.throwsTypes());
        assertTrue(ctor.throwsTypes().isEmpty());
    }

    @Test
    void parameters_isImmutable() {
        List<ParameterDecl> params = new ArrayList<>();
        params.add(ParameterDecl.of("x", PrimitiveType.of(PrimitiveKind.INT)));
        ConstructorDecl ctor = new ConstructorDecl(params, List.of(), Set.of(), List.of(), null, List.of());

        assertThrows(UnsupportedOperationException.class,
            () -> ctor.parameters().add(ParameterDecl.of("y", PrimitiveType.of(PrimitiveKind.INT))));
    }

    @Test
    void body_isImmutable() {
        List<Statement> body = new ArrayList<>();
        ConstructorDecl ctor = new ConstructorDecl(List.of(), body, Set.of(), List.of(), null, List.of());

        assertThrows(UnsupportedOperationException.class,
            () -> ctor.body().add(new ReturnStmt(null)));
    }

    @Test
    void modifiers_isImmutable() {
        Set<Modifier> modifiers = new HashSet<>();
        modifiers.add(Modifier.PUBLIC);
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(), modifiers, List.of(), null, List.of());

        assertThrows(UnsupportedOperationException.class,
            () -> ctor.modifiers().add(Modifier.PRIVATE));
    }

    @Test
    void hasJavadoc_withJavadoc_returnsTrue() {
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(), Set.of(), List.of(),
            "Documentation.", List.of());

        assertTrue(ctor.hasJavadoc());
    }

    @Test
    void hasJavadoc_withoutJavadoc_returnsFalse() {
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(), Set.of(), List.of(), null, List.of());

        assertFalse(ctor.hasJavadoc());
    }

    @Test
    void hasParameters_withParams_returnsTrue() {
        ConstructorDecl ctor = new ConstructorDecl(
            List.of(ParameterDecl.of("x", PrimitiveType.of(PrimitiveKind.INT))),
            List.of(), Set.of(), List.of(), null, List.of());

        assertTrue(ctor.hasParameters());
        assertEquals(1, ctor.parameterCount());
    }

    @Test
    void hasParameters_withoutParams_returnsFalse() {
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(), Set.of(), List.of(), null, List.of());

        assertFalse(ctor.hasParameters());
    }

    @Test
    void hasThrows_withThrows_returnsTrue() {
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(), Set.of(), List.of(), null,
            List.of(ClassType.of("java.io.IOException")));

        assertTrue(ctor.hasThrows());
    }

    @Test
    void hasThrows_withoutThrows_returnsFalse() {
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(), Set.of(), List.of(), null, List.of());

        assertFalse(ctor.hasThrows());
    }

    @Test
    void isPublic_withPublic_returnsTrue() {
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(),
            Set.of(Modifier.PUBLIC), List.of(), null, List.of());

        assertTrue(ctor.isPublic());
    }

    @Test
    void isPrivate_withPrivate_returnsTrue() {
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(),
            Set.of(Modifier.PRIVATE), List.of(), null, List.of());

        assertTrue(ctor.isPrivate());
    }

    @Test
    void isNoArg_withoutParams_returnsTrue() {
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(), Set.of(), List.of(), null, List.of());

        assertTrue(ctor.isNoArg());
    }

    @Test
    void isNoArg_withParams_returnsFalse() {
        ConstructorDecl ctor = new ConstructorDecl(
            List.of(ParameterDecl.of("x", PrimitiveType.of(PrimitiveKind.INT))),
            List.of(), Set.of(), List.of(), null, List.of());

        assertFalse(ctor.isNoArg());
    }

    @Test
    void parameterCount_returnsCorrectCount() {
        ConstructorDecl ctor = new ConstructorDecl(
            List.of(
                ParameterDecl.of("a", PrimitiveType.of(PrimitiveKind.INT)),
                ParameterDecl.of("b", PrimitiveType.of(PrimitiveKind.INT))
            ),
            List.of(), Set.of(), List.of(), null, List.of());

        assertEquals(2, ctor.parameterCount());
    }

    @Test
    void equals_sameComponents_areEqual() {
        ConstructorDecl a = new ConstructorDecl(List.of(), List.of(), Set.of(), List.of(), null, List.of());
        ConstructorDecl b = new ConstructorDecl(List.of(), List.of(), Set.of(), List.of(), null, List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentParameters_areNotEqual() {
        ConstructorDecl a = new ConstructorDecl(List.of(), List.of(), Set.of(), List.of(), null, List.of());
        ConstructorDecl b = new ConstructorDecl(
            List.of(ParameterDecl.of("x", PrimitiveType.of(PrimitiveKind.INT))),
            List.of(), Set.of(), List.of(), null, List.of());

        assertNotEquals(a, b);
    }

    @Test
    void implementsMemberDeclaration() {
        ConstructorDecl ctor = new ConstructorDecl(List.of(), List.of(), Set.of(), List.of(), null, List.of());

        assertInstanceOf(MemberDeclaration.class, ctor);
    }
}
