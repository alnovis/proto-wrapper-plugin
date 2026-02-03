package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.decl.ParameterDecl;
import io.alnovis.protowrapper.ir.expr.*;
import io.alnovis.protowrapper.ir.type.TypeRef;

import java.util.Arrays;
import java.util.List;

/**
 * Factory class for creating expressions.
 *
 * <p>This utility class provides convenient factory methods for creating
 * all kinds of expressions used in the IR. It serves as the main entry
 * point for building expression trees in the DSL.
 *
 * <h2>Literals</h2>
 * <pre>{@code
 * Expression nullLit = Expr.null_();
 * Expression trueLit = Expr.literal(true);
 * Expression intLit = Expr.literal(42);
 * Expression strLit = Expr.literal("hello");
 * Expression classLit = Expr.classLiteral(Types.STRING);
 * }</pre>
 *
 * <h2>Variable References</h2>
 * <pre>{@code
 * Expression var = Expr.var("myVariable");
 * Expression this_ = Expr.this_();
 * }</pre>
 *
 * <h2>Field Access</h2>
 * <pre>{@code
 * Expression thisField = Expr.field("name");           // this.name
 * Expression objField = Expr.field(obj, "name");       // obj.name
 * Expression staticField = Expr.staticField(Types.type("Math"), "PI");
 * }</pre>
 *
 * <h2>Method Calls</h2>
 * <pre>{@code
 * Expression thisCall = Expr.call("getName");          // this.getName()
 * Expression objCall = Expr.call(obj, "getName");      // obj.getName()
 * Expression staticCall = Expr.staticCall(Types.type("Math"), "abs", Expr.literal(-1));
 * }</pre>
 *
 * <h2>Constructor Calls</h2>
 * <pre>{@code
 * Expression newObj = Expr.new_(Types.type("Person"), Expr.literal("John"));
 * }</pre>
 *
 * <h2>Operators</h2>
 * <pre>{@code
 * // Arithmetic
 * Expression sum = Expr.add(a, b);      // a + b
 * Expression diff = Expr.subtract(a, b); // a - b
 *
 * // Comparison
 * Expression eq = Expr.eq(a, b);        // a == b
 * Expression gt = Expr.gt(a, b);        // a > b
 *
 * // Logical
 * Expression and = Expr.and(a, b);      // a && b
 * Expression not = Expr.not(a);         // !a
 *
 * // Ternary
 * Expression cond = Expr.ternary(condition, thenExpr, elseExpr);
 * }</pre>
 *
 * <h2>Type Operations</h2>
 * <pre>{@code
 * Expression cast = Expr.cast(Types.STRING, expr);
 * Expression check = Expr.instanceOf(expr, Types.STRING);
 * Expression pattern = Expr.instanceOf(expr, Types.STRING, "str");
 * }</pre>
 *
 * <h2>Lambda Expressions</h2>
 * <pre>{@code
 * Expression lambda = Expr.lambda(List.of(param), body);
 * }</pre>
 *
 * @see Expression
 * @see LiteralExpr
 * @see MethodCallExpr
 * @see BinaryExpr
 * @since 2.4.0
 */
public final class Expr {

    private Expr() {
        // Utility class, not instantiable
    }

    // ========================================================================
    // Literal Expressions
    // ========================================================================

    /**
     * Creates a null literal.
     *
     * @return a null literal expression
     */
    public static LiteralExpr null_() {
        return new LiteralExpr(null, Types.OBJECT);
    }

    /**
     * Creates a boolean literal.
     *
     * @param value the boolean value
     * @return a boolean literal expression
     */
    public static LiteralExpr literal(boolean value) {
        return new LiteralExpr(value, Types.BOOLEAN);
    }

    /**
     * Creates an integer literal.
     *
     * @param value the integer value
     * @return an integer literal expression
     */
    public static LiteralExpr literal(int value) {
        return new LiteralExpr(value, Types.INT);
    }

    /**
     * Creates a long literal.
     *
     * @param value the long value
     * @return a long literal expression
     */
    public static LiteralExpr literal(long value) {
        return new LiteralExpr(value, Types.LONG);
    }

    /**
     * Creates a float literal.
     *
     * @param value the float value
     * @return a float literal expression
     */
    public static LiteralExpr literal(float value) {
        return new LiteralExpr(value, Types.FLOAT);
    }

    /**
     * Creates a double literal.
     *
     * @param value the double value
     * @return a double literal expression
     */
    public static LiteralExpr literal(double value) {
        return new LiteralExpr(value, Types.DOUBLE);
    }

    /**
     * Creates a character literal.
     *
     * @param value the character value
     * @return a character literal expression
     */
    public static LiteralExpr literal(char value) {
        return new LiteralExpr(value, Types.CHAR);
    }

    /**
     * Creates a string literal.
     *
     * @param value the string value
     * @return a string literal expression
     */
    public static LiteralExpr literal(String value) {
        return new LiteralExpr(value, Types.STRING);
    }

    /**
     * Creates a class literal ({@code SomeClass.class}).
     *
     * @param type the type for the class literal
     * @return a class literal expression
     */
    public static LiteralExpr classLiteral(TypeRef type) {
        return new LiteralExpr(type, Types.classOf(Types.wildcard()));
    }

    // ========================================================================
    // Variable References
    // ========================================================================

    /**
     * Creates a variable reference.
     *
     * @param name the variable name
     * @return a variable reference expression
     */
    public static VarRefExpr var(String name) {
        return new VarRefExpr(name);
    }

    /**
     * Returns the {@code this} reference.
     *
     * @return the this expression
     */
    public static ThisExpr this_() {
        return ThisExpr.INSTANCE;
    }

    /**
     * Creates a type reference expression for static member access.
     *
     * <p>This is used as the target for static field access and static method calls.
     * It represents a reference to the type itself, not a class literal.
     *
     * <p>Example:
     * <pre>{@code
     * // For Math.PI, the target is a type reference to Math
     * Expression mathRef = Expr.typeRef(Types.type("java.lang.Math"));
     * }</pre>
     *
     * @param type the type to reference
     * @return a type reference expression
     */
    public static TypeRefExpr typeRef(TypeRef type) {
        return new TypeRefExpr(type);
    }

    // ========================================================================
    // Field Access
    // ========================================================================

    /**
     * Creates a field access on {@code this}.
     *
     * @param fieldName the field name
     * @return a field access expression (this.fieldName)
     */
    public static FieldAccessExpr field(String fieldName) {
        return new FieldAccessExpr(ThisExpr.INSTANCE, fieldName);
    }

    /**
     * Creates a field access on a target expression.
     *
     * @param target    the target expression
     * @param fieldName the field name
     * @return a field access expression (target.fieldName)
     */
    public static FieldAccessExpr field(Expression target, String fieldName) {
        return new FieldAccessExpr(target, fieldName);
    }

    /**
     * Creates a static field access.
     *
     * <p>Example:
     * <pre>{@code
     * Expr.staticField(Types.type("java.lang.Math"), "PI")
     * // Math.PI
     * }</pre>
     *
     * @param declaringType the class containing the field
     * @param fieldName     the field name
     * @return a field access expression
     */
    public static FieldAccessExpr staticField(TypeRef declaringType, String fieldName) {
        return new FieldAccessExpr(typeRef(declaringType), fieldName);
    }

    // ========================================================================
    // Method Calls
    // ========================================================================

    /**
     * Creates a method call on {@code this}.
     *
     * @param methodName the method name
     * @param arguments  the arguments
     * @return a method call expression (this.methodName(args))
     */
    public static MethodCallExpr call(String methodName, Expression... arguments) {
        return new MethodCallExpr(ThisExpr.INSTANCE, methodName, Arrays.asList(arguments), List.of());
    }

    /**
     * Creates a method call on a target expression.
     *
     * @param target     the target expression
     * @param methodName the method name
     * @param arguments  the arguments
     * @return a method call expression (target.methodName(args))
     */
    public static MethodCallExpr call(Expression target, String methodName, Expression... arguments) {
        return new MethodCallExpr(target, methodName, Arrays.asList(arguments), List.of());
    }

    /**
     * Creates a method call on a target expression with type arguments.
     *
     * @param target        the target expression
     * @param methodName    the method name
     * @param typeArguments the type arguments
     * @param arguments     the arguments
     * @return a method call expression
     */
    public static MethodCallExpr call(Expression target, String methodName,
                                      List<TypeRef> typeArguments, Expression... arguments) {
        return new MethodCallExpr(target, methodName, Arrays.asList(arguments), typeArguments);
    }

    /**
     * Creates a static method call.
     *
     * <p>Example:
     * <pre>{@code
     * Expr.staticCall(Types.type("java.lang.Math"), "abs", Expr.literal(-1))
     * // Math.abs(-1)
     * }</pre>
     *
     * @param declaringType the class containing the method
     * @param methodName    the method name
     * @param arguments     the arguments
     * @return a method call expression
     */
    public static MethodCallExpr staticCall(TypeRef declaringType, String methodName, Expression... arguments) {
        return new MethodCallExpr(typeRef(declaringType), methodName, Arrays.asList(arguments), List.of());
    }

    /**
     * Creates a static method call with type arguments.
     *
     * @param declaringType the class containing the method
     * @param methodName    the method name
     * @param typeArguments the type arguments
     * @param arguments     the arguments
     * @return a method call expression
     */
    public static MethodCallExpr staticCall(TypeRef declaringType, String methodName,
                                            List<TypeRef> typeArguments, Expression... arguments) {
        return new MethodCallExpr(typeRef(declaringType), methodName, Arrays.asList(arguments), typeArguments);
    }

    // ========================================================================
    // Constructor Calls
    // ========================================================================

    /**
     * Creates a constructor call (new expression).
     *
     * <p>Example:
     * <pre>{@code
     * Expr.new_(Types.type("Person"), Expr.literal("John"))
     * // new Person("John")
     * }</pre>
     *
     * @param type      the type to instantiate
     * @param arguments the constructor arguments
     * @return a constructor call expression
     */
    public static ConstructorCallExpr new_(TypeRef type, Expression... arguments) {
        return new ConstructorCallExpr(type, Arrays.asList(arguments));
    }

    /**
     * Creates a constructor call with arguments list.
     *
     * @param type      the type to instantiate
     * @param arguments the constructor arguments
     * @return a constructor call expression
     */
    public static ConstructorCallExpr new_(TypeRef type, List<Expression> arguments) {
        return new ConstructorCallExpr(type, arguments);
    }

    // ========================================================================
    // Unary Operators
    // ========================================================================

    /**
     * Creates a logical NOT expression ({@code !expr}).
     *
     * @param expr the operand
     * @return a unary NOT expression
     */
    public static UnaryExpr not(Expression expr) {
        return new UnaryExpr(UnaryOp.NOT, expr);
    }

    /**
     * Creates a unary minus expression ({@code -expr}).
     *
     * @param expr the operand
     * @return a unary minus expression
     */
    public static UnaryExpr negate(Expression expr) {
        return new UnaryExpr(UnaryOp.NEG, expr);
    }

    /**
     * Creates a unary plus expression ({@code +expr}).
     *
     * @param expr the operand
     * @return a unary plus expression
     */
    public static UnaryExpr plus(Expression expr) {
        return new UnaryExpr(UnaryOp.PLUS, expr);
    }

    /**
     * Creates a bitwise complement expression ({@code ~expr}).
     *
     * @param expr the operand
     * @return a bitwise complement expression
     */
    public static UnaryExpr complement(Expression expr) {
        return new UnaryExpr(UnaryOp.BIT_NOT, expr);
    }

    // ========================================================================
    // Binary Operators - Arithmetic
    // ========================================================================

    /**
     * Creates an addition expression ({@code left + right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return an addition expression
     */
    public static BinaryExpr add(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.ADD, right);
    }

    /**
     * Creates a subtraction expression ({@code left - right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a subtraction expression
     */
    public static BinaryExpr subtract(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.SUB, right);
    }

    /**
     * Creates a multiplication expression ({@code left * right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a multiplication expression
     */
    public static BinaryExpr multiply(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.MUL, right);
    }

    /**
     * Creates a division expression ({@code left / right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a division expression
     */
    public static BinaryExpr divide(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.DIV, right);
    }

    /**
     * Creates a modulo expression ({@code left % right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a modulo expression
     */
    public static BinaryExpr modulo(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.MOD, right);
    }

    // ========================================================================
    // Binary Operators - Comparison
    // ========================================================================

    /**
     * Creates an equality expression ({@code left == right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return an equality expression
     */
    public static BinaryExpr eq(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.EQ, right);
    }

    /**
     * Creates an inequality expression ({@code left != right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return an inequality expression
     */
    public static BinaryExpr ne(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.NE, right);
    }

    /**
     * Creates a less-than expression ({@code left < right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a less-than expression
     */
    public static BinaryExpr lt(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.LT, right);
    }

    /**
     * Creates a less-than-or-equal expression ({@code left <= right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a less-than-or-equal expression
     */
    public static BinaryExpr le(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.LE, right);
    }

    /**
     * Creates a greater-than expression ({@code left > right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a greater-than expression
     */
    public static BinaryExpr gt(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.GT, right);
    }

    /**
     * Creates a greater-than-or-equal expression ({@code left >= right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a greater-than-or-equal expression
     */
    public static BinaryExpr ge(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.GE, right);
    }

    // ========================================================================
    // Binary Operators - Logical
    // ========================================================================

    /**
     * Creates a logical AND expression ({@code left && right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a logical AND expression
     */
    public static BinaryExpr and(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.AND, right);
    }

    /**
     * Creates a logical OR expression ({@code left || right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a logical OR expression
     */
    public static BinaryExpr or(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.OR, right);
    }

    // ========================================================================
    // Binary Operators - Bitwise
    // ========================================================================

    /**
     * Creates a bitwise AND expression ({@code left & right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a bitwise AND expression
     */
    public static BinaryExpr bitwiseAnd(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.BIT_AND, right);
    }

    /**
     * Creates a bitwise OR expression ({@code left | right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a bitwise OR expression
     */
    public static BinaryExpr bitwiseOr(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.BIT_OR, right);
    }

    /**
     * Creates a bitwise XOR expression ({@code left ^ right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a bitwise XOR expression
     */
    public static BinaryExpr bitwiseXor(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.BIT_XOR, right);
    }

    /**
     * Creates a left shift expression ({@code left << right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a left shift expression
     */
    public static BinaryExpr leftShift(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.LSHIFT, right);
    }

    /**
     * Creates a right shift expression ({@code left >> right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a right shift expression
     */
    public static BinaryExpr rightShift(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.RSHIFT, right);
    }

    /**
     * Creates an unsigned right shift expression ({@code left >>> right}).
     *
     * @param left  the left operand
     * @param right the right operand
     * @return an unsigned right shift expression
     */
    public static BinaryExpr unsignedRightShift(Expression left, Expression right) {
        return new BinaryExpr(left, BinaryOp.URSHIFT, right);
    }

    // ========================================================================
    // Ternary Operator
    // ========================================================================

    /**
     * Creates a ternary conditional expression ({@code condition ? thenExpr : elseExpr}).
     *
     * @param condition the condition
     * @param thenExpr  the expression if true
     * @param elseExpr  the expression if false
     * @return a ternary expression
     */
    public static TernaryExpr ternary(Expression condition, Expression thenExpr, Expression elseExpr) {
        return new TernaryExpr(condition, thenExpr, elseExpr);
    }

    // ========================================================================
    // Type Operations
    // ========================================================================

    /**
     * Creates a cast expression ({@code (Type) expr}).
     *
     * @param targetType the target type
     * @param expr       the expression to cast
     * @return a cast expression
     */
    public static CastExpr cast(TypeRef targetType, Expression expr) {
        return new CastExpr(targetType, expr);
    }

    /**
     * Creates an instanceof expression ({@code expr instanceof Type}).
     *
     * @param expr the expression to check
     * @param type the type to check against
     * @return an instanceof expression
     */
    public static InstanceOfExpr instanceOf(Expression expr, TypeRef type) {
        return new InstanceOfExpr(expr, type, null);
    }

    /**
     * Creates an instanceof expression with pattern matching ({@code expr instanceof Type name}).
     *
     * <p>This is a Java 16+ feature (pattern matching for instanceof).
     *
     * @param expr        the expression to check
     * @param type        the type to check against
     * @param patternName the pattern variable name
     * @return an instanceof expression with pattern
     */
    public static InstanceOfExpr instanceOf(Expression expr, TypeRef type, String patternName) {
        return new InstanceOfExpr(expr, type, patternName);
    }

    // ========================================================================
    // Lambda Expressions
    // ========================================================================

    /**
     * Creates a lambda expression.
     *
     * <p>Example:
     * <pre>{@code
     * ParameterDecl param = new ParameterDecl(Types.STRING, "s", List.of(), false);
     * Expression body = Expr.call(Expr.var("s"), "length");
     * Expression lambda = Expr.lambda(List.of(param), body);
     * // (String s) -> s.length()
     * }</pre>
     *
     * @param parameters the lambda parameters
     * @param body       the lambda body (expression)
     * @return a lambda expression
     */
    public static LambdaExpr lambda(List<ParameterDecl> parameters, Expression body) {
        return new LambdaExpr(parameters, body);
    }

    /**
     * Creates a lambda expression with a single parameter.
     *
     * @param paramType the parameter type
     * @param paramName the parameter name
     * @param body      the lambda body
     * @return a lambda expression
     */
    public static LambdaExpr lambda(TypeRef paramType, String paramName, Expression body) {
        ParameterDecl param = new ParameterDecl(paramName, paramType, List.of(), false);
        return new LambdaExpr(List.of(param), body);
    }

    /**
     * Creates a lambda expression with inferred parameter type.
     *
     * <p>When the parameter type is null (inferred), the emitter should generate
     * a lambda without explicit type: {@code name -> body} or {@code (name) -> body}.
     *
     * <p>Example:
     * <pre>{@code
     * // x -> x * 2
     * Expression lambda = Expr.lambda("x", Expr.multiply(Expr.var("x"), Expr.literal(2)));
     *
     * // Used with streams:
     * // list.stream().map(x -> x.toUpperCase())
     * }</pre>
     *
     * @param paramName the parameter name
     * @param body      the lambda body
     * @return a lambda expression with inferred parameter type
     * @see ParameterDecl#hasInferredType()
     */
    public static LambdaExpr lambda(String paramName, Expression body) {
        ParameterDecl param = new ParameterDecl(paramName, null, List.of(), false);
        return new LambdaExpr(List.of(param), body);
    }

    // ========================================================================
    // Array Expressions
    // ========================================================================

    /**
     * Creates an array initializer expression.
     *
     * <p>Example:
     * <pre>{@code
     * Expression array = Expr.arrayInit(Types.INT,
     *     Expr.literal(1), Expr.literal(2), Expr.literal(3));
     * // new int[] {1, 2, 3}
     * }</pre>
     *
     * @param componentType the array component type
     * @param elements      the array elements
     * @return an array initializer expression
     */
    public static ArrayInitExpr arrayInit(TypeRef componentType, Expression... elements) {
        return new ArrayInitExpr(componentType, Arrays.asList(elements));
    }

    /**
     * Creates an array initializer expression.
     *
     * @param componentType the array component type
     * @param elements      the array elements
     * @return an array initializer expression
     */
    public static ArrayInitExpr arrayInit(TypeRef componentType, List<Expression> elements) {
        return new ArrayInitExpr(componentType, elements);
    }

    // ========================================================================
    // Convenience Methods
    // ========================================================================

    /**
     * Creates a null check expression ({@code expr == null}).
     *
     * @param expr the expression to check
     * @return an equality expression with null
     */
    public static BinaryExpr isNull(Expression expr) {
        return eq(expr, null_());
    }

    /**
     * Creates a non-null check expression ({@code expr != null}).
     *
     * @param expr the expression to check
     * @return an inequality expression with null
     */
    public static BinaryExpr isNotNull(Expression expr) {
        return ne(expr, null_());
    }

    /**
     * Creates a string concatenation expression.
     *
     * <p>Example:
     * <pre>{@code
     * Expression concat = Expr.concat(Expr.literal("Hello, "), Expr.var("name"));
     * // "Hello, " + name
     * }</pre>
     *
     * @param left  the left operand
     * @param right the right operand
     * @return a string concatenation expression (uses ADD operator)
     */
    public static BinaryExpr concat(Expression left, Expression right) {
        return add(left, right);
    }

    /**
     * Creates a chained method call expression.
     *
     * <p>Example:
     * <pre>{@code
     * Expression chain = Expr.chain(
     *     Expr.var("builder"),
     *     "setName", List.of(Expr.literal("John")),
     *     "setAge", List.of(Expr.literal(30)),
     *     "build", List.of()
     * );
     * // builder.setName("John").setAge(30).build()
     * }</pre>
     *
     * @param target         the initial target expression
     * @param callsAndArgs   alternating method names and argument lists
     * @return a chained method call expression
     */
    @SuppressWarnings("unchecked")
    public static Expression chain(Expression target, Object... callsAndArgs) {
        Expression current = target;
        for (int i = 0; i < callsAndArgs.length; i += 2) {
            String methodName = (String) callsAndArgs[i];
            List<Expression> args = (List<Expression>) callsAndArgs[i + 1];
            current = new MethodCallExpr(current, methodName, args, List.of());
        }
        return current;
    }
}
