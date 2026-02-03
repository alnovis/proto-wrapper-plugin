package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.decl.AnnotationSpec;
import io.alnovis.protowrapper.ir.decl.MethodDecl;
import io.alnovis.protowrapper.ir.decl.Modifier;
import io.alnovis.protowrapper.ir.decl.ParameterDecl;
import io.alnovis.protowrapper.ir.type.VoidType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Method}.
 */
class MethodBuilderTest {

    @Test
    void method_simpleGetter_buildsCorrectly() {
        MethodDecl method = Method.method("getName")
            .public_()
            .returns(Types.STRING)
            .body(Stmt.return_(Expr.field("name")))
            .build();

        assertEquals("getName", method.name());
        assertEquals(Types.STRING, method.returnType());
        assertTrue(method.isPublic());
        assertFalse(method.returnsVoid());
        assertEquals(1, method.body().size());
    }

    @Test
    void method_voidMethod_buildsCorrectly() {
        MethodDecl method = Method.method("process")
            .public_()
            .build();

        assertInstanceOf(VoidType.class, method.returnType());
        assertTrue(method.returnsVoid());
    }

    @Test
    void method_withParameters_buildsCorrectly() {
        MethodDecl method = Method.method("setName")
            .public_()
            .parameter(Types.STRING, "name")
            .body(Stmt.assign(Expr.field("name"), Expr.var("name")))
            .build();

        assertTrue(method.hasParameters());
        assertEquals(1, method.parameterCount());
        assertEquals("name", method.parameters().get(0).name());
    }

    @Test
    void method_withMultipleParameters_buildsCorrectly() {
        MethodDecl method = Method.method("setDetails")
            .public_()
            .parameter(Types.STRING, "name")
            .parameter(Types.INT, "age")
            .build();

        assertEquals(2, method.parameterCount());
    }

    @Test
    void method_withVarargs_buildsCorrectly() {
        MethodDecl method = Method.method("printf")
            .public_()
            .parameter(Types.STRING, "format")
            .varargs(Types.OBJECT, "args")
            .build();

        assertEquals(2, method.parameterCount());
        assertTrue(method.parameters().get(1).isFinal()); // varargs flag reused as final
    }

    @Test
    void method_withParameterDecl_buildsCorrectly() {
        ParameterDecl param = ParameterDecl.of("value", Types.INT);
        MethodDecl method = Method.method("setValue")
            .parameter(param)
            .build();

        assertEquals(1, method.parameterCount());
    }

    @Test
    void method_withParametersVarargs_buildsCorrectly() {
        MethodDecl method = Method.method("test")
            .parameters(
                ParameterDecl.of("a", Types.INT),
                ParameterDecl.of("b", Types.INT)
            )
            .build();

        assertEquals(2, method.parameterCount());
    }

    @Test
    void abstractMethod_buildsCorrectly() {
        MethodDecl method = Method.abstractMethod("process")
            .public_()
            .returns(Types.VOID)
            .build();

        assertTrue(method.isAbstract());
        assertTrue(method.modifiers().contains(Modifier.ABSTRACT));
        assertTrue(method.body().isEmpty());
    }

    @Test
    void defaultMethod_buildsCorrectly() {
        MethodDecl method = Method.defaultMethod("getDisplayName")
            .returns(Types.STRING)
            .body(Stmt.return_(Expr.call("getName")))
            .build();

        assertTrue(method.isDefault());
        assertTrue(method.modifiers().contains(Modifier.DEFAULT));
    }

    @Test
    void method_allModifiers_buildsCorrectly() {
        MethodDecl publicMethod = Method.method("test").public_().build();
        MethodDecl protectedMethod = Method.method("test").protected_().build();
        MethodDecl privateMethod = Method.method("test").private_().build();
        MethodDecl staticMethod = Method.method("test").static_().build();
        MethodDecl finalMethod = Method.method("test").final_().build();
        MethodDecl syncMethod = Method.method("test").synchronized_().build();
        MethodDecl nativeMethod = Method.method("test").native_().build();

        assertTrue(publicMethod.isPublic());
        assertTrue(protectedMethod.modifiers().contains(Modifier.PROTECTED));
        assertTrue(privateMethod.modifiers().contains(Modifier.PRIVATE));
        assertTrue(staticMethod.isStatic());
        assertTrue(finalMethod.isFinal());
        assertTrue(syncMethod.modifiers().contains(Modifier.SYNCHRONIZED));
        assertTrue(nativeMethod.modifiers().contains(Modifier.NATIVE));
    }

    @Test
    void method_modifier_addsModifier() {
        MethodDecl method = Method.method("test")
            .modifier(Modifier.STRICTFP)
            .build();

        assertTrue(method.modifiers().contains(Modifier.STRICTFP));
    }

    @Test
    void method_modifiers_addsMultiple() {
        MethodDecl method = Method.method("test")
            .modifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .build();

        assertTrue(method.isPublic());
        assertTrue(method.isStatic());
        assertTrue(method.isFinal());
    }

    @Test
    void method_withTypeParameters_buildsCorrectly() {
        MethodDecl method = Method.method("transform")
            .public_()
            .typeParameter("T")
            .typeParameter("R")
            .returns(Types.typeVar("R"))
            .parameter(Types.typeVar("T"), "input")
            .build();

        assertTrue(method.hasTypeParameters());
        assertEquals(2, method.typeParameters().size());
    }

    @Test
    void method_withTypeParameterObject_buildsCorrectly() {
        MethodDecl method = Method.method("filter")
            .typeParameter(Types.typeVar("T", Types.type("java.lang.Comparable")))
            .build();

        assertEquals(1, method.typeParameters().size());
    }

    @Test
    void method_withTypeParametersVarargs_buildsCorrectly() {
        MethodDecl method = Method.method("test")
            .typeParameters(Types.typeVar("T"), Types.typeVar("R"))
            .build();

        assertEquals(2, method.typeParameters().size());
    }

    @Test
    void method_withThrows_buildsCorrectly() {
        MethodDecl method = Method.method("readFile")
            .public_()
            .returns(Types.STRING)
            .throws_(Types.type("java.io.IOException"))
            .build();

        assertTrue(method.hasThrows());
        assertEquals(1, method.throwsTypes().size());
    }

    @Test
    void method_withMultipleThrows_buildsCorrectly() {
        MethodDecl method = Method.method("process")
            .throws_(
                Types.type("java.io.IOException"),
                Types.type("java.lang.InterruptedException")
            )
            .build();

        assertEquals(2, method.throwsTypes().size());
    }

    @Test
    void method_withAnnotation_buildsCorrectly() {
        MethodDecl method = Method.method("test")
            .annotation(AnnotationSpec.of(Types.type("org.junit.Test")))
            .build();

        assertEquals(1, method.annotations().size());
    }

    @Test
    void method_withMarkerAnnotation_buildsCorrectly() {
        MethodDecl method = Method.method("test")
            .annotation(Types.type("java.lang.Override"))
            .build();

        assertEquals(1, method.annotations().size());
    }

    @Test
    void method_override_addsOverrideAnnotation() {
        MethodDecl method = Method.method("toString")
            .public_()
            .override()
            .returns(Types.STRING)
            .build();

        assertEquals(1, method.annotations().size());
    }

    @Test
    void method_deprecated_addsDeprecatedAnnotation() {
        MethodDecl method = Method.method("oldMethod")
            .deprecated()
            .build();

        assertEquals(1, method.annotations().size());
    }

    @Test
    void method_withJavadoc_buildsCorrectly() {
        MethodDecl method = Method.method("getName")
            .javadoc("Returns the name.")
            .build();

        assertTrue(method.hasJavadoc());
        assertEquals("Returns the name.", method.javadoc());
    }

    @Test
    void method_bodyVarargs_wrapsInBlock() {
        MethodDecl method = Method.method("process")
            .body(Stmt.return_(), Stmt.return_())
            .build();

        // body creates block with statements
        assertEquals(1, method.body().size());
    }

    @Test
    void method_parameterWithAnnotations_buildsCorrectly() {
        MethodDecl method = Method.method("process")
            .parameter(Types.STRING, "input",
                List.of(AnnotationSpec.of(Types.type("javax.annotation.Nonnull"))))
            .build();

        assertTrue(method.parameters().get(0).hasAnnotations());
    }
}
