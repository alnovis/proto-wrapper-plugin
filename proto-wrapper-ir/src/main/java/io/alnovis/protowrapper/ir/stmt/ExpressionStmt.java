package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.ConstructorCallExpr;
import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.expr.MethodCallExpr;

import java.util.Objects;

/**
 * Represents an expression used as a statement in the IR.
 *
 * <p>An expression statement evaluates an expression for its side effects
 * and discards the result. This is commonly used for method calls that
 * don't return a useful value.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Method call as statement
 * doSomething();
 * list.add(item);
 * System.out.println("Hello");
 *
 * // Constructor call as statement (rarely useful)
 * new SideEffectClass();
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // doSomething();
 * ExpressionStmt stmt = new ExpressionStmt(
 *     Expr.call(null, "doSomething")
 * );
 *
 * // list.add(item);
 * ExpressionStmt addStmt = new ExpressionStmt(
 *     Expr.call(Expr.var("list"), "add", Expr.var("item"))
 * );
 *
 * // Using the Stmt DSL (recommended)
 * Statement stmt = Stmt.expr(Expr.call(null, "doSomething"));
 * Statement addStmt = Stmt.expr(Expr.call(Expr.var("list"), "add", Expr.var("item")));
 * }</pre>
 *
 * @param expression the expression to evaluate; must not be null
 * @see io.alnovis.protowrapper.dsl.Stmt#expr(Expression)
 * @since 2.4.0
 */
public record ExpressionStmt(Expression expression) implements Statement {

    /**
     * Creates a new ExpressionStmt with validation.
     *
     * @param expression the expression to evaluate
     * @throws NullPointerException if expression is null
     */
    public ExpressionStmt {
        Objects.requireNonNull(expression, "expression must not be null");
    }

    /**
     * Returns {@code true} if this is a method call statement.
     *
     * @return {@code true} if the expression is a MethodCallExpr
     */
    public boolean isMethodCall() {
        return expression instanceof MethodCallExpr;
    }

    /**
     * Returns {@code true} if this is a constructor call statement.
     *
     * @return {@code true} if the expression is a ConstructorCallExpr
     */
    public boolean isConstructorCall() {
        return expression instanceof ConstructorCallExpr;
    }
}
