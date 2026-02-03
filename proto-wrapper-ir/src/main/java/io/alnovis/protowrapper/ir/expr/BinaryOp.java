package io.alnovis.protowrapper.ir.expr;

/**
 * Enumeration of binary operators for {@link BinaryExpr}.
 *
 * <p>Binary operators take two operands and produce a result. They are categorized as:
 * <ul>
 *   <li><b>Arithmetic:</b> Numeric calculations (+, -, *, /, %)</li>
 *   <li><b>Comparison:</b> Produce boolean results (==, !=, &lt;, &lt;=, &gt;, &gt;=)</li>
 *   <li><b>Logical:</b> Boolean logic (&amp;&amp;, ||)</li>
 *   <li><b>Bitwise:</b> Bit manipulation (&amp;, |, ^, &lt;&lt;, &gt;&gt;, &gt;&gt;&gt;)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Arithmetic: a + b
 * BinaryExpr add = new BinaryExpr(Expr.var("a"), BinaryOp.ADD, Expr.var("b"));
 *
 * // Comparison: x == y
 * BinaryExpr eq = new BinaryExpr(Expr.var("x"), BinaryOp.EQ, Expr.var("y"));
 *
 * // Logical: condition1 && condition2
 * BinaryExpr and = new BinaryExpr(Expr.var("c1"), BinaryOp.AND, Expr.var("c2"));
 * }</pre>
 *
 * @see BinaryExpr
 * @since 2.4.0
 */
public enum BinaryOp {
    // ==================== Arithmetic Operators ====================

    /**
     * Addition operator ({@code +}).
     * Adds two numeric values or concatenates strings.
     */
    ADD("+", Category.ARITHMETIC),

    /**
     * Subtraction operator ({@code -}).
     * Subtracts the right operand from the left.
     */
    SUB("-", Category.ARITHMETIC),

    /**
     * Multiplication operator ({@code *}).
     * Multiplies two numeric values.
     */
    MUL("*", Category.ARITHMETIC),

    /**
     * Division operator ({@code /}).
     * Divides the left operand by the right.
     */
    DIV("/", Category.ARITHMETIC),

    /**
     * Modulo (remainder) operator ({@code %}).
     * Returns the remainder of division.
     */
    MOD("%", Category.ARITHMETIC),

    // ==================== Comparison Operators ====================

    /**
     * Equality operator ({@code ==}).
     * Tests if two values are equal.
     */
    EQ("==", Category.COMPARISON),

    /**
     * Inequality operator ({@code !=}).
     * Tests if two values are not equal.
     */
    NE("!=", Category.COMPARISON),

    /**
     * Less than operator ({@code <}).
     * Tests if left operand is less than right.
     */
    LT("<", Category.COMPARISON),

    /**
     * Less than or equal operator ({@code <=}).
     * Tests if left operand is less than or equal to right.
     */
    LE("<=", Category.COMPARISON),

    /**
     * Greater than operator ({@code >}).
     * Tests if left operand is greater than right.
     */
    GT(">", Category.COMPARISON),

    /**
     * Greater than or equal operator ({@code >=}).
     * Tests if left operand is greater than or equal to right.
     */
    GE(">=", Category.COMPARISON),

    // ==================== Logical Operators ====================

    /**
     * Logical AND operator ({@code &&}).
     * Returns true if both operands are true. Short-circuit evaluation.
     */
    AND("&&", Category.LOGICAL),

    /**
     * Logical OR operator ({@code ||}).
     * Returns true if either operand is true. Short-circuit evaluation.
     */
    OR("||", Category.LOGICAL),

    // ==================== Bitwise Operators ====================

    /**
     * Bitwise AND operator ({@code &}).
     * Performs bitwise AND on integer operands.
     */
    BIT_AND("&", Category.BITWISE),

    /**
     * Bitwise OR operator ({@code |}).
     * Performs bitwise OR on integer operands.
     */
    BIT_OR("|", Category.BITWISE),

    /**
     * Bitwise XOR operator ({@code ^}).
     * Performs bitwise exclusive OR on integer operands.
     */
    BIT_XOR("^", Category.BITWISE),

    /**
     * Left shift operator ({@code <<}).
     * Shifts bits left, filling with zeros.
     */
    LSHIFT("<<", Category.BITWISE),

    /**
     * Signed right shift operator ({@code >>}).
     * Shifts bits right, preserving sign.
     */
    RSHIFT(">>", Category.BITWISE),

    /**
     * Unsigned right shift operator ({@code >>>}).
     * Shifts bits right, filling with zeros.
     */
    URSHIFT(">>>", Category.BITWISE);

    private final String symbol;
    private final Category category;

    BinaryOp(String symbol, Category category) {
        this.symbol = symbol;
        this.category = category;
    }

    /**
     * Returns the Java symbol for this operator.
     *
     * @return the operator symbol (e.g., "+", "==", "&&")
     */
    public String symbol() {
        return symbol;
    }

    /**
     * Returns the category of this operator.
     *
     * @return the operator category
     */
    public Category category() {
        return category;
    }

    /**
     * Returns {@code true} if this is an arithmetic operator.
     *
     * @return {@code true} for ADD, SUB, MUL, DIV, MOD
     */
    public boolean isArithmetic() {
        return category == Category.ARITHMETIC;
    }

    /**
     * Returns {@code true} if this is a comparison operator.
     *
     * @return {@code true} for EQ, NE, LT, LE, GT, GE
     */
    public boolean isComparison() {
        return category == Category.COMPARISON;
    }

    /**
     * Returns {@code true} if this is a logical operator.
     *
     * @return {@code true} for AND, OR
     */
    public boolean isLogical() {
        return category == Category.LOGICAL;
    }

    /**
     * Returns {@code true} if this is a bitwise operator.
     *
     * @return {@code true} for BIT_AND, BIT_OR, BIT_XOR, LSHIFT, RSHIFT, URSHIFT
     */
    public boolean isBitwise() {
        return category == Category.BITWISE;
    }

    /**
     * Categories of binary operators.
     */
    public enum Category {
        /** Arithmetic operators: +, -, *, /, % */
        ARITHMETIC,
        /** Comparison operators: ==, !=, <, <=, >, >= */
        COMPARISON,
        /** Logical operators: &&, || */
        LOGICAL,
        /** Bitwise operators: &, |, ^, <<, >>, >>> */
        BITWISE
    }
}
