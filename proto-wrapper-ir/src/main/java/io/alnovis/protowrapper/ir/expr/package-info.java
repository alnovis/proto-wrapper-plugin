/**
 * Expression nodes for the IR.
 *
 * <p>This package contains all expression types for the Intermediate Representation.
 * Expressions represent computations that produce values.
 *
 * <h2>Expression Hierarchy</h2>
 * <pre>
 * Expression (sealed)
 * ├── LiteralExpr        - Literal values (strings, numbers, null, class literals)
 * ├── VarRefExpr         - Variable references
 * ├── ThisExpr           - {@code this} reference
 * ├── TypeRefExpr        - Type reference for static member access
 * ├── FieldAccessExpr    - Field access (obj.field)
 * ├── MethodCallExpr     - Method invocation
 * ├── ConstructorCallExpr - Constructor call (new)
 * ├── CastExpr           - Type cast
 * ├── InstanceOfExpr     - instanceof check (with optional pattern matching)
 * ├── BinaryExpr         - Binary operations (+, -, ==, etc.)
 * ├── UnaryExpr          - Unary operations (!, -, etc.)
 * ├── TernaryExpr        - Ternary conditional (? :)
 * ├── LambdaExpr         - Lambda expression
 * └── ArrayInitExpr      - Array initializer
 * </pre>
 *
 * <h2>Supporting Types</h2>
 * <ul>
 *   <li>{@link BinaryOp} - Binary operators (ADD, SUB, EQ, etc.)</li>
 *   <li>{@link UnaryOp} - Unary operators (NOT, NEG, etc.)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Use the Expr DSL for convenience
 * Expression literal = Expr.literal("hello");
 * Expression variable = Expr.var("count");
 * Expression methodCall = Expr.call(target, "getName");
 * Expression binary = Expr.add(Expr.var("a"), Expr.var("b"));
 * Expression lambda = Expr.lambda("x", Expr.multiply(Expr.var("x"), Expr.literal(2)));
 * }</pre>
 *
 * @see io.alnovis.protowrapper.dsl.Expr
 * @since 2.4.0
 */
package io.alnovis.protowrapper.ir.expr;
