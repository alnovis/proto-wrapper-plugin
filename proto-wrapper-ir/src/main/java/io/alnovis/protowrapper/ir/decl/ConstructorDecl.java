package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.stmt.Statement;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a constructor declaration in the IR.
 *
 * <p>A constructor declaration includes:
 * <ul>
 *   <li>Parameters with types</li>
 *   <li>Body (statements)</li>
 *   <li>Modifiers (public, protected, private)</li>
 *   <li>Annotations</li>
 *   <li>JavaDoc comment</li>
 *   <li>Thrown exception types</li>
 * </ul>
 *
 * <p>Note: The constructor name is not stored; it is always the class name.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Simple constructor
 * public Person(String name) {
 *     this.name = name;
 * }
 *
 * // Constructor with throws
 * public FileProcessor(String path) throws IOException {
 *     this.file = new File(path);
 * }
 *
 * // Private constructor (singleton pattern)
 * private Singleton() { }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // public Person(String name) { this.name = name; }
 * ConstructorDecl ctor = new ConstructorDecl(
 *     List.of(ParameterDecl.of("name", Types.STRING)),
 *     List.of(Stmt.assign(
 *         Expr.field(Expr.this_(), "name"),
 *         Expr.var("name")
 *     )),
 *     Set.of(Modifier.PUBLIC),
 *     List.of(),
 *     "Creates a new Person with the given name.",
 *     List.of()
 * );
 *
 * // Using the DSL (recommended)
 * ConstructorDecl ctor = Constructor.constructor()
 *     .public_()
 *     .param("name", Types.STRING)
 *     .body(Stmt.assign(
 *         Expr.field(Expr.this_(), "name"),
 *         Expr.var("name")
 *     ))
 *     .javadoc("Creates a new Person.")
 *     .build();
 * }</pre>
 *
 * @param parameters  the constructor parameters; the list is copied and made immutable
 * @param body        the constructor body statements; the list is copied
 * @param modifiers   the constructor modifiers; the set is copied and made immutable
 * @param annotations annotations on this constructor; the list is copied and made immutable
 * @param javadoc     JavaDoc comment; null if none
 * @param throwsTypes exception types declared in throws clause; the list is copied
 * @see io.alnovis.protowrapper.dsl.Constructor
 * @since 2.4.0
 */
public record ConstructorDecl(
        List<ParameterDecl> parameters,
        List<Statement> body,
        Set<Modifier> modifiers,
        List<AnnotationSpec> annotations,
        String javadoc,
        List<TypeRef> throwsTypes
) implements MemberDeclaration {

    /**
     * Creates a new ConstructorDecl with validation.
     *
     * @param parameters  the parameters (may be null, treated as empty list)
     * @param body        the body statements; must not be null
     * @param modifiers   the modifiers (may be null, treated as empty set)
     * @param annotations the annotations (may be null, treated as empty list)
     * @param javadoc     the JavaDoc comment (may be null)
     * @param throwsTypes the thrown exception types (may be null, treated as empty list)
     * @throws NullPointerException if body is null
     */
    public ConstructorDecl {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        Objects.requireNonNull(body, "body must not be null");
        body = List.copyOf(body);
        modifiers = modifiers == null ? Set.of() : Set.copyOf(modifiers);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
        throwsTypes = throwsTypes == null ? List.of() : List.copyOf(throwsTypes);
    }

    /**
     * Returns {@code true} if this constructor has JavaDoc.
     *
     * @return {@code true} if javadoc is not null
     */
    public boolean hasJavadoc() {
        return javadoc != null;
    }

    /**
     * Returns {@code true} if this constructor has parameters.
     *
     * @return {@code true} if parameters is not empty
     */
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    /**
     * Returns {@code true} if this constructor declares thrown exceptions.
     *
     * @return {@code true} if throwsTypes is not empty
     */
    public boolean hasThrows() {
        return !throwsTypes.isEmpty();
    }

    /**
     * Returns {@code true} if this constructor is public.
     *
     * @return {@code true} if modifiers contain PUBLIC
     */
    public boolean isPublic() {
        return modifiers.contains(Modifier.PUBLIC);
    }

    /**
     * Returns {@code true} if this constructor is private.
     *
     * @return {@code true} if modifiers contain PRIVATE
     */
    public boolean isPrivate() {
        return modifiers.contains(Modifier.PRIVATE);
    }

    /**
     * Returns {@code true} if this is a no-argument constructor.
     *
     * @return {@code true} if parameters is empty
     */
    public boolean isNoArg() {
        return parameters.isEmpty();
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
