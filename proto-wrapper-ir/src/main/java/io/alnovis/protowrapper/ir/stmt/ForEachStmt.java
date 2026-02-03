package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;

import java.util.List;
import java.util.Objects;

/**
 * Represents an enhanced for loop (for-each) statement in the IR.
 *
 * <p>A for-each loop iterates over elements of an array or Iterable.
 * It is simpler than a traditional for loop when you need to process
 * each element without knowing the index.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Iterate over array
 * for (int value : numbers) {
 *     sum += value;
 * }
 *
 * // Iterate over collection
 * for (String name : names) {
 *     System.out.println(name);
 * }
 *
 * // With final variable
 * for (final String item : items) {
 *     process(item);
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // for (String name : names) { ... }
 * ForEachStmt forEach = new ForEachStmt(
 *     "name",
 *     Types.STRING,
 *     Expr.var("names"),
 *     List.of(
 *         Stmt.expr(Expr.call(Expr.var("System.out"), "println", Expr.var("name")))
 *     )
 * );
 *
 * // Using the Stmt DSL (recommended)
 * Statement forEach = Stmt.forEach("item", Types.STRING, Expr.var("items"),
 *     Stmt.expr(Expr.call(Expr.this_(), "process", Expr.var("item")))
 * );
 * }</pre>
 *
 * @param variableName the name of the loop variable; must not be null
 * @param variableType the type of the loop variable; must not be null
 * @param iterable     the array or Iterable to iterate over; must not be null
 * @param body         the statements to execute for each element;
 *                     the list is copied and made immutable
 * @see ForStmt
 * @see io.alnovis.protowrapper.dsl.Stmt#forEach(String, TypeRef, Expression, Statement...)
 * @since 2.4.0
 */
public record ForEachStmt(
        String variableName,
        TypeRef variableType,
        Expression iterable,
        List<Statement> body
) implements Statement {

    /**
     * Creates a new ForEachStmt with validation.
     *
     * @param variableName the loop variable name
     * @param variableType the loop variable type
     * @param iterable     the iterable expression
     * @param body         the loop body statements (may be null, treated as empty list)
     * @throws NullPointerException if variableName, variableType, or iterable is null
     */
    public ForEachStmt {
        Objects.requireNonNull(variableName, "variableName must not be null");
        if (variableName.isBlank()) {
            throw new IllegalArgumentException("variableName must not be empty or blank");
        }
        Objects.requireNonNull(variableType, "variableType must not be null");
        Objects.requireNonNull(iterable, "iterable must not be null");
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
