package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;

import java.util.Arrays;
import java.util.List;

/**
 * Factory class for creating type references.
 *
 * <p>This utility class provides convenient factory methods for creating
 * all kinds of type references used in the IR. It serves as the main entry
 * point for working with types in the DSL.
 *
 * <h2>Primitive Types</h2>
 * <pre>{@code
 * TypeRef intType = Types.INT;
 * TypeRef longType = Types.LONG;
 * TypeRef booleanType = Types.BOOLEAN;
 * }</pre>
 *
 * <h2>Class Types</h2>
 * <pre>{@code
 * // Simple class type
 * TypeRef string = Types.STRING;
 * TypeRef object = Types.OBJECT;
 *
 * // Custom class type
 * TypeRef myClass = Types.type("com.example.MyClass");
 *
 * // Generic class type
 * TypeRef listOfString = Types.type("java.util.List", Types.STRING);
 * TypeRef mapType = Types.type("java.util.Map", Types.STRING, Types.OBJECT);
 * }</pre>
 *
 * <h2>Array Types</h2>
 * <pre>{@code
 * // One-dimensional array
 * TypeRef intArray = Types.array(Types.INT);
 * TypeRef stringArray = Types.array(Types.STRING);
 *
 * // Multi-dimensional array
 * TypeRef int2DArray = Types.array(Types.INT, 2);
 * }</pre>
 *
 * <h2>Wildcard Types</h2>
 * <pre>{@code
 * // Unbounded wildcard: ?
 * TypeRef unbounded = Types.wildcard();
 *
 * // Upper bounded: ? extends Number
 * TypeRef extendsNumber = Types.wildcardExtends(Types.type("java.lang.Number"));
 *
 * // Lower bounded: ? super Integer
 * TypeRef superInteger = Types.wildcardSuper(Types.type("java.lang.Integer"));
 * }</pre>
 *
 * <h2>Type Variables</h2>
 * <pre>{@code
 * // Simple type variable: T
 * TypeVariable t = Types.typeVar("T");
 *
 * // Bounded type variable: T extends Comparable<T>
 * TypeVariable comparable = Types.typeVar("T", Types.type("java.lang.Comparable", Types.typeVar("T")));
 * }</pre>
 *
 * <h2>Common Types</h2>
 * <pre>{@code
 * TypeRef list = Types.list(Types.STRING);        // List<String>
 * TypeRef set = Types.set(Types.INT);             // Set<Integer> (auto-boxed)
 * TypeRef map = Types.map(Types.STRING, Types.INT); // Map<String, Integer>
 * TypeRef optional = Types.optional(Types.STRING); // Optional<String>
 * }</pre>
 *
 * @see TypeRef
 * @see PrimitiveType
 * @see ClassType
 * @see ArrayType
 * @see WildcardType
 * @see TypeVariable
 * @since 2.4.0
 */
public final class Types {

    // ========================================================================
    // Primitive Types
    // ========================================================================

    /** The {@code boolean} primitive type. */
    public static final PrimitiveType BOOLEAN = PrimitiveType.of(PrimitiveKind.BOOLEAN);

    /** The {@code byte} primitive type. */
    public static final PrimitiveType BYTE = PrimitiveType.of(PrimitiveKind.BYTE);

    /** The {@code char} primitive type. */
    public static final PrimitiveType CHAR = PrimitiveType.of(PrimitiveKind.CHAR);

    /** The {@code short} primitive type. */
    public static final PrimitiveType SHORT = PrimitiveType.of(PrimitiveKind.SHORT);

    /** The {@code int} primitive type. */
    public static final PrimitiveType INT = PrimitiveType.of(PrimitiveKind.INT);

    /** The {@code long} primitive type. */
    public static final PrimitiveType LONG = PrimitiveType.of(PrimitiveKind.LONG);

    /** The {@code float} primitive type. */
    public static final PrimitiveType FLOAT = PrimitiveType.of(PrimitiveKind.FLOAT);

    /** The {@code double} primitive type. */
    public static final PrimitiveType DOUBLE = PrimitiveType.of(PrimitiveKind.DOUBLE);

    /** The {@code void} type. */
    public static final VoidType VOID = VoidType.INSTANCE;

    // ========================================================================
    // Common Class Types
    // ========================================================================

    /** The {@code java.lang.Object} type. */
    public static final ClassType OBJECT = ClassType.of("java.lang.Object");

    /** The {@code java.lang.String} type. */
    public static final ClassType STRING = ClassType.of("java.lang.String");

    /** The {@code java.lang.Class} type (raw). */
    public static final ClassType CLASS = ClassType.of("java.lang.Class");

    /** The {@code java.lang.Boolean} wrapper type. */
    public static final ClassType BOOLEAN_BOXED = ClassType.of("java.lang.Boolean");

    /** The {@code java.lang.Byte} wrapper type. */
    public static final ClassType BYTE_BOXED = ClassType.of("java.lang.Byte");

    /** The {@code java.lang.Character} wrapper type. */
    public static final ClassType CHAR_BOXED = ClassType.of("java.lang.Character");

    /** The {@code java.lang.Short} wrapper type. */
    public static final ClassType SHORT_BOXED = ClassType.of("java.lang.Short");

    /** The {@code java.lang.Integer} wrapper type. */
    public static final ClassType INT_BOXED = ClassType.of("java.lang.Integer");

    /** The {@code java.lang.Long} wrapper type. */
    public static final ClassType LONG_BOXED = ClassType.of("java.lang.Long");

    /** The {@code java.lang.Float} wrapper type. */
    public static final ClassType FLOAT_BOXED = ClassType.of("java.lang.Float");

    /** The {@code java.lang.Double} wrapper type. */
    public static final ClassType DOUBLE_BOXED = ClassType.of("java.lang.Double");

    private Types() {
        // Utility class, not instantiable
    }

    // ========================================================================
    // Class Type Factory Methods
    // ========================================================================

    /**
     * Creates a class type for the given fully qualified name.
     *
     * @param qualifiedName the fully qualified class name
     * @return a new ClassType
     */
    public static ClassType type(String qualifiedName) {
        return ClassType.of(qualifiedName);
    }

    /**
     * Creates a parameterized class type.
     *
     * <p>Example:
     * <pre>{@code
     * TypeRef listOfString = Types.type("java.util.List", Types.STRING);
     * TypeRef mapType = Types.type("java.util.Map", Types.STRING, Types.INT_BOXED);
     * }</pre>
     *
     * @param qualifiedName the fully qualified class name
     * @param typeArguments the type arguments
     * @return a new parameterized ClassType
     */
    public static ClassType type(String qualifiedName, TypeRef... typeArguments) {
        return ClassType.of(qualifiedName).withTypeArguments(typeArguments);
    }

    /**
     * Creates a parameterized class type.
     *
     * @param qualifiedName the fully qualified class name
     * @param typeArguments the type arguments
     * @return a new parameterized ClassType
     */
    public static ClassType type(String qualifiedName, List<TypeRef> typeArguments) {
        return ClassType.of(qualifiedName).withTypeArguments(typeArguments);
    }

    // ========================================================================
    // Array Type Factory Methods
    // ========================================================================

    /**
     * Creates a one-dimensional array type.
     *
     * <p>Example:
     * <pre>{@code
     * TypeRef intArray = Types.array(Types.INT);      // int[]
     * TypeRef stringArray = Types.array(Types.STRING); // String[]
     * }</pre>
     *
     * @param componentType the array component type
     * @return a new ArrayType
     */
    public static ArrayType array(TypeRef componentType) {
        return ArrayType.of(componentType);
    }

    /**
     * Creates a multi-dimensional array type.
     *
     * <p>Example:
     * <pre>{@code
     * TypeRef int2D = Types.array(Types.INT, 2);    // int[][]
     * TypeRef int3D = Types.array(Types.INT, 3);    // int[][][]
     * }</pre>
     *
     * @param componentType the base component type
     * @param dimensions    the number of dimensions
     * @return a new multi-dimensional ArrayType
     * @throws IllegalArgumentException if dimensions is less than 1
     */
    public static ArrayType array(TypeRef componentType, int dimensions) {
        if (dimensions < 1) {
            throw new IllegalArgumentException("dimensions must be at least 1");
        }
        ArrayType result = ArrayType.of(componentType);
        for (int i = 1; i < dimensions; i++) {
            result = ArrayType.of(result);
        }
        return result;
    }

    // ========================================================================
    // Wildcard Type Factory Methods
    // ========================================================================

    /**
     * Creates an unbounded wildcard type ({@code ?}).
     *
     * @return an unbounded WildcardType
     */
    public static WildcardType wildcard() {
        return WildcardType.unbounded();
    }

    /**
     * Creates an upper-bounded wildcard type ({@code ? extends T}).
     *
     * <p>Example:
     * <pre>{@code
     * TypeRef extendsNumber = Types.wildcardExtends(Types.type("java.lang.Number"));
     * }</pre>
     *
     * @param upperBound the upper bound
     * @return a new upper-bounded WildcardType
     */
    public static WildcardType wildcardExtends(TypeRef upperBound) {
        return WildcardType.extendsType(upperBound);
    }

    /**
     * Creates a lower-bounded wildcard type ({@code ? super T}).
     *
     * <p>Example:
     * <pre>{@code
     * TypeRef superInteger = Types.wildcardSuper(Types.type("java.lang.Integer"));
     * }</pre>
     *
     * @param lowerBound the lower bound
     * @return a new lower-bounded WildcardType
     */
    public static WildcardType wildcardSuper(TypeRef lowerBound) {
        return WildcardType.superType(lowerBound);
    }

    // ========================================================================
    // Type Variable Factory Methods
    // ========================================================================

    /**
     * Creates an unbounded type variable.
     *
     * <p>Example:
     * <pre>{@code
     * TypeVariable t = Types.typeVar("T");  // T
     * TypeVariable e = Types.typeVar("E");  // E
     * }</pre>
     *
     * @param name the type variable name
     * @return a new TypeVariable
     */
    public static TypeVariable typeVar(String name) {
        return TypeVariable.of(name);
    }

    /**
     * Creates a bounded type variable.
     *
     * <p>Example:
     * <pre>{@code
     * TypeVariable t = Types.typeVar("T", Types.type("java.lang.Number"));
     * // T extends Number
     *
     * TypeVariable e = Types.typeVar("E", Types.type("java.lang.Comparable", Types.typeVar("E")));
     * // E extends Comparable<E>
     * }</pre>
     *
     * @param name   the type variable name
     * @param bounds the upper bounds
     * @return a new bounded TypeVariable
     */
    public static TypeVariable typeVar(String name, TypeRef... bounds) {
        return new TypeVariable(name, Arrays.asList(bounds));
    }

    /**
     * Creates a bounded type variable.
     *
     * @param name   the type variable name
     * @param bounds the upper bounds
     * @return a new bounded TypeVariable
     */
    public static TypeVariable typeVar(String name, List<TypeRef> bounds) {
        return new TypeVariable(name, bounds);
    }

    // ========================================================================
    // Convenience Methods for Common Generic Types
    // ========================================================================

    /**
     * Creates a {@code List<T>} type.
     *
     * <p>Example:
     * <pre>{@code
     * TypeRef listOfString = Types.list(Types.STRING);  // List<String>
     * }</pre>
     *
     * @param elementType the element type
     * @return a parameterized List type
     */
    public static ClassType list(TypeRef elementType) {
        return type("java.util.List", boxIfPrimitive(elementType));
    }

    /**
     * Creates a {@code Set<T>} type.
     *
     * <p>Example:
     * <pre>{@code
     * TypeRef setOfInt = Types.set(Types.INT);  // Set<Integer>
     * }</pre>
     *
     * @param elementType the element type
     * @return a parameterized Set type
     */
    public static ClassType set(TypeRef elementType) {
        return type("java.util.Set", boxIfPrimitive(elementType));
    }

    /**
     * Creates a {@code Map<K, V>} type.
     *
     * <p>Example:
     * <pre>{@code
     * TypeRef map = Types.map(Types.STRING, Types.INT);  // Map<String, Integer>
     * }</pre>
     *
     * @param keyType   the key type
     * @param valueType the value type
     * @return a parameterized Map type
     */
    public static ClassType map(TypeRef keyType, TypeRef valueType) {
        return type("java.util.Map", boxIfPrimitive(keyType), boxIfPrimitive(valueType));
    }

    /**
     * Creates an {@code Optional<T>} type.
     *
     * <p>Example:
     * <pre>{@code
     * TypeRef optString = Types.optional(Types.STRING);  // Optional<String>
     * }</pre>
     *
     * @param valueType the value type
     * @return a parameterized Optional type
     */
    public static ClassType optional(TypeRef valueType) {
        return type("java.util.Optional", boxIfPrimitive(valueType));
    }

    /**
     * Creates a {@code Class<T>} type.
     *
     * <p>Example:
     * <pre>{@code
     * TypeRef classOfString = Types.classOf(Types.STRING);  // Class<String>
     * TypeRef classOfWildcard = Types.classOf(Types.wildcard());  // Class<?>
     * }</pre>
     *
     * @param typeArgument the type argument
     * @return a parameterized Class type
     */
    public static ClassType classOf(TypeRef typeArgument) {
        return type("java.lang.Class", typeArgument);
    }

    /**
     * Creates a {@code Collection<T>} type.
     *
     * @param elementType the element type
     * @return a parameterized Collection type
     */
    public static ClassType collection(TypeRef elementType) {
        return type("java.util.Collection", boxIfPrimitive(elementType));
    }

    /**
     * Creates an {@code Iterable<T>} type.
     *
     * @param elementType the element type
     * @return a parameterized Iterable type
     */
    public static ClassType iterable(TypeRef elementType) {
        return type("java.lang.Iterable", boxIfPrimitive(elementType));
    }

    /**
     * Creates a {@code Supplier<T>} type.
     *
     * @param resultType the result type
     * @return a parameterized Supplier type
     */
    public static ClassType supplier(TypeRef resultType) {
        return type("java.util.function.Supplier", boxIfPrimitive(resultType));
    }

    /**
     * Creates a {@code Consumer<T>} type.
     *
     * @param argumentType the argument type
     * @return a parameterized Consumer type
     */
    public static ClassType consumer(TypeRef argumentType) {
        return type("java.util.function.Consumer", boxIfPrimitive(argumentType));
    }

    /**
     * Creates a {@code Function<T, R>} type.
     *
     * @param argumentType the argument type
     * @param resultType   the result type
     * @return a parameterized Function type
     */
    public static ClassType function(TypeRef argumentType, TypeRef resultType) {
        return type("java.util.function.Function", boxIfPrimitive(argumentType), boxIfPrimitive(resultType));
    }

    /**
     * Creates a {@code Predicate<T>} type.
     *
     * @param argumentType the argument type
     * @return a parameterized Predicate type
     */
    public static ClassType predicate(TypeRef argumentType) {
        return type("java.util.function.Predicate", boxIfPrimitive(argumentType));
    }

    // ========================================================================
    // Boxing Utilities
    // ========================================================================

    /**
     * Returns the boxed equivalent of a primitive type, or the original type if not primitive.
     *
     * <p>Example:
     * <pre>{@code
     * Types.boxIfPrimitive(Types.INT)     // Integer
     * Types.boxIfPrimitive(Types.STRING)  // String (unchanged)
     * }</pre>
     *
     * @param type the type to box if primitive
     * @return the boxed type or the original type
     */
    public static TypeRef boxIfPrimitive(TypeRef type) {
        if (type instanceof PrimitiveType primitive) {
            return box(primitive);
        }
        return type;
    }

    /**
     * Returns the boxed wrapper type for a primitive type.
     *
     * @param primitive the primitive type
     * @return the corresponding wrapper class type
     */
    public static ClassType box(PrimitiveType primitive) {
        return switch (primitive.kind()) {
            case BOOLEAN -> BOOLEAN_BOXED;
            case BYTE -> BYTE_BOXED;
            case CHAR -> CHAR_BOXED;
            case SHORT -> SHORT_BOXED;
            case INT -> INT_BOXED;
            case LONG -> LONG_BOXED;
            case FLOAT -> FLOAT_BOXED;
            case DOUBLE -> DOUBLE_BOXED;
        };
    }

    /**
     * Checks if the given type is a primitive type.
     *
     * @param type the type to check
     * @return {@code true} if the type is primitive
     */
    public static boolean isPrimitive(TypeRef type) {
        return type instanceof PrimitiveType;
    }

    /**
     * Checks if the given type is void.
     *
     * @param type the type to check
     * @return {@code true} if the type is void
     */
    public static boolean isVoid(TypeRef type) {
        return type instanceof VoidType;
    }

    /**
     * Checks if the given type is an array type.
     *
     * @param type the type to check
     * @return {@code true} if the type is an array
     */
    public static boolean isArray(TypeRef type) {
        return type instanceof ArrayType;
    }
}
