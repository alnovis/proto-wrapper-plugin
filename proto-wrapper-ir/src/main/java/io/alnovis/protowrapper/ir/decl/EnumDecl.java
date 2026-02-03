package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.type.TypeRef;

import java.util.List;
import java.util.Objects;

/**
 * Represents an enum declaration in the IR.
 *
 * <p>An enum declaration includes:
 * <ul>
 *   <li>Package name and enum name</li>
 *   <li>Enum constants (the named instances)</li>
 *   <li>Implemented interfaces</li>
 *   <li>Members: fields, constructors, methods</li>
 *   <li>Annotations</li>
 *   <li>JavaDoc comment</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * /**
 *  * Represents the status of an operation.
 *  *{@literal /}
 * public enum Status {
 *     PENDING,
 *     ACTIVE,
 *     COMPLETED
 * }
 *
 * // Enum with fields and methods
 * public enum Planet {
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
 *
 *     public double getMass() { return mass; }
 *     public double getRadius() { return radius; }
 * }
 *
 * // Enum implementing interface
 * public enum Operation implements BinaryOperator<Integer> {
 *     ADD { ... },
 *     SUBTRACT { ... };
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // public enum Status { PENDING, ACTIVE, COMPLETED }
 * EnumDecl status = new EnumDecl(
 *     "com.example",
 *     "Status",
 *     List.of(
 *         EnumConstant.of("PENDING"),
 *         EnumConstant.of("ACTIVE"),
 *         EnumConstant.of("COMPLETED")
 *     ),
 *     List.of(),  // no interfaces
 *     List.of(),  // no members
 *     List.of(),  // no annotations
 *     "Represents the status of an operation."
 * );
 *
 * // Using the DSL (recommended)
 * EnumDecl enumDecl = Enum_.enum_("Status")
 *     .in("com.example")
 *     .constant("PENDING")
 *     .constant("ACTIVE")
 *     .constant("COMPLETED")
 *     .javadoc("Represents the status of an operation.")
 *     .build();
 * }</pre>
 *
 * @param packageName the package name; must not be null (empty for default package)
 * @param name        the enum name; must not be null
 * @param constants   the enum constants; the list is copied
 * @param interfaces  interfaces this enum implements; the list is copied
 * @param members     enum members (fields, constructors, methods); the list is copied
 * @param annotations annotations on this enum; the list is copied
 * @param javadoc     JavaDoc comment; null if none
 * @see EnumConstant
 * @see io.alnovis.protowrapper.dsl.Enum_
 * @since 2.4.0
 */
public record EnumDecl(
        String packageName,
        String name,
        List<EnumConstant> constants,
        List<TypeRef> interfaces,
        List<MemberDeclaration> members,
        List<AnnotationSpec> annotations,
        String javadoc
) implements TypeDeclaration {

    /**
     * Creates a new EnumDecl with validation.
     *
     * @param packageName the package name (must not be null)
     * @param name        the enum name (must not be null)
     * @param constants   the enum constants (may be null, treated as empty list)
     * @param interfaces  the implemented interfaces (may be null, treated as empty list)
     * @param members     the members (may be null, treated as empty list)
     * @param annotations the annotations (may be null, treated as empty list)
     * @param javadoc     the JavaDoc comment (may be null)
     * @throws NullPointerException if packageName or name is null
     */
    public EnumDecl {
        Objects.requireNonNull(packageName, "packageName must not be null");
        Objects.requireNonNull(name, "name must not be null");
        constants = constants == null ? List.of() : List.copyOf(constants);
        interfaces = interfaces == null ? List.of() : List.copyOf(interfaces);
        members = members == null ? List.of() : List.copyOf(members);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }

    /**
     * Returns {@code true} if this enum has JavaDoc.
     *
     * @return {@code true} if javadoc is not null
     */
    public boolean hasJavadoc() {
        return javadoc != null;
    }

    /**
     * Returns {@code true} if this enum has constants.
     *
     * @return {@code true} if constants is not empty
     */
    public boolean hasConstants() {
        return !constants.isEmpty();
    }

    /**
     * Returns {@code true} if this enum implements interfaces.
     *
     * @return {@code true} if interfaces is not empty
     */
    public boolean hasInterfaces() {
        return !interfaces.isEmpty();
    }

    /**
     * Returns {@code true} if this enum has members (fields, methods).
     *
     * @return {@code true} if members is not empty
     */
    public boolean hasMembers() {
        return !members.isEmpty();
    }

    /**
     * Returns the number of constants in this enum.
     *
     * @return the constant count
     */
    public int constantCount() {
        return constants.size();
    }
}
