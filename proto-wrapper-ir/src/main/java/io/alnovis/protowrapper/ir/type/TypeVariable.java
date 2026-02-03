package io.alnovis.protowrapper.ir.type;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a type variable (generic type parameter) in the IR.
 *
 * <p>Type variables are declared in generic classes, interfaces, and methods.
 * They serve as placeholders for actual types that will be specified when
 * the generic is instantiated.
 *
 * <p>A type variable has:
 * <ul>
 *   <li>A name (e.g., T, E, K, V)</li>
 *   <li>Optional bounds that constrain what types can be substituted</li>
 * </ul>
 *
 * <p>Example declarations in Java:
 * <pre>{@code
 * // Simple unbounded: <T>
 * class Box<T> { ... }
 *
 * // Single bound: <T extends Number>
 * class NumberBox<T extends Number> { ... }
 *
 * // Multiple bounds: <T extends Comparable<T> & Serializable>
 * class ComparableBox<T extends Comparable<T> & Serializable> { ... }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Simple type variable: T
 * TypeVariable t = new TypeVariable("T", List.of());
 *
 * // Bounded type variable: T extends Number
 * TypeVariable tExtendsNumber = new TypeVariable("T",
 *     List.of(ClassType.of("java.lang.Number")));
 *
 * // Multiple bounds: T extends Comparable<T> & Serializable
 * TypeVariable tComparable = new TypeVariable("T", List.of(
 *     ClassType.of("java.lang.Comparable").withTypeArguments(
 *         new TypeVariable("T", List.of())  // self-referential
 *     ),
 *     ClassType.of("java.io.Serializable")
 * ));
 *
 * // Using the Types DSL (recommended)
 * TypeRef t = Types.typeVar("T");
 * TypeRef tNum = Types.typeVar("T", Types.type("java.lang.Number"));
 * }</pre>
 *
 * <p><b>Note on bounds:</b>
 * <ul>
 *   <li>If no bounds are specified, the implicit bound is {@code Object}</li>
 *   <li>In Java, at most one bound can be a class; others must be interfaces</li>
 *   <li>The class bound (if any) must be listed first</li>
 * </ul>
 *
 * @param name   the name of the type variable (e.g., "T", "E"); must not be null
 * @param bounds the upper bounds of this type variable; empty list means unbounded
 *               (implicitly bounded by Object); the list is copied and made immutable
 * @see io.alnovis.protowrapper.dsl.Types#typeVar(String)
 * @see io.alnovis.protowrapper.dsl.Types#typeVar(String, TypeRef...)
 * @since 2.4.0
 */
public record TypeVariable(
        String name,
        List<TypeRef> bounds
) implements TypeRef {

    /**
     * Creates a new TypeVariable with validation.
     *
     * @param name   the name of the type variable
     * @param bounds the upper bounds, may be null (treated as empty list)
     * @throws NullPointerException     if name is null
     * @throws IllegalArgumentException if name is empty or blank
     */
    public TypeVariable {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty or blank");
        }
        bounds = bounds == null ? List.of() : List.copyOf(bounds);
    }

    /**
     * Creates an unbounded type variable.
     *
     * <p>An unbounded type variable has an implicit upper bound of {@code Object}.
     *
     * <p>Example:
     * <pre>{@code
     * TypeVariable t = TypeVariable.of("T");  // equivalent to <T>
     * }</pre>
     *
     * @param name the name of the type variable
     * @return a new unbounded TypeVariable
     * @throws NullPointerException if name is null
     */
    public static TypeVariable of(String name) {
        return new TypeVariable(name, List.of());
    }

    /**
     * Creates a type variable with the specified bounds.
     *
     * <p>Example:
     * <pre>{@code
     * // <T extends Number>
     * TypeVariable tNum = TypeVariable.of("T", ClassType.of("java.lang.Number"));
     *
     * // <T extends Comparable<T> & Serializable>
     * TypeVariable tComp = TypeVariable.of("T",
     *     ClassType.of("java.lang.Comparable").withTypeArguments(TypeVariable.of("T")),
     *     ClassType.of("java.io.Serializable")
     * );
     * }</pre>
     *
     * @param name   the name of the type variable
     * @param bounds the upper bounds
     * @return a new bounded TypeVariable
     * @throws NullPointerException if name is null
     */
    public static TypeVariable of(String name, TypeRef... bounds) {
        return new TypeVariable(name, List.of(bounds));
    }

    /**
     * Returns {@code true} if this type variable has no explicit bounds.
     *
     * <p>An unbounded type variable has an implicit bound of {@code Object}.
     *
     * @return {@code true} if this type variable has no bounds
     */
    public boolean isUnbounded() {
        return bounds.isEmpty();
    }

    /**
     * Returns {@code true} if this type variable has explicit bounds.
     *
     * @return {@code true} if this type variable has one or more bounds
     */
    public boolean isBounded() {
        return !bounds.isEmpty();
    }

    /**
     * Returns a new TypeVariable with additional bounds.
     *
     * <p>Example:
     * <pre>{@code
     * TypeVariable t = TypeVariable.of("T");
     * TypeVariable tNum = t.withBounds(ClassType.of("java.lang.Number"));
     * }</pre>
     *
     * @param additionalBounds additional bounds to add
     * @return a new TypeVariable with the combined bounds
     */
    public TypeVariable withBounds(TypeRef... additionalBounds) {
        var newBounds = new java.util.ArrayList<>(bounds);
        newBounds.addAll(List.of(additionalBounds));
        return new TypeVariable(name, newBounds);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the type variable name, optionally with bounds:
     * <ul>
     *   <li>{@code T} for unbounded</li>
     *   <li>{@code T extends Number} for single bound</li>
     *   <li>{@code T extends Comparable & Serializable} for multiple bounds</li>
     * </ul>
     *
     * @return the debug string representation
     */
    @Override
    public String toDebugString() {
        if (bounds.isEmpty()) {
            return name;
        }
        String boundsStr = bounds.stream()
                .map(TypeRef::toDebugString)
                .collect(Collectors.joining(" & "));
        return name + " extends " + boundsStr;
    }
}
