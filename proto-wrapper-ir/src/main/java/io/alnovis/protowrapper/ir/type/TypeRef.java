package io.alnovis.protowrapper.ir.type;

/**
 * Represents a type reference in the Intermediate Representation (IR).
 *
 * <p>This is a language-agnostic representation of types that can be emitted
 * to various target languages (Java, Kotlin, etc.). The IR type system supports:
 *
 * <ul>
 *   <li>{@link PrimitiveType} - primitive types (int, long, boolean, etc.)</li>
 *   <li>{@link ClassType} - class/interface types with optional type arguments</li>
 *   <li>{@link ArrayType} - array types</li>
 *   <li>{@link WildcardType} - wildcard types (?, ? extends T, ? super T)</li>
 *   <li>{@link TypeVariable} - type variables (generics parameters like T, E)</li>
 *   <li>{@link VoidType} - the void type for method return types</li>
 * </ul>
 *
 * <p>Example usage with DSL:
 * <pre>{@code
 * // Simple types
 * TypeRef intType = Types.INT;
 * TypeRef stringType = Types.STRING;
 *
 * // Generic types
 * TypeRef listOfStrings = Types.list(Types.STRING);
 * TypeRef mapType = Types.map(Types.STRING, Types.INT);
 *
 * // Arrays
 * TypeRef intArray = Types.array(Types.INT);
 * }</pre>
 *
 * @see PrimitiveType
 * @see ClassType
 * @see ArrayType
 * @see WildcardType
 * @see TypeVariable
 * @see VoidType
 * @since 2.4.0
 */
public sealed interface TypeRef permits
        PrimitiveType,
        ClassType,
        ArrayType,
        WildcardType,
        TypeVariable,
        VoidType {

    /**
     * Returns a human-readable debug string representation of this type.
     *
     * <p>The format is similar to Java source code syntax:
     * <ul>
     *   <li>Primitives: {@code int}, {@code boolean}</li>
     *   <li>Classes: {@code java.util.List}, {@code java.util.Map<String, Integer>}</li>
     *   <li>Arrays: {@code int[]}, {@code String[]}</li>
     *   <li>Wildcards: {@code ?}, {@code ? extends Number}, {@code ? super Integer}</li>
     *   <li>Type variables: {@code T}, {@code E extends Comparable<E>}</li>
     * </ul>
     *
     * @return a debug-friendly string representation of this type
     */
    String toDebugString();
}
