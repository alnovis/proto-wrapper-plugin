package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.type.ClassType;
import io.alnovis.protowrapper.ir.type.PrimitiveType;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import io.alnovis.protowrapper.ir.type.TypeRef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ParameterDecl}.
 */
class ParameterDeclTest {

    @Test
    void constructor_validArguments_succeeds() {
        TypeRef type = ClassType.of("java.lang.String");
        ParameterDecl param = new ParameterDecl("name", type, List.of(), false);

        assertEquals("name", param.name());
        assertEquals(type, param.type());
        assertTrue(param.annotations().isEmpty());
        assertFalse(param.isFinal());
    }

    @Test
    void constructor_nullName_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new ParameterDecl(null, PrimitiveType.of(PrimitiveKind.INT), List.of(), false));
    }

    @Test
    void constructor_emptyName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new ParameterDecl("", PrimitiveType.of(PrimitiveKind.INT), List.of(), false));
    }

    @Test
    void constructor_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new ParameterDecl("   ", PrimitiveType.of(PrimitiveKind.INT), List.of(), false));
    }

    @Test
    void constructor_nullType_succeeds() {
        // null type is allowed for lambda parameters with inferred types
        ParameterDecl param = new ParameterDecl("x", null, List.of(), false);

        assertNull(param.type());
        assertTrue(param.hasInferredType());
    }

    @Test
    void constructor_nullAnnotations_treatedAsEmpty() {
        ParameterDecl param = new ParameterDecl("x", PrimitiveType.of(PrimitiveKind.INT), null, false);

        assertNotNull(param.annotations());
        assertTrue(param.annotations().isEmpty());
    }

    @Test
    void annotations_isImmutable() {
        List<AnnotationSpec> annotations = new ArrayList<>();
        annotations.add(AnnotationSpec.of(ClassType.of("java.lang.Override")));
        ParameterDecl param = new ParameterDecl("x", PrimitiveType.of(PrimitiveKind.INT), annotations, false);

        assertThrows(UnsupportedOperationException.class,
            () -> param.annotations().add(AnnotationSpec.of(ClassType.of("java.lang.Deprecated"))));
    }

    @Test
    void of_createsSimpleParameter() {
        ParameterDecl param = ParameterDecl.of("value", PrimitiveType.of(PrimitiveKind.INT));

        assertEquals("value", param.name());
        assertEquals(PrimitiveType.of(PrimitiveKind.INT), param.type());
        assertFalse(param.isFinal());
        assertTrue(param.annotations().isEmpty());
    }

    @Test
    void finalParam_createsFinalParameter() {
        ParameterDecl param = ParameterDecl.finalParam("value", PrimitiveType.of(PrimitiveKind.INT));

        assertTrue(param.isFinal());
    }

    @Test
    void asFinal_returnsFinalCopy() {
        ParameterDecl param = ParameterDecl.of("x", PrimitiveType.of(PrimitiveKind.INT));
        ParameterDecl finalParam = param.asFinal();

        assertFalse(param.isFinal());
        assertTrue(finalParam.isFinal());
        assertEquals(param.name(), finalParam.name());
        assertEquals(param.type(), finalParam.type());
    }

    @Test
    void withAnnotation_addsAnnotation() {
        ParameterDecl param = ParameterDecl.of("x", PrimitiveType.of(PrimitiveKind.INT));
        AnnotationSpec annotation = AnnotationSpec.of(ClassType.of("javax.annotation.Nonnull"));
        ParameterDecl annotated = param.withAnnotation(annotation);

        assertTrue(param.annotations().isEmpty());
        assertEquals(1, annotated.annotations().size());
        assertEquals(annotation, annotated.annotations().get(0));
    }

    @Test
    void hasAnnotations_withAnnotations_returnsTrue() {
        AnnotationSpec annotation = AnnotationSpec.of(ClassType.of("javax.annotation.Nonnull"));
        ParameterDecl param = new ParameterDecl("x", PrimitiveType.of(PrimitiveKind.INT),
            List.of(annotation), false);

        assertTrue(param.hasAnnotations());
    }

    @Test
    void hasAnnotations_withoutAnnotations_returnsFalse() {
        ParameterDecl param = ParameterDecl.of("x", PrimitiveType.of(PrimitiveKind.INT));

        assertFalse(param.hasAnnotations());
    }

    @Test
    void hasInferredType_nullType_returnsTrue() {
        ParameterDecl param = new ParameterDecl("x", null, List.of(), false);

        assertTrue(param.hasInferredType());
        assertFalse(param.hasExplicitType());
    }

    @Test
    void hasExplicitType_withType_returnsTrue() {
        ParameterDecl param = ParameterDecl.of("x", PrimitiveType.of(PrimitiveKind.INT));

        assertTrue(param.hasExplicitType());
        assertFalse(param.hasInferredType());
    }

    @Test
    void equals_sameComponents_areEqual() {
        TypeRef type = PrimitiveType.of(PrimitiveKind.INT);
        ParameterDecl a = new ParameterDecl("x", type, List.of(), false);
        ParameterDecl b = new ParameterDecl("x", type, List.of(), false);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentName_areNotEqual() {
        TypeRef type = PrimitiveType.of(PrimitiveKind.INT);
        ParameterDecl a = new ParameterDecl("x", type, List.of(), false);
        ParameterDecl b = new ParameterDecl("y", type, List.of(), false);

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentFinal_areNotEqual() {
        TypeRef type = PrimitiveType.of(PrimitiveKind.INT);
        ParameterDecl a = new ParameterDecl("x", type, List.of(), false);
        ParameterDecl b = new ParameterDecl("x", type, List.of(), true);

        assertNotEquals(a, b);
    }
}
