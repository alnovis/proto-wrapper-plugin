package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;

/**
 * Represents a literal value expression in the IR.
 *
 * <p>Literals are constant values that appear directly in source code:
 * <ul>
 *   <li>String literals: {@code "hello"}</li>
 *   <li>Character literals: {@code 'a'}</li>
 *   <li>Integer literals: {@code 42}, {@code 0xFF}</li>
 *   <li>Long literals: {@code 42L}</li>
 *   <li>Float literals: {@code 3.14f}</li>
 *   <li>Double literals: {@code 3.14}</li>
 *   <li>Boolean literals: {@code true}, {@code false}</li>
 *   <li>Null literal: {@code null}</li>
 *   <li>Class literals: {@code String.class}</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // String literal
 * LiteralExpr str = new LiteralExpr("hello", Types.STRING);
 *
 * // Integer literal
 * LiteralExpr num = new LiteralExpr(42, Types.INT);
 *
 * // Null literal
 * LiteralExpr nullVal = new LiteralExpr(null, Types.type("java.lang.String"));
 *
 * // Using the Expr DSL (recommended)
 * Expression str = Expr.literal("hello");
 * Expression num = Expr.literal(42);
 * Expression nullVal = Expr.nullLiteral();
 * }</pre>
 *
 * <p>The {@code value} field stores the Java representation of the literal:
 * <table border="1">
 *   <caption>Value type mappings</caption>
 *   <tr><th>Literal Type</th><th>Java Value Type</th></tr>
 *   <tr><td>String</td><td>{@code String}</td></tr>
 *   <tr><td>Character</td><td>{@code Character}</td></tr>
 *   <tr><td>Integer</td><td>{@code Integer}</td></tr>
 *   <tr><td>Long</td><td>{@code Long}</td></tr>
 *   <tr><td>Float</td><td>{@code Float}</td></tr>
 *   <tr><td>Double</td><td>{@code Double}</td></tr>
 *   <tr><td>Boolean</td><td>{@code Boolean}</td></tr>
 *   <tr><td>Null</td><td>{@code null}</td></tr>
 *   <tr><td>Class</td><td>{@code ClassType}</td></tr>
 * </table>
 *
 * @param value the literal value; null represents the null literal
 * @param type  the type of this literal expression
 * @see io.alnovis.protowrapper.dsl.Expr#literal(String)
 * @see io.alnovis.protowrapper.dsl.Expr#literal(int)
 * @see io.alnovis.protowrapper.dsl.Expr#nullLiteral()
 * @since 2.4.0
 */
public record LiteralExpr(Object value, TypeRef type) implements Expression {

    /**
     * Returns {@code true} if this is a null literal.
     *
     * @return {@code true} if the value is null
     */
    public boolean isNull() {
        return value == null;
    }

    /**
     * Returns {@code true} if this is a string literal.
     *
     * @return {@code true} if the value is a String
     */
    public boolean isString() {
        return value instanceof String;
    }

    /**
     * Returns {@code true} if this is a numeric literal (int, long, float, double).
     *
     * @return {@code true} if the value is a Number
     */
    public boolean isNumeric() {
        return value instanceof Number;
    }

    /**
     * Returns {@code true} if this is a boolean literal.
     *
     * @return {@code true} if the value is a Boolean
     */
    public boolean isBoolean() {
        return value instanceof Boolean;
    }

    /**
     * Returns {@code true} if this is a class literal (e.g., String.class).
     *
     * @return {@code true} if the value is a ClassType representing a class literal
     */
    public boolean isClass() {
        return value instanceof ClassType;
    }
}
