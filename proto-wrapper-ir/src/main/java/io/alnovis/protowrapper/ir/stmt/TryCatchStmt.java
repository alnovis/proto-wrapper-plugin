package io.alnovis.protowrapper.ir.stmt;

import java.util.List;
import java.util.Objects;

/**
 * Represents a try-catch-finally statement in the IR.
 *
 * <p>A try-catch-finally statement provides exception handling:
 * <ul>
 *   <li>The try block contains code that might throw exceptions</li>
 *   <li>Catch clauses handle specific exception types</li>
 *   <li>The finally block (optional) always executes, for cleanup</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Try-catch
 * try {
 *     riskyOperation();
 * } catch (IOException e) {
 *     handleError(e);
 * }
 *
 * // Try-catch-finally
 * try {
 *     resource.open();
 *     process(resource);
 * } catch (Exception e) {
 *     log(e);
 * } finally {
 *     resource.close();
 * }
 *
 * // Try-finally (no catch)
 * try {
 *     acquire();
 *     use();
 * } finally {
 *     release();
 * }
 *
 * // Multiple catch clauses
 * try {
 *     operation();
 * } catch (IOException e) {
 *     handleIO(e);
 * } catch (SQLException e) {
 *     handleSQL(e);
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // try { ... } catch (IOException e) { ... }
 * TryCatchStmt tryCatch = new TryCatchStmt(
 *     List.of(Stmt.expr(Expr.call(null, "riskyOperation"))),
 *     List.of(new CatchClause("e", Types.type("java.io.IOException"),
 *         List.of(Stmt.expr(Expr.call(null, "handleError", Expr.var("e")))))),
 *     List.of()  // no finally
 * );
 * }</pre>
 *
 * <p><b>Note:</b> Try-with-resources (Java 7+) is not directly supported.
 * It should be expanded to equivalent try-finally code.
 *
 * @param tryBlock     the statements in the try block; must not be null
 * @param catchClauses the catch clauses; may be empty if finally is present;
 *                     the list is copied and made immutable
 * @param finallyBlock the statements in the finally block; may be empty;
 *                     the list is copied and made immutable
 * @see CatchClause
 * @see ThrowStmt
 * @since 2.4.0
 */
public record TryCatchStmt(
        List<Statement> tryBlock,
        List<CatchClause> catchClauses,
        List<Statement> finallyBlock
) implements Statement {

    /**
     * Creates a new TryCatchStmt with validation.
     *
     * @param tryBlock     the try block statements
     * @param catchClauses the catch clauses (may be null, treated as empty list)
     * @param finallyBlock the finally block statements (may be null, treated as empty list)
     * @throws NullPointerException     if tryBlock is null
     * @throws IllegalArgumentException if both catchClauses and finallyBlock are empty
     */
    public TryCatchStmt {
        Objects.requireNonNull(tryBlock, "tryBlock must not be null");
        tryBlock = List.copyOf(tryBlock);
        catchClauses = catchClauses == null ? List.of() : List.copyOf(catchClauses);
        finallyBlock = finallyBlock == null ? List.of() : List.copyOf(finallyBlock);

        if (catchClauses.isEmpty() && finallyBlock.isEmpty()) {
            throw new IllegalArgumentException("Try statement must have at least one catch or a finally block");
        }
    }

    /**
     * Returns {@code true} if this statement has catch clauses.
     *
     * @return {@code true} if catchClauses is not empty
     */
    public boolean hasCatch() {
        return !catchClauses.isEmpty();
    }

    /**
     * Returns {@code true} if this statement has a finally block.
     *
     * @return {@code true} if finallyBlock is not empty
     */
    public boolean hasFinally() {
        return !finallyBlock.isEmpty();
    }

    /**
     * Returns {@code true} if this is a try-finally (no catch clauses).
     *
     * @return {@code true} if catchClauses is empty and finallyBlock is not
     */
    public boolean isTryFinally() {
        return catchClauses.isEmpty() && !finallyBlock.isEmpty();
    }

    /**
     * Returns the number of catch clauses.
     *
     * @return the catch clause count
     */
    public int catchCount() {
        return catchClauses.size();
    }
}
