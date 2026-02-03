package io.alnovis.protowrapper.ir.expr;

import java.util.Objects;

/**
 * Represents a variable reference expression in the IR.
 *
 * <p>A variable reference reads the value of a local variable or parameter.
 * It consists of just the variable name.
 *
 * <p>Example in Java:
 * <pre>{@code
 * int x = 10;
 * int y = x;  // 'x' here is a VarRefExpr
 *
 * void process(String input) {
 *     System.out.println(input);  // 'input' here is a VarRefExpr
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Direct construction
 * VarRefExpr x = new VarRefExpr("x");
 *
 * // Using the Expr DSL (recommended)
 * Expression x = Expr.var("x");
 * Expression param = Expr.var("input");
 * }</pre>
 *
 * <p><b>Note:</b> Field references (e.g., {@code this.field} or {@code obj.field})
 * should use {@link FieldAccessExpr} instead.
 *
 * @param name the name of the variable being referenced; must not be null
 * @see FieldAccessExpr
 * @see io.alnovis.protowrapper.dsl.Expr#var(String)
 * @since 2.4.0
 */
public record VarRefExpr(String name) implements Expression {

    /**
     * Creates a new VarRefExpr with validation.
     *
     * @param name the variable name
     * @throws NullPointerException     if name is null
     * @throws IllegalArgumentException if name is empty or blank
     */
    public VarRefExpr {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty or blank");
        }
    }
}
