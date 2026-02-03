package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.stmt.Statement;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a class declaration in the IR.
 *
 * <p>A class declaration includes:
 * <ul>
 *   <li>Package name and class name</li>
 *   <li>Optional superclass (extends)</li>
 *   <li>Implemented interfaces</li>
 *   <li>Type parameters (generics)</li>
 *   <li>Members: fields, constructors, methods</li>
 *   <li>Nested types</li>
 *   <li>Modifiers (public, abstract, final, static for inner classes)</li>
 *   <li>Annotations</li>
 *   <li>JavaDoc comment</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * /**
 *  * Represents a person.
 *  *{@literal /}
 * public class Person implements Named {
 *     private String name;
 *
 *     public Person(String name) {
 *         this.name = name;
 *     }
 *
 *     public String getName() {
 *         return name;
 *     }
 * }
 *
 * // Abstract class
 * public abstract class AbstractEntity {
 *     public abstract String getId();
 * }
 *
 * // Generic class
 * public class Box<T> {
 *     private T value;
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // public class Person { ... }
 * ClassDecl person = new ClassDecl(
 *     "com.example",
 *     "Person",
 *     null,  // no superclass (extends Object)
 *     List.of(Types.type("com.example.Named")),  // implements
 *     List.of(),  // no type params
 *     List.of(field, constructor, method),
 *     List.of(),  // no nested types
 *     Set.of(Modifier.PUBLIC),
 *     List.of(),
 *     "Represents a person."
 * );
 *
 * // Using the DSL (recommended)
 * ClassDecl clazz = Class_.class_("Person")
 *     .in("com.example")
 *     .implements_(Types.type("com.example.Named"))
 *     .public_()
 *     .field(...)
 *     .constructor(...)
 *     .method(...)
 *     .javadoc("Represents a person.")
 *     .build();
 * }</pre>
 *
 * @param packageName    the package name; must not be null (empty for default package)
 * @param name           the class name; must not be null
 * @param superClass     the superclass type; null if extends Object
 * @param interfaces     interfaces this class implements; the list is copied
 * @param typeParameters generic type parameters; the list is copied
 * @param members        class members (fields, constructors, methods); the list is copied
 * @param nestedTypes    nested type declarations; the list is copied
 * @param modifiers      class modifiers; the set is copied
 * @param annotations    annotations on this class; the list is copied
 * @param javadoc        JavaDoc comment; null if none
 * @see io.alnovis.protowrapper.dsl.Class_
 * @since 2.4.0
 */
public record ClassDecl(
        String packageName,
        String name,
        TypeRef superClass,
        List<TypeRef> interfaces,
        List<TypeVariable> typeParameters,
        List<MemberDeclaration> members,
        List<TypeDeclaration> nestedTypes,
        Set<Modifier> modifiers,
        List<AnnotationSpec> annotations,
        String javadoc
) implements TypeDeclaration {

    /**
     * Creates a new ClassDecl with validation.
     *
     * @param packageName    the package name (must not be null)
     * @param name           the class name (must not be null)
     * @param superClass     the superclass (may be null)
     * @param interfaces     the implemented interfaces (may be null, treated as empty list)
     * @param typeParameters the type parameters (may be null, treated as empty list)
     * @param members        the members (may be null, treated as empty list)
     * @param nestedTypes    the nested types (may be null, treated as empty list)
     * @param modifiers      the modifiers (may be null, treated as empty set)
     * @param annotations    the annotations (may be null, treated as empty list)
     * @param javadoc        the JavaDoc comment (may be null)
     * @throws NullPointerException if packageName or name is null
     */
    public ClassDecl {
        Objects.requireNonNull(packageName, "packageName must not be null");
        Objects.requireNonNull(name, "name must not be null");
        interfaces = interfaces == null ? List.of() : List.copyOf(interfaces);
        typeParameters = typeParameters == null ? List.of() : List.copyOf(typeParameters);
        members = members == null ? List.of() : List.copyOf(members);
        nestedTypes = nestedTypes == null ? List.of() : List.copyOf(nestedTypes);
        modifiers = modifiers == null ? Set.of() : Set.copyOf(modifiers);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }

    /**
     * Returns {@code true} if this class has JavaDoc.
     *
     * @return {@code true} if javadoc is not null
     */
    public boolean hasJavadoc() {
        return javadoc != null;
    }

    /**
     * Returns {@code true} if this class has an explicit superclass.
     *
     * @return {@code true} if superClass is not null
     */
    public boolean hasSuperClass() {
        return superClass != null;
    }

    /**
     * Returns {@code true} if this class implements interfaces.
     *
     * @return {@code true} if interfaces is not empty
     */
    public boolean hasInterfaces() {
        return !interfaces.isEmpty();
    }

    /**
     * Returns {@code true} if this class has type parameters (is generic).
     *
     * @return {@code true} if typeParameters is not empty
     */
    public boolean hasTypeParameters() {
        return !typeParameters.isEmpty();
    }

    /**
     * Returns {@code true} if this class has nested types.
     *
     * @return {@code true} if nestedTypes is not empty
     */
    public boolean hasNestedTypes() {
        return !nestedTypes.isEmpty();
    }

    /**
     * Returns {@code true} if this class has members.
     *
     * @return {@code true} if members is not empty
     */
    public boolean hasMembers() {
        return !members.isEmpty();
    }

    /**
     * Returns {@code true} if this is an abstract class.
     *
     * @return {@code true} if modifiers contain ABSTRACT
     */
    public boolean isAbstract() {
        return modifiers.contains(Modifier.ABSTRACT);
    }

    /**
     * Returns {@code true} if this is a final class.
     *
     * @return {@code true} if modifiers contain FINAL
     */
    public boolean isFinal() {
        return modifiers.contains(Modifier.FINAL);
    }

    /**
     * Returns {@code true} if this class is public.
     *
     * @return {@code true} if modifiers contain PUBLIC
     */
    public boolean isPublic() {
        return modifiers.contains(Modifier.PUBLIC);
    }

    /**
     * Returns {@code true} if this is a static nested class.
     *
     * @return {@code true} if modifiers contain STATIC
     */
    public boolean isStatic() {
        return modifiers.contains(Modifier.STATIC);
    }
}
