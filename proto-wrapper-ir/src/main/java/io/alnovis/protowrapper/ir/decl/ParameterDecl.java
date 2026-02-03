package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.stmt.Statement;

import java.util.List;
import java.util.Objects;

/**
 * Represents a method or constructor parameter declaration in the IR.
 *
 * <p>A parameter declaration consists of:
 * <ul>
 *   <li>A name</li>
 *   <li>A type (may be null for inferred types in lambdas)</li>
 *   <li>Optional annotations</li>
 *   <li>An optional final modifier</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Simple parameter
 * void process(String input) { }
 *
 * // Final parameter
 * void process(final String input) { }
 *
 * // Annotated parameter
 * void process(@NotNull String input) { }
 *
 * // Combined
 * void process(@NotNull final String input) { }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Simple parameter: String input
 * ParameterDecl param = new ParameterDecl(
 *     "input",
 *     Types.STRING,
 *     List.of(),
 *     false
 * );
 *
 * // Final parameter: final int value
 * ParameterDecl finalParam = new ParameterDecl(
 *     "value",
 *     Types.INT,
 *     List.of(),
 *     true
 * );
 *
 * // Factory methods
 * ParameterDecl p1 = ParameterDecl.of("name", Types.STRING);
 * ParameterDecl p2 = ParameterDecl.finalParam("value", Types.INT);
 * }</pre>
 *
 * @param name        the parameter name; must not be null
 * @param type        the parameter type; may be null for inferred types in lambdas
 * @param annotations annotations on this parameter; empty list if none;
 *                    the list is copied and made immutable
 * @param isFinal     {@code true} if the parameter is declared final
 * @see MethodDecl
 * @see ConstructorDecl
 * @see LambdaExpr
 * @since 2.4.0
 */
public record ParameterDecl(
        String name,
        TypeRef type,
        List<AnnotationSpec> annotations,
        boolean isFinal
) {

    /**
     * Creates a new ParameterDecl with validation.
     *
     * @param name        the parameter name
     * @param type        the parameter type (may be null for inferred types)
     * @param annotations the annotations (may be null, treated as empty list)
     * @param isFinal     whether the parameter is final
     * @throws NullPointerException if name is null
     */
    public ParameterDecl {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty or blank");
        }
        // type may be null for lambda parameters with inferred types
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }

    /**
     * Creates a simple parameter without annotations or final modifier.
     *
     * @param name the parameter name
     * @param type the parameter type
     * @return a new ParameterDecl
     */
    public static ParameterDecl of(String name, TypeRef type) {
        return new ParameterDecl(name, type, List.of(), false);
    }

    /**
     * Creates a final parameter without annotations.
     *
     * @param name the parameter name
     * @param type the parameter type
     * @return a new final ParameterDecl
     */
    public static ParameterDecl finalParam(String name, TypeRef type) {
        return new ParameterDecl(name, type, List.of(), true);
    }

    /**
     * Returns a new ParameterDecl with the final modifier set.
     *
     * @return a final copy of this parameter
     */
    public ParameterDecl asFinal() {
        return new ParameterDecl(name, type, annotations, true);
    }

    /**
     * Returns a new ParameterDecl with the given annotation added.
     *
     * @param annotation the annotation to add
     * @return a new ParameterDecl with the annotation
     */
    public ParameterDecl withAnnotation(AnnotationSpec annotation) {
        var newAnnotations = new java.util.ArrayList<>(annotations);
        newAnnotations.add(annotation);
        return new ParameterDecl(name, type, newAnnotations, isFinal);
    }

    /**
     * Returns {@code true} if this parameter has annotations.
     *
     * @return {@code true} if annotations list is not empty
     */
    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }

    /**
     * Returns {@code true} if this parameter has an inferred type.
     *
     * <p>Parameters with inferred types have null type and are used
     * in lambda expressions where the compiler infers the type.
     *
     * @return {@code true} if type is null
     */
    public boolean hasInferredType() {
        return type == null;
    }

    /**
     * Returns {@code true} if this parameter has an explicit type.
     *
     * @return {@code true} if type is not null
     */
    public boolean hasExplicitType() {
        return type != null;
    }
}
