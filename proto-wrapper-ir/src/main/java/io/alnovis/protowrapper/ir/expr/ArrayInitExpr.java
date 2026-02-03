package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.*;

import java.util.List;
import java.util.Objects;

/**
 * Represents an array initializer expression in the IR.
 *
 * <p>An array initializer creates a new array with the specified elements.
 * It can be used in:
 * <ul>
 *   <li>Variable declarations: {@code int[] arr = {1, 2, 3}}</li>
 *   <li>Array creation: {@code new int[] {1, 2, 3}}</li>
 *   <li>Method arguments: {@code process(new String[] {"a", "b"})}</li>
 * </ul>
 *
 * <p>Example in Java:
 * <pre>{@code
 * // In declaration (shorthand)
 * int[] numbers = {1, 2, 3};
 *
 * // Explicit array creation
 * int[] numbers = new int[] {1, 2, 3};
 *
 * // String array
 * String[] names = {"Alice", "Bob", "Charlie"};
 *
 * // Empty array
 * int[] empty = {};
 *
 * // Multidimensional (nested initializers)
 * int[][] matrix = {{1, 2}, {3, 4}};
 * }</pre>
 *
 * <p>Example usage in IR:
 * <pre>{@code
 * // {1, 2, 3}
 * ArrayInitExpr intArray = new ArrayInitExpr(
 *     Types.INT,
 *     List.of(Expr.literal(1), Expr.literal(2), Expr.literal(3))
 * );
 *
 * // {"Alice", "Bob"}
 * ArrayInitExpr stringArray = new ArrayInitExpr(
 *     Types.STRING,
 *     List.of(Expr.literal("Alice"), Expr.literal("Bob"))
 * );
 *
 * // Empty array: {}
 * ArrayInitExpr empty = new ArrayInitExpr(Types.INT, List.of());
 *
 * // Using the Expr DSL (recommended)
 * Expression arr = Expr.arrayInit(Types.INT,
 *     Expr.literal(1), Expr.literal(2), Expr.literal(3)
 * );
 * }</pre>
 *
 * <p><b>Note:</b> For arrays with a specific size but no initial values,
 * use {@link ConstructorCallExpr} with an ArrayType and a size expression:
 * {@code new int[10]}.
 *
 * @param componentType the type of elements in the array; must not be null
 * @param elements      the initial elements; empty list for empty array;
 *                      the list is copied and made immutable
 * @see ArrayType
 * @see io.alnovis.protowrapper.dsl.Expr#arrayInit(TypeRef, Expression...)
 * @since 2.4.0
 */
public record ArrayInitExpr(
        TypeRef componentType,
        List<Expression> elements
) implements Expression {

    /**
     * Creates a new ArrayInitExpr with validation.
     *
     * @param componentType the component type of the array
     * @param elements      the array elements (may be null, treated as empty list)
     * @throws NullPointerException if componentType is null
     */
    public ArrayInitExpr {
        Objects.requireNonNull(componentType, "componentType must not be null");
        elements = elements == null ? List.of() : List.copyOf(elements);
    }

    /**
     * Returns {@code true} if this is an empty array initializer.
     *
     * @return {@code true} if elements list is empty
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Returns the number of elements in this array initializer.
     *
     * @return the element count
     */
    public int size() {
        return elements.size();
    }

    /**
     * Returns the array type that this initializer creates.
     *
     * @return an ArrayType with the component type of this initializer
     */
    public ArrayType arrayType() {
        return new ArrayType(componentType);
    }
}
