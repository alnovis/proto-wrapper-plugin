package io.alnovis.protowrapper.ir.stmt;

/**
 * Represents a break statement in the IR.
 *
 * <p>A break statement exits the immediately enclosing loop (for, while, do-while)
 * or switch statement. An optional label can be used to break out of a labeled
 * outer loop.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Simple break
 * for (int i = 0; i < n; i++) {
 *     if (found) break;
 * }
 *
 * // Labeled break (breaks outer loop)
 * outer:
 * for (int i = 0; i < rows; i++) {
 *     for (int j = 0; j < cols; j++) {
 *         if (found) break outer;
 *     }
 * }
 *
 * // Break in switch
 * switch (value) {
 *     case 1:
 *         doSomething();
 *         break;
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // break;
 * BreakStmt simpleBreak = new BreakStmt(null);
 *
 * // break outer;
 * BreakStmt labeledBreak = new BreakStmt("outer");
 *
 * // Using the singleton for unlabeled break
 * Statement brk = BreakStmt.INSTANCE;
 * }</pre>
 *
 * @param label the label to break to; null for unlabeled break
 * @see ContinueStmt
 * @see ForStmt
 * @see WhileStmt
 * @since 2.4.0
 */
public record BreakStmt(String label) implements Statement {

    /**
     * Singleton instance for unlabeled break statements.
     *
     * <p>Use this constant instead of creating new instances for simple break.
     */
    public static final BreakStmt INSTANCE = new BreakStmt(null);

    /**
     * Returns {@code true} if this is a labeled break.
     *
     * @return {@code true} if label is not null
     */
    public boolean isLabeled() {
        return label != null;
    }

    /**
     * Returns {@code true} if this is an unlabeled break.
     *
     * @return {@code true} if label is null
     */
    public boolean isUnlabeled() {
        return label == null;
    }
}
