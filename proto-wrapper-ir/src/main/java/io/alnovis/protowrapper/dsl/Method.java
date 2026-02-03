package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.decl.AnnotationSpec;
import io.alnovis.protowrapper.ir.decl.MethodDecl;
import io.alnovis.protowrapper.ir.decl.Modifier;
import io.alnovis.protowrapper.ir.decl.ParameterDecl;
import io.alnovis.protowrapper.ir.stmt.Statement;
import io.alnovis.protowrapper.ir.type.TypeRef;
import io.alnovis.protowrapper.ir.type.TypeVariable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builder for creating method declarations.
 *
 * <p>This builder provides a fluent API for constructing {@link MethodDecl}
 * instances. It supports all method features including modifiers, type parameters,
 * parameters, return types, exceptions, annotations, and JavaDoc.
 *
 * <h2>Simple Method</h2>
 * <pre>{@code
 * MethodDecl getter = Method.method("getName")
 *     .public_()
 *     .returns(Types.STRING)
 *     .body(Stmt.return_(Expr.field("name")))
 *     .build();
 * // public String getName() { return this.name; }
 * }</pre>
 *
 * <h2>Abstract Method</h2>
 * <pre>{@code
 * MethodDecl abstractMethod = Method.abstractMethod("process")
 *     .public_()
 *     .returns(Types.VOID)
 *     .parameter(Types.STRING, "input")
 *     .build();
 * // public abstract void process(String input);
 * }</pre>
 *
 * <h2>Static Method</h2>
 * <pre>{@code
 * MethodDecl staticMethod = Method.method("create")
 *     .public_()
 *     .static_()
 *     .returns(Types.type("com.example.Person"))
 *     .parameter(Types.STRING, "name")
 *     .body(Stmt.return_(Expr.new_(Types.type("com.example.Person"), Expr.var("name"))))
 *     .build();
 * // public static Person create(String name) { return new Person(name); }
 * }</pre>
 *
 * <h2>Generic Method</h2>
 * <pre>{@code
 * MethodDecl genericMethod = Method.method("transform")
 *     .public_()
 *     .typeParameter(Types.typeVar("T"))
 *     .typeParameter(Types.typeVar("R"))
 *     .returns(Types.typeVar("R"))
 *     .parameter(Types.typeVar("T"), "input")
 *     .parameter(Types.function(Types.typeVar("T"), Types.typeVar("R")), "transformer")
 *     .body(...)
 *     .build();
 * // public <T, R> R transform(T input, Function<T, R> transformer) { ... }
 * }</pre>
 *
 * <h2>Default Interface Method</h2>
 * <pre>{@code
 * MethodDecl defaultMethod = Method.defaultMethod("getDisplayName")
 *     .returns(Types.STRING)
 *     .body(Stmt.return_(Expr.call("getName")))
 *     .build();
 * // default String getDisplayName() { return getName(); }
 * }</pre>
 *
 * <h2>Method with Exceptions</h2>
 * <pre>{@code
 * MethodDecl method = Method.method("readFile")
 *     .public_()
 *     .returns(Types.STRING)
 *     .parameter(Types.STRING, "path")
 *     .throws_(Types.type("java.io.IOException"))
 *     .body(...)
 *     .build();
 * // public String readFile(String path) throws IOException { ... }
 * }</pre>
 *
 * @see MethodDecl
 * @see ParameterDecl
 * @since 2.4.0
 */
public final class Method {

    private final String name;
    private final Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
    private final List<TypeVariable> typeParameters = new ArrayList<>();
    private final List<ParameterDecl> parameters = new ArrayList<>();
    private TypeRef returnType = Types.VOID;
    private Statement body;
    private final List<TypeRef> thrownExceptions = new ArrayList<>();
    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private String javadoc;
    private boolean isAbstract;
    private boolean isDefault;

    private Method(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Creates a new method builder with the given name.
     *
     * @param name the method name
     * @return a new Method builder
     */
    public static Method method(String name) {
        return new Method(name);
    }

    /**
     * Creates a new abstract method builder with the given name.
     *
     * <p>Abstract methods have no body and are implicitly public when in interfaces.
     *
     * @param name the method name
     * @return a new Method builder configured for abstract method
     */
    public static Method abstractMethod(String name) {
        Method builder = new Method(name);
        builder.isAbstract = true;
        return builder;
    }

    /**
     * Creates a new default interface method builder with the given name.
     *
     * <p>Default methods are only valid in interfaces and have a body.
     *
     * @param name the method name
     * @return a new Method builder configured for default method
     */
    public static Method defaultMethod(String name) {
        Method builder = new Method(name);
        builder.isDefault = true;
        return builder;
    }

    // ========================================================================
    // Modifiers
    // ========================================================================

    /**
     * Adds the {@code public} modifier.
     *
     * @return this builder
     */
    public Method public_() {
        modifiers.add(Modifier.PUBLIC);
        return this;
    }

    /**
     * Adds the {@code protected} modifier.
     *
     * @return this builder
     */
    public Method protected_() {
        modifiers.add(Modifier.PROTECTED);
        return this;
    }

    /**
     * Adds the {@code private} modifier.
     *
     * @return this builder
     */
    public Method private_() {
        modifiers.add(Modifier.PRIVATE);
        return this;
    }

    /**
     * Adds the {@code static} modifier.
     *
     * @return this builder
     */
    public Method static_() {
        modifiers.add(Modifier.STATIC);
        return this;
    }

    /**
     * Adds the {@code final} modifier.
     *
     * @return this builder
     */
    public Method final_() {
        modifiers.add(Modifier.FINAL);
        return this;
    }

    /**
     * Adds the {@code synchronized} modifier.
     *
     * @return this builder
     */
    public Method synchronized_() {
        modifiers.add(Modifier.SYNCHRONIZED);
        return this;
    }

    /**
     * Adds the {@code native} modifier.
     *
     * @return this builder
     */
    public Method native_() {
        modifiers.add(Modifier.NATIVE);
        return this;
    }

    /**
     * Adds the specified modifier.
     *
     * @param modifier the modifier to add
     * @return this builder
     */
    public Method modifier(Modifier modifier) {
        modifiers.add(modifier);
        return this;
    }

    /**
     * Adds multiple modifiers.
     *
     * @param modifiers the modifiers to add
     * @return this builder
     */
    public Method modifiers(Modifier... modifiers) {
        for (Modifier m : modifiers) {
            this.modifiers.add(m);
        }
        return this;
    }

    // ========================================================================
    // Type Parameters
    // ========================================================================

    /**
     * Adds a type parameter (generic type variable).
     *
     * @param typeParameter the type parameter
     * @return this builder
     */
    public Method typeParameter(TypeVariable typeParameter) {
        typeParameters.add(typeParameter);
        return this;
    }

    /**
     * Adds a simple unbounded type parameter.
     *
     * @param name the type parameter name (e.g., "T")
     * @return this builder
     */
    public Method typeParameter(String name) {
        typeParameters.add(Types.typeVar(name));
        return this;
    }

    /**
     * Adds multiple type parameters.
     *
     * @param typeParameters the type parameters
     * @return this builder
     */
    public Method typeParameters(TypeVariable... typeParameters) {
        for (TypeVariable tp : typeParameters) {
            this.typeParameters.add(tp);
        }
        return this;
    }

    // ========================================================================
    // Return Type
    // ========================================================================

    /**
     * Sets the return type.
     *
     * @param returnType the return type
     * @return this builder
     */
    public Method returns(TypeRef returnType) {
        this.returnType = returnType;
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
    public Method parameter(TypeRef type, String name) {
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
    public Method varargs(TypeRef type, String name) {
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
    public Method parameter(TypeRef type, String name, List<AnnotationSpec> annotations) {
        parameters.add(new ParameterDecl(name, type, annotations, false));
        return this;
    }

    /**
     * Adds a fully-configured parameter.
     *
     * @param parameter the parameter declaration
     * @return this builder
     */
    public Method parameter(ParameterDecl parameter) {
        parameters.add(parameter);
        return this;
    }

    /**
     * Adds multiple parameters.
     *
     * @param parameters the parameters
     * @return this builder
     */
    public Method parameters(ParameterDecl... parameters) {
        for (ParameterDecl p : parameters) {
            this.parameters.add(p);
        }
        return this;
    }

    // ========================================================================
    // Body
    // ========================================================================

    /**
     * Sets the method body.
     *
     * <p>For abstract methods, the body should be null (which is the default).
     *
     * @param body the method body
     * @return this builder
     */
    public Method body(Statement body) {
        this.body = body;
        return this;
    }

    /**
     * Sets the method body to multiple statements wrapped in a block.
     *
     * @param statements the statements
     * @return this builder
     */
    public Method body(Statement... statements) {
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
    public Method throws_(TypeRef exceptionType) {
        thrownExceptions.add(exceptionType);
        return this;
    }

    /**
     * Adds multiple thrown exception types.
     *
     * @param exceptionTypes the exception types
     * @return this builder
     */
    public Method throws_(TypeRef... exceptionTypes) {
        for (TypeRef ex : exceptionTypes) {
            thrownExceptions.add(ex);
        }
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
    public Method annotation(AnnotationSpec annotation) {
        annotations.add(annotation);
        return this;
    }

    /**
     * Adds a simple marker annotation (no members).
     *
     * @param annotationType the annotation type
     * @return this builder
     */
    public Method annotation(TypeRef annotationType) {
        annotations.add(AnnotationSpec.of(annotationType));
        return this;
    }

    /**
     * Adds an @Override annotation.
     *
     * @return this builder
     */
    public Method override() {
        return annotation(Types.type("java.lang.Override"));
    }

    /**
     * Adds a @Deprecated annotation.
     *
     * @return this builder
     */
    public Method deprecated() {
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
    public Method javadoc(String javadoc) {
        this.javadoc = javadoc;
        return this;
    }

    // ========================================================================
    // Build
    // ========================================================================

    /**
     * Builds the method declaration.
     *
     * @return a new MethodDecl
     */
    public MethodDecl build() {
        Set<Modifier> effectiveModifiers = EnumSet.copyOf(modifiers);

        if (isAbstract) {
            effectiveModifiers.add(Modifier.ABSTRACT);
        }
        if (isDefault) {
            effectiveModifiers.add(Modifier.DEFAULT);
        }

        return new MethodDecl(
            name,
            returnType,
            parameters,
            typeParameters,
            body != null ? List.of(body) : List.of(),
            effectiveModifiers,
            annotations,
            javadoc,
            isAbstract,
            isDefault,
            thrownExceptions
        );
    }
}
