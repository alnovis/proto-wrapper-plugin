package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.stmt.*;
import io.alnovis.protowrapper.ir.type.TypeRef;

import java.util.Arrays;
import java.util.List;

/**
 * Factory class for creating statements.
 *
 * <p>This utility class provides convenient factory methods for creating
 * all kinds of statements used in the IR. It serves as the main entry
 * point for building statement trees in the DSL.
 *
 * <h2>Return Statements</h2>
 * <pre>{@code
 * Statement ret = Stmt.return_();                    // return;
 * Statement retValue = Stmt.return_(Expr.var("x")); // return x;
 * }</pre>
 *
 * <h2>Variable Declarations</h2>
 * <pre>{@code
 * Statement decl = Stmt.var(Types.INT, "count", Expr.literal(0));
 * // int count = 0;
 *
 * Statement finalDecl = Stmt.finalVar(Types.STRING, "name", Expr.literal("John"));
 * // final String name = "John";
 * }</pre>
 *
 * <h2>Assignments</h2>
 * <pre>{@code
 * Statement assign = Stmt.assign(Expr.var("x"), Expr.literal(10));
 * // x = 10;
 *
 * Statement fieldAssign = Stmt.assign(Expr.field("name"), Expr.var("newName"));
 * // this.name = newName;
 * }</pre>
 *
 * <h2>If Statements</h2>
 * <pre>{@code
 * Statement ifStmt = Stmt.if_(condition, thenBody);
 * Statement ifElse = Stmt.if_(condition, thenBody, elseBody);
 * }</pre>
 *
 * <h2>Loops</h2>
 * <pre>{@code
 * // For-each loop
 * Statement forEach = Stmt.forEach(Types.STRING, "item", Expr.var("items"), body);
 * // for (String item : items) { body }
 *
 * // While loop
 * Statement while_ = Stmt.while_(condition, body);
 * // while (condition) { body }
 *
 * // Traditional for loop
 * Statement for_ = Stmt.for_(init, condition, update, body);
 * // for (init; condition; update) { body }
 * }</pre>
 *
 * <h2>Exception Handling</h2>
 * <pre>{@code
 * Statement throw_ = Stmt.throw_(Expr.new_(Types.type("IllegalArgumentException")));
 * // throw new IllegalArgumentException();
 *
 * Statement tryCatch = Stmt.tryCatch(tryBody, catches, finallyBody);
 * // try { tryBody } catch (Ex e) { ... } finally { finallyBody }
 * }</pre>
 *
 * <h2>Control Flow</h2>
 * <pre>{@code
 * Statement break_ = Stmt.break_();       // break;
 * Statement continue_ = Stmt.continue_(); // continue;
 * Statement breakLabel = Stmt.break_("outer"); // break outer;
 * }</pre>
 *
 * @see Statement
 * @see ReturnStmt
 * @see IfStmt
 * @see ForEachStmt
 * @since 2.4.0
 */
public final class Stmt {

    private Stmt() {
        // Utility class, not instantiable
    }

    // ========================================================================
    // Return Statements
    // ========================================================================

    /**
     * Creates a void return statement ({@code return;}).
     *
     * @return a void return statement
     */
    public static ReturnStmt return_() {
        return new ReturnStmt(null);
    }

    /**
     * Creates a return statement with a value ({@code return expr;}).
     *
     * @param value the value to return
     * @return a return statement
     */
    public static ReturnStmt return_(Expression value) {
        return new ReturnStmt(value);
    }

    // ========================================================================
    // Variable Declarations
    // ========================================================================

    /**
     * Creates a variable declaration with initialization.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.var(Types.INT, "count", Expr.literal(0))
     * // int count = 0;
     * }</pre>
     *
     * @param type        the variable type
     * @param name        the variable name
     * @param initializer the initializer expression
     * @return a variable declaration statement
     */
    public static VarDeclStmt var(TypeRef type, String name, Expression initializer) {
        return new VarDeclStmt(name, type, initializer, false);
    }

    /**
     * Creates a variable declaration without initialization.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.var(Types.STRING, "result")
     * // String result;
     * }</pre>
     *
     * @param type the variable type
     * @param name the variable name
     * @return a variable declaration statement
     */
    public static VarDeclStmt var(TypeRef type, String name) {
        return new VarDeclStmt(name, type, null, false);
    }

    /**
     * Creates a final variable declaration with initialization.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.finalVar(Types.STRING, "name", Expr.literal("John"))
     * // final String name = "John";
     * }</pre>
     *
     * @param type        the variable type
     * @param name        the variable name
     * @param initializer the initializer expression
     * @return a final variable declaration statement
     */
    public static VarDeclStmt finalVar(TypeRef type, String name, Expression initializer) {
        return new VarDeclStmt(name, type, initializer, true);
    }

    // ========================================================================
    // Assignment Statements
    // ========================================================================

    /**
     * Creates an assignment statement ({@code target = value;}).
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.assign(Expr.var("x"), Expr.literal(10))
     * // x = 10;
     *
     * Stmt.assign(Expr.field("name"), Expr.var("newName"))
     * // this.name = newName;
     * }</pre>
     *
     * @param target the assignment target (must be assignable)
     * @param value  the value to assign
     * @return an assignment statement
     */
    public static AssignStmt assign(Expression target, Expression value) {
        return new AssignStmt(target, value);
    }

    // ========================================================================
    // Expression Statements
    // ========================================================================

    /**
     * Creates an expression statement.
     *
     * <p>Use this to execute an expression for its side effects, discarding
     * any return value.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.expr(Expr.call("doSomething"))
     * // doSomething();
     * }</pre>
     *
     * @param expression the expression to execute
     * @return an expression statement
     */
    public static ExpressionStmt expr(Expression expression) {
        return new ExpressionStmt(expression);
    }

    // ========================================================================
    // If Statements
    // ========================================================================

    /**
     * Creates an if statement without else.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.if_(condition, Stmt.return_(Expr.literal(true)))
     * // if (condition) return true;
     * }</pre>
     *
     * @param condition the condition expression
     * @param thenBody  the body to execute if true
     * @return an if statement
     */
    public static IfStmt if_(Expression condition, Statement thenBody) {
        return new IfStmt(condition, List.of(thenBody), List.of());
    }

    /**
     * Creates an if-else statement.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.if_(condition,
     *     Stmt.return_(Expr.literal(true)),
     *     Stmt.return_(Expr.literal(false)))
     * // if (condition) return true; else return false;
     * }</pre>
     *
     * @param condition the condition expression
     * @param thenBody  the body to execute if true
     * @param elseBody  the body to execute if false
     * @return an if-else statement
     */
    public static IfStmt if_(Expression condition, Statement thenBody, Statement elseBody) {
        return new IfStmt(condition, List.of(thenBody), List.of(elseBody));
    }

    /**
     * Creates an if statement with statement lists.
     *
     * @param condition  the condition expression
     * @param thenBranch the then branch statements
     * @param elseBranch the else branch statements
     * @return an if-else statement
     */
    public static IfStmt if_(Expression condition, List<Statement> thenBranch, List<Statement> elseBranch) {
        return new IfStmt(condition, thenBranch, elseBranch);
    }

    // ========================================================================
    // Loop Statements
    // ========================================================================

    /**
     * Creates a for-each loop statement.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.forEach(Types.STRING, "item", Expr.var("items"),
     *     Stmt.expr(Expr.call("process", Expr.var("item"))))
     * // for (String item : items) { process(item); }
     * }</pre>
     *
     * @param elementType  the element type
     * @param variableName the loop variable name
     * @param iterable     the iterable expression
     * @param body         the loop body
     * @return a for-each loop statement
     */
    public static ForEachStmt forEach(TypeRef elementType, String variableName,
                                       Expression iterable, Statement body) {
        return new ForEachStmt(variableName, elementType, iterable, List.of(body));
    }

    /**
     * Creates a for-each loop with statement list body.
     *
     * @param elementType  the element type
     * @param variableName the loop variable name
     * @param iterable     the iterable expression
     * @param body         the loop body statements
     * @return a for-each loop statement
     */
    public static ForEachStmt forEach(TypeRef elementType, String variableName,
                                       Expression iterable, List<Statement> body) {
        return new ForEachStmt(variableName, elementType, iterable, body);
    }

    /**
     * Creates a while loop statement.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.while_(Expr.call("hasNext"), body)
     * // while (hasNext()) { body }
     * }</pre>
     *
     * @param condition the loop condition
     * @param body      the loop body
     * @return a while loop statement
     */
    public static WhileStmt while_(Expression condition, Statement body) {
        return new WhileStmt(condition, List.of(body));
    }

    /**
     * Creates a while loop with statement list body.
     *
     * @param condition the loop condition
     * @param body      the loop body statements
     * @return a while loop statement
     */
    public static WhileStmt while_(Expression condition, List<Statement> body) {
        return new WhileStmt(condition, body);
    }

    /**
     * Creates a traditional for loop statement.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.for_(
     *     Stmt.var(Types.INT, "i", Expr.literal(0)),
     *     Expr.lt(Expr.var("i"), Expr.literal(10)),
     *     Expr.postIncrement(Expr.var("i")),
     *     body)
     * // for (int i = 0; i < 10; i++) { body }
     * }</pre>
     *
     * @param init      the initialization statement (may be null)
     * @param condition the loop condition (may be null for infinite loop)
     * @param update    the update expression (may be null)
     * @param body      the loop body
     * @return a for loop statement
     */
    public static ForStmt for_(Statement init, Expression condition, Expression update, Statement body) {
        return new ForStmt(init, condition, update, List.of(body));
    }

    /**
     * Creates a traditional for loop with statement list body.
     *
     * @param init      the initialization statement (may be null)
     * @param condition the loop condition (may be null for infinite loop)
     * @param update    the update expression (may be null)
     * @param body      the loop body statements
     * @return a for loop statement
     */
    public static ForStmt for_(Statement init, Expression condition, Expression update, List<Statement> body) {
        return new ForStmt(init, condition, update, body);
    }

    /**
     * Creates an infinite for loop ({@code for (;;) { body }}).
     *
     * @param body the loop body
     * @return an infinite for loop statement
     */
    public static ForStmt forEver(Statement body) {
        return new ForStmt(null, null, null, List.of(body));
    }

    // ========================================================================
    // Exception Handling
    // ========================================================================

    /**
     * Creates a throw statement.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.throw_(Expr.new_(Types.type("IllegalArgumentException"),
     *     Expr.literal("Invalid argument")))
     * // throw new IllegalArgumentException("Invalid argument");
     * }</pre>
     *
     * @param exception the exception expression
     * @return a throw statement
     */
    public static ThrowStmt throw_(Expression exception) {
        return new ThrowStmt(exception);
    }

    /**
     * Creates a try-catch statement without finally.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.tryCatch(tryBody, List.of(
     *     new CatchClause("e", Types.type("IOException"), catchBody)))
     * // try { tryBody } catch (IOException e) { catchBody }
     * }</pre>
     *
     * @param tryBody the try block statements
     * @param catches the catch clauses
     * @return a try-catch statement
     */
    public static TryCatchStmt tryCatch(List<Statement> tryBody, List<CatchClause> catches) {
        return new TryCatchStmt(tryBody, catches, List.of());
    }

    /**
     * Creates a try-catch-finally statement.
     *
     * @param tryBody     the try block statements
     * @param catches     the catch clauses
     * @param finallyBody the finally block statements
     * @return a try-catch-finally statement
     */
    public static TryCatchStmt tryCatch(List<Statement> tryBody, List<CatchClause> catches, List<Statement> finallyBody) {
        return new TryCatchStmt(tryBody, catches, finallyBody);
    }

    /**
     * Creates a try-finally statement without catch.
     *
     * @param tryBody     the try block statements
     * @param finallyBody the finally block statements
     * @return a try-finally statement
     */
    public static TryCatchStmt tryFinally(List<Statement> tryBody, List<Statement> finallyBody) {
        return new TryCatchStmt(tryBody, List.of(), finallyBody);
    }

    /**
     * Creates a simple try-catch with single exception type.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.tryCatch(tryBody,
     *     Types.type("Exception"), "e",
     *     catchBody)
     * // try { tryBody } catch (Exception e) { catchBody }
     * }</pre>
     *
     * @param tryBody        the try block statement
     * @param exceptionType  the exception type to catch
     * @param variableName   the exception variable name
     * @param catchBody      the catch block statement
     * @return a try-catch statement
     */
    public static TryCatchStmt tryCatch(Statement tryBody, TypeRef exceptionType,
                                        String variableName, Statement catchBody) {
        CatchClause catchClause = new CatchClause(variableName, exceptionType, List.of(catchBody));
        return new TryCatchStmt(List.of(tryBody), List.of(catchClause), List.of());
    }

    // ========================================================================
    // Control Flow
    // ========================================================================

    /**
     * Creates a break statement.
     *
     * @return a break statement
     */
    public static BreakStmt break_() {
        return BreakStmt.INSTANCE;
    }

    /**
     * Creates a break statement with a label.
     *
     * @param label the label to break to
     * @return a labeled break statement
     */
    public static BreakStmt break_(String label) {
        return new BreakStmt(label);
    }

    /**
     * Creates a continue statement.
     *
     * @return a continue statement
     */
    public static ContinueStmt continue_() {
        return ContinueStmt.INSTANCE;
    }

    /**
     * Creates a continue statement with a label.
     *
     * @param label the label to continue to
     * @return a labeled continue statement
     */
    public static ContinueStmt continue_(String label) {
        return new ContinueStmt(label);
    }

    // ========================================================================
    // Block Statements
    // ========================================================================

    /**
     * Creates a block statement from multiple statements.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.block(
     *     Stmt.var(Types.INT, "x", Expr.literal(1)),
     *     Stmt.var(Types.INT, "y", Expr.literal(2)),
     *     Stmt.return_(Expr.add(Expr.var("x"), Expr.var("y"))))
     * // { int x = 1; int y = 2; return x + y; }
     * }</pre>
     *
     * @param statements the statements in the block
     * @return a block statement
     */
    public static BlockStmt block(Statement... statements) {
        return new BlockStmt(Arrays.asList(statements));
    }

    /**
     * Creates a block statement from a list of statements.
     *
     * @param statements the statements in the block
     * @return a block statement
     */
    public static BlockStmt block(List<Statement> statements) {
        return new BlockStmt(statements);
    }

    /**
     * Creates an empty block statement.
     *
     * @return an empty block statement
     */
    public static BlockStmt emptyBlock() {
        return new BlockStmt(List.of());
    }

    // ========================================================================
    // Convenience Methods
    // ========================================================================

    /**
     * Creates a null check with throw statement.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.requireNonNull(Expr.var("name"), "name")
     * // if (name == null) throw new NullPointerException("name must not be null");
     * }</pre>
     *
     * @param expr      the expression to check
     * @param paramName the parameter name for the error message
     * @return an if statement that throws NPE if expr is null
     */
    public static IfStmt requireNonNull(Expression expr, String paramName) {
        return if_(
            Expr.isNull(expr),
            throw_(Expr.new_(
                Types.type("java.lang.NullPointerException"),
                Expr.literal(paramName + " must not be null")
            ))
        );
    }

    /**
     * Creates a method call statement (expression statement wrapping a method call).
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.call("doSomething", Expr.var("x"))
     * // doSomething(x);
     * }</pre>
     *
     * @param methodName the method name
     * @param arguments  the method arguments
     * @return an expression statement wrapping the method call
     */
    public static ExpressionStmt call(String methodName, Expression... arguments) {
        return expr(Expr.call(methodName, arguments));
    }

    /**
     * Creates a method call statement on a target.
     *
     * <p>Example:
     * <pre>{@code
     * Stmt.call(Expr.var("list"), "add", Expr.var("item"))
     * // list.add(item);
     * }</pre>
     *
     * @param target     the target expression
     * @param methodName the method name
     * @param arguments  the method arguments
     * @return an expression statement wrapping the method call
     */
    public static ExpressionStmt call(Expression target, String methodName, Expression... arguments) {
        return expr(Expr.call(target, methodName, arguments));
    }
}
