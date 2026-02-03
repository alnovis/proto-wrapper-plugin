/**
 * Domain-Specific Language (DSL) for building IR (Intermediate Representation) nodes.
 *
 * <p>This package provides a fluent API for constructing IR nodes. It consists of:
 * <ul>
 *   <li><strong>Factory classes</strong> for creating primitives:
 *     <ul>
 *       <li>{@link io.alnovis.protowrapper.dsl.Types} - Type references (primitives, classes, arrays, etc.)</li>
 *       <li>{@link io.alnovis.protowrapper.dsl.Expr} - Expression nodes (literals, calls, operators, etc.)</li>
 *       <li>{@link io.alnovis.protowrapper.dsl.Stmt} - Statement nodes (if, for, return, etc.)</li>
 *     </ul>
 *   </li>
 *   <li><strong>Builder classes</strong> for creating declarations:
 *     <ul>
 *       <li>{@link io.alnovis.protowrapper.dsl.Method} - Method declarations</li>
 *       <li>{@link io.alnovis.protowrapper.dsl.Field} - Field declarations</li>
 *       <li>{@link io.alnovis.protowrapper.dsl.Constructor} - Constructor declarations</li>
 *       <li>{@link io.alnovis.protowrapper.dsl.Interface_} - Interface declarations</li>
 *       <li>{@link io.alnovis.protowrapper.dsl.Class_} - Class declarations</li>
 *       <li>{@link io.alnovis.protowrapper.dsl.Enum_} - Enum declarations</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * import static io.alnovis.protowrapper.dsl.Types.*;
 * import static io.alnovis.protowrapper.dsl.Expr.*;
 * import static io.alnovis.protowrapper.dsl.Stmt.*;
 *
 * // Create a simple class
 * ClassDecl person = Class_.class_("Person")
 *     .in("com.example")
 *     .public_()
 *     .field(Field.field(STRING, "name").private_().final_().build())
 *     .constructor(Constructor.constructor()
 *         .public_()
 *         .parameter(STRING, "name")
 *         .body(assign(field("name"), var("name")))
 *         .build())
 *     .method(Method.method("getName")
 *         .public_()
 *         .returns(STRING)
 *         .body(return_(field("name")))
 *         .build())
 *     .javadoc("Represents a person.")
 *     .build();
 *
 * // Create an interface
 * InterfaceDecl named = Interface_.interface_("Named")
 *     .in("com.example")
 *     .method(Method.abstractMethod("getName")
 *         .returns(STRING)
 *         .build())
 *     .build();
 *
 * // Create an enum
 * EnumDecl status = Enum_.enum_("Status")
 *     .in("com.example")
 *     .constants("PENDING", "ACTIVE", "COMPLETED")
 *     .build();
 * }</pre>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Immutability</strong>: All IR nodes are immutable records or sealed interfaces</li>
 *   <li><strong>Type Safety</strong>: The DSL uses typed references (TypeRef, Expression, Statement)
 *       instead of raw strings to prevent errors</li>
 *   <li><strong>Fluent API</strong>: Builder methods return {@code this} for method chaining</li>
 *   <li><strong>Language-Agnostic</strong>: The IR and DSL are not tied to any specific
 *       code generation framework (JavaPoet, KotlinPoet, etc.)</li>
 * </ul>
 *
 * @see io.alnovis.protowrapper.ir
 * @since 2.4.0
 */
package io.alnovis.protowrapper.dsl;
