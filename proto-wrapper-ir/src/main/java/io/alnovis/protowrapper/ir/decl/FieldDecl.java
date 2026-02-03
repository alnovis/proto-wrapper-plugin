package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.stmt.Statement;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a field declaration in the IR.
 *
 * <p>A field declaration includes:
 * <ul>
 *   <li>Name and type</li>
 *   <li>Optional initializer expression</li>
 *   <li>Modifiers (public, private, static, final, etc.)</li>
 *   <li>Annotations</li>
 *   <li>JavaDoc comment</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Simple field
 * private String name;
 *
 * // Field with initializer
 * private int count = 0;
 *
 * // Static final constant
 * public static final int MAX_SIZE = 100;
 *
 * // Annotated field
 * @NotNull
 * private final String id;
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // private String name;
 * FieldDecl nameField = new FieldDecl(
 *     "name",
 *     Types.STRING,
 *     null,  // no initializer
 *     Set.of(Modifier.PRIVATE),
 *     List.of(),
 *     null   // no javadoc
 * );
 *
 * // public static final int MAX_SIZE = 100;
 * FieldDecl constant = new FieldDecl(
 *     "MAX_SIZE",
 *     Types.INT,
 *     Expr.literal(100),
 *     Set.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL),
 *     List.of(),
 *     "Maximum allowed size."
 * );
 *
 * // Using the DSL (recommended)
 * FieldDecl field = Field.field("name")
 *     .type(Types.STRING)
 *     .private_()
 *     .build();
 * }</pre>
 *
 * @param name        the field name; must not be null
 * @param type        the field type; must not be null
 * @param initializer the initial value expression; null if not initialized
 * @param modifiers   the field modifiers; the set is copied and made immutable
 * @param annotations annotations on this field; the list is copied and made immutable
 * @param javadoc     JavaDoc comment; null if none
 * @see io.alnovis.protowrapper.dsl.Field
 * @since 2.4.0
 */
public record FieldDecl(
        String name,
        TypeRef type,
        Expression initializer,
        Set<Modifier> modifiers,
        List<AnnotationSpec> annotations,
        String javadoc
) implements MemberDeclaration {

    /**
     * Creates a new FieldDecl with validation.
     *
     * @param name        the field name
     * @param type        the field type
     * @param initializer the initializer (may be null)
     * @param modifiers   the modifiers (may be null, treated as empty set)
     * @param annotations the annotations (may be null, treated as empty list)
     * @param javadoc     the JavaDoc comment (may be null)
     * @throws NullPointerException if name or type is null
     */
    public FieldDecl {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty or blank");
        }
        Objects.requireNonNull(type, "type must not be null");
        modifiers = modifiers == null ? Set.of() : Set.copyOf(modifiers);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }

    /**
     * Returns {@code true} if this field has an initializer.
     *
     * @return {@code true} if initializer is not null
     */
    public boolean hasInitializer() {
        return initializer != null;
    }

    /**
     * Returns {@code true} if this field has JavaDoc.
     *
     * @return {@code true} if javadoc is not null
     */
    public boolean hasJavadoc() {
        return javadoc != null;
    }

    /**
     * Returns {@code true} if this field is static.
     *
     * @return {@code true} if modifiers contain STATIC
     */
    public boolean isStatic() {
        return modifiers.contains(Modifier.STATIC);
    }

    /**
     * Returns {@code true} if this field is final.
     *
     * @return {@code true} if modifiers contain FINAL
     */
    public boolean isFinal() {
        return modifiers.contains(Modifier.FINAL);
    }

    /**
     * Returns {@code true} if this field is private.
     *
     * @return {@code true} if modifiers contain PRIVATE
     */
    public boolean isPrivate() {
        return modifiers.contains(Modifier.PRIVATE);
    }

    /**
     * Returns {@code true} if this field is public.
     *
     * @return {@code true} if modifiers contain PUBLIC
     */
    public boolean isPublic() {
        return modifiers.contains(Modifier.PUBLIC);
    }

    /**
     * Returns {@code true} if this is a constant (public static final).
     *
     * @return {@code true} if field is public, static, and final
     */
    public boolean isConstant() {
        return modifiers.containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL));
    }
}
