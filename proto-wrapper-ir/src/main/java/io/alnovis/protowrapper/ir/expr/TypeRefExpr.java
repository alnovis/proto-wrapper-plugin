package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;

import java.util.Objects;

/**
 * Represents a type reference expression used for static member access in the IR.
 *
 * <p>This expression represents a reference to a type (class, interface, enum)
 * used as a target for static field access or static method calls. It is distinct
 * from a class literal ({@code Type.class}) which produces a {@code Class<T>} object.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Static field access - TypeRefExpr is the target
 * Math.PI
 * System.out
 * Integer.MAX_VALUE
 *
 * // Static method call - TypeRefExpr is the target
 * Math.abs(-1)
 * String.valueOf(42)
 * Collections.emptyList()
 *
 * // This is NOT a TypeRefExpr (it's a class literal):
 * String.class  // This is LiteralExpr with ClassType value
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Math.PI
 * FieldAccessExpr pi = new FieldAccessExpr(
 *     new TypeRefExpr(Types.type("java.lang.Math")),
 *     "PI"
 * );
 *
 * // Math.abs(-1)
 * MethodCallExpr abs = new MethodCallExpr(
 *     new TypeRefExpr(Types.type("java.lang.Math")),
 *     "abs",
 *     List.of(Expr.literal(-1)),
 *     List.of()
 * );
 *
 * // Using the Expr DSL (recommended)
 * Expression pi = Expr.staticField(Types.type("java.lang.Math"), "PI");
 * Expression abs = Expr.staticCall(Types.type("java.lang.Math"), "abs", Expr.literal(-1));
 * }</pre>
 *
 * @param type the type being referenced; must not be null
 * @see FieldAccessExpr
 * @see MethodCallExpr
 * @see LiteralExpr
 * @see io.alnovis.protowrapper.dsl.Expr#staticField(TypeRef, String)
 * @see io.alnovis.protowrapper.dsl.Expr#staticCall(TypeRef, String, Expression...)
 * @since 2.4.0
 */
public record TypeRefExpr(TypeRef type) implements Expression {

    /**
     * Creates a new TypeRefExpr with validation.
     *
     * @param type the type being referenced
     * @throws NullPointerException if type is null
     */
    public TypeRefExpr {
        Objects.requireNonNull(type, "type must not be null");
    }

    /**
     * Creates a TypeRefExpr for the given type.
     *
     * @param type the type to reference
     * @return a new TypeRefExpr
     */
    public static TypeRefExpr of(TypeRef type) {
        return new TypeRefExpr(type);
    }
}
