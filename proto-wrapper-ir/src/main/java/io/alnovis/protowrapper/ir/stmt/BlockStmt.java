package io.alnovis.protowrapper.ir.stmt;

import java.util.List;

/**
 * Represents a block of statements in the IR.
 *
 * <p>A block groups multiple statements together and creates a new scope
 * for local variables. Blocks are commonly used in control flow structures
 * and can also be used standalone for scoping.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Block in method
 * void process() {
 *     // statements
 * }
 *
 * // Standalone block for scoping
 * {
 *     int temp = compute();
 *     use(temp);
 * }
 * // temp is out of scope here
 *
 * // Block in if statement
 * if (condition) {
 *     doA();
 *     doB();
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // { doA(); doB(); }
 * BlockStmt block = new BlockStmt(List.of(
 *     Stmt.expr(Expr.call(null, "doA")),
 *     Stmt.expr(Expr.call(null, "doB"))
 * ));
 *
 * // Using the Stmt DSL (recommended)
 * Statement block = Stmt.block(
 *     Stmt.expr(Expr.call(null, "doA")),
 *     Stmt.expr(Expr.call(null, "doB"))
 * );
 * }</pre>
 *
 * <p><b>Note:</b> In many cases, blocks are implicit in other statement types
 * (like the body of an if or loop). BlockStmt is mainly useful for:
 * <ul>
 *   <li>Grouping statements for explicit scoping</li>
 *   <li>Method/constructor bodies</li>
 * </ul>
 *
 * @param statements the statements in this block; the list is copied and made immutable
 * @see io.alnovis.protowrapper.dsl.Stmt#block(Statement...)
 * @since 2.4.0
 */
public record BlockStmt(List<Statement> statements) implements Statement {

    /**
     * Creates a new BlockStmt.
     *
     * @param statements the statements (may be null, treated as empty list)
     */
    public BlockStmt {
        statements = statements == null ? List.of() : List.copyOf(statements);
    }

    /**
     * Creates an empty block.
     *
     * @return a block with no statements
     */
    public static BlockStmt empty() {
        return new BlockStmt(List.of());
    }

    /**
     * Creates a block with the given statements.
     *
     * @param statements the statements
     * @return a new block
     */
    public static BlockStmt of(Statement... statements) {
        return new BlockStmt(List.of(statements));
    }

    /**
     * Returns {@code true} if this block is empty.
     *
     * @return {@code true} if statements list is empty
     */
    public boolean isEmpty() {
        return statements.isEmpty();
    }

    /**
     * Returns the number of statements in this block.
     *
     * @return the statement count
     */
    public int size() {
        return statements.size();
    }
}
