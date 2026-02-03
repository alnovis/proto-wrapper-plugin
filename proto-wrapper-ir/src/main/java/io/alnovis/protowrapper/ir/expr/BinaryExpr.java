package io.alnovis.protowrapper.ir.expr;

import java.util.Objects;

/**
 * Represents a binary expression in the IR.
 *
 * <p>A binary expression combines two operands with an operator. The supported
 * operators are defined in {@link BinaryOp} and include:
 * <ul>
 *   <li>Arithmetic: {@code +}, {@code -}, {@code *}, {@code /}, {@code %}</li>
 *   <li>Comparison: {@code ==}, {@code !=}, {@code <}, {@code <=}, {@code >}, {@code >=}</li>
 *   <li>Logical: {@code &&}, {@code ||}</li>
 *   <li>Bitwise: {@code &}, {@code |}, {@code ^}, {@code <<}, {@code >>}, {@code >>>}</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * a + b
 * x == y
 * condition1 && condition2
 * flags & mask
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Arithmetic: a + b
 * BinaryExpr add = new BinaryExpr(Expr.var("a"), BinaryOp.ADD, Expr.var("b"));
 *
 * // Comparison: x == 0
 * BinaryExpr eq = new BinaryExpr(Expr.var("x"), BinaryOp.EQ, Expr.literal(0));
 *
 * // Logical: cond1 && cond2
 * BinaryExpr and = new BinaryExpr(Expr.var("cond1"), BinaryOp.AND, Expr.var("cond2"));
 *
 * // Using the Expr DSL (recommended)
 * Expression add = Expr.add(Expr.var("a"), Expr.var("b"));
 * Expression eq = Expr.eq(Expr.var("x"), Expr.literal(0));
 * Expression and = Expr.and(Expr.var("cond1"), Expr.var("cond2"));
 * }</pre>
 *
 * <p><b>Operator precedence:</b> The IR represents expressions as a tree, so precedence
 * is implicit in the structure. The emitter should add parentheses when necessary
 * based on target language precedence rules.
 *
 * @param left     the left operand; must not be null
 * @param operator the binary operator; must not be null
 * @param right    the right operand; must not be null
 * @see BinaryOp
 * @see io.alnovis.protowrapper.dsl.Expr#add(Expression, Expression)
 * @see io.alnovis.protowrapper.dsl.Expr#eq(Expression, Expression)
 * @see io.alnovis.protowrapper.dsl.Expr#and(Expression, Expression)
 * @since 2.4.0
 */
public record BinaryExpr(
        Expression left,
        BinaryOp operator,
        Expression right
) implements Expression {

    /**
     * Creates a new BinaryExpr with validation.
     *
     * @param left     the left operand
     * @param operator the binary operator
     * @param right    the right operand
     * @throws NullPointerException if any argument is null
     */
    public BinaryExpr {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(operator, "operator must not be null");
        Objects.requireNonNull(right, "right must not be null");
    }

    /**
     * Returns {@code true} if this is an arithmetic expression.
     *
     * @return {@code true} if the operator is arithmetic
     */
    public boolean isArithmetic() {
        return operator.isArithmetic();
    }

    /**
     * Returns {@code true} if this is a comparison expression.
     *
     * @return {@code true} if the operator is a comparison
     */
    public boolean isComparison() {
        return operator.isComparison();
    }

    /**
     * Returns {@code true} if this is a logical expression.
     *
     * @return {@code true} if the operator is logical
     */
    public boolean isLogical() {
        return operator.isLogical();
    }

    /**
     * Returns {@code true} if this is a bitwise expression.
     *
     * @return {@code true} if the operator is bitwise
     */
    public boolean isBitwise() {
        return operator.isBitwise();
    }
}
