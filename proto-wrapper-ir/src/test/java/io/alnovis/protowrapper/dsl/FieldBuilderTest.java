package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.decl.AnnotationSpec;
import io.alnovis.protowrapper.ir.decl.FieldDecl;
import io.alnovis.protowrapper.ir.decl.Modifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Field}.
 */
class FieldBuilderTest {

    @Test
    void field_simple_buildsCorrectly() {
        FieldDecl field = Field.field(Types.STRING, "name")
            .private_()
            .build();

        assertEquals("name", field.name());
        assertEquals(Types.STRING, field.type());
        assertTrue(field.isPrivate());
        assertFalse(field.hasInitializer());
    }

    @Test
    void field_withInitializer_buildsCorrectly() {
        FieldDecl field = Field.field(Types.INT, "count")
            .private_()
            .initializer(Expr.literal(0))
            .build();

        assertTrue(field.hasInitializer());
        assertNotNull(field.initializer());
    }

    @Test
    void field_final_buildsCorrectly() {
        FieldDecl field = Field.field(Types.STRING, "id")
            .private_()
            .final_()
            .build();

        assertTrue(field.isFinal());
    }

    @Test
    void field_static_buildsCorrectly() {
        FieldDecl field = Field.field(Types.STRING, "INSTANCE")
            .private_()
            .static_()
            .build();

        assertTrue(field.isStatic());
    }

    @Test
    void field_constant_buildsCorrectly() {
        FieldDecl field = Field.field(Types.INT, "MAX_SIZE")
            .public_()
            .static_()
            .final_()
            .initializer(Expr.literal(100))
            .build();

        assertTrue(field.isConstant());
    }

    @Test
    void field_allModifiers_buildCorrectly() {
        FieldDecl publicField = Field.field(Types.INT, "x").public_().build();
        FieldDecl protectedField = Field.field(Types.INT, "x").protected_().build();
        FieldDecl privateField = Field.field(Types.INT, "x").private_().build();
        FieldDecl staticField = Field.field(Types.INT, "x").static_().build();
        FieldDecl finalField = Field.field(Types.INT, "x").final_().build();
        FieldDecl volatileField = Field.field(Types.INT, "x").volatile_().build();
        FieldDecl transientField = Field.field(Types.INT, "x").transient_().build();

        assertTrue(publicField.isPublic());
        assertTrue(protectedField.modifiers().contains(Modifier.PROTECTED));
        assertTrue(privateField.isPrivate());
        assertTrue(staticField.isStatic());
        assertTrue(finalField.isFinal());
        assertTrue(volatileField.modifiers().contains(Modifier.VOLATILE));
        assertTrue(transientField.modifiers().contains(Modifier.TRANSIENT));
    }

    @Test
    void field_modifier_addsModifier() {
        FieldDecl field = Field.field(Types.INT, "x")
            .modifier(Modifier.VOLATILE)
            .build();

        assertTrue(field.modifiers().contains(Modifier.VOLATILE));
    }

    @Test
    void field_modifiers_addsMultiple() {
        FieldDecl field = Field.field(Types.INT, "x")
            .modifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .build();

        assertTrue(field.isPrivate());
        assertTrue(field.isStatic());
        assertTrue(field.isFinal());
    }

    @Test
    void field_withAnnotation_buildsCorrectly() {
        FieldDecl field = Field.field(Types.STRING, "email")
            .private_()
            .annotation(AnnotationSpec.of(Types.type("javax.validation.constraints.NotNull")))
            .build();

        assertEquals(1, field.annotations().size());
    }

    @Test
    void field_withMarkerAnnotation_buildsCorrectly() {
        FieldDecl field = Field.field(Types.INT, "x")
            .annotation(Types.type("java.lang.Deprecated"))
            .build();

        assertEquals(1, field.annotations().size());
    }

    @Test
    void field_deprecated_addsDeprecatedAnnotation() {
        FieldDecl field = Field.field(Types.INT, "oldField")
            .deprecated()
            .build();

        assertEquals(1, field.annotations().size());
    }

    @Test
    void field_withJavadoc_buildsCorrectly() {
        FieldDecl field = Field.field(Types.STRING, "name")
            .private_()
            .javadoc("The name of the entity.")
            .build();

        assertTrue(field.hasJavadoc());
        assertEquals("The name of the entity.", field.javadoc());
    }

    // ========================================================================
    // Convenience Factory Methods
    // ========================================================================

    @Test
    void privateField_createsPrivateField() {
        FieldDecl field = Field.privateField(Types.STRING, "name");

        assertTrue(field.isPrivate());
        assertFalse(field.isFinal());
    }

    @Test
    void privateFinalField_createsPrivateFinalField() {
        FieldDecl field = Field.privateFinalField(Types.STRING, "id");

        assertTrue(field.isPrivate());
        assertTrue(field.isFinal());
    }

    @Test
    void constant_createsPublicStaticFinalField() {
        FieldDecl field = Field.constant(Types.INT, "MAX_VALUE", Expr.literal(100));

        assertTrue(field.isConstant());
        assertTrue(field.isPublic());
        assertTrue(field.isStatic());
        assertTrue(field.isFinal());
        assertTrue(field.hasInitializer());
    }
}
