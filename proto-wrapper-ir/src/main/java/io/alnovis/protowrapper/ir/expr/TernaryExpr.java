package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;

import java.util.Objects;

/**
 * Represents a ternary conditional expression ({@code ? :}) in the IR.
 *
 * <p>A ternary expression evaluates a condition and returns one of two values
 * depending on whether the condition is true or false. It is the only ternary
 * operator in Java.
 *
 * <p>Syntax: {@code condition ? thenExpr : elseExpr}
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Simple ternary
 * String result = isValid ? "yes" : "no";
 *
 * // Nested ternary (use sparingly)
 * int sign = x > 0 ? 1 : (x < 0 ? -1 : 0);
 *
 * // With method calls
 * String name = user != null ? user.getName() : "anonymous";
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // isValid ? "yes" : "no"
 * TernaryExpr ternary = new TernaryExpr(
 *     Expr.var("isValid"),
 *     Expr.literal("yes"),
 *     Expr.literal("no")
 * );
 *
 * // user != null ? user.getName() : "anonymous"
 * TernaryExpr userTernary = new TernaryExpr(
 *     Expr.ne(Expr.var("user"), Expr.nullLiteral()),
 *     Expr.call(Expr.var("user"), "getName"),
 *     Expr.literal("anonymous")
 * );
 *
 * // Using the Expr DSL (recommended)
 * Expression ternary = Expr.ternary(
 *     Expr.var("isValid"),
 *     Expr.literal("yes"),
 *     Expr.literal("no")
 * );
 * }</pre>
 *
 * <p><b>Best practices:</b>
 * <ul>
 *   <li>Use ternary for simple conditions; prefer if-else for complex logic</li>
 *   <li>Avoid deeply nested ternary expressions</li>
 *   <li>Both branches should have compatible types</li>
 * </ul>
 *
 * @param condition the condition to evaluate; must not be null
 * @param thenExpr  the expression to return if condition is true; must not be null
 * @param elseExpr  the expression to return if condition is false; must not be null
 * @see io.alnovis.protowrapper.dsl.Expr#ternary(Expression, Expression, Expression)
 * @since 2.4.0
 */
public record TernaryExpr(
        Expression condition,
        Expression thenExpr,
        Expression elseExpr
) implements Expression {

    /**
     * Creates a new TernaryExpr with validation.
     *
     * @param condition the condition expression
     * @param thenExpr  the expression for true branch
     * @param elseExpr  the expression for false branch
     * @throws NullPointerException if any argument is null
     */
    public TernaryExpr {
        Objects.requireNonNull(condition, "condition must not be null");
        Objects.requireNonNull(thenExpr, "thenExpr must not be null");
        Objects.requireNonNull(elseExpr, "elseExpr must not be null");
    }
}
