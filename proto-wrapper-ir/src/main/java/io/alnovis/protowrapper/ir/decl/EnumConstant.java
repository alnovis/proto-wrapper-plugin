package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.expr.Expression;

import java.util.List;
import java.util.Objects;

/**
 * Represents an enum constant in the IR.
 *
 * <p>An enum constant is a named instance of an enum type. It can have:
 * <ul>
 *   <li>A name (the constant identifier)</li>
 *   <li>Optional constructor arguments</li>
 *   <li>Annotations</li>
 *   <li>JavaDoc comment</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * public enum Status {
 *     // Simple constants
 *     PENDING,
 *     ACTIVE,
 *     COMPLETED;
 * }
 *
 * public enum Planet {
 *     // Constants with constructor arguments
 *     MERCURY(3.303e+23, 2.4397e6),
 *     VENUS(4.869e+24, 6.0518e6),
 *     EARTH(5.976e+24, 6.37814e6);
 *
 *     private final double mass;
 *     private final double radius;
 *
 *     Planet(double mass, double radius) {
 *         this.mass = mass;
 *         this.radius = radius;
 *     }
 * }
 *
 * public enum HttpStatus {
 *     /**
 *      * Request succeeded.
 *      *{@literal /}
 *     OK(200, "OK"),
 *
 *     /**
 *      * Resource not found.
 *      *{@literal /}
 *     NOT_FOUND(404, "Not Found");
 *
 *     // ...
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Simple constant: PENDING
 * EnumConstant pending = new EnumConstant(
 *     "PENDING",
 *     List.of(),  // no arguments
 *     List.of(),  // no annotations
 *     null        // no javadoc
 * );
 *
 * // Constant with arguments: EARTH(5.976e+24, 6.37814e6)
 * EnumConstant earth = new EnumConstant(
 *     "EARTH",
 *     List.of(Expr.literal(5.976e+24), Expr.literal(6.37814e6)),
 *     List.of(),
 *     null
 * );
 * }</pre>
 *
 * @param name        the constant name; must not be null
 * @param arguments   constructor arguments; the list is copied
 * @param annotations annotations on this constant; the list is copied
 * @param javadoc     JavaDoc comment; null if none
 * @see EnumDecl
 * @since 2.4.0
 */
public record EnumConstant(
        String name,
        List<Expression> arguments,
        List<AnnotationSpec> annotations,
        String javadoc
) {

    /**
     * Creates a new EnumConstant with validation.
     *
     * @param name        the constant name (must not be null)
     * @param arguments   the constructor arguments (may be null, treated as empty list)
     * @param annotations the annotations (may be null, treated as empty list)
     * @param javadoc     the JavaDoc comment (may be null)
     * @throws NullPointerException if name is null
     */
    public EnumConstant {
        Objects.requireNonNull(name, "name must not be null");
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }

    /**
     * Creates a simple enum constant without arguments, annotations, or JavaDoc.
     *
     * @param name the constant name
     * @return a new EnumConstant
     */
    public static EnumConstant of(String name) {
        return new EnumConstant(name, List.of(), List.of(), null);
    }

    /**
     * Returns {@code true} if this constant has constructor arguments.
     *
     * @return {@code true} if arguments is not empty
     */
    public boolean hasArguments() {
        return !arguments.isEmpty();
    }

    /**
     * Returns {@code true} if this constant has JavaDoc.
     *
     * @return {@code true} if javadoc is not null
     */
    public boolean hasJavadoc() {
        return javadoc != null;
    }

    /**
     * Returns {@code true} if this constant has annotations.
     *
     * @return {@code true} if annotations is not empty
     */
    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }
}
