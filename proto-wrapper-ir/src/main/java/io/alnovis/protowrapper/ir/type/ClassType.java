package io.alnovis.protowrapper.ir.type;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a class or interface type reference in the IR.
 *
 * <p>A ClassType consists of:
 * <ul>
 *   <li>A package name (may be empty for default package or nested classes)</li>
 *   <li>A simple name (the class name without package)</li>
 *   <li>Optional type arguments for parameterized types (generics)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Simple class type
 * ClassType stringType = ClassType.of("java.lang", "String");
 * ClassType stringType2 = ClassType.of("java.lang.String");
 *
 * // Parameterized type: List<String>
 * ClassType listOfString = ClassType.of("java.util", "List")
 *     .withTypeArguments(ClassType.of("java.lang.String"));
 *
 * // Nested parameterized type: Map<String, List<Integer>>
 * ClassType mapType = ClassType.of("java.util", "Map")
 *     .withTypeArguments(
 *         ClassType.of("java.lang.String"),
 *         ClassType.of("java.util.List")
 *             .withTypeArguments(ClassType.of("java.lang.Integer"))
 *     );
 *
 * // Using the Types DSL (recommended)
 * TypeRef listType = Types.list(Types.STRING);
 * TypeRef mapType = Types.map(Types.STRING, Types.INTEGER);
 * }</pre>
 *
 * <p>Nested classes should use the outer class name as part of the simple name,
 * separated by a dot (e.g., "Outer.Inner" for package.Outer.Inner).
 *
 * @param packageName   the package name, or empty string for default package;
 *                      must not be null
 * @param simpleName    the simple class name (without package); must not be null
 * @param typeArguments the type arguments for parameterized types; empty list if none;
 *                      the list is copied and made immutable
 * @see io.alnovis.protowrapper.dsl.Types
 * @since 2.4.0
 */
public record ClassType(
        String packageName,
        String simpleName,
        List<TypeRef> typeArguments
) implements TypeRef {

    /**
     * Creates a new ClassType with validation.
     *
     * @param packageName   the package name, must not be null
     * @param simpleName    the simple class name, must not be null or blank
     * @param typeArguments the type arguments, may be null (treated as empty list)
     * @throws NullPointerException     if packageName or simpleName is null
     * @throws IllegalArgumentException if simpleName is empty or blank
     */
    public ClassType {
        Objects.requireNonNull(packageName, "packageName must not be null");
        Objects.requireNonNull(simpleName, "simpleName must not be null");
        if (simpleName.isBlank()) {
            throw new IllegalArgumentException("simpleName must not be empty or blank");
        }
        typeArguments = typeArguments == null ? List.of() : List.copyOf(typeArguments);
    }

    /**
     * Creates a ClassType without type arguments.
     *
     * <p>Example:
     * <pre>{@code
     * ClassType stringType = ClassType.of("java.lang", "String");
     * ClassType myClass = ClassType.of("com.example", "MyClass");
     * }</pre>
     *
     * @param packageName the package name, or empty string for default package
     * @param simpleName  the simple class name
     * @return a new ClassType instance
     * @throws NullPointerException if any argument is null
     */
    public static ClassType of(String packageName, String simpleName) {
        return new ClassType(packageName, simpleName, List.of());
    }

    /**
     * Creates a ClassType from a fully qualified name.
     *
     * <p>The qualified name is split at the last dot to separate package from class name.
     * If there is no dot, the entire name is treated as the simple name with an empty package.
     *
     * <p>Example:
     * <pre>{@code
     * ClassType stringType = ClassType.of("java.lang.String");
     * // packageName = "java.lang", simpleName = "String"
     *
     * ClassType simpleType = ClassType.of("MyClass");
     * // packageName = "", simpleName = "MyClass"
     * }</pre>
     *
     * @param qualifiedName the fully qualified class name (e.g., "java.util.List")
     * @return a new ClassType instance
     * @throws NullPointerException if qualifiedName is null
     */
    public static ClassType of(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot < 0) {
            return new ClassType("", qualifiedName, List.of());
        }
        return new ClassType(
                qualifiedName.substring(0, lastDot),
                qualifiedName.substring(lastDot + 1),
                List.of()
        );
    }

    /**
     * Returns a new ClassType with the given type arguments.
     *
     * <p>This method is used to create parameterized types (generics).
     *
     * <p>Example:
     * <pre>{@code
     * // List<String>
     * ClassType listOfString = ClassType.of("java.util.List")
     *     .withTypeArguments(List.of(ClassType.of("java.lang.String")));
     * }</pre>
     *
     * @param typeArgs the type arguments to apply
     * @return a new ClassType with the specified type arguments
     * @throws NullPointerException if typeArgs is null
     */
    public ClassType withTypeArguments(List<TypeRef> typeArgs) {
        return new ClassType(packageName, simpleName, typeArgs);
    }

    /**
     * Returns a new ClassType with the given type arguments (varargs version).
     *
     * <p>Example:
     * <pre>{@code
     * // Map<String, Integer>
     * ClassType mapType = ClassType.of("java.util.Map")
     *     .withTypeArguments(
     *         ClassType.of("java.lang.String"),
     *         ClassType.of("java.lang.Integer")
     *     );
     * }</pre>
     *
     * @param typeArgs the type arguments to apply
     * @return a new ClassType with the specified type arguments
     */
    public ClassType withTypeArguments(TypeRef... typeArgs) {
        return withTypeArguments(List.of(typeArgs));
    }

    /**
     * Returns the fully qualified name of this class.
     *
     * <p>If the package is empty, returns just the simple name.
     * Otherwise, returns package + "." + simpleName.
     *
     * <p>Example:
     * <pre>{@code
     * ClassType.of("java.util", "List").qualifiedName() // "java.util.List"
     * ClassType.of("", "MyClass").qualifiedName()       // "MyClass"
     * }</pre>
     *
     * @return the fully qualified name
     */
    public String qualifiedName() {
        if (packageName.isEmpty()) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }

    /**
     * Returns {@code true} if this type is in the {@code java.lang} package.
     *
     * <p>Types in java.lang do not require imports in Java source code.
     *
     * @return {@code true} if this is a java.lang type
     */
    public boolean isJavaLang() {
        return "java.lang".equals(packageName);
    }

    /**
     * Returns {@code true} if this type has type arguments (is a parameterized type).
     *
     * <p>Example:
     * <pre>{@code
     * ClassType.of("java.lang.String").isParameterized()           // false
     * ClassType.of("java.util.List")
     *     .withTypeArguments(Types.STRING).isParameterized()       // true
     * }</pre>
     *
     * @return {@code true} if this type has type arguments
     */
    public boolean isParameterized() {
        return !typeArguments.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the qualified name, optionally with type arguments in angle brackets.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code java.lang.String}</li>
     *   <li>{@code java.util.List<java.lang.String>}</li>
     *   <li>{@code java.util.Map<java.lang.String, java.lang.Integer>}</li>
     * </ul>
     *
     * @return the debug string representation
     */
    @Override
    public String toDebugString() {
        if (typeArguments.isEmpty()) {
            return qualifiedName();
        }
        String args = typeArguments.stream()
                .map(TypeRef::toDebugString)
                .collect(Collectors.joining(", "));
        return qualifiedName() + "<" + args + ">";
    }
}
