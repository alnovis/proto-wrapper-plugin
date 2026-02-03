package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;

/**
 * Represents a return statement in the IR.
 *
 * <p>A return statement exits the current method and optionally returns a value
 * to the caller. For void methods, the value should be null.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Return with value
 * return result;
 * return x + y;
 * return null;
 *
 * // Return without value (void methods)
 * return;
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Return with value: return result;
 * ReturnStmt retValue = new ReturnStmt(Expr.var("result"));
 *
 * // Return without value (void): return;
 * ReturnStmt retVoid = new ReturnStmt(null);
 *
 * // Using the Stmt DSL (recommended)
 * Statement ret = Stmt.return_(Expr.var("result"));
 * Statement retVoid = Stmt.returnVoid();
 * }</pre>
 *
 * @param value the expression to return; null for void return
 * @see io.alnovis.protowrapper.dsl.Stmt#return_(Expression)
 * @see io.alnovis.protowrapper.dsl.Stmt#returnVoid()
 * @since 2.4.0
 */
public record ReturnStmt(Expression value) implements Statement {

    /**
     * Creates a void return statement (return without a value).
     *
     * @return a return statement with no value
     */
    public static ReturnStmt voidReturn() {
        return new ReturnStmt(null);
    }

    /**
     * Returns {@code true} if this is a void return (no value).
     *
     * @return {@code true} if value is null
     */
    public boolean isVoid() {
        return value == null;
    }

    /**
     * Returns {@code true} if this return has a value.
     *
     * @return {@code true} if value is not null
     */
    public boolean hasValue() {
        return value != null;
    }
}
