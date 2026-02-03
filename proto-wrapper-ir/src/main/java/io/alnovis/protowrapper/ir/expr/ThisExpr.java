package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;

/**
 * Represents the {@code this} reference expression in the IR.
 *
 * <p>The {@code this} keyword refers to the current instance of a class.
 * It is used to:
 * <ul>
 *   <li>Access instance fields: {@code this.field}</li>
 *   <li>Call instance methods: {@code this.method()}</li>
 *   <li>Pass the current instance as an argument</li>
 *   <li>Disambiguate local variables from fields</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * class Person {
 *     private String name;
 *
 *     public Person(String name) {
 *         this.name = name;  // 'this' disambiguates field from parameter
 *     }
 *
 *     public Person getSelf() {
 *         return this;  // 'this' as return value
 *     }
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Direct construction
 * ThisExpr this_ = ThisExpr.INSTANCE;
 *
 * // Using the Expr DSL (recommended)
 * Expression this_ = Expr.this_();
 *
 * // Field access: this.name
 * Expression fieldAccess = Expr.field(Expr.this_(), "name");
 *
 * // Method call: this.process()
 * Expression methodCall = Expr.call(Expr.this_(), "process");
 * }</pre>
 *
 * <p>This class uses the singleton pattern since all {@code this} references
 * are identical within a given context.
 *
 * @see FieldAccessExpr
 * @see MethodCallExpr
 * @see io.alnovis.protowrapper.dsl.Expr#this_()
 * @since 2.4.0
 */
public record ThisExpr() implements Expression {

    /**
     * The singleton instance of ThisExpr.
     *
     * <p>Since all {@code this} expressions are semantically identical,
     * this instance should be used instead of creating new instances.
     */
    public static final ThisExpr INSTANCE = new ThisExpr();
}
