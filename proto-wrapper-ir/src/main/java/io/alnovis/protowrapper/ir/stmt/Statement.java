package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;

/**
 * Represents a statement in the IR.
 *
 * <p>Statements are executable code constructs that perform actions but do not
 * produce values (unlike expressions). They form the body of methods and
 * constructors.
 *
 * <p>The IR supports the following statement types:
 * <ul>
 *   <li>{@link ReturnStmt} - return statement</li>
 *   <li>{@link IfStmt} - if-else conditional</li>
 *   <li>{@link ForEachStmt} - enhanced for loop (for-each)</li>
 *   <li>{@link ForStmt} - traditional for loop</li>
 *   <li>{@link WhileStmt} - while loop</li>
 *   <li>{@link VarDeclStmt} - local variable declaration</li>
 *   <li>{@link AssignStmt} - assignment statement</li>
 *   <li>{@link ExpressionStmt} - expression used as statement</li>
 *   <li>{@link ThrowStmt} - throw exception</li>
 *   <li>{@link TryCatchStmt} - try-catch-finally</li>
 *   <li>{@link BlockStmt} - block of statements</li>
 *   <li>{@link BreakStmt} - break statement</li>
 *   <li>{@link ContinueStmt} - continue statement</li>
 * </ul>
 *
 * <p>Example usage with DSL:
 * <pre>{@code
 * // Return statement
 * Statement ret = Stmt.return_(Expr.var("result"));
 *
 * // Variable declaration
 * Statement varDecl = Stmt.var(Types.INT, "count", Expr.literal(0));
 *
 * // If statement
 * Statement ifStmt = Stmt.if_(Expr.var("condition"),
 *     Stmt.return_(Expr.literal(true)),
 *     Stmt.return_(Expr.literal(false)));
 *
 * // For-each loop
 * Statement forEach = Stmt.forEach(Types.STRING, "item", Expr.var("items"),
 *     Stmt.expr(Expr.call(Expr.this_(), "process", Expr.var("item")))
 * );
 * }</pre>
 *
 * @see io.alnovis.protowrapper.dsl.Stmt
 * @since 2.4.0
 */
public sealed interface Statement permits
        ReturnStmt,
        IfStmt,
        ForEachStmt,
        ForStmt,
        WhileStmt,
        VarDeclStmt,
        AssignStmt,
        ExpressionStmt,
        ThrowStmt,
        TryCatchStmt,
        BlockStmt,
        BreakStmt,
        ContinueStmt {
}
