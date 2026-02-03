package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.decl.ParameterDecl;

import java.util.List;
import java.util.Objects;

/**
 * Represents a lambda expression in the IR.
 *
 * <p>A lambda expression is an anonymous function that can be used wherever
 * a functional interface is expected. This IR supports expression lambdas
 * (single expression body). For block lambdas (multiple statements), use
 * a separate method or inner class.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // No parameters
 * () -> "hello"
 *
 * // Single parameter (can omit type and parentheses)
 * x -> x * 2
 * (int x) -> x * 2
 *
 * // Multiple parameters
 * (a, b) -> a + b
 * (int a, int b) -> a + b
 *
 * // Method reference alternative
 * String::toUpperCase
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // () -> "hello"
 * LambdaExpr noParams = new LambdaExpr(
 *     List.of(),
 *     Expr.literal("hello")
 * );
 *
 * // x -> x * 2
 * LambdaExpr singleParam = new LambdaExpr(
 *     List.of(ParameterDecl.of("x", Types.INT)),
 *     Expr.mul(Expr.var("x"), Expr.literal(2))
 * );
 *
 * // (a, b) -> a + b
 * LambdaExpr twoParams = new LambdaExpr(
 *     List.of(
 *         ParameterDecl.of("a", Types.INT),
 *         ParameterDecl.of("b", Types.INT)
 *     ),
 *     Expr.add(Expr.var("a"), Expr.var("b"))
 * );
 *
 * // Using the Expr DSL (recommended)
 * Expression lambda = Expr.lambda(
 *     List.of(ParameterDecl.of("x", Types.INT)),
 *     Expr.mul(Expr.var("x"), Expr.literal(2))
 * );
 *
 * // Shorthand for single-parameter lambda
 * Expression lambda = Expr.lambda("x", Types.INT,
 *     Expr.mul(Expr.var("x"), Expr.literal(2))
 * );
 * }</pre>
 *
 * <p><b>Note:</b> Block lambdas (with multiple statements) are not directly
 * supported by this IR. If you need a block lambda, consider:
 * <ul>
 *   <li>Extract logic to a separate method and use method reference</li>
 *   <li>Use a method that returns the computed value</li>
 * </ul>
 *
 * @param parameters the lambda parameters; empty list for no-arg lambda;
 *                   the list is copied and made immutable
 * @param body       the expression that forms the lambda body; must not be null
 * @see ParameterDecl
 * @see io.alnovis.protowrapper.dsl.Expr#lambda(List, Expression)
 * @since 2.4.0
 */
public record LambdaExpr(
        List<ParameterDecl> parameters,
        Expression body
) implements Expression {

    /**
     * Creates a new LambdaExpr with validation.
     *
     * @param parameters the lambda parameters (may be null, treated as empty list)
     * @param body       the lambda body expression
     * @throws NullPointerException if body is null
     */
    public LambdaExpr {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        Objects.requireNonNull(body, "body must not be null");
    }

    /**
     * Returns {@code true} if this is a no-argument lambda.
     *
     * @return {@code true} if parameters list is empty
     */
    public boolean hasNoParameters() {
        return parameters.isEmpty();
    }

    /**
     * Returns {@code true} if this is a single-parameter lambda.
     *
     * <p>Single-parameter lambdas can omit parentheses around the parameter
     * in Java source code.
     *
     * @return {@code true} if there is exactly one parameter
     */
    public boolean isSingleParameter() {
        return parameters.size() == 1;
    }

    /**
     * Returns the number of parameters.
     *
     * @return the parameter count
     */
    public int parameterCount() {
        return parameters.size();
    }
}
