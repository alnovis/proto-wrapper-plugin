package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;

import java.util.List;
import java.util.Objects;

/**
 * Represents an if-else statement in the IR.
 *
 * <p>An if statement conditionally executes one of two branches based on
 * a boolean condition. The else branch is optional.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Simple if
 * if (condition) {
 *     doSomething();
 * }
 *
 * // If-else
 * if (condition) {
 *     doA();
 * } else {
 *     doB();
 * }
 *
 * // If-else if-else (chain)
 * if (condition1) {
 *     doA();
 * } else if (condition2) {
 *     doB();
 * } else {
 *     doC();
 * }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Simple if
 * IfStmt simpleIf = new IfStmt(
 *     Expr.var("condition"),
 *     List.of(Stmt.expr(Expr.call("doSomething"))),
 *     List.of()  // no else branch
 * );
 *
 * // If-else
 * IfStmt ifElse = new IfStmt(
 *     Expr.var("condition"),
 *     List.of(Stmt.expr(Expr.call("doA"))),
 *     List.of(Stmt.expr(Expr.call("doB")))
 * );
 *
 * // Using the Stmt DSL (recommended)
 * Statement ifStmt = Stmt.if_(Expr.var("condition"))
 *     .then(Stmt.return_(Expr.literal(true)))
 *     .else_(Stmt.return_(Expr.literal(false)))
 *     .build();
 * }</pre>
 *
 * <p><b>Else-if chains:</b> An else-if is represented by having an IfStmt
 * as the only statement in the else branch.
 *
 * @param condition  the boolean condition; must not be null
 * @param thenBranch the statements to execute if true; may be empty;
 *                   the list is copied and made immutable
 * @param elseBranch the statements to execute if false; may be empty for no else;
 *                   the list is copied and made immutable
 * @see io.alnovis.protowrapper.dsl.Stmt#if_(Expression)
 * @since 2.4.0
 */
public record IfStmt(
        Expression condition,
        List<Statement> thenBranch,
        List<Statement> elseBranch
) implements Statement {

    /**
     * Creates a new IfStmt with validation.
     *
     * @param condition  the condition expression
     * @param thenBranch the then statements (may be null, treated as empty list)
     * @param elseBranch the else statements (may be null, treated as empty list)
     * @throws NullPointerException if condition is null
     */
    public IfStmt {
        Objects.requireNonNull(condition, "condition must not be null");
        thenBranch = thenBranch == null ? List.of() : List.copyOf(thenBranch);
        elseBranch = elseBranch == null ? List.of() : List.copyOf(elseBranch);
    }

    /**
     * Returns {@code true} if this if statement has an else branch.
     *
     * @return {@code true} if else branch is not empty
     */
    public boolean hasElse() {
        return !elseBranch.isEmpty();
    }

    /**
     * Returns {@code true} if this if statement has no else branch.
     *
     * @return {@code true} if else branch is empty
     */
    public boolean hasNoElse() {
        return elseBranch.isEmpty();
    }

    /**
     * Returns {@code true} if the else branch is an else-if (single IfStmt).
     *
     * @return {@code true} if else contains exactly one IfStmt
     */
    public boolean isElseIf() {
        return elseBranch.size() == 1 && elseBranch.get(0) instanceof IfStmt;
    }
}
