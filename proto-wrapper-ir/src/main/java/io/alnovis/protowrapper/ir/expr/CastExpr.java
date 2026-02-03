package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;

import java.util.Objects;

/**
 * Represents a type cast expression in the IR.
 *
 * <p>A cast expression converts an expression to a different type. In Java, casts are:
 * <ul>
 *   <li><b>Widening primitive:</b> Implicit, no cast needed (int to long)</li>
 *   <li><b>Narrowing primitive:</b> Explicit cast required, may lose precision (long to int)</li>
 *   <li><b>Reference upcast:</b> Implicit, to supertype (String to Object)</li>
 *   <li><b>Reference downcast:</b> Explicit, checked at runtime (Object to String)</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Primitive casts
 * int i = (int) longValue;
 * double d = (double) intValue;
 *
 * // Reference casts
 * String s = (String) object;
 * List<String> list = (List<String>) rawList;
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Cast to int: (int) longValue
 * CastExpr toInt = new CastExpr(Types.INT, Expr.var("longValue"));
 *
 * // Cast to String: (String) object
 * CastExpr toString = new CastExpr(
 *     Types.STRING,
 *     Expr.var("object")
 * );
 *
 * // Using the Expr DSL (recommended)
 * Expression cast = Expr.cast(Types.STRING, Expr.var("object"));
 * Expression intCast = Expr.cast(Types.INT, Expr.var("longValue"));
 * }</pre>
 *
 * <p><b>Note:</b> Reference downcasts can throw {@code ClassCastException} at runtime
 * if the object is not an instance of the target type. Consider using
 * {@link InstanceOfExpr} to check before casting.
 *
 * @param type       the target type to cast to; must not be null
 * @param expression the expression being cast; must not be null
 * @see InstanceOfExpr
 * @see io.alnovis.protowrapper.dsl.Expr#cast(TypeRef, Expression)
 * @since 2.4.0
 */
public record CastExpr(TypeRef type, Expression expression) implements Expression {

    /**
     * Creates a new CastExpr with validation.
     *
     * @param type       the target type
     * @param expression the expression to cast
     * @throws NullPointerException if type or expression is null
     */
    public CastExpr {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(expression, "expression must not be null");
    }

    /**
     * Returns {@code true} if this is a cast to a primitive type.
     *
     * @return {@code true} if the target type is a primitive
     */
    public boolean isPrimitiveCast() {
        return type instanceof PrimitiveType;
    }

    /**
     * Returns {@code true} if this is a cast to a reference type.
     *
     * @return {@code true} if the target type is a class type
     */
    public boolean isReferenceCast() {
        return type instanceof ClassType;
    }
}
