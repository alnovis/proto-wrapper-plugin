/**
 * Statement nodes for the IR.
 *
 * <p>This package contains all statement types for the Intermediate Representation.
 * Statements represent executable actions in method bodies.
 *
 * <h2>Statement Hierarchy</h2>
 * <pre>
 * Statement (sealed)
 * ├── ReturnStmt      - Return statement (with or without value)
 * ├── IfStmt          - If-else statement
 * ├── ForEachStmt     - Enhanced for loop
 * ├── ForStmt         - Traditional for loop
 * ├── WhileStmt       - While loop
 * ├── VarDeclStmt     - Variable declaration
 * ├── AssignStmt      - Assignment statement
 * ├── ExpressionStmt  - Expression as statement (method calls, etc.)
 * ├── ThrowStmt       - Throw statement
 * ├── TryCatchStmt    - Try-catch-finally
 * ├── BlockStmt       - Block of statements
 * ├── BreakStmt       - Break statement (with optional label)
 * └── ContinueStmt    - Continue statement (with optional label)
 * </pre>
 *
 * <h2>Supporting Types</h2>
 * <ul>
 *   <li>{@link CatchClause} - Catch clause for try-catch statements</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Use the Stmt DSL for convenience
 * Statement varDecl = Stmt.var(Types.INT, "count", Expr.literal(0));
 * Statement ifStmt = Stmt.if_(condition, thenBody, elseBody);
 * Statement forEach = Stmt.forEach(Types.STRING, "item", items, body);
 * Statement returnStmt = Stmt.return_(Expr.var("result"));
 * Statement tryCatch = Stmt.tryCatch(tryBody, catches, finallyBody);
 * }</pre>
 *
 * @see io.alnovis.protowrapper.dsl.Stmt
 * @since 2.4.0
 */
package io.alnovis.protowrapper.ir.stmt;
