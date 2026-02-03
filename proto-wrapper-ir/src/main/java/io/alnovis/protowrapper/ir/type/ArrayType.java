package io.alnovis.protowrapper.ir.type;

import java.util.Objects;

/**
 * Represents an array type in the IR.
 *
 * <p>An array type wraps a component type, which can be any {@link TypeRef}:
 * primitives, classes, or even other arrays (for multidimensional arrays).
 *
 * <p>Example usage:
 * <pre>{@code
 * // int[]
 * ArrayType intArray = ArrayType.of(new PrimitiveType(PrimitiveKind.INT));
 *
 * // String[]
 * ArrayType stringArray = ArrayType.of(ClassType.of("java.lang.String"));
 *
 * // int[][] (multidimensional)
 * ArrayType int2dArray = ArrayType.of(ArrayType.of(new PrimitiveType(PrimitiveKind.INT)));
 *
 * // Using the Types DSL (recommended)
 * TypeRef intArray = Types.array(Types.INT);
 * TypeRef stringArray = Types.array(Types.STRING);
 * }</pre>
 *
 * <p>In Java, arrays are covariant: if B extends A, then B[] is a subtype of A[].
 * This IR does not model subtype relationships; those are determined by the emitter.
 *
 * @param componentType the type of elements in the array; must not be null
 * @see io.alnovis.protowrapper.dsl.Types#array(TypeRef)
 * @since 2.4.0
 */
public record ArrayType(TypeRef componentType) implements TypeRef {

    /**
     * Creates a new ArrayType with validation.
     *
     * @param componentType the component type of the array
     * @throws NullPointerException if componentType is null
     */
    public ArrayType {
        Objects.requireNonNull(componentType, "componentType must not be null");
    }

    /**
     * Creates an ArrayType for the given component type.
     *
     * <p>This is a convenience factory method equivalent to calling the constructor directly.
     *
     * <p>Example:
     * <pre>{@code
     * ArrayType intArray = ArrayType.of(Types.INT);
     * ArrayType stringArray = ArrayType.of(Types.STRING);
     * }</pre>
     *
     * @param componentType the type of elements in the array
     * @return a new ArrayType instance
     * @throws NullPointerException if componentType is null
     */
    public static ArrayType of(TypeRef componentType) {
        return new ArrayType(componentType);
    }

    /**
     * Returns the number of dimensions for this array type.
     *
     * <p>For a simple array like {@code int[]}, returns 1.
     * For nested arrays like {@code int[][]}, returns 2.
     *
     * <p>Example:
     * <pre>{@code
     * ArrayType.of(Types.INT).dimensions()                        // 1
     * ArrayType.of(ArrayType.of(Types.INT)).dimensions()          // 2
     * ArrayType.of(ArrayType.of(ArrayType.of(Types.INT))).dimensions() // 3
     * }</pre>
     *
     * @return the number of array dimensions (always &gt;= 1)
     */
    public int dimensions() {
        if (componentType instanceof ArrayType nested) {
            return 1 + nested.dimensions();
        }
        return 1;
    }

    /**
     * Returns the innermost (non-array) element type.
     *
     * <p>For a simple array, this is the same as componentType.
     * For multidimensional arrays, this unwraps all array layers.
     *
     * <p>Example:
     * <pre>{@code
     * // int[]
     * ArrayType.of(Types.INT).elementType()  // returns Types.INT
     *
     * // int[][]
     * ArrayType.of(ArrayType.of(Types.INT)).elementType()  // returns Types.INT
     * }</pre>
     *
     * @return the innermost non-array element type
     */
    public TypeRef elementType() {
        if (componentType instanceof ArrayType nested) {
            return nested.elementType();
        }
        return componentType;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the component type's debug string followed by "[]".
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code int[]}</li>
     *   <li>{@code java.lang.String[]}</li>
     *   <li>{@code int[][]} (for nested arrays)</li>
     * </ul>
     *
     * @return the debug string representation
     */
    @Override
    public String toDebugString() {
        return componentType.toDebugString() + "[]";
    }
}
