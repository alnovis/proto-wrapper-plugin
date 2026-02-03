package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;

import java.util.List;
import java.util.Objects;

/**
 * Represents a constructor invocation expression ({@code new}) in the IR.
 *
 * <p>A constructor call creates a new instance of a class. It consists of:
 * <ul>
 *   <li>The type to instantiate</li>
 *   <li>Arguments passed to the constructor</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Simple constructor
 * new ArrayList()
 * new StringBuilder("initial")
 *
 * // Generic constructor
 * new ArrayList<String>()
 * new HashMap<String, Integer>()
 *
 * // Anonymous class (not directly supported in IR)
 * new Runnable() { public void run() { } }
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Simple constructor: new ArrayList()
 * ConstructorCallExpr newList = new ConstructorCallExpr(
 *     ClassType.of("java.util.ArrayList"),
 *     List.of()
 * );
 *
 * // Constructor with argument: new StringBuilder("text")
 * ConstructorCallExpr newBuilder = new ConstructorCallExpr(
 *     ClassType.of("java.lang.StringBuilder"),
 *     List.of(Expr.literal("text"))
 * );
 *
 * // Generic constructor: new ArrayList<String>()
 * ConstructorCallExpr newGenericList = new ConstructorCallExpr(
 *     ClassType.of("java.util.ArrayList").withTypeArguments(Types.STRING),
 *     List.of()
 * );
 *
 * // Using the Expr DSL (recommended)
 * Expression newList = Expr.new_(Types.type("java.util.ArrayList"));
 * Expression newBuilder = Expr.new_(Types.type("java.lang.StringBuilder"),
 *     Expr.literal("text"));
 * }</pre>
 *
 * <p><b>Note:</b> Anonymous classes are not directly represented in this IR.
 * They should be converted to separate class declarations or lambda expressions
 * where applicable.
 *
 * @param type      the type to instantiate; must not be null
 * @param arguments the arguments passed to the constructor; empty list if none;
 *                  the list is copied and made immutable
 * @see io.alnovis.protowrapper.dsl.Expr#new_(TypeRef, Expression...)
 * @since 2.4.0
 */
public record ConstructorCallExpr(
        TypeRef type,
        List<Expression> arguments
) implements Expression {

    /**
     * Creates a new ConstructorCallExpr with validation.
     *
     * @param type      the type to instantiate
     * @param arguments the constructor arguments (may be null, treated as empty list)
     * @throws NullPointerException if type is null
     */
    public ConstructorCallExpr {
        Objects.requireNonNull(type, "type must not be null");
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
    }

    /**
     * Returns {@code true} if this constructor call has no arguments.
     *
     * @return {@code true} if arguments list is empty
     */
    public boolean hasNoArguments() {
        return arguments.isEmpty();
    }

    /**
     * Returns {@code true} if the type is parameterized (has type arguments).
     *
     * @return {@code true} if the type has type arguments
     */
    public boolean isParameterized() {
        return type instanceof ClassType ct && ct.isParameterized();
    }
}
