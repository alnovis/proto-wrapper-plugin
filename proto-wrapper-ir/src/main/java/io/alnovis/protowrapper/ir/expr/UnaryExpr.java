package io.alnovis.protowrapper.ir.expr;

import java.util.Objects;

/**
 * Represents a unary expression in the IR.
 *
 * <p>A unary expression applies a single operator to one operand. The supported
 * operators are defined in {@link UnaryOp}:
 * <ul>
 *   <li>{@code !} - Logical NOT</li>
 *   <li>{@code -} - Arithmetic negation</li>
 *   <li>{@code ~} - Bitwise complement</li>
 *   <li>{@code +} - Unary plus (rarely used)</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * !isValid
 * -value
 * ~mask
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Logical NOT: !isValid
 * UnaryExpr not = new UnaryExpr(UnaryOp.NOT, Expr.var("isValid"));
 *
 * // Arithmetic negation: -value
 * UnaryExpr neg = new UnaryExpr(UnaryOp.NEG, Expr.var("value"));
 *
 * // Bitwise complement: ~mask
 * UnaryExpr bitNot = new UnaryExpr(UnaryOp.BIT_NOT, Expr.var("mask"));
 *
 * // Using the Expr DSL (recommended)
 * Expression not = Expr.not(Expr.var("isValid"));
 * Expression neg = Expr.neg(Expr.var("value"));
 * }</pre>
 *
 * @param operator the unary operator; must not be null
 * @param operand  the operand expression; must not be null
 * @see UnaryOp
 * @see io.alnovis.protowrapper.dsl.Expr#not(Expression)
 * @see io.alnovis.protowrapper.dsl.Expr#neg(Expression)
 * @since 2.4.0
 */
public record UnaryExpr(UnaryOp operator, Expression operand) implements Expression {

    /**
     * Creates a new UnaryExpr with validation.
     *
     * @param operator the unary operator
     * @param operand  the operand expression
     * @throws NullPointerException if any argument is null
     */
    public UnaryExpr {
        Objects.requireNonNull(operator, "operator must not be null");
        Objects.requireNonNull(operand, "operand must not be null");
    }

    /**
     * Returns {@code true} if this is a logical NOT expression.
     *
     * @return {@code true} if the operator is NOT
     */
    public boolean isNot() {
        return operator == UnaryOp.NOT;
    }

    /**
     * Returns {@code true} if this is an arithmetic negation.
     *
     * @return {@code true} if the operator is NEG
     */
    public boolean isNegation() {
        return operator == UnaryOp.NEG;
    }

    /**
     * Returns {@code true} if this is a bitwise complement.
     *
     * @return {@code true} if the operator is BIT_NOT
     */
    public boolean isBitwiseComplement() {
        return operator == UnaryOp.BIT_NOT;
    }
}
