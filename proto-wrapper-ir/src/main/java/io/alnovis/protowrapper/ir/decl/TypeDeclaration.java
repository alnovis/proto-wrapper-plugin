package io.alnovis.protowrapper.ir.decl;

/**
 * Represents a type declaration (class, interface, or enum) in the IR.
 *
 * <p>A type declaration defines a new type with its members, modifiers,
 * superclass/interfaces, and nested types.
 *
 * <p>The IR supports the following type declaration kinds:
 * <ul>
 *   <li>{@link InterfaceDecl} - interface declarations</li>
 *   <li>{@link ClassDecl} - class declarations (including abstract classes)</li>
 *   <li>{@link EnumDecl} - enum declarations</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Interface
 * public interface Named {
 *     String getName();
 * }
 *
 * // Class
 * public class Person implements Named {
 *     private String name;
 *     public String getName() { return name; }
 * }
 *
 * // Enum
 * public enum Status { PENDING, ACTIVE, COMPLETED }
 *
 * // Nested type
 * public class Outer {
 *     public static class Inner { }
 * }
 * }</pre>
 *
 * <p>Example usage with DSL:
 * <pre>{@code
 * // Interface
 * InterfaceDecl iface = Interface_.interface_("Named")
 *     .in("com.example")
 *     .method(Method.abstractMethod("getName").returns(Types.STRING).build())
 *     .build();
 *
 * // Class
 * ClassDecl clazz = Class_.class_("Person")
 *     .in("com.example")
 *     .implements_(Types.type("com.example.Named"))
 *     .field(...)
 *     .method(...)
 *     .build();
 *
 * // Enum
 * EnumDecl enumDecl = Enum_.enum_("Status")
 *     .in("com.example")
 *     .constant("PENDING")
 *     .constant("ACTIVE")
 *     .constant("COMPLETED")
 *     .build();
 * }</pre>
 *
 * @see InterfaceDecl
 * @see ClassDecl
 * @see EnumDecl
 * @since 2.4.0
 */
public sealed interface TypeDeclaration permits
        InterfaceDecl,
        ClassDecl,
        EnumDecl {

    /**
     * Returns the package name of this type.
     *
     * @return the package name, or empty string for default package
     */
    String packageName();

    /**
     * Returns the simple name of this type.
     *
     * @return the type name without package
     */
    String name();

    /**
     * Returns the fully qualified name of this type.
     *
     * @return packageName + "." + name, or just name if package is empty
     */
    default String qualifiedName() {
        if (packageName().isEmpty()) {
            return name();
        }
        return packageName() + "." + name();
    }
}
