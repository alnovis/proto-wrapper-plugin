package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.decl.AnnotationSpec;
import io.alnovis.protowrapper.ir.decl.FieldDecl;
import io.alnovis.protowrapper.ir.decl.Modifier;
import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.type.TypeRef;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builder for creating field declarations.
 *
 * <p>This builder provides a fluent API for constructing {@link FieldDecl}
 * instances. It supports all field features including modifiers, type,
 * initializer, annotations, and JavaDoc.
 *
 * <h2>Simple Field</h2>
 * <pre>{@code
 * FieldDecl field = Field.field(Types.STRING, "name")
 *     .private_()
 *     .build();
 * // private String name;
 * }</pre>
 *
 * <h2>Field with Initializer</h2>
 * <pre>{@code
 * FieldDecl field = Field.field(Types.INT, "count")
 *     .private_()
 *     .initializer(Expr.literal(0))
 *     .build();
 * // private int count = 0;
 * }</pre>
 *
 * <h2>Final Field</h2>
 * <pre>{@code
 * FieldDecl field = Field.field(Types.STRING, "id")
 *     .private_()
 *     .final_()
 *     .build();
 * // private final String id;
 * }</pre>
 *
 * <h2>Static Constant</h2>
 * <pre>{@code
 * FieldDecl constant = Field.field(Types.STRING, "DEFAULT_NAME")
 *     .public_()
 *     .static_()
 *     .final_()
 *     .initializer(Expr.literal("Unknown"))
 *     .build();
 * // public static final String DEFAULT_NAME = "Unknown";
 * }</pre>
 *
 * <h2>Field with Annotations</h2>
 * <pre>{@code
 * FieldDecl field = Field.field(Types.STRING, "email")
 *     .private_()
 *     .annotation(Types.type("javax.validation.constraints.NotNull"))
 *     .annotation(Types.type("javax.validation.constraints.Email"))
 *     .javadoc("The user's email address.")
 *     .build();
 * // @NotNull @Email private String email;
 * }</pre>
 *
 * <h2>Transient Field</h2>
 * <pre>{@code
 * FieldDecl field = Field.field(Types.type("java.util.List", Types.STRING), "cache")
 *     .private_()
 *     .transient_()
 *     .build();
 * // private transient List<String> cache;
 * }</pre>
 *
 * @see FieldDecl
 * @since 2.4.0
 */
public final class Field {

    private final TypeRef type;
    private final String name;
    private final Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
    private Expression initializer;
    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private String javadoc;

    private Field(TypeRef type, String name) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Creates a new field builder with the given type and name.
     *
     * @param type the field type
     * @param name the field name
     * @return a new Field builder
     */
    public static Field field(TypeRef type, String name) {
        return new Field(type, name);
    }

    // ========================================================================
    // Modifiers
    // ========================================================================

    /**
     * Adds the {@code public} modifier.
     *
     * @return this builder
     */
    public Field public_() {
        modifiers.add(Modifier.PUBLIC);
        return this;
    }

    /**
     * Adds the {@code protected} modifier.
     *
     * @return this builder
     */
    public Field protected_() {
        modifiers.add(Modifier.PROTECTED);
        return this;
    }

    /**
     * Adds the {@code private} modifier.
     *
     * @return this builder
     */
    public Field private_() {
        modifiers.add(Modifier.PRIVATE);
        return this;
    }

    /**
     * Adds the {@code static} modifier.
     *
     * @return this builder
     */
    public Field static_() {
        modifiers.add(Modifier.STATIC);
        return this;
    }

    /**
     * Adds the {@code final} modifier.
     *
     * @return this builder
     */
    public Field final_() {
        modifiers.add(Modifier.FINAL);
        return this;
    }

    /**
     * Adds the {@code volatile} modifier.
     *
     * @return this builder
     */
    public Field volatile_() {
        modifiers.add(Modifier.VOLATILE);
        return this;
    }

    /**
     * Adds the {@code transient} modifier.
     *
     * @return this builder
     */
    public Field transient_() {
        modifiers.add(Modifier.TRANSIENT);
        return this;
    }

    /**
     * Adds the specified modifier.
     *
     * @param modifier the modifier to add
     * @return this builder
     */
    public Field modifier(Modifier modifier) {
        modifiers.add(modifier);
        return this;
    }

    /**
     * Adds multiple modifiers.
     *
     * @param modifiers the modifiers to add
     * @return this builder
     */
    public Field modifiers(Modifier... modifiers) {
        for (Modifier m : modifiers) {
            this.modifiers.add(m);
        }
        return this;
    }

    // ========================================================================
    // Initializer
    // ========================================================================

    /**
     * Sets the field initializer expression.
     *
     * <p>Example:
     * <pre>{@code
     * Field.field(Types.INT, "count").initializer(Expr.literal(0))
     * // int count = 0;
     * }</pre>
     *
     * @param initializer the initializer expression
     * @return this builder
     */
    public Field initializer(Expression initializer) {
        this.initializer = initializer;
        return this;
    }

    // ========================================================================
    // Annotations
    // ========================================================================

    /**
     * Adds an annotation.
     *
     * @param annotation the annotation
     * @return this builder
     */
    public Field annotation(AnnotationSpec annotation) {
        annotations.add(annotation);
        return this;
    }

    /**
     * Adds a simple marker annotation (no members).
     *
     * @param annotationType the annotation type
     * @return this builder
     */
    public Field annotation(TypeRef annotationType) {
        annotations.add(AnnotationSpec.of(annotationType));
        return this;
    }

    /**
     * Adds a @Deprecated annotation.
     *
     * @return this builder
     */
    public Field deprecated() {
        return annotation(Types.type("java.lang.Deprecated"));
    }

    // ========================================================================
    // JavaDoc
    // ========================================================================

    /**
     * Sets the JavaDoc comment.
     *
     * @param javadoc the JavaDoc comment
     * @return this builder
     */
    public Field javadoc(String javadoc) {
        this.javadoc = javadoc;
        return this;
    }

    // ========================================================================
    // Build
    // ========================================================================

    /**
     * Builds the field declaration.
     *
     * @return a new FieldDecl
     */
    public FieldDecl build() {
        return new FieldDecl(
            name,
            type,
            initializer,
            modifiers,
            annotations,
            javadoc
        );
    }

    // ========================================================================
    // Convenience Factory Methods
    // ========================================================================

    /**
     * Creates a private field.
     *
     * @param type the field type
     * @param name the field name
     * @return a new private field declaration
     */
    public static FieldDecl privateField(TypeRef type, String name) {
        return field(type, name).private_().build();
    }

    /**
     * Creates a private final field.
     *
     * @param type the field type
     * @param name the field name
     * @return a new private final field declaration
     */
    public static FieldDecl privateFinalField(TypeRef type, String name) {
        return field(type, name).private_().final_().build();
    }

    /**
     * Creates a public static final constant.
     *
     * @param type        the constant type
     * @param name        the constant name
     * @param initializer the constant value
     * @return a new public static final field declaration
     */
    public static FieldDecl constant(TypeRef type, String name, Expression initializer) {
        return field(type, name)
            .public_()
            .static_()
            .final_()
            .initializer(initializer)
            .build();
    }
}
