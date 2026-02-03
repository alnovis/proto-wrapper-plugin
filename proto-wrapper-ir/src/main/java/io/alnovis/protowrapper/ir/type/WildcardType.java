package io.alnovis.protowrapper.ir.type;

import java.util.Objects;

/**
 * Represents a wildcard type in the IR.
 *
 * <p>Wildcard types are used in Java generics to express variance:
 * <ul>
 *   <li>{@code ?} - unbounded wildcard, accepts any type</li>
 *   <li>{@code ? extends T} - upper-bounded wildcard (covariance), accepts T or subtype</li>
 *   <li>{@code ? super T} - lower-bounded wildcard (contravariance), accepts T or supertype</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Unbounded: List<?>
 * WildcardType unbounded = WildcardType.unbounded();
 *
 * // Upper-bounded: List<? extends Number>
 * WildcardType extendsNumber = WildcardType.extendsType(
 *     ClassType.of("java.lang.Number")
 * );
 *
 * // Lower-bounded: List<? super Integer>
 * WildcardType superInteger = WildcardType.superType(
 *     ClassType.of("java.lang.Integer")
 * );
 *
 * // Using with ClassType for full generic types
 * ClassType listOfUnknown = ClassType.of("java.util.List")
 *     .withTypeArguments(WildcardType.unbounded());
 *
 * // Using the Types DSL (recommended)
 * TypeRef wildcard = Types.wildcard();
 * TypeRef extendsNum = Types.wildcardExtends(Types.type("java.lang.Number"));
 * TypeRef superInt = Types.wildcardSuper(Types.INTEGER);
 * }</pre>
 *
 * <p><b>PECS (Producer Extends, Consumer Super):</b>
 * <ul>
 *   <li>Use {@code ? extends T} when you only read from a structure (covariant)</li>
 *   <li>Use {@code ? super T} when you only write to a structure (contravariant)</li>
 *   <li>Use exact type when you both read and write (invariant)</li>
 * </ul>
 *
 * <p>Note: A wildcard cannot have both an upper and lower bound simultaneously.
 * The record allows this technically, but it is considered an invalid state.
 *
 * @param upperBound the upper bound for {@code ? extends T}, or null for unbounded/lower-bounded
 * @param lowerBound the lower bound for {@code ? super T}, or null for unbounded/upper-bounded
 * @see io.alnovis.protowrapper.dsl.Types#wildcard()
 * @see io.alnovis.protowrapper.dsl.Types#wildcardExtends(TypeRef)
 * @see io.alnovis.protowrapper.dsl.Types#wildcardSuper(TypeRef)
 * @since 2.4.0
 */
public record WildcardType(
        TypeRef upperBound,
        TypeRef lowerBound
) implements TypeRef {

    /**
     * Creates an unbounded wildcard ({@code ?}).
     *
     * <p>An unbounded wildcard accepts any type and is useful when:
     * <ul>
     *   <li>The actual type parameter is unknown</li>
     *   <li>You only use methods from {@code Object}</li>
     *   <li>The code doesn't depend on the type parameter</li>
     * </ul>
     *
     * <p>Example:
     * <pre>{@code
     * // List<?> - a list of unknown element type
     * ClassType listOfUnknown = ClassType.of("java.util.List")
     *     .withTypeArguments(WildcardType.unbounded());
     * }</pre>
     *
     * @return an unbounded wildcard type
     */
    public static WildcardType unbounded() {
        return new WildcardType(null, null);
    }

    /**
     * Creates a wildcard with an upper bound ({@code ? extends T}).
     *
     * <p>An upper-bounded wildcard accepts the bound type or any of its subtypes.
     * This provides covariance - useful when reading from a structure.
     *
     * <p>Example:
     * <pre>{@code
     * // List<? extends Number> - can read Numbers, but can't add (except null)
     * WildcardType extendsNumber = WildcardType.extendsType(
     *     ClassType.of("java.lang.Number")
     * );
     * }</pre>
     *
     * @param bound the upper bound type
     * @return a wildcard with the specified upper bound
     * @throws NullPointerException if bound is null
     */
    public static WildcardType extendsType(TypeRef bound) {
        Objects.requireNonNull(bound, "bound must not be null");
        return new WildcardType(bound, null);
    }

    /**
     * Creates a wildcard with a lower bound ({@code ? super T}).
     *
     * <p>A lower-bounded wildcard accepts the bound type or any of its supertypes.
     * This provides contravariance - useful when writing to a structure.
     *
     * <p>Example:
     * <pre>{@code
     * // List<? super Integer> - can add Integers, but reads return Object
     * WildcardType superInteger = WildcardType.superType(
     *     ClassType.of("java.lang.Integer")
     * );
     * }</pre>
     *
     * @param bound the lower bound type
     * @return a wildcard with the specified lower bound
     * @throws NullPointerException if bound is null
     */
    public static WildcardType superType(TypeRef bound) {
        Objects.requireNonNull(bound, "bound must not be null");
        return new WildcardType(null, bound);
    }

    /**
     * Returns {@code true} if this is an unbounded wildcard ({@code ?}).
     *
     * <p>An unbounded wildcard has neither an upper nor a lower bound.
     *
     * @return {@code true} if this wildcard has no bounds
     */
    public boolean isUnbounded() {
        return upperBound == null && lowerBound == null;
    }

    /**
     * Returns {@code true} if this wildcard has an upper bound ({@code ? extends T}).
     *
     * @return {@code true} if this wildcard is upper-bounded
     */
    public boolean hasUpperBound() {
        return upperBound != null;
    }

    /**
     * Returns {@code true} if this wildcard has a lower bound ({@code ? super T}).
     *
     * @return {@code true} if this wildcard is lower-bounded
     */
    public boolean hasLowerBound() {
        return lowerBound != null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the wildcard in Java source syntax:
     * <ul>
     *   <li>{@code ?} for unbounded</li>
     *   <li>{@code ? extends T} for upper-bounded</li>
     *   <li>{@code ? super T} for lower-bounded</li>
     * </ul>
     *
     * @return the debug string representation
     */
    @Override
    public String toDebugString() {
        if (upperBound != null) {
            return "? extends " + upperBound.toDebugString();
        }
        if (lowerBound != null) {
            return "? super " + lowerBound.toDebugString();
        }
        return "?";
    }
}
