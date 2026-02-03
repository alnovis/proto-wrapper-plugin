package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;

import java.util.List;
import java.util.Objects;

/**
 * Represents a catch clause in a try-catch statement.
 *
 * <p>A catch clause specifies an exception type to catch, a variable name
 * to hold the caught exception, and a body of statements to execute.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Single exception type
 * catch (IOException e) {
 *     handleIOError(e);
 * }
 *
 * // Multi-catch (Java 7+) - not directly supported, use separate clauses
 * catch (IOException | SQLException e) {
 *     handleError(e);
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // catch (IOException e) { ... }
 * CatchClause ioClause = new CatchClause(
 *     "e",
 *     Types.type("java.io.IOException"),
 *     List.of(Stmt.expr(Expr.call(null, "handleIOError", Expr.var("e"))))
 * );
 * }</pre>
 *
 * @param variableName  the name of the exception variable; must not be null
 * @param exceptionType the type of exception to catch; must not be null
 * @param body          the statements to execute when this exception is caught;
 *                      the list is copied and made immutable
 * @see TryCatchStmt
 * @since 2.4.0
 */
public record CatchClause(
        String variableName,
        TypeRef exceptionType,
        List<Statement> body
) {

    /**
     * Creates a new CatchClause with validation.
     *
     * @param variableName  the exception variable name
     * @param exceptionType the exception type to catch
     * @param body          the catch body (may be null, treated as empty list)
     * @throws NullPointerException if variableName or exceptionType is null
     */
    public CatchClause {
        Objects.requireNonNull(variableName, "variableName must not be null");
        if (variableName.isBlank()) {
            throw new IllegalArgumentException("variableName must not be empty or blank");
        }
        Objects.requireNonNull(exceptionType, "exceptionType must not be null");
        body = body == null ? List.of() : List.copyOf(body);
    }

    /**
     * Returns {@code true} if this catch clause has an empty body.
     *
     * @return {@code true} if body is empty
     */
    public boolean hasEmptyBody() {
        return body.isEmpty();
    }
}
