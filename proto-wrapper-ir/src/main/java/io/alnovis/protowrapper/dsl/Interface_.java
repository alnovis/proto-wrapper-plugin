package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.decl.AnnotationSpec;
import io.alnovis.protowrapper.ir.decl.InterfaceDecl;
import io.alnovis.protowrapper.ir.decl.MemberDeclaration;
import io.alnovis.protowrapper.ir.decl.MethodDecl;
import io.alnovis.protowrapper.ir.decl.TypeDeclaration;
import io.alnovis.protowrapper.ir.type.TypeRef;
import io.alnovis.protowrapper.ir.type.TypeVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for creating interface declarations.
 *
 * <p>This builder provides a fluent API for constructing {@link InterfaceDecl}
 * instances. It supports all interface features including extends, type parameters,
 * methods, nested types, annotations, and JavaDoc.
 *
 * <h2>Simple Interface</h2>
 * <pre>{@code
 * InterfaceDecl iface = Interface_.interface_("Named")
 *     .in("com.example")
 *     .method(Method.abstractMethod("getName")
 *         .returns(Types.STRING)
 *         .build())
 *     .javadoc("Represents a named entity.")
 *     .build();
 * // public interface Named {
 * //     String getName();
 * // }
 * }</pre>
 *
 * <h2>Interface Extending Others</h2>
 * <pre>{@code
 * InterfaceDecl iface = Interface_.interface_("NamedEntity")
 *     .in("com.example")
 *     .extends_(Types.type("com.example.Named"))
 *     .extends_(Types.type("com.example.Identifiable"))
 *     .build();
 * // public interface NamedEntity extends Named, Identifiable { }
 * }</pre>
 *
 * <h2>Generic Interface</h2>
 * <pre>{@code
 * InterfaceDecl iface = Interface_.interface_("Repository")
 *     .in("com.example")
 *     .typeParameter(Types.typeVar("T"))
 *     .typeParameter(Types.typeVar("ID"))
 *     .method(Method.abstractMethod("findById")
 *         .returns(Types.optional(Types.typeVar("T")))
 *         .parameter(Types.typeVar("ID"), "id")
 *         .build())
 *     .build();
 * // public interface Repository<T, ID> {
 * //     Optional<T> findById(ID id);
 * // }
 * }</pre>
 *
 * <h2>Interface with Default Methods</h2>
 * <pre>{@code
 * InterfaceDecl iface = Interface_.interface_("Named")
 *     .in("com.example")
 *     .method(Method.abstractMethod("getName")
 *         .returns(Types.STRING)
 *         .build())
 *     .method(Method.defaultMethod("getDisplayName")
 *         .returns(Types.STRING)
 *         .body(Stmt.return_(Expr.call("getName")))
 *         .build())
 *     .build();
 * // public interface Named {
 * //     String getName();
 * //     default String getDisplayName() { return getName(); }
 * // }
 * }</pre>
 *
 * <h2>Functional Interface</h2>
 * <pre>{@code
 * InterfaceDecl iface = Interface_.interface_("Processor")
 *     .in("com.example")
 *     .annotation(Types.type("java.lang.FunctionalInterface"))
 *     .typeParameter("T")
 *     .method(Method.abstractMethod("process")
 *         .returns(Types.typeVar("T"))
 *         .parameter(Types.typeVar("T"), "input")
 *         .build())
 *     .build();
 * // @FunctionalInterface
 * // public interface Processor<T> {
 * //     T process(T input);
 * // }
 * }</pre>
 *
 * @see InterfaceDecl
 * @see MethodDecl
 * @since 2.4.0
 */
public final class Interface_ {

    private final String name;
    private String packageName = "";
    private final List<TypeRef> superInterfaces = new ArrayList<>();
    private final List<TypeVariable> typeParameters = new ArrayList<>();
    private final List<MemberDeclaration> members = new ArrayList<>();
    private final List<TypeDeclaration> nestedTypes = new ArrayList<>();
    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private String javadoc;

    private Interface_(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Creates a new interface builder with the given name.
     *
     * @param name the interface name
     * @return a new Interface_ builder
     */
    public static Interface_ interface_(String name) {
        return new Interface_(name);
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
    public Interface_ in(String packageName) {
        this.packageName = Objects.requireNonNull(packageName, "packageName must not be null");
        return this;
    }

    // ========================================================================
    // Extends
    // ========================================================================

    /**
     * Adds a super interface (extends clause).
     *
     * @param superInterface the super interface type
     * @return this builder
     */
    public Interface_ extends_(TypeRef superInterface) {
        superInterfaces.add(superInterface);
        return this;
    }

    /**
     * Adds multiple super interfaces.
     *
     * @param superInterfaces the super interface types
     * @return this builder
     */
    public Interface_ extends_(TypeRef... superInterfaces) {
        for (TypeRef si : superInterfaces) {
            this.superInterfaces.add(si);
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
    public Interface_ typeParameter(TypeVariable typeParameter) {
        typeParameters.add(typeParameter);
        return this;
    }

    /**
     * Adds a simple unbounded type parameter.
     *
     * @param name the type parameter name (e.g., "T")
     * @return this builder
     */
    public Interface_ typeParameter(String name) {
        typeParameters.add(Types.typeVar(name));
        return this;
    }

    /**
     * Adds multiple type parameters.
     *
     * @param typeParameters the type parameters
     * @return this builder
     */
    public Interface_ typeParameters(TypeVariable... typeParameters) {
        for (TypeVariable tp : typeParameters) {
            this.typeParameters.add(tp);
        }
        return this;
    }

    // ========================================================================
    // Members
    // ========================================================================

    /**
     * Adds a method declaration.
     *
     * @param method the method declaration
     * @return this builder
     */
    public Interface_ method(MethodDecl method) {
        members.add(method);
        return this;
    }

    /**
     * Adds a member declaration.
     *
     * @param member the member declaration
     * @return this builder
     */
    public Interface_ member(MemberDeclaration member) {
        members.add(member);
        return this;
    }

    /**
     * Adds multiple member declarations.
     *
     * @param members the member declarations
     * @return this builder
     */
    public Interface_ members(MemberDeclaration... members) {
        for (MemberDeclaration m : members) {
            this.members.add(m);
        }
        return this;
    }

    // ========================================================================
    // Nested Types
    // ========================================================================

    /**
     * Adds a nested type declaration.
     *
     * @param nestedType the nested type declaration
     * @return this builder
     */
    public Interface_ nestedType(TypeDeclaration nestedType) {
        nestedTypes.add(nestedType);
        return this;
    }

    /**
     * Adds multiple nested type declarations.
     *
     * @param nestedTypes the nested type declarations
     * @return this builder
     */
    public Interface_ nestedTypes(TypeDeclaration... nestedTypes) {
        for (TypeDeclaration nt : nestedTypes) {
            this.nestedTypes.add(nt);
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
    public Interface_ annotation(AnnotationSpec annotation) {
        annotations.add(annotation);
        return this;
    }

    /**
     * Adds a simple marker annotation (no members).
     *
     * @param annotationType the annotation type
     * @return this builder
     */
    public Interface_ annotation(TypeRef annotationType) {
        annotations.add(AnnotationSpec.of(annotationType));
        return this;
    }

    /**
     * Adds a @Deprecated annotation.
     *
     * @return this builder
     */
    public Interface_ deprecated() {
        return annotation(Types.type("java.lang.Deprecated"));
    }

    /**
     * Adds a @FunctionalInterface annotation.
     *
     * @return this builder
     */
    public Interface_ functionalInterface() {
        return annotation(Types.type("java.lang.FunctionalInterface"));
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
    public Interface_ javadoc(String javadoc) {
        this.javadoc = javadoc;
        return this;
    }

    // ========================================================================
    // Build
    // ========================================================================

    /**
     * Builds the interface declaration.
     *
     * @return a new InterfaceDecl
     */
    public InterfaceDecl build() {
        return new InterfaceDecl(
            packageName,
            name,
            superInterfaces,
            typeParameters,
            members,
            nestedTypes,
            annotations,
            javadoc
        );
    }
}
