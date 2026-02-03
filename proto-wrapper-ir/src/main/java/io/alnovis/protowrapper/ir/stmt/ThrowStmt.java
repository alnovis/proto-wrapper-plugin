package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.ConstructorCallExpr;
import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.type.TypeRef;

import java.util.Objects;

/**
 * Represents a throw statement in the IR.
 *
 * <p>A throw statement throws an exception, transferring control to the
 * nearest matching catch block or propagating up the call stack.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Throw existing exception
 * throw e;
 *
 * // Throw new exception
 * throw new IllegalArgumentException("Invalid value");
 *
 * // Throw with cause
 * throw new RuntimeException("Failed", cause);
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // throw e;
 * ThrowStmt throwExisting = new ThrowStmt(Expr.var("e"));
 *
 * // throw new IllegalArgumentException("Invalid value");
 * ThrowStmt throwNew = new ThrowStmt(
 *     Expr.new_(Types.type("java.lang.IllegalArgumentException"),
 *         Expr.literal("Invalid value"))
 * );
 *
 * // Using the Stmt DSL (recommended)
 * Statement throwStmt = Stmt.throw_(Expr.var("e"));
 * Statement throwNew = Stmt.throwNew(
 *     Types.type("java.lang.IllegalArgumentException"),
 *     Expr.literal("Invalid value")
 * );
 * }</pre>
 *
 * @param exception the exception to throw; must not be null
 * @see TryCatchStmt
 * @see io.alnovis.protowrapper.dsl.Stmt#throw_(Expression)
 * @see io.alnovis.protowrapper.dsl.Stmt#throwNew(TypeRef, Expression...)
 * @since 2.4.0
 */
public record ThrowStmt(Expression exception) implements Statement {

    /**
     * Creates a new ThrowStmt with validation.
     *
     * @param exception the exception to throw
     * @throws NullPointerException if exception is null
     */
    public ThrowStmt {
        Objects.requireNonNull(exception, "exception must not be null");
    }

    /**
     * Returns {@code true} if this throws a new exception (constructor call).
     *
     * @return {@code true} if exception is a ConstructorCallExpr
     */
    public boolean throwsNewException() {
        return exception instanceof ConstructorCallExpr;
    }
}
