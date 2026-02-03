package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.stmt.ReturnStmt;
import io.alnovis.protowrapper.ir.type.ClassType;
import io.alnovis.protowrapper.ir.type.PrimitiveType;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import io.alnovis.protowrapper.ir.type.TypeVariable;
import io.alnovis.protowrapper.ir.type.VoidType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MethodDecl}.
 */
class MethodDeclTest {

    @Test
    void constructor_validArguments_succeeds() {
        MethodDecl method = new MethodDecl(
            "getName",
            ClassType.of("java.lang.String"),
            List.of(),
            List.of(),
            List.of(new ReturnStmt(null)),
            Set.of(Modifier.PUBLIC),
            List.of(),
            "Returns the name.",
            false, false,
            List.of()
        );

        assertEquals("getName", method.name());
        assertEquals(ClassType.of("java.lang.String"), method.returnType());
        assertTrue(method.hasJavadoc());
        assertTrue(method.isPublic());
    }

    @Test
    void constructor_nullName_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new MethodDecl(null, VoidType.INSTANCE, List.of(), List.of(),
                List.of(), Set.of(), List.of(), null, false, false, List.of()));
    }

    @Test
    void constructor_emptyName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new MethodDecl("", VoidType.INSTANCE, List.of(), List.of(),
                List.of(), Set.of(), List.of(), null, false, false, List.of()));
    }

    @Test
    void constructor_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new MethodDecl("   ", VoidType.INSTANCE, List.of(), List.of(),
                List.of(), Set.of(), List.of(), null, false, false, List.of()));
    }

    @Test
    void constructor_nullReturnType_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new MethodDecl("method", null, List.of(), List.of(),
                List.of(), Set.of(), List.of(), null, false, false, List.of()));
    }

    @Test
    void constructor_nullParameters_treatedAsEmpty() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, null, List.of(),
            List.of(), Set.of(), List.of(), null, false, false, List.of());

        assertNotNull(method.parameters());
        assertTrue(method.parameters().isEmpty());
    }

    @Test
    void constructor_nullBody_treatedAsEmpty() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            null, Set.of(), List.of(), null, false, false, List.of());

        assertNotNull(method.body());
        assertTrue(method.body().isEmpty());
    }

    @Test
    void constructor_nullModifiers_treatedAsEmpty() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), null, List.of(), null, false, false, List.of());

        assertNotNull(method.modifiers());
        assertTrue(method.modifiers().isEmpty());
    }

    @Test
    void parameters_isImmutable() {
        List<ParameterDecl> params = new ArrayList<>();
        params.add(ParameterDecl.of("x", PrimitiveType.of(PrimitiveKind.INT)));
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, params, List.of(),
            List.of(), Set.of(), List.of(), null, false, false, List.of());

        assertThrows(UnsupportedOperationException.class,
            () -> method.parameters().add(ParameterDecl.of("y", PrimitiveType.of(PrimitiveKind.INT))));
    }

    @Test
    void body_isImmutable() {
        List<io.alnovis.protowrapper.ir.stmt.Statement> body = new ArrayList<>();
        body.add(new ReturnStmt(null));
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            body, Set.of(), List.of(), null, false, false, List.of());

        assertThrows(UnsupportedOperationException.class,
            () -> method.body().add(new ReturnStmt(null)));
    }

    @Test
    void hasJavadoc_withJavadoc_returnsTrue() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(), List.of(), "Docs.", false, false, List.of());

        assertTrue(method.hasJavadoc());
    }

    @Test
    void hasJavadoc_withoutJavadoc_returnsFalse() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(), List.of(), null, false, false, List.of());

        assertFalse(method.hasJavadoc());
    }

    @Test
    void returnsVoid_voidType_returnsTrue() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(), List.of(), null, false, false, List.of());

        assertTrue(method.returnsVoid());
    }

    @Test
    void returnsVoid_nonVoidType_returnsFalse() {
        MethodDecl method = new MethodDecl("method", PrimitiveType.of(PrimitiveKind.INT),
            List.of(), List.of(), List.of(), Set.of(), List.of(), null, false, false, List.of());

        assertFalse(method.returnsVoid());
    }

    @Test
    void hasParameters_withParams_returnsTrue() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE,
            List.of(ParameterDecl.of("x", PrimitiveType.of(PrimitiveKind.INT))),
            List.of(), List.of(), Set.of(), List.of(), null, false, false, List.of());

        assertTrue(method.hasParameters());
        assertEquals(1, method.parameterCount());
    }

    @Test
    void hasParameters_withoutParams_returnsFalse() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(), List.of(), null, false, false, List.of());

        assertFalse(method.hasParameters());
        assertEquals(0, method.parameterCount());
    }

    @Test
    void hasTypeParameters_withTypeParams_returnsTrue() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(),
            List.of(new TypeVariable("T", List.of())),
            List.of(), Set.of(), List.of(), null, false, false, List.of());

        assertTrue(method.hasTypeParameters());
    }

    @Test
    void hasThrows_withThrows_returnsTrue() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(), List.of(), null, false, false,
            List.of(ClassType.of("java.io.IOException")));

        assertTrue(method.hasThrows());
    }

    @Test
    void isStatic_withStatic_returnsTrue() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(Modifier.STATIC), List.of(), null, false, false, List.of());

        assertTrue(method.isStatic());
    }

    @Test
    void isFinal_withFinal_returnsTrue() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(Modifier.FINAL), List.of(), null, false, false, List.of());

        assertTrue(method.isFinal());
    }

    @Test
    void isPublic_withPublic_returnsTrue() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(Modifier.PUBLIC), List.of(), null, false, false, List.of());

        assertTrue(method.isPublic());
    }

    @Test
    void isAbstract_abstractFlag_returnsTrue() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(), List.of(), null, true, false, List.of());

        assertTrue(method.isAbstract());
    }

    @Test
    void isDefault_defaultFlag_returnsTrue() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(), List.of(), null, false, true, List.of());

        assertTrue(method.isDefault());
    }

    @Test
    void equals_sameComponents_areEqual() {
        MethodDecl a = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(), List.of(), null, false, false, List.of());
        MethodDecl b = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(), List.of(), null, false, false, List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentName_areNotEqual() {
        MethodDecl a = new MethodDecl("method1", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(), List.of(), null, false, false, List.of());
        MethodDecl b = new MethodDecl("method2", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(), List.of(), null, false, false, List.of());

        assertNotEquals(a, b);
    }

    @Test
    void implementsMemberDeclaration() {
        MethodDecl method = new MethodDecl("method", VoidType.INSTANCE, List.of(), List.of(),
            List.of(), Set.of(), List.of(), null, false, false, List.of());

        assertInstanceOf(MemberDeclaration.class, method);
    }
}
