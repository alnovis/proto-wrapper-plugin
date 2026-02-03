package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;

import java.util.List;
import java.util.Objects;

/**
 * Represents a traditional for loop statement in the IR.
 *
 * <p>A traditional for loop has three parts:
 * <ul>
 *   <li>Initialization: executed once before the loop starts</li>
 *   <li>Condition: checked before each iteration</li>
 *   <li>Update: executed after each iteration</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Standard counting loop
 * for (int i = 0; i < n; i++) {
 *     process(i);
 * }
 *
 * // Reverse iteration
 * for (int i = n - 1; i >= 0; i--) {
 *     process(i);
 * }
 *
 * // Multiple variables
 * for (int i = 0, j = n; i < j; i++, j--) {
 *     swap(arr, i, j);
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // for (int i = 0; i < n; i++) { ... }
 * ForStmt forLoop = new ForStmt(
 *     Stmt.var("i", Types.INT, Expr.literal(0)),  // initialization
 *     Expr.lt(Expr.var("i"), Expr.var("n")),      // condition
 *     Expr.call(null, "i++"),                      // update (simplified)
 *     List.of(...)                                 // body
 * );
 * }</pre>
 *
 * <p><b>Note:</b> For most iteration over collections, prefer {@link ForEachStmt}.
 * Use ForStmt when you need the index or more control over iteration.
 *
 * @param initialization the initialization statement (typically VarDeclStmt); may be null
 * @param condition      the loop condition; null means infinite loop (while true)
 * @param update         the update expression executed after each iteration; may be null
 * @param body           the loop body statements; the list is copied and made immutable
 * @see ForEachStmt
 * @see WhileStmt
 * @since 2.4.0
 */
public record ForStmt(
        Statement initialization,
        Expression condition,
        Expression update,
        List<Statement> body
) implements Statement {

    /**
     * Creates a new ForStmt with validation.
     *
     * @param initialization the initialization (may be null)
     * @param condition      the loop condition (may be null for infinite loop)
     * @param update         the update expression (may be null)
     * @param body           the loop body (may be null, treated as empty list)
     */
    public ForStmt {
        body = body == null ? List.of() : List.copyOf(body);
    }

    /**
     * Returns {@code true} if this loop has initialization.
     *
     * @return {@code true} if initialization is not null
     */
    public boolean hasInitialization() {
        return initialization != null;
    }

    /**
     * Returns {@code true} if this loop has a condition.
     *
     * @return {@code true} if condition is not null
     */
    public boolean hasCondition() {
        return condition != null;
    }

    /**
     * Returns {@code true} if this loop has an update expression.
     *
     * @return {@code true} if update is not null
     */
    public boolean hasUpdate() {
        return update != null;
    }

    /**
     * Returns {@code true} if this is an infinite loop (no condition).
     *
     * @return {@code true} if condition is null
     */
    public boolean isInfinite() {
        return condition == null;
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
