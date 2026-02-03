package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.decl.AnnotationSpec;
import io.alnovis.protowrapper.ir.decl.ClassDecl;
import io.alnovis.protowrapper.ir.decl.ConstructorDecl;
import io.alnovis.protowrapper.ir.decl.FieldDecl;
import io.alnovis.protowrapper.ir.decl.MemberDeclaration;
import io.alnovis.protowrapper.ir.decl.MethodDecl;
import io.alnovis.protowrapper.ir.decl.Modifier;
import io.alnovis.protowrapper.ir.decl.TypeDeclaration;
import io.alnovis.protowrapper.ir.type.TypeRef;
import io.alnovis.protowrapper.ir.type.TypeVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builder for creating class declarations.
 *
 * <p>This builder provides a fluent API for constructing {@link ClassDecl}
 * instances. It supports all class features including extends, implements,
 * type parameters, members, nested types, modifiers, annotations, and JavaDoc.
 *
 * <h2>Simple Class</h2>
 * <pre>{@code
 * ClassDecl clazz = Class_.class_("Person")
 *     .in("com.example")
 *     .public_()
 *     .field(Field.field(Types.STRING, "name").private_().build())
 *     .constructor(Constructor.constructor()
 *         .public_()
 *         .parameter(Types.STRING, "name")
 *         .body(Stmt.assign(Expr.field("name"), Expr.var("name")))
 *         .build())
 *     .method(Method.method("getName")
 *         .public_()
 *         .returns(Types.STRING)
 *         .body(Stmt.return_(Expr.field("name")))
 *         .build())
 *     .build();
 * // public class Person {
 * //     private String name;
 * //     public Person(String name) { this.name = name; }
 * //     public String getName() { return this.name; }
 * // }
 * }</pre>
 *
 * <h2>Class with Inheritance</h2>
 * <pre>{@code
 * ClassDecl clazz = Class_.class_("Employee")
 *     .in("com.example")
 *     .public_()
 *     .extends_(Types.type("com.example.Person"))
 *     .implements_(Types.type("com.example.Named"))
 *     .build();
 * // public class Employee extends Person implements Named { }
 * }</pre>
 *
 * <h2>Abstract Class</h2>
 * <pre>{@code
 * ClassDecl clazz = Class_.class_("AbstractEntity")
 *     .in("com.example")
 *     .public_()
 *     .abstract_()
 *     .method(Method.abstractMethod("getId")
 *         .public_()
 *         .returns(Types.STRING)
 *         .build())
 *     .build();
 * // public abstract class AbstractEntity {
 * //     public abstract String getId();
 * // }
 * }</pre>
 *
 * <h2>Generic Class</h2>
 * <pre>{@code
 * ClassDecl clazz = Class_.class_("Box")
 *     .in("com.example")
 *     .public_()
 *     .typeParameter(Types.typeVar("T"))
 *     .field(Field.field(Types.typeVar("T"), "value").private_().build())
 *     .build();
 * // public class Box<T> {
 * //     private T value;
 * // }
 * }</pre>
 *
 * <h2>Final Class</h2>
 * <pre>{@code
 * ClassDecl clazz = Class_.class_("ImmutablePerson")
 *     .in("com.example")
 *     .public_()
 *     .final_()
 *     .field(Field.field(Types.STRING, "name").private_().final_().build())
 *     .build();
 * // public final class ImmutablePerson {
 * //     private final String name;
 * // }
 * }</pre>
 *
 * <h2>Static Nested Class</h2>
 * <pre>{@code
 * ClassDecl clazz = Class_.class_("Builder")
 *     .public_()
 *     .static_()
 *     .build();
 * // public static class Builder { }
 * }</pre>
 *
 * @see ClassDecl
 * @see FieldDecl
 * @see MethodDecl
 * @see ConstructorDecl
 * @since 2.4.0
 */
public final class Class_ {

    private final String name;
    private String packageName = "";
    private TypeRef superClass;
    private final List<TypeRef> interfaces = new ArrayList<>();
    private final List<TypeVariable> typeParameters = new ArrayList<>();
    private final List<MemberDeclaration> members = new ArrayList<>();
    private final List<TypeDeclaration> nestedTypes = new ArrayList<>();
    private final Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private String javadoc;

    private Class_(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Creates a new class builder with the given name.
     *
     * @param name the class name
     * @return a new Class_ builder
     */
    public static Class_ class_(String name) {
        return new Class_(name);
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
    public Class_ in(String packageName) {
        this.packageName = Objects.requireNonNull(packageName, "packageName must not be null");
        return this;
    }

    // ========================================================================
    // Modifiers
    // ========================================================================

    /**
     * Adds the {@code public} modifier.
     *
     * @return this builder
     */
    public Class_ public_() {
        modifiers.add(Modifier.PUBLIC);
        return this;
    }

    /**
     * Adds the {@code protected} modifier (for nested classes).
     *
     * @return this builder
     */
    public Class_ protected_() {
        modifiers.add(Modifier.PROTECTED);
        return this;
    }

    /**
     * Adds the {@code private} modifier (for nested classes).
     *
     * @return this builder
     */
    public Class_ private_() {
        modifiers.add(Modifier.PRIVATE);
        return this;
    }

    /**
     * Adds the {@code abstract} modifier.
     *
     * @return this builder
     */
    public Class_ abstract_() {
        modifiers.add(Modifier.ABSTRACT);
        return this;
    }

    /**
     * Adds the {@code final} modifier.
     *
     * @return this builder
     */
    public Class_ final_() {
        modifiers.add(Modifier.FINAL);
        return this;
    }

    /**
     * Adds the {@code static} modifier (for nested classes).
     *
     * @return this builder
     */
    public Class_ static_() {
        modifiers.add(Modifier.STATIC);
        return this;
    }

    /**
     * Adds the specified modifier.
     *
     * @param modifier the modifier to add
     * @return this builder
     */
    public Class_ modifier(Modifier modifier) {
        modifiers.add(modifier);
        return this;
    }

    /**
     * Adds multiple modifiers.
     *
     * @param modifiers the modifiers to add
     * @return this builder
     */
    public Class_ modifiers(Modifier... modifiers) {
        Collections.addAll(this.modifiers, modifiers);
        return this;
    }

    // ========================================================================
    // Extends / Implements
    // ========================================================================

    /**
     * Sets the superclass (extends clause).
     *
     * @param superClass the superclass type
     * @return this builder
     */
    public Class_ extends_(TypeRef superClass) {
        this.superClass = superClass;
        return this;
    }

    /**
     * Adds an implemented interface.
     *
     * @param interfaceType the interface type
     * @return this builder
     */
    public Class_ implements_(TypeRef interfaceType) {
        interfaces.add(interfaceType);
        return this;
    }

    /**
     * Adds multiple implemented interfaces.
     *
     * @param interfaceTypes the interface types
     * @return this builder
     */
    public Class_ implements_(TypeRef... interfaceTypes) {
        Collections.addAll(interfaces, interfaceTypes);
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
    public Class_ typeParameter(TypeVariable typeParameter) {
        typeParameters.add(typeParameter);
        return this;
    }

    /**
     * Adds a simple unbounded type parameter.
     *
     * @param name the type parameter name (e.g., "T")
     * @return this builder
     */
    public Class_ typeParameter(String name) {
        typeParameters.add(Types.typeVar(name));
        return this;
    }

    /**
     * Adds multiple type parameters.
     *
     * @param typeParameters the type parameters
     * @return this builder
     */
    public Class_ typeParameters(TypeVariable... typeParameters) {
        Collections.addAll(this.typeParameters, typeParameters);
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
    public Class_ field(FieldDecl field) {
        members.add(field);
        return this;
    }

    /**
     * Adds a constructor declaration.
     *
     * @param constructor the constructor declaration
     * @return this builder
     */
    public Class_ constructor(ConstructorDecl constructor) {
        members.add(constructor);
        return this;
    }

    /**
     * Adds a method declaration.
     *
     * @param method the method declaration
     * @return this builder
     */
    public Class_ method(MethodDecl method) {
        members.add(method);
        return this;
    }

    /**
     * Adds a member declaration.
     *
     * @param member the member declaration
     * @return this builder
     */
    public Class_ member(MemberDeclaration member) {
        members.add(member);
        return this;
    }

    /**
     * Adds multiple member declarations.
     *
     * @param members the member declarations
     * @return this builder
     */
    public Class_ members(MemberDeclaration... members) {
        Collections.addAll(this.members, members);
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
    public Class_ nestedType(TypeDeclaration nestedType) {
        nestedTypes.add(nestedType);
        return this;
    }

    /**
     * Adds multiple nested type declarations.
     *
     * @param nestedTypes the nested type declarations
     * @return this builder
     */
    public Class_ nestedTypes(TypeDeclaration... nestedTypes) {
        Collections.addAll(this.nestedTypes, nestedTypes);
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
    public Class_ annotation(AnnotationSpec annotation) {
        annotations.add(annotation);
        return this;
    }

    /**
     * Adds a simple marker annotation (no members).
     *
     * @param annotationType the annotation type
     * @return this builder
     */
    public Class_ annotation(TypeRef annotationType) {
        annotations.add(AnnotationSpec.of(annotationType));
        return this;
    }

    /**
     * Adds a @Deprecated annotation.
     *
     * @return this builder
     */
    public Class_ deprecated() {
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
    public Class_ javadoc(String javadoc) {
        this.javadoc = javadoc;
        return this;
    }

    // ========================================================================
    // Build
    // ========================================================================

    /**
     * Builds the class declaration.
     *
     * @return a new ClassDecl
     */
    public ClassDecl build() {
        return new ClassDecl(
            packageName,
            name,
            superClass,
            interfaces,
            typeParameters,
            members,
            nestedTypes,
            modifiers,
            annotations,
            javadoc
        );
    }
}
