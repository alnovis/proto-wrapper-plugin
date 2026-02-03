package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.decl.AnnotationSpec;
import io.alnovis.protowrapper.ir.decl.ConstructorDecl;
import io.alnovis.protowrapper.ir.decl.EnumConstant;
import io.alnovis.protowrapper.ir.decl.EnumDecl;
import io.alnovis.protowrapper.ir.decl.FieldDecl;
import io.alnovis.protowrapper.ir.decl.MemberDeclaration;
import io.alnovis.protowrapper.ir.decl.MethodDecl;
import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.type.TypeRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Builder for creating enum declarations.
 *
 * <p>This builder provides a fluent API for constructing {@link EnumDecl}
 * instances. It supports all enum features including constants, implements,
 * members, annotations, and JavaDoc.
 *
 * <h2>Simple Enum</h2>
 * <pre>{@code
 * EnumDecl enumDecl = Enum_.enum_("Status")
 *     .in("com.example")
 *     .constant("PENDING")
 *     .constant("ACTIVE")
 *     .constant("COMPLETED")
 *     .javadoc("Represents the status of an operation.")
 *     .build();
 * // public enum Status { PENDING, ACTIVE, COMPLETED }
 * }</pre>
 *
 * <h2>Enum with Constructor Arguments</h2>
 * <pre>{@code
 * EnumDecl enumDecl = Enum_.enum_("HttpStatus")
 *     .in("com.example")
 *     .constant("OK", Expr.literal(200), Expr.literal("OK"))
 *     .constant("NOT_FOUND", Expr.literal(404), Expr.literal("Not Found"))
 *     .constant("INTERNAL_ERROR", Expr.literal(500), Expr.literal("Internal Server Error"))
 *     .field(Field.field(Types.INT, "code").private_().final_().build())
 *     .field(Field.field(Types.STRING, "message").private_().final_().build())
 *     .constructor(Constructor.constructor()
 *         .private_()
 *         .parameter(Types.INT, "code")
 *         .parameter(Types.STRING, "message")
 *         .body(
 *             Stmt.assign(Expr.field("code"), Expr.var("code")),
 *             Stmt.assign(Expr.field("message"), Expr.var("message")))
 *         .build())
 *     .method(Method.method("getCode")
 *         .public_()
 *         .returns(Types.INT)
 *         .body(Stmt.return_(Expr.field("code")))
 *         .build())
 *     .build();
 * // public enum HttpStatus {
 * //     OK(200, "OK"),
 * //     NOT_FOUND(404, "Not Found"),
 * //     INTERNAL_ERROR(500, "Internal Server Error");
 * //
 * //     private final int code;
 * //     private final String message;
 * //
 * //     HttpStatus(int code, String message) {
 * //         this.code = code;
 * //         this.message = message;
 * //     }
 * //
 * //     public int getCode() { return this.code; }
 * // }
 * }</pre>
 *
 * <h2>Enum Implementing Interface</h2>
 * <pre>{@code
 * EnumDecl enumDecl = Enum_.enum_("Operation")
 *     .in("com.example")
 *     .implements_(Types.type("java.util.function.BinaryOperator", Types.INT_BOXED))
 *     .constant("ADD")
 *     .constant("SUBTRACT")
 *     .build();
 * // public enum Operation implements BinaryOperator<Integer> {
 * //     ADD, SUBTRACT
 * // }
 * }</pre>
 *
 * <h2>Enum with JavaDoc on Constants</h2>
 * <pre>{@code
 * EnumDecl enumDecl = Enum_.enum_("Priority")
 *     .in("com.example")
 *     .constant(new EnumConstant("LOW", List.of(), List.of(), "Lowest priority."))
 *     .constant(new EnumConstant("MEDIUM", List.of(), List.of(), "Medium priority."))
 *     .constant(new EnumConstant("HIGH", List.of(), List.of(), "Highest priority."))
 *     .build();
 * }</pre>
 *
 * @see EnumDecl
 * @see EnumConstant
 * @since 2.4.0
 */
public final class Enum_ {

    private final String name;
    private String packageName = "";
    private final List<EnumConstant> constants = new ArrayList<>();
    private final List<TypeRef> interfaces = new ArrayList<>();
    private final List<MemberDeclaration> members = new ArrayList<>();
    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private String javadoc;

    private Enum_(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Creates a new enum builder with the given name.
     *
     * @param name the enum name
     * @return a new Enum_ builder
     */
    public static Enum_ enum_(String name) {
        return new Enum_(name);
    }

    // ========================================================================
    // Package
    // ========================================================================

    /**
     * Sets the package name.
     *
     * @param packageName the package name
     * @return this builder
     */
    public Enum_ in(String packageName) {
        this.packageName = Objects.requireNonNull(packageName, "packageName must not be null");
        return this;
    }

    // ========================================================================
    // Constants
    // ========================================================================

    /**
     * Adds a simple enum constant.
     *
     * @param name the constant name
     * @return this builder
     */
    public Enum_ constant(String name) {
        constants.add(EnumConstant.of(name));
        return this;
    }

    /**
     * Adds an enum constant with constructor arguments.
     *
     * <p>Example:
     * <pre>{@code
     * Enum_.enum_("HttpStatus")
     *     .constant("OK", Expr.literal(200))
     *     .constant("NOT_FOUND", Expr.literal(404))
     * }</pre>
     *
     * @param name      the constant name
     * @param arguments the constructor arguments
     * @return this builder
     */
    public Enum_ constant(String name, Expression... arguments) {
        constants.add(new EnumConstant(name, Arrays.asList(arguments), List.of(), null));
        return this;
    }

    /**
     * Adds an enum constant with constructor arguments.
     *
     * @param name      the constant name
     * @param arguments the constructor arguments
     * @return this builder
     */
    public Enum_ constant(String name, List<Expression> arguments) {
        constants.add(new EnumConstant(name, arguments, List.of(), null));
        return this;
    }

    /**
     * Adds a fully-configured enum constant.
     *
     * @param constant the enum constant
     * @return this builder
     */
    public Enum_ constant(EnumConstant constant) {
        constants.add(constant);
        return this;
    }

    /**
     * Adds multiple enum constants.
     *
     * @param constantNames the constant names
     * @return this builder
     */
    public Enum_ constants(String... constantNames) {
        for (String name : constantNames) {
            constants.add(EnumConstant.of(name));
        }
        return this;
    }

    /**
     * Adds a constant with JavaDoc.
     *
     * @param name    the constant name
     * @param javadoc the JavaDoc comment
     * @return this builder
     */
    public Enum_ constantWithJavadoc(String name, String javadoc) {
        constants.add(new EnumConstant(name, List.of(), List.of(), javadoc));
        return this;
    }

    // ========================================================================
    // Implements
    // ========================================================================

    /**
     * Adds an implemented interface.
     *
     * @param interfaceType the interface type
     * @return this builder
     */
    public Enum_ implements_(TypeRef interfaceType) {
        interfaces.add(interfaceType);
        return this;
    }

    /**
     * Adds multiple implemented interfaces.
     *
     * @param interfaceTypes the interface types
     * @return this builder
     */
    public Enum_ implements_(TypeRef... interfaceTypes) {
        for (TypeRef it : interfaceTypes) {
            interfaces.add(it);
        }
        return this;
    }

    // ========================================================================
    // Members
    // ========================================================================

    /**
     * Adds a field declaration.
     *
     * @param field the field declaration
     * @return this builder
     */
    public Enum_ field(FieldDecl field) {
        members.add(field);
        return this;
    }

    /**
     * Adds a constructor declaration.
     *
     * <p>Note: Enum constructors must be private (implicit if not specified).
     *
     * @param constructor the constructor declaration
     * @return this builder
     */
    public Enum_ constructor(ConstructorDecl constructor) {
        members.add(constructor);
        return this;
    }

    /**
     * Adds a method declaration.
     *
     * @param method the method declaration
     * @return this builder
     */
    public Enum_ method(MethodDecl method) {
        members.add(method);
        return this;
    }

    /**
     * Adds a member declaration.
     *
     * @param member the member declaration
     * @return this builder
     */
    public Enum_ member(MemberDeclaration member) {
        members.add(member);
        return this;
    }

    /**
     * Adds multiple member declarations.
     *
     * @param members the member declarations
     * @return this builder
     */
    public Enum_ members(MemberDeclaration... members) {
        for (MemberDeclaration m : members) {
            this.members.add(m);
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
    public Enum_ annotation(AnnotationSpec annotation) {
        annotations.add(annotation);
        return this;
    }

    /**
     * Adds a simple marker annotation (no members).
     *
     * @param annotationType the annotation type
     * @return this builder
     */
    public Enum_ annotation(TypeRef annotationType) {
        annotations.add(AnnotationSpec.of(annotationType));
        return this;
    }

    /**
     * Adds a @Deprecated annotation.
     *
     * @return this builder
     */
    public Enum_ deprecated() {
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
    public Enum_ javadoc(String javadoc) {
        this.javadoc = javadoc;
        return this;
    }

    // ========================================================================
    // Build
    // ========================================================================

    /**
     * Builds the enum declaration.
     *
     * @return a new EnumDecl
     */
    public EnumDecl build() {
        return new EnumDecl(
            packageName,
            name,
            constants,
            interfaces,
            members,
            annotations,
            javadoc
        );
    }
}
