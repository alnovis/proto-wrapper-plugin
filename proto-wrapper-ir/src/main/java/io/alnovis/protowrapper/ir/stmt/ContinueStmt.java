package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;

/**
 * Represents a continue statement in the IR.
 *
 * <p>A continue statement skips the rest of the current iteration and proceeds
 * to the next iteration of the immediately enclosing loop. An optional label
 * can be used to continue an outer loop.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Simple continue
 * for (int i = 0; i < n; i++) {
 *     if (skip(i)) continue;
 *     process(i);
 * }
 *
 * // Labeled continue (continues outer loop)
 * outer:
 * for (int i = 0; i < rows; i++) {
 *     for (int j = 0; j < cols; j++) {
 *         if (skip(i, j)) continue outer;
 *         process(i, j);
 *     }
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // continue;
 * ContinueStmt simpleContinue = new ContinueStmt(null);
 *
 * // continue outer;
 * ContinueStmt labeledContinue = new ContinueStmt("outer");
 *
 * // Using the singleton for unlabeled continue
 * Statement cont = ContinueStmt.INSTANCE;
 * }</pre>
 *
 * @param label the label to continue to; null for unlabeled continue
 * @see BreakStmt
 * @see ForStmt
 * @see WhileStmt
 * @since 2.4.0
 */
public record ContinueStmt(String label) implements Statement {

    /**
     * Singleton instance for unlabeled continue statements.
     *
     * <p>Use this constant instead of creating new instances for simple continue.
     */
    public static final ContinueStmt INSTANCE = new ContinueStmt(null);

    /**
     * Returns {@code true} if this is a labeled continue.
     *
     * @return {@code true} if label is not null
     */
    public boolean isLabeled() {
        return label != null;
    }

    /**
     * Returns {@code true} if this is an unlabeled continue.
     *
     * @return {@code true} if label is null
     */
    public boolean isUnlabeled() {
        return label == null;
    }
}
