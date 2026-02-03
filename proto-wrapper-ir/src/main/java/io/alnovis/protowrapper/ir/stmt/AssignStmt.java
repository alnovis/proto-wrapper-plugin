package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.expr.FieldAccessExpr;
import io.alnovis.protowrapper.ir.expr.VarRefExpr;

import java.util.Objects;

/**
 * Represents an assignment statement in the IR.
 *
 * <p>An assignment statement sets the value of a variable or field.
 * The target must be an assignable expression (variable reference or field access).
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Variable assignment
 * count = 0;
 * name = "John";
 *
 * // Field assignment
 * this.value = newValue;
 * obj.field = expr;
 *
 * // Array element assignment (not directly supported, use method call)
 * array[i] = value;
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // count = 0;
 * AssignStmt assign = new AssignStmt(
 *     Expr.var("count"),
 *     Expr.literal(0)
 * );
 *
 * // this.value = newValue;
 * AssignStmt fieldAssign = new AssignStmt(
 *     Expr.field(Expr.this_(), "value"),
 *     Expr.var("newValue")
 * );
 *
 * // Using the Stmt DSL (recommended)
 * Statement assign = Stmt.assign(Expr.var("count"), Expr.literal(0));
 * Statement fieldAssign = Stmt.assign(
 *     Expr.field(Expr.this_(), "value"),
 *     Expr.var("newValue")
 * );
 * }</pre>
 *
 * <p><b>Note:</b> Compound assignments (+=, -=, etc.) and increment/decrement
 * operators are not directly supported. They should be expanded to full
 * assignment expressions or represented as method calls.
 *
 * @param target the expression to assign to (VarRefExpr or FieldAccessExpr);
 *               must not be null
 * @param value  the value to assign; must not be null
 * @see io.alnovis.protowrapper.dsl.Stmt#assign(Expression, Expression)
 * @since 2.4.0
 */
public record AssignStmt(
        Expression target,
        Expression value
) implements Statement {

    /**
     * Creates a new AssignStmt with validation.
     *
     * @param target the assignment target
     * @param value  the value to assign
     * @throws NullPointerException if target or value is null
     */
    public AssignStmt {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * Returns {@code true} if the target is a simple variable reference.
     *
     * @return {@code true} if target is a VarRefExpr
     */
    public boolean isVariableAssignment() {
        return target instanceof VarRefExpr;
    }

    /**
     * Returns {@code true} if the target is a field access.
     *
     * @return {@code true} if target is a FieldAccessExpr
     */
    public boolean isFieldAssignment() {
        return target instanceof FieldAccessExpr;
    }
}
