package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;

import java.util.Objects;

/**
 * Represents an {@code instanceof} expression in the IR.
 *
 * <p>An instanceof expression tests whether an object is an instance of a particular
 * type. Since Java 16, pattern matching allows binding the cast result to a variable
 * directly in the instanceof expression.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Traditional instanceof
 * if (obj instanceof String) {
 *     String s = (String) obj;
 *     // use s
 * }
 *
 * // Pattern matching instanceof (Java 16+)
 * if (obj instanceof String s) {
 *     // s is already cast to String and in scope
 * }
 *
 * // As expression
 * boolean isString = obj instanceof String;
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Traditional instanceof: obj instanceof String
 * InstanceOfExpr check = new InstanceOfExpr(
 *     Expr.var("obj"),
 *     Types.STRING,
 *     null  // no binding variable
 * );
 *
 * // Pattern matching: obj instanceof String s
 * InstanceOfExpr patternMatch = new InstanceOfExpr(
 *     Expr.var("obj"),
 *     Types.STRING,
 *     "s"  // binding variable
 * );
 *
 * // Using the Expr DSL (recommended)
 * Expression check = Expr.instanceOf(Expr.var("obj"), Types.STRING);
 * Expression patternMatch = Expr.instanceOf(Expr.var("obj"), Types.STRING, "s");
 * }</pre>
 *
 * <p><b>Pattern matching note:</b> When a binding variable is specified, the variable
 * is in scope and cast to the target type within the true branch of a conditional.
 * The emitter should generate appropriate code for the target Java version.
 *
 * @param expression      the expression to test; must not be null
 * @param type            the type to test against; must not be null
 * @param bindingVariable the variable name to bind the cast result (Java 16+);
 *                        null if not using pattern matching
 * @see CastExpr
 * @see io.alnovis.protowrapper.dsl.Expr#instanceOf(Expression, TypeRef)
 * @see io.alnovis.protowrapper.dsl.Expr#instanceOf(Expression, TypeRef, String)
 * @since 2.4.0
 */
public record InstanceOfExpr(
        Expression expression,
        TypeRef type,
        String bindingVariable
) implements Expression {

    /**
     * Creates a new InstanceOfExpr with validation.
     *
     * @param expression      the expression to test
     * @param type            the type to test against
     * @param bindingVariable the binding variable name (may be null)
     * @throws NullPointerException if expression or type is null
     */
    public InstanceOfExpr {
        Objects.requireNonNull(expression, "expression must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }

    /**
     * Returns {@code true} if this instanceof uses pattern matching (has a binding variable).
     *
     * <p>Pattern matching instanceof (Java 16+) binds the cast result to a variable.
     *
     * @return {@code true} if a binding variable is specified
     */
    public boolean isPatternMatch() {
        return bindingVariable != null;
    }

    /**
     * Returns {@code true} if this is a traditional instanceof without pattern matching.
     *
     * @return {@code true} if no binding variable is specified
     */
    public boolean isTraditional() {
        return bindingVariable == null;
    }
}
