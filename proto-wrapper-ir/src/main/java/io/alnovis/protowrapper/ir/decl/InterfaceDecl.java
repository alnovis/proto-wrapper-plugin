package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.stmt.Statement;

import java.util.List;
import java.util.Objects;

/**
 * Represents an interface declaration in the IR.
 *
 * <p>An interface declaration includes:
 * <ul>
 *   <li>Package name and interface name</li>
 *   <li>Extended interfaces (super interfaces)</li>
 *   <li>Type parameters (generics)</li>
 *   <li>Members: abstract methods, default methods, static methods, constants</li>
 *   <li>Nested types</li>
 *   <li>Annotations</li>
 *   <li>JavaDoc comment</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * /**
 *  * Represents a named entity.
 *  *{@literal /}
 * public interface Named {
 *     /**
 *      * Returns the name.
 *      *{@literal /}
 *     String getName();
 *
 *     /**
 *      * Returns a display name.
 *      *{@literal /}
 *     default String getDisplayName() {
 *         return getName().toUpperCase();
 *     }
 * }
 *
 * // Generic interface
 * public interface Container<T> {
 *     T get();
 *     void set(T value);
 * }
 *
 * // Interface extending another
 * public interface NamedEntity extends Named, Identifiable { }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // public interface Named { String getName(); }
 * InterfaceDecl named = new InterfaceDecl(
 *     "com.example",
 *     "Named",
 *     List.of(),  // no super interfaces
 *     List.of(),  // no type params
 *     List.of(Method.abstractMethod("getName").returns(Types.STRING).build()),
 *     List.of(),  // no nested types
 *     List.of(),  // no annotations
 *     "Represents a named entity."
 * );
 *
 * // Using the DSL (recommended)
 * InterfaceDecl iface = Interface_.interface_("Named")
 *     .in("com.example")
 *     .method(Method.abstractMethod("getName").returns(Types.STRING).build())
 *     .javadoc("Represents a named entity.")
 *     .build();
 * }</pre>
 *
 * @param packageName     the package name; must not be null (empty for default package)
 * @param name            the interface name; must not be null
 * @param superInterfaces interfaces this interface extends; the list is copied
 * @param typeParameters  generic type parameters; the list is copied
 * @param members         interface members (methods, constants); the list is copied
 * @param nestedTypes     nested type declarations; the list is copied
 * @param annotations     annotations on this interface; the list is copied
 * @param javadoc         JavaDoc comment; null if none
 * @see io.alnovis.protowrapper.dsl.Interface_
 * @since 2.4.0
 */
public record InterfaceDecl(
        String packageName,
        String name,
        List<TypeRef> superInterfaces,
        List<TypeVariable> typeParameters,
        List<MemberDeclaration> members,
        List<TypeDeclaration> nestedTypes,
        List<AnnotationSpec> annotations,
        String javadoc
) implements TypeDeclaration {

    /**
     * Creates a new InterfaceDecl with validation.
     *
     * @param packageName     the package name (must not be null)
     * @param name            the interface name (must not be null)
     * @param superInterfaces the super interfaces (may be null, treated as empty list)
     * @param typeParameters  the type parameters (may be null, treated as empty list)
     * @param members         the members (may be null, treated as empty list)
     * @param nestedTypes     the nested types (may be null, treated as empty list)
     * @param annotations     the annotations (may be null, treated as empty list)
     * @param javadoc         the JavaDoc comment (may be null)
     * @throws NullPointerException if packageName or name is null
     */
    public InterfaceDecl {
        Objects.requireNonNull(packageName, "packageName must not be null");
        Objects.requireNonNull(name, "name must not be null");
        superInterfaces = superInterfaces == null ? List.of() : List.copyOf(superInterfaces);
        typeParameters = typeParameters == null ? List.of() : List.copyOf(typeParameters);
        members = members == null ? List.of() : List.copyOf(members);
        nestedTypes = nestedTypes == null ? List.of() : List.copyOf(nestedTypes);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }

    /**
     * Returns {@code true} if this interface has JavaDoc.
     *
     * @return {@code true} if javadoc is not null
     */
    public boolean hasJavadoc() {
        return javadoc != null;
    }

    /**
     * Returns {@code true} if this interface extends other interfaces.
     *
     * @return {@code true} if superInterfaces is not empty
     */
    public boolean hasSupertypes() {
        return !superInterfaces.isEmpty();
    }

    /**
     * Returns {@code true} if this interface has type parameters (is generic).
     *
     * @return {@code true} if typeParameters is not empty
     */
    public boolean hasTypeParameters() {
        return !typeParameters.isEmpty();
    }

    /**
     * Returns {@code true} if this interface has nested types.
     *
     * @return {@code true} if nestedTypes is not empty
     */
    public boolean hasNestedTypes() {
        return !nestedTypes.isEmpty();
    }

    /**
     * Returns {@code true} if this interface has members.
     *
     * @return {@code true} if members is not empty
     */
    public boolean hasMembers() {
        return !members.isEmpty();
    }
}
