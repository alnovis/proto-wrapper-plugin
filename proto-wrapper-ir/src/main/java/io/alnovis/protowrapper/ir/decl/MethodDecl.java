package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.type.TypeRef;
import io.alnovis.protowrapper.ir.type.TypeVariable;
import io.alnovis.protowrapper.ir.type.VoidType;
import io.alnovis.protowrapper.ir.stmt.Statement;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a method declaration in the IR.
 *
 * <p>A method declaration includes:
 * <ul>
 *   <li>Name and return type</li>
 *   <li>Parameters with types</li>
 *   <li>Optional type parameters (generics)</li>
 *   <li>Body (statements), or empty for abstract methods</li>
 *   <li>Modifiers (public, abstract, static, final, default, etc.)</li>
 *   <li>Annotations</li>
 *   <li>JavaDoc comment</li>
 *   <li>Thrown exception types</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Simple method
 * public String getName() {
 *     return name;
 * }
 *
 * // Method with parameters
 * public void setName(String name) {
 *     this.name = name;
 * }
 *
 * // Abstract method
 * public abstract void process();
 *
 * // Interface default method
 * default String getDisplayName() {
 *     return getName().toUpperCase();
 * }
 *
 * // Generic method
 * public <T> List<T> filter(List<T> items, Predicate<T> predicate) { ... }
 *
 * // Method with throws
 * public void read() throws IOException { ... }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // public String getName() { return name; }
 * MethodDecl getter = new MethodDecl(
 *     "getName",
 *     Types.STRING,
 *     List.of(),  // no params
 *     List.of(),  // no type params
 *     List.of(Stmt.return_(Expr.field(Expr.this_(), "name"))),
 *     Set.of(Modifier.PUBLIC),
 *     List.of(),
 *     "Returns the name.",
 *     false, false,
 *     List.of()
 * );
 *
 * // Using the DSL (recommended)
 * MethodDecl method = Method.method("getName")
 *     .returns(Types.STRING)
 *     .public_()
 *     .body(Stmt.return_(Expr.field(Expr.this_(), "name")))
 *     .javadoc("Returns the name.")
 *     .build();
 * }</pre>
 *
 * @param name           the method name; must not be null
 * @param returnType     the return type; must not be null (use VoidType for void)
 * @param parameters     the method parameters; the list is copied and made immutable
 * @param typeParameters generic type parameters; the list is copied and made immutable
 * @param body           the method body statements; empty for abstract; the list is copied
 * @param modifiers      the method modifiers; the set is copied and made immutable
 * @param annotations    annotations on this method; the list is copied and made immutable
 * @param javadoc        JavaDoc comment; null if none
 * @param isAbstract     {@code true} if this is an abstract method (body should be empty)
 * @param isDefault      {@code true} if this is an interface default method
 * @param throwsTypes    exception types declared in throws clause; the list is copied
 * @see io.alnovis.protowrapper.dsl.Method
 * @since 2.4.0
 */
public record MethodDecl(
        String name,
        TypeRef returnType,
        List<ParameterDecl> parameters,
        List<TypeVariable> typeParameters,
        List<Statement> body,
        Set<Modifier> modifiers,
        List<AnnotationSpec> annotations,
        String javadoc,
        boolean isAbstract,
        boolean isDefault,
        List<TypeRef> throwsTypes
) implements MemberDeclaration {

    /**
     * Creates a new MethodDecl with validation.
     *
     * @param name           the method name
     * @param returnType     the return type
     * @param parameters     the parameters (may be null, treated as empty list)
     * @param typeParameters the type parameters (may be null, treated as empty list)
     * @param body           the body statements (may be null, treated as empty list)
     * @param modifiers      the modifiers (may be null, treated as empty set)
     * @param annotations    the annotations (may be null, treated as empty list)
     * @param javadoc        the JavaDoc comment (may be null)
     * @param isAbstract     whether the method is abstract
     * @param isDefault      whether the method is a default interface method
     * @param throwsTypes    the thrown exception types (may be null, treated as empty list)
     * @throws NullPointerException if name or returnType is null
     */
    public MethodDecl {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty or blank");
        }
        Objects.requireNonNull(returnType, "returnType must not be null");
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        typeParameters = typeParameters == null ? List.of() : List.copyOf(typeParameters);
        body = body == null ? List.of() : List.copyOf(body);
        modifiers = modifiers == null ? Set.of() : Set.copyOf(modifiers);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
        throwsTypes = throwsTypes == null ? List.of() : List.copyOf(throwsTypes);
    }

    /**
     * Returns {@code true} if this method has JavaDoc.
     *
     * @return {@code true} if javadoc is not null
     */
    public boolean hasJavadoc() {
        return javadoc != null;
    }

    /**
     * Returns {@code true} if this method returns void.
     *
     * @return {@code true} if returnType is VoidType
     */
    public boolean returnsVoid() {
        return returnType instanceof VoidType;
    }

    /**
     * Returns {@code true} if this method has parameters.
     *
     * @return {@code true} if parameters is not empty
     */
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    /**
     * Returns {@code true} if this method has type parameters (is generic).
     *
     * @return {@code true} if typeParameters is not empty
     */
    public boolean hasTypeParameters() {
        return !typeParameters.isEmpty();
    }

    /**
     * Returns {@code true} if this method declares thrown exceptions.
     *
     * @return {@code true} if throwsTypes is not empty
     */
    public boolean hasThrows() {
        return !throwsTypes.isEmpty();
    }

    /**
     * Returns {@code true} if this method is static.
     *
     * @return {@code true} if modifiers contain STATIC
     */
    public boolean isStatic() {
        return modifiers.contains(Modifier.STATIC);
    }

    /**
     * Returns {@code true} if this method is final.
     *
     * @return {@code true} if modifiers contain FINAL
     */
    public boolean isFinal() {
        return modifiers.contains(Modifier.FINAL);
    }

    /**
     * Returns {@code true} if this method is public.
     *
     * @return {@code true} if modifiers contain PUBLIC
     */
    public boolean isPublic() {
        return modifiers.contains(Modifier.PUBLIC);
    }

    /**
     * Returns the number of parameters.
     *
     * @return the parameter count
     */
    public int parameterCount() {
        return parameters.size();
    }
}
