package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.type.*;
import io.alnovis.protowrapper.ir.expr.Expression;

import java.util.Objects;

/**
 * Represents a local variable declaration statement in the IR.
 *
 * <p>A variable declaration introduces a new local variable with a name,
 * type, and optional initializer. Variables can be marked as final.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Declaration without initialization
 * String name;
 *
 * // Declaration with initialization
 * int count = 0;
 * String greeting = "Hello";
 *
 * // Final variable
 * final int MAX = 100;
 * final String result = compute();
 *
 * // Type inference (var - Java 10+)
 * var list = new ArrayList<String>();
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // int count = 0;
 * VarDeclStmt countDecl = new VarDeclStmt(
 *     "count",
 *     Types.INT,
 *     Expr.literal(0),
 *     false  // not final
 * );
 *
 * // final String name;
 * VarDeclStmt finalDecl = new VarDeclStmt(
 *     "name",
 *     Types.STRING,
 *     null,  // no initializer
 *     true   // final
 * );
 *
 * // Using the Stmt DSL (recommended)
 * Statement varDecl = Stmt.var("count", Types.INT, Expr.literal(0));
 * Statement finalVar = Stmt.finalVar("name", Types.STRING, Expr.var("input"));
 * }</pre>
 *
 * @param name        the variable name; must not be null
 * @param type        the variable type; must not be null
 * @param initializer the initial value expression; null if not initialized
 * @param isFinal     {@code true} if the variable is declared final
 * @see io.alnovis.protowrapper.dsl.Stmt#var(String, TypeRef)
 * @see io.alnovis.protowrapper.dsl.Stmt#var(String, TypeRef, Expression)
 * @see io.alnovis.protowrapper.dsl.Stmt#finalVar(String, TypeRef, Expression)
 * @since 2.4.0
 */
public record VarDeclStmt(
        String name,
        TypeRef type,
        Expression initializer,
        boolean isFinal
) implements Statement {

    /**
     * Creates a new VarDeclStmt with validation.
     *
     * @param name        the variable name
     * @param type        the variable type
     * @param initializer the initial value (may be null)
     * @param isFinal     whether the variable is final
     * @throws NullPointerException if name or type is null
     */
    public VarDeclStmt {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty or blank");
        }
        Objects.requireNonNull(type, "type must not be null");
    }

    /**
     * Creates a non-final variable declaration with an initializer.
     *
     * @param name        the variable name
     * @param type        the variable type
     * @param initializer the initial value
     * @return a new VarDeclStmt
     */
    public static VarDeclStmt of(String name, TypeRef type, Expression initializer) {
        return new VarDeclStmt(name, type, initializer, false);
    }

    /**
     * Creates a non-final variable declaration without an initializer.
     *
     * @param name the variable name
     * @param type the variable type
     * @return a new VarDeclStmt
     */
    public static VarDeclStmt of(String name, TypeRef type) {
        return new VarDeclStmt(name, type, null, false);
    }

    /**
     * Creates a final variable declaration with an initializer.
     *
     * @param name        the variable name
     * @param type        the variable type
     * @param initializer the initial value
     * @return a new final VarDeclStmt
     */
    public static VarDeclStmt finalVar(String name, TypeRef type, Expression initializer) {
        return new VarDeclStmt(name, type, initializer, true);
    }

    /**
     * Returns {@code true} if this declaration has an initializer.
     *
     * @return {@code true} if initializer is not null
     */
    public boolean hasInitializer() {
        return initializer != null;
    }
}
