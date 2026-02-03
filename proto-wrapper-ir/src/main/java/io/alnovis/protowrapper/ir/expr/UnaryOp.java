package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;

/**
 * Enumeration of unary operators for {@link UnaryExpr}.
 *
 * <p>Unary operators take a single operand and produce a result:
 * <ul>
 *   <li><b>NOT:</b> Logical negation ({@code !})</li>
 *   <li><b>NEG:</b> Arithmetic negation ({@code -})</li>
 *   <li><b>BIT_NOT:</b> Bitwise complement ({@code ~})</li>
 *   <li><b>PLUS:</b> Unary plus ({@code +}), rarely used explicitly</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Logical NOT: !condition
 * UnaryExpr not = new UnaryExpr(UnaryOp.NOT, Expr.var("condition"));
 *
 * // Arithmetic negation: -value
 * UnaryExpr neg = new UnaryExpr(UnaryOp.NEG, Expr.var("value"));
 *
 * // Bitwise complement: ~mask
 * UnaryExpr bitNot = new UnaryExpr(UnaryOp.BIT_NOT, Expr.var("mask"));
 * }</pre>
 *
 * <p><b>Note:</b> Increment ({@code ++}) and decrement ({@code --}) operators
 * are not included as they have side effects and are better represented as
 * statements or compound expressions.
 *
 * @see UnaryExpr
 * @since 2.4.0
 */
public enum UnaryOp {
    /**
     * Logical NOT operator ({@code !}).
     *
     * <p>Inverts a boolean value: {@code !true == false}, {@code !false == true}.
     *
     * <p>Example: {@code !isValid}
     */
    NOT("!", true),

    /**
     * Arithmetic negation operator ({@code -}).
     *
     * <p>Negates a numeric value: {@code -5}, {@code -x}.
     *
     * <p>Example: {@code -value}
     */
    NEG("-", true),

    /**
     * Bitwise complement operator ({@code ~}).
     *
     * <p>Inverts all bits in an integer value.
     *
     * <p>Example: {@code ~mask}
     */
    BIT_NOT("~", true),

    /**
     * Unary plus operator ({@code +}).
     *
     * <p>Explicit positive sign for a numeric value. Rarely needed explicitly
     * but included for completeness.
     *
     * <p>Example: {@code +value}
     */
    PLUS("+", true);

    private final String symbol;
    private final boolean prefix;

    UnaryOp(String symbol, boolean prefix) {
        this.symbol = symbol;
        this.prefix = prefix;
    }

    /**
     * Returns the Java symbol for this operator.
     *
     * @return the operator symbol (e.g., "!", "-", "~")
     */
    public String symbol() {
        return symbol;
    }

    /**
     * Returns {@code true} if this is a prefix operator.
     *
     * <p>All unary operators in this enum are prefix operators.
     *
     * @return {@code true} if the operator appears before the operand
     */
    public boolean isPrefix() {
        return prefix;
    }
}
