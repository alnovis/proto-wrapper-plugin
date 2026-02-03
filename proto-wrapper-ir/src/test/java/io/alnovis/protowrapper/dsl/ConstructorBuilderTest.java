package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.decl.AnnotationSpec;
import io.alnovis.protowrapper.ir.decl.ConstructorDecl;
import io.alnovis.protowrapper.ir.decl.Modifier;
import io.alnovis.protowrapper.ir.decl.ParameterDecl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Constructor}.
 */
class ConstructorBuilderTest {

    @Test
    void constructor_noArgs_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .public_()
            .body(Stmt.emptyBlock())
            .build();

        assertTrue(ctor.isPublic());
        assertTrue(ctor.isNoArg());
        assertFalse(ctor.hasParameters());
    }

    @Test
    void constructor_withParameters_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .public_()
            .parameter(Types.STRING, "name")
            .parameter(Types.INT, "age")
            .body(
                Stmt.assign(Expr.field("name"), Expr.var("name")),
                Stmt.assign(Expr.field("age"), Expr.var("age"))
            )
            .build();

        assertTrue(ctor.hasParameters());
        assertEquals(2, ctor.parameterCount());
    }

    @Test
    void constructor_private_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .private_()
            .body(Stmt.emptyBlock())
            .build();

        assertTrue(ctor.isPrivate());
    }

    @Test
    void constructor_protected_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .protected_()
            .body(Stmt.emptyBlock())
            .build();

        assertTrue(ctor.modifiers().contains(Modifier.PROTECTED));
    }

    @Test
    void constructor_modifier_addsModifier() {
        ConstructorDecl ctor = Constructor.constructor()
            .modifier(Modifier.PRIVATE)
            .body(Stmt.emptyBlock())
            .build();

        assertTrue(ctor.isPrivate());
    }

    @Test
    void constructor_withVarargs_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .public_()
            .parameter(Types.STRING, "name")
            .varargs(Types.STRING, "tags")
            .body(Stmt.emptyBlock())
            .build();

        assertEquals(2, ctor.parameterCount());
    }

    @Test
    void constructor_withParameterAnnotations_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .public_()
            .parameter(Types.STRING, "name",
                List.of(AnnotationSpec.of(Types.type("javax.annotation.Nonnull"))))
            .body(Stmt.emptyBlock())
            .build();

        assertTrue(ctor.parameters().get(0).hasAnnotations());
    }

    @Test
    void constructor_withParameterDecl_buildsCorrectly() {
        ParameterDecl param = ParameterDecl.of("value", Types.INT);
        ConstructorDecl ctor = Constructor.constructor()
            .parameter(param)
            .body(Stmt.emptyBlock())
            .build();

        assertEquals(1, ctor.parameterCount());
    }

    @Test
    void constructor_withParametersVarargs_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .parameters(
                ParameterDecl.of("a", Types.INT),
                ParameterDecl.of("b", Types.INT)
            )
            .body(Stmt.emptyBlock())
            .build();

        assertEquals(2, ctor.parameterCount());
    }

    @Test
    void constructor_withSingleBody_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .public_()
            .body(Stmt.expr(Expr.call("init")))
            .build();

        assertEquals(1, ctor.body().size());
    }

    @Test
    void constructor_withMultipleBodyStatements_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .public_()
            .body(
                Stmt.expr(Expr.call("init")),
                Stmt.expr(Expr.call("setup"))
            )
            .build();

        // body() with varargs wraps in block, so we get 1 block containing statements
        assertEquals(1, ctor.body().size());
    }

    @Test
    void constructor_withThrows_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .public_()
            .parameter(Types.STRING, "path")
            .throws_(Types.type("java.io.IOException"))
            .body(Stmt.emptyBlock())
            .build();

        assertTrue(ctor.hasThrows());
        assertEquals(1, ctor.throwsTypes().size());
    }

    @Test
    void constructor_withMultipleThrows_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .throws_(
                Types.type("java.io.IOException"),
                Types.type("java.lang.ReflectiveOperationException")
            )
            .body(Stmt.emptyBlock())
            .build();

        assertEquals(2, ctor.throwsTypes().size());
    }

    @Test
    void constructor_withAnnotation_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .annotation(AnnotationSpec.of(Types.type("javax.inject.Inject")))
            .body(Stmt.emptyBlock())
            .build();

        assertEquals(1, ctor.annotations().size());
    }

    @Test
    void constructor_withMarkerAnnotation_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .annotation(Types.type("javax.inject.Inject"))
            .body(Stmt.emptyBlock())
            .build();

        assertEquals(1, ctor.annotations().size());
    }

    @Test
    void constructor_deprecated_addsDeprecatedAnnotation() {
        ConstructorDecl ctor = Constructor.constructor()
            .deprecated()
            .body(Stmt.emptyBlock())
            .build();

        assertEquals(1, ctor.annotations().size());
    }

    @Test
    void constructor_withJavadoc_buildsCorrectly() {
        ConstructorDecl ctor = Constructor.constructor()
            .public_()
            .javadoc("Creates a new instance.")
            .body(Stmt.emptyBlock())
            .build();

        assertTrue(ctor.hasJavadoc());
        assertEquals("Creates a new instance.", ctor.javadoc());
    }

    @Test
    void constructor_noBody_buildsWithEmptyBody() {
        ConstructorDecl ctor = Constructor.constructor()
            .public_()
            .build();

        // When no body is set, build() creates empty list
        assertTrue(ctor.body().isEmpty());
    }
}
