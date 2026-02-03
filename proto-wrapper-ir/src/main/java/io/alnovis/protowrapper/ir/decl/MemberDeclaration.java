package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.stmt.Statement;

/**
 * Represents a member declaration within a class or interface in the IR.
 *
 * <p>Members are the elements declared inside a type: fields, methods, and constructors.
 * Each member has modifiers, annotations, and optional JavaDoc.
 *
 * <p>The IR supports the following member types:
 * <ul>
 *   <li>{@link FieldDecl} - field declarations</li>
 *   <li>{@link MethodDecl} - method declarations (including abstract and default)</li>
 *   <li>{@link ConstructorDecl} - constructor declarations</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * public class Example {
 *     // Field declaration
 *     private final String name;
 *
 *     // Constructor declaration
 *     public Example(String name) {
 *         this.name = name;
 *     }
 *
 *     // Method declaration
 *     public String getName() {
 *         return name;
 *     }
 * }
 * }</pre>
 *
 * <p>Example usage with DSL:
 * <pre>{@code
 * // Field
 * FieldDecl field = Field.field("name")
 *     .type(Types.STRING)
 *     .private_().final_()
 *     .build();
 *
 * // Constructor
 * ConstructorDecl ctor = Constructor.constructor()
 *     .public_()
 *     .param("name", Types.STRING)
 *     .body(...)
 *     .build();
 *
 * // Method
 * MethodDecl method = Method.method("getName")
 *     .returns(Types.STRING)
 *     .public_()
 *     .body(Stmt.return_(Expr.field(Expr.this_(), "name")))
 *     .build();
 * }</pre>
 *
 * @see FieldDecl
 * @see MethodDecl
 * @see ConstructorDecl
 * @since 2.4.0
 */
public sealed interface MemberDeclaration permits
        FieldDecl,
        MethodDecl,
        ConstructorDecl {
}
