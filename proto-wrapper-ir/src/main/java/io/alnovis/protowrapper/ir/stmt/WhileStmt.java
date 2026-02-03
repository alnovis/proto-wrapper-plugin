package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.Expression;

import java.util.List;
import java.util.Objects;

/**
 * Represents a while loop statement in the IR.
 *
 * <p>A while loop repeatedly executes a body as long as the condition is true.
 * The condition is checked before each iteration.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Standard while
 * while (hasMore()) {
 *     process(next());
 * }
 *
 * // Infinite loop (use break to exit)
 * while (true) {
 *     if (done) break;
 *     doWork();
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // while (hasMore()) { ... }
 * WhileStmt whileLoop = new WhileStmt(
 *     Expr.call(null, "hasMore"),
 *     List.of(
 *         Stmt.expr(Expr.call(null, "process", Expr.call(null, "next")))
 *     )
 * );
 *
 * // Using the Stmt DSL (recommended)
 * Statement whileLoop = Stmt.while_(Expr.call(null, "hasMore"),
 *     Stmt.expr(Expr.call(null, "process", Expr.call(null, "next")))
 * );
 * }</pre>
 *
 * @param condition the loop condition; must not be null
 * @param body      the loop body statements; the list is copied and made immutable
 * @see ForStmt
 * @see ForEachStmt
 * @since 2.4.0
 */
public record WhileStmt(
        Expression condition,
        List<Statement> body
) implements Statement {

    /**
     * Creates a new WhileStmt with validation.
     *
     * @param condition the loop condition
     * @param body      the loop body (may be null, treated as empty list)
     * @throws NullPointerException if condition is null
     */
    public WhileStmt {
        Objects.requireNonNull(condition, "condition must not be null");
        body = body == null ? List.of() : List.copyOf(body);
    }

    /**
     * Returns {@code true} if this loop has an empty body.
     *
     * @return {@code true} if body is empty
     */
    public boolean hasEmptyBody() {
        return body.isEmpty();
    }
}
