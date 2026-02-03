package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;

/**
 * Represents an expression in the IR.
 *
 * <p>Expressions are code constructs that produce a value when evaluated.
 * They can be used as:
 * <ul>
 *   <li>Right-hand side of assignments</li>
 *   <li>Method arguments</li>
 *   <li>Return values</li>
 *   <li>Conditions in control flow statements</li>
 *   <li>Parts of larger expressions</li>
 * </ul>
 *
 * <p>The IR supports the following expression types:
 * <ul>
 *   <li>{@link LiteralExpr} - literal values (strings, numbers, booleans, null)</li>
 *   <li>{@link VarRefExpr} - variable references</li>
 *   <li>{@link ThisExpr} - the {@code this} reference</li>
 *   <li>{@link TypeRefExpr} - type reference for static member access</li>
 *   <li>{@link FieldAccessExpr} - field access (e.g., {@code obj.field})</li>
 *   <li>{@link MethodCallExpr} - method invocation</li>
 *   <li>{@link ConstructorCallExpr} - constructor invocation ({@code new})</li>
 *   <li>{@link CastExpr} - type cast</li>
 *   <li>{@link InstanceOfExpr} - instanceof check</li>
 *   <li>{@link BinaryExpr} - binary operators (+, -, ==, etc.)</li>
 *   <li>{@link UnaryExpr} - unary operators (!, -, ~)</li>
 *   <li>{@link TernaryExpr} - ternary conditional (? :)</li>
 *   <li>{@link LambdaExpr} - lambda expression</li>
 *   <li>{@link ArrayInitExpr} - array initializer</li>
 * </ul>
 *
 * <p>Example usage with DSL:
 * <pre>{@code
 * // Simple expressions
 * Expression str = Expr.literal("hello");
 * Expression num = Expr.literal(42);
 * Expression var = Expr.var("myVar");
 *
 * // Method call: this.getValue()
 * Expression call = Expr.call(Expr.this_(), "getValue");
 *
 * // Binary expression: a + b
 * Expression add = Expr.add(Expr.var("a"), Expr.var("b"));
 *
 * // Ternary: condition ? trueValue : falseValue
 * Expression ternary = Expr.ternary(
 *     Expr.var("condition"),
 *     Expr.literal("yes"),
 *     Expr.literal("no")
 * );
 * }</pre>
 *
 * @see io.alnovis.protowrapper.dsl.Expr
 * @since 2.4.0
 */
public sealed interface Expression permits
        LiteralExpr,
        VarRefExpr,
        ThisExpr,
        TypeRefExpr,
        FieldAccessExpr,
        MethodCallExpr,
        ConstructorCallExpr,
        CastExpr,
        InstanceOfExpr,
        BinaryExpr,
        UnaryExpr,
        TernaryExpr,
        LambdaExpr,
        ArrayInitExpr {
}
