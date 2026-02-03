/**
 * Declaration nodes for the IR.
 *
 * <p>This package contains all declaration types for the Intermediate Representation.
 * Declarations represent type, member, and parameter definitions.
 *
 * <h2>Type Declaration Hierarchy</h2>
 * <pre>
 * TypeDeclaration (sealed)
 * ├── ClassDecl      - Class declaration
 * ├── InterfaceDecl  - Interface declaration
 * └── EnumDecl       - Enum declaration
 * </pre>
 *
 * <h2>Member Declaration Hierarchy</h2>
 * <pre>
 * MemberDeclaration (sealed)
 * ├── FieldDecl       - Field declaration
 * ├── MethodDecl      - Method declaration
 * └── ConstructorDecl - Constructor declaration
 * </pre>
 *
 * <h2>Supporting Types</h2>
 * <ul>
 *   <li>{@link ParameterDecl} - Method/constructor parameter</li>
 *   <li>{@link AnnotationSpec} - Annotation specification</li>
 *   <li>{@link EnumConstant} - Enum constant with optional arguments</li>
 *   <li>{@link Modifier} - Java modifiers (public, static, final, etc.)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Use the DSL builders for convenience
 * ClassDecl clazz = Class_.class_("Person")
 *     .in("com.example")
 *     .public_()
 *     .field(Field.field(Types.STRING, "name").private_().build())
 *     .method(Method.method("getName")
 *         .public_()
 *         .returns(Types.STRING)
 *         .body(Stmt.return_(Expr.field("name")))
 *         .build())
 *     .build();
 *
 * InterfaceDecl iface = Interface_.interface_("Named")
 *     .in("com.example")
 *     .method(Method.abstractMethod("getName")
 *         .returns(Types.STRING)
 *         .build())
 *     .build();
 *
 * EnumDecl enumDecl = Enum_.enum_("Status")
 *     .in("com.example")
 *     .constant("PENDING")
 *     .constant("ACTIVE")
 *     .constant("COMPLETED")
 *     .build();
 * }</pre>
 *
 * @see io.alnovis.protowrapper.dsl.Class_
 * @see io.alnovis.protowrapper.dsl.Interface_
 * @see io.alnovis.protowrapper.dsl.Enum_
 * @see io.alnovis.protowrapper.dsl.Method
 * @see io.alnovis.protowrapper.dsl.Field
 * @see io.alnovis.protowrapper.dsl.Constructor
 * @since 2.4.0
 */
package io.alnovis.protowrapper.ir.decl;
