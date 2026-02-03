package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.decl.AnnotationSpec;
import io.alnovis.protowrapper.ir.decl.ConstructorDecl;
import io.alnovis.protowrapper.ir.decl.Modifier;
import io.alnovis.protowrapper.ir.decl.ParameterDecl;
import io.alnovis.protowrapper.ir.stmt.Statement;
import io.alnovis.protowrapper.ir.type.TypeRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Builder for creating constructor declarations.
 *
 * <p>This builder provides a fluent API for constructing {@link ConstructorDecl}
 * instances. It supports all constructor features including modifiers, parameters,
 * body, exceptions, annotations, and JavaDoc.
 *
 * <h2>Simple Constructor</h2>
 * <pre>{@code
 * ConstructorDecl ctor = Constructor.constructor()
 *     .public_()
 *     .body(Stmt.emptyBlock())
 *     .build();
 * // public MyClass() { }
 * }</pre>
 *
 * <h2>Constructor with Parameters</h2>
 * <pre>{@code
 * ConstructorDecl ctor = Constructor.constructor()
 *     .public_()
 *     .parameter(Types.STRING, "name")
 *     .parameter(Types.INT, "age")
 *     .body(
 *         Stmt.assign(Expr.field("name"), Expr.var("name")),
 *         Stmt.assign(Expr.field("age"), Expr.var("age")))
 *     .build();
 * // public MyClass(String name, int age) {
 * //     this.name = name;
 * //     this.age = age;
 * // }
 * }</pre>
 *
 * <h2>Private Constructor (Singleton)</h2>
 * <pre>{@code
 * ConstructorDecl ctor = Constructor.constructor()
 *     .private_()
 *     .body(Stmt.emptyBlock())
 *     .build();
 * // private MyClass() { }
 * }</pre>
 *
 * <h2>Constructor with Exceptions</h2>
 * <pre>{@code
 * ConstructorDecl ctor = Constructor.constructor()
 *     .public_()
 *     .parameter(Types.STRING, "path")
 *     .throws_(Types.type("java.io.IOException"))
 *     .body(...)
 *     .build();
 * // public MyClass(String path) throws IOException { ... }
 * }</pre>
 *
 * <h2>Constructor with Validation</h2>
 * <pre>{@code
 * ConstructorDecl ctor = Constructor.constructor()
 *     .public_()
 *     .parameter(Types.STRING, "name")
 *     .body(
 *         Stmt.requireNonNull(Expr.var("name"), "name"),
 *         Stmt.assign(Expr.field("name"), Expr.var("name")))
 *     .build();
 * // public MyClass(String name) {
 * //     if (name == null) throw new NullPointerException("name must not be null");
 * //     this.name = name;
 * // }
 * }</pre>
 *
 * @see ConstructorDecl
 * @see ParameterDecl
 * @since 2.4.0
 */
public final class Constructor {

    private final Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
    private final List<ParameterDecl> parameters = new ArrayList<>();
    private Statement body;
    private final List<TypeRef> thrownExceptions = new ArrayList<>();
    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private String javadoc;

    private Constructor() {
    }

    /**
     * Creates a new constructor builder.
     *
     * @return a new Constructor builder
     */
    public static Constructor constructor() {
        return new Constructor();
    }

    // ========================================================================
    // Modifiers
    // ========================================================================

    /**
     * Adds the {@code public} modifier.
     *
     * @return this builder
     */
    public Constructor public_() {
        modifiers.add(Modifier.PUBLIC);
        return this;
    }

    /**
     * Adds the {@code protected} modifier.
     *
     * @return this builder
     */
    public Constructor protected_() {
        modifiers.add(Modifier.PROTECTED);
        return this;
    }

    /**
     * Adds the {@code private} modifier.
     *
     * @return this builder
     */
    public Constructor private_() {
        modifiers.add(Modifier.PRIVATE);
        return this;
    }

    /**
     * Adds the specified modifier.
     *
     * @param modifier the modifier to add
     * @return this builder
     */
    public Constructor modifier(Modifier modifier) {
        modifiers.add(modifier);
        return this;
    }

    // ========================================================================
    // Parameters
    // ========================================================================

    /**
     * Adds a parameter.
     *
     * @param type the parameter type
     * @param name the parameter name
     * @return this builder
     */
    public Constructor parameter(TypeRef type, String name) {
        parameters.add(new ParameterDecl(name, type, List.of(), false));
        return this;
    }

    /**
     * Adds a varargs parameter.
     *
     * <p>Note: Only the last parameter can be varargs.
     *
     * @param type the parameter component type
     * @param name the parameter name
     * @return this builder
     */
    public Constructor varargs(TypeRef type, String name) {
        parameters.add(new ParameterDecl(name, type, List.of(), true));
        return this;
    }

    /**
     * Adds a parameter with annotations.
     *
     * @param type        the parameter type
     * @param name        the parameter name
     * @param annotations the parameter annotations
     * @return this builder
     */
    public Constructor parameter(TypeRef type, String name, List<AnnotationSpec> annotations) {
        parameters.add(new ParameterDecl(name, type, annotations, false));
        return this;
    }

    /**
     * Adds a fully-configured parameter.
     *
     * @param parameter the parameter declaration
     * @return this builder
     */
    public Constructor parameter(ParameterDecl parameter) {
        parameters.add(parameter);
        return this;
    }

    /**
     * Adds multiple parameters.
     *
     * @param parameters the parameters
     * @return this builder
     */
    public Constructor parameters(ParameterDecl... parameters) {
        Collections.addAll(this.parameters, parameters);
        return this;
    }

    // ========================================================================
    // Body
    // ========================================================================

    /**
     * Sets the constructor body.
     *
     * @param body the constructor body
     * @return this builder
     */
    public Constructor body(Statement body) {
        this.body = body;
        return this;
    }

    /**
     * Sets the constructor body to multiple statements wrapped in a block.
     *
     * @param statements the statements
     * @return this builder
     */
    public Constructor body(Statement... statements) {
        this.body = Stmt.block(statements);
        return this;
    }

    // ========================================================================
    // Exceptions
    // ========================================================================

    /**
     * Adds a thrown exception type.
     *
     * @param exceptionType the exception type
     * @return this builder
     */
    public Constructor throws_(TypeRef exceptionType) {
        thrownExceptions.add(exceptionType);
        return this;
    }

    /**
     * Adds multiple thrown exception types.
     *
     * @param exceptionTypes the exception types
     * @return this builder
     */
    public Constructor throws_(TypeRef... exceptionTypes) {
        Collections.addAll(thrownExceptions, exceptionTypes);
        return this;
    }

    // ========================================================================
    // Annotations
    // ========================================================================

    /**
     * Adds an annotation.
     *
     * @param annotation the annotation
     * @return this builder
     */
    public Constructor annotation(AnnotationSpec annotation) {
        annotations.add(annotation);
        return this;
    }

    /**
     * Adds a simple marker annotation (no members).
     *
     * @param annotationType the annotation type
     * @return this builder
     */
    public Constructor annotation(TypeRef annotationType) {
        annotations.add(AnnotationSpec.of(annotationType));
        return this;
    }

    /**
     * Adds a @Deprecated annotation.
     *
     * @return this builder
     */
    public Constructor deprecated() {
        return annotation(Types.type("java.lang.Deprecated"));
    }

    // ========================================================================
    // JavaDoc
    // ========================================================================

    /**
     * Sets the JavaDoc comment.
     *
     * @param javadoc the JavaDoc comment
     * @return this builder
     */
    public Constructor javadoc(String javadoc) {
        this.javadoc = javadoc;
        return this;
    }

    // ========================================================================
    // Build
    // ========================================================================

    /**
     * Builds the constructor declaration.
     *
     * @return a new ConstructorDecl
     */
    public ConstructorDecl build() {
        return new ConstructorDecl(
            parameters,
            body != null ? List.of(body) : List.of(),
            modifiers,
            annotations,
            javadoc,
            thrownExceptions
        );
    }
}
