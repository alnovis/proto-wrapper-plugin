package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;

import java.util.List;
import java.util.Objects;

/**
 * Represents a method invocation expression in the IR.
 *
 * <p>A method call consists of:
 * <ul>
 *   <li>A target expression (the object or class on which the method is called)</li>
 *   <li>A method name</li>
 *   <li>Arguments passed to the method</li>
 *   <li>Optional type arguments for generic methods</li>
 * </ul>
 *
 * <p>For instance methods, the target is typically {@link ThisExpr} or another object.
 * For static methods, the target is null and the method is called on the enclosing class,
 * or you can use a special marker to indicate the target class.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Instance method calls
 * this.process()
 * list.size()
 * getName().toUpperCase()
 *
 * // Static method calls
 * Math.max(a, b)
 * String.valueOf(42)
 * Collections.<String>emptyList()  // with type argument
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Simple instance method: this.process()
 * MethodCallExpr call = new MethodCallExpr(
 *     ThisExpr.INSTANCE,
 *     "process",
 *     List.of(),
 *     List.of()
 * );
 *
 * // Method with arguments: list.get(0)
 * MethodCallExpr getCall = new MethodCallExpr(
 *     Expr.var("list"),
 *     "get",
 *     List.of(Expr.literal(0)),
 *     List.of()
 * );
 *
 * // Using the Expr DSL (recommended)
 * Expression process = Expr.call(Expr.this_(), "process");
 * Expression getFirst = Expr.call(Expr.var("list"), "get", Expr.literal(0));
 * Expression max = Expr.staticCall(Types.type("java.lang.Math"), "max",
 *     Expr.var("a"), Expr.var("b"));
 * }</pre>
 *
 * @param target        the expression that evaluates to the object on which the method is called;
 *                      null for static method calls or implicit this
 * @param methodName    the name of the method being called; must not be null
 * @param arguments     the arguments passed to the method; empty list if none;
 *                      the list is copied and made immutable
 * @param typeArguments the type arguments for generic method calls; empty list if none;
 *                      the list is copied and made immutable
 * @see io.alnovis.protowrapper.dsl.Expr#call(Expression, String, Expression...)
 * @see io.alnovis.protowrapper.dsl.Expr#staticCall(TypeRef, String, Expression...)
 * @since 2.4.0
 */
public record MethodCallExpr(
        Expression target,
        String methodName,
        List<Expression> arguments,
        List<TypeRef> typeArguments
) implements Expression {

    /**
     * Creates a new MethodCallExpr with validation.
     *
     * @param target        the target expression (must not be null; use TypeRefExpr for static methods)
     * @param methodName    the method name
     * @param arguments     the arguments (may be null, treated as empty list)
     * @param typeArguments the type arguments (may be null, treated as empty list)
     * @throws NullPointerException     if target or methodName is null
     * @throws IllegalArgumentException if methodName is empty or blank
     */
    public MethodCallExpr {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        if (methodName.isBlank()) {
            throw new IllegalArgumentException("methodName must not be empty or blank");
        }
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
        typeArguments = typeArguments == null ? List.of() : List.copyOf(typeArguments);
    }

    /**
     * Returns {@code true} if this is a static method call.
     *
     * <p>Static method calls have a {@link TypeRefExpr} target,
     * meaning the method is called on a type rather than an instance.
     *
     * @return {@code true} if this is a static method call
     */
    public boolean isStatic() {
        return target instanceof TypeRefExpr;
    }

    /**
     * Returns {@code true} if this is an instance method call on {@code this}.
     *
     * @return {@code true} if the target is a ThisExpr
     */
    public boolean isThisCall() {
        return target instanceof ThisExpr;
    }

    /**
     * Returns {@code true} if this is a generic method call with type arguments.
     *
     * @return {@code true} if type arguments are specified
     */
    public boolean hasTypeArguments() {
        return !typeArguments.isEmpty();
    }

    /**
     * Returns {@code true} if this method call has no arguments.
     *
     * @return {@code true} if arguments list is empty
     */
    public boolean hasNoArguments() {
        return arguments.isEmpty();
    }
}
