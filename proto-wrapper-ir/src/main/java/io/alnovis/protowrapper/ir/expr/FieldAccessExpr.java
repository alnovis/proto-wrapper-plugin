package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;

import java.util.Objects;

/**
 * Represents a field access expression in the IR.
 *
 * <p>A field access reads the value of an instance or static field.
 * It consists of:
 * <ul>
 *   <li>A target expression (the object or class containing the field)</li>
 *   <li>A field name</li>
 * </ul>
 *
 * <p>For instance fields, the target is typically {@link ThisExpr} or another object.
 * For static fields, the target can be null (indicating the current class) or a
 * {@link ClassType} wrapped expression.
 *
 * <p>Example in Java:
 * <pre>{@code
 * // Instance field access
 * this.name
 * person.age
 * getObject().field
 *
 * // Static field access
 * Math.PI
 * System.out
 * MyClass.CONSTANT
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // Instance field: this.name
 * FieldAccessExpr nameField = new FieldAccessExpr(ThisExpr.INSTANCE, "name");
 *
 * // Chained field: obj.inner.value
 * FieldAccessExpr chainedField = new FieldAccessExpr(
 *     new FieldAccessExpr(Expr.var("obj"), "inner"),
 *     "value"
 * );
 *
 * // Using the Expr DSL (recommended)
 * Expression thisName = Expr.field(Expr.this_(), "name");
 * Expression objField = Expr.field(Expr.var("obj"), "field");
 *
 * // Static field: System.out
 * Expression systemOut = Expr.staticField(Types.type("java.lang.System"), "out");
 * }</pre>
 *
 * @param target    the expression that evaluates to the object or class containing the field;
 *                  may be null for static field access on the enclosing class
 * @param fieldName the name of the field being accessed; must not be null
 * @see VarRefExpr
 * @see io.alnovis.protowrapper.dsl.Expr#field(Expression, String)
 * @see io.alnovis.protowrapper.dsl.Expr#staticField(TypeRef, String)
 * @since 2.4.0
 */
public record FieldAccessExpr(Expression target, String fieldName) implements Expression {

    /**
     * Creates a new FieldAccessExpr with validation.
     *
     * @param target    the target expression (must not be null; use TypeRefExpr for static fields)
     * @param fieldName the field name
     * @throws NullPointerException     if target or fieldName is null
     * @throws IllegalArgumentException if fieldName is empty or blank
     */
    public FieldAccessExpr {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        if (fieldName.isBlank()) {
            throw new IllegalArgumentException("fieldName must not be empty or blank");
        }
    }

    /**
     * Returns {@code true} if this is a static field access.
     *
     * <p>Static field access is indicated by a {@link TypeRefExpr} target,
     * meaning the field is accessed on a type rather than an instance.
     *
     * @return {@code true} if this is a static field access
     */
    public boolean isStatic() {
        return target instanceof TypeRefExpr;
    }

    /**
     * Returns {@code true} if this is an instance field access on {@code this}.
     *
     * @return {@code true} if the target is a ThisExpr
     */
    public boolean isThisAccess() {
        return target instanceof ThisExpr;
    }
}
