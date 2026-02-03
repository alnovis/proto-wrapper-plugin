package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.decl.ParameterDecl;
import io.alnovis.protowrapper.ir.expr.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Expr}.
 */
class ExprTest {

    // ========================================================================
    // Literal Expressions
    // ========================================================================

    @Test
    void null_createsNullLiteral() {
        LiteralExpr expr = Expr.null_();

        assertTrue(expr.isNull());
        assertNull(expr.value());
    }

    @Test
    void literal_boolean_createsBooleanLiteral() {
        LiteralExpr trueExpr = Expr.literal(true);
        LiteralExpr falseExpr = Expr.literal(false);

        assertEquals(true, trueExpr.value());
        assertEquals(false, falseExpr.value());
        assertEquals(Types.BOOLEAN, trueExpr.type());
    }

    @Test
    void literal_int_createsIntLiteral() {
        LiteralExpr expr = Expr.literal(42);

        assertEquals(42, expr.value());
        assertEquals(Types.INT, expr.type());
    }

    @Test
    void literal_long_createsLongLiteral() {
        LiteralExpr expr = Expr.literal(42L);

        assertEquals(42L, expr.value());
        assertEquals(Types.LONG, expr.type());
    }

    @Test
    void literal_float_createsFloatLiteral() {
        LiteralExpr expr = Expr.literal(3.14f);

        assertEquals(3.14f, expr.value());
        assertEquals(Types.FLOAT, expr.type());
    }

    @Test
    void literal_double_createsDoubleLiteral() {
        LiteralExpr expr = Expr.literal(3.14);

        assertEquals(3.14, expr.value());
        assertEquals(Types.DOUBLE, expr.type());
    }

    @Test
    void literal_char_createsCharLiteral() {
        LiteralExpr expr = Expr.literal('A');

        assertEquals('A', expr.value());
        assertEquals(Types.CHAR, expr.type());
    }

    @Test
    void literal_string_createsStringLiteral() {
        LiteralExpr expr = Expr.literal("hello");

        assertEquals("hello", expr.value());
        assertEquals(Types.STRING, expr.type());
    }

    @Test
    void classLiteral_createsClassLiteral() {
        LiteralExpr expr = Expr.classLiteral(Types.STRING);

        assertTrue(expr.isClass());
        assertEquals(Types.STRING, expr.value());
    }

    // ========================================================================
    // Variable References
    // ========================================================================

    @Test
    void var_createsVarRef() {
        VarRefExpr expr = Expr.var("myVar");

        assertEquals("myVar", expr.name());
    }

    @Test
    void this_returnsThisExpr() {
        ThisExpr expr = Expr.this_();

        assertSame(ThisExpr.INSTANCE, expr);
    }

    @Test
    void typeRef_createsTypeRefExpr() {
        TypeRefExpr expr = Expr.typeRef(Types.type("java.lang.Math"));

        assertEquals(Types.type("java.lang.Math"), expr.type());
    }

    // ========================================================================
    // Field Access
    // ========================================================================

    @Test
    void field_thisField_createsFieldAccess() {
        FieldAccessExpr expr = Expr.field("name");

        assertTrue(expr.isThisAccess());
        assertEquals("name", expr.fieldName());
    }

    @Test
    void field_targetField_createsFieldAccess() {
        FieldAccessExpr expr = Expr.field(Expr.var("obj"), "name");

        assertEquals("name", expr.fieldName());
        assertInstanceOf(VarRefExpr.class, expr.target());
    }

    @Test
    void staticField_createsStaticFieldAccess() {
        FieldAccessExpr expr = Expr.staticField(Types.type("java.lang.Math"), "PI");

        assertTrue(expr.isStatic());
        assertEquals("PI", expr.fieldName());
    }

    // ========================================================================
    // Method Calls
    // ========================================================================

    @Test
    void call_thisMethod_createsMethodCall() {
        MethodCallExpr expr = Expr.call("getName");

        assertTrue(expr.isThisCall());
        assertEquals("getName", expr.methodName());
        assertTrue(expr.hasNoArguments());
    }

    @Test
    void call_thisMethodWithArgs_createsMethodCall() {
        MethodCallExpr expr = Expr.call("setName", Expr.literal("John"));

        assertEquals("setName", expr.methodName());
        assertEquals(1, expr.arguments().size());
    }

    @Test
    void call_targetMethod_createsMethodCall() {
        MethodCallExpr expr = Expr.call(Expr.var("list"), "add", Expr.var("item"));

        assertEquals("add", expr.methodName());
        assertInstanceOf(VarRefExpr.class, expr.target());
    }

    @Test
    void call_withTypeArguments_createsGenericMethodCall() {
        MethodCallExpr expr = Expr.call(Expr.var("list"), "stream",
            List.of(Types.STRING));

        assertTrue(expr.hasTypeArguments());
    }

    @Test
    void staticCall_createsStaticMethodCall() {
        MethodCallExpr expr = Expr.staticCall(Types.type("java.lang.Math"), "abs",
            Expr.literal(-1));

        assertTrue(expr.isStatic());
        assertEquals("abs", expr.methodName());
    }

    @Test
    void staticCall_withTypeArguments_createsStaticMethodCall() {
        MethodCallExpr expr = Expr.staticCall(
            Types.type("java.util.Collections"), "emptyList",
            List.of(Types.STRING));

        assertTrue(expr.isStatic());
        assertTrue(expr.hasTypeArguments());
    }

    // ========================================================================
    // Constructor Calls
    // ========================================================================

    @Test
    void new_createsConstructorCall() {
        ConstructorCallExpr expr = Expr.new_(Types.type("java.util.ArrayList"));

        assertEquals(Types.type("java.util.ArrayList"), expr.type());
        assertTrue(expr.hasNoArguments());
    }

    @Test
    void new_withArguments_createsConstructorCall() {
        ConstructorCallExpr expr = Expr.new_(Types.type("Person"), Expr.literal("John"));

        assertEquals(1, expr.arguments().size());
    }

    @Test
    void new_withListArguments_createsConstructorCall() {
        ConstructorCallExpr expr = Expr.new_(Types.type("Person"),
            List.of(Expr.literal("John")));

        assertEquals(1, expr.arguments().size());
    }

    // ========================================================================
    // Unary Operators
    // ========================================================================

    @Test
    void not_createsNotExpression() {
        UnaryExpr expr = Expr.not(Expr.var("flag"));

        assertEquals(UnaryOp.NOT, expr.operator());
    }

    @Test
    void negate_createsNegateExpression() {
        UnaryExpr expr = Expr.negate(Expr.var("x"));

        assertEquals(UnaryOp.NEG, expr.operator());
    }

    @Test
    void plus_createsPlusExpression() {
        UnaryExpr expr = Expr.plus(Expr.var("x"));

        assertEquals(UnaryOp.PLUS, expr.operator());
    }

    @Test
    void complement_createsComplementExpression() {
        UnaryExpr expr = Expr.complement(Expr.var("x"));

        assertEquals(UnaryOp.BIT_NOT, expr.operator());
    }

    // ========================================================================
    // Binary Operators - Arithmetic
    // ========================================================================

    @Test
    void add_createsAddExpression() {
        BinaryExpr expr = Expr.add(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.ADD, expr.operator());
    }

    @Test
    void subtract_createsSubtractExpression() {
        BinaryExpr expr = Expr.subtract(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.SUB, expr.operator());
    }

    @Test
    void multiply_createsMultiplyExpression() {
        BinaryExpr expr = Expr.multiply(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.MUL, expr.operator());
    }

    @Test
    void divide_createsDivideExpression() {
        BinaryExpr expr = Expr.divide(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.DIV, expr.operator());
    }

    @Test
    void modulo_createsModuloExpression() {
        BinaryExpr expr = Expr.modulo(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.MOD, expr.operator());
    }

    // ========================================================================
    // Binary Operators - Comparison
    // ========================================================================

    @Test
    void eq_createsEqualityExpression() {
        BinaryExpr expr = Expr.eq(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.EQ, expr.operator());
    }

    @Test
    void ne_createsInequalityExpression() {
        BinaryExpr expr = Expr.ne(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.NE, expr.operator());
    }

    @Test
    void lt_createsLessThanExpression() {
        BinaryExpr expr = Expr.lt(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.LT, expr.operator());
    }

    @Test
    void le_createsLessThanOrEqualExpression() {
        BinaryExpr expr = Expr.le(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.LE, expr.operator());
    }

    @Test
    void gt_createsGreaterThanExpression() {
        BinaryExpr expr = Expr.gt(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.GT, expr.operator());
    }

    @Test
    void ge_createsGreaterThanOrEqualExpression() {
        BinaryExpr expr = Expr.ge(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.GE, expr.operator());
    }

    // ========================================================================
    // Binary Operators - Logical
    // ========================================================================

    @Test
    void and_createsLogicalAndExpression() {
        BinaryExpr expr = Expr.and(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.AND, expr.operator());
    }

    @Test
    void or_createsLogicalOrExpression() {
        BinaryExpr expr = Expr.or(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.OR, expr.operator());
    }

    // ========================================================================
    // Binary Operators - Bitwise
    // ========================================================================

    @Test
    void bitwiseAnd_createsBitwiseAndExpression() {
        BinaryExpr expr = Expr.bitwiseAnd(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.BIT_AND, expr.operator());
    }

    @Test
    void bitwiseOr_createsBitwiseOrExpression() {
        BinaryExpr expr = Expr.bitwiseOr(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.BIT_OR, expr.operator());
    }

    @Test
    void bitwiseXor_createsBitwiseXorExpression() {
        BinaryExpr expr = Expr.bitwiseXor(Expr.var("a"), Expr.var("b"));

        assertEquals(BinaryOp.BIT_XOR, expr.operator());
    }

    @Test
    void leftShift_createsLeftShiftExpression() {
        BinaryExpr expr = Expr.leftShift(Expr.var("a"), Expr.literal(2));

        assertEquals(BinaryOp.LSHIFT, expr.operator());
    }

    @Test
    void rightShift_createsRightShiftExpression() {
        BinaryExpr expr = Expr.rightShift(Expr.var("a"), Expr.literal(2));

        assertEquals(BinaryOp.RSHIFT, expr.operator());
    }

    @Test
    void unsignedRightShift_createsUnsignedRightShiftExpression() {
        BinaryExpr expr = Expr.unsignedRightShift(Expr.var("a"), Expr.literal(2));

        assertEquals(BinaryOp.URSHIFT, expr.operator());
    }

    // ========================================================================
    // Ternary Operator
    // ========================================================================

    @Test
    void ternary_createsTernaryExpression() {
        TernaryExpr expr = Expr.ternary(
            Expr.var("cond"),
            Expr.literal(1),
            Expr.literal(2)
        );

        assertInstanceOf(VarRefExpr.class, expr.condition());
        assertInstanceOf(LiteralExpr.class, expr.thenExpr());
        assertInstanceOf(LiteralExpr.class, expr.elseExpr());
    }

    // ========================================================================
    // Type Operations
    // ========================================================================

    @Test
    void cast_createsCastExpression() {
        CastExpr expr = Expr.cast(Types.STRING, Expr.var("obj"));

        assertEquals(Types.STRING, expr.type());
    }

    @Test
    void instanceOf_createsInstanceOfExpression() {
        InstanceOfExpr expr = Expr.instanceOf(Expr.var("obj"), Types.STRING);

        assertEquals(Types.STRING, expr.type());
        assertNull(expr.bindingVariable());
    }

    @Test
    void instanceOf_withPattern_createsPatternInstanceOf() {
        InstanceOfExpr expr = Expr.instanceOf(Expr.var("obj"), Types.STRING, "str");

        assertEquals("str", expr.bindingVariable());
    }

    // ========================================================================
    // Lambda Expressions
    // ========================================================================

    @Test
    void lambda_withParameterList_createsLambda() {
        ParameterDecl param = ParameterDecl.of("x", Types.INT);
        LambdaExpr expr = Expr.lambda(List.of(param), Expr.var("x"));

        assertEquals(1, expr.parameters().size());
        assertInstanceOf(VarRefExpr.class, expr.body());
    }

    @Test
    void lambda_singleTypedParam_createsLambda() {
        LambdaExpr expr = Expr.lambda(Types.STRING, "s",
            Expr.call(Expr.var("s"), "length"));

        assertEquals(1, expr.parameters().size());
        assertEquals("s", expr.parameters().get(0).name());
        assertEquals(Types.STRING, expr.parameters().get(0).type());
    }

    @Test
    void lambda_inferredType_createsLambda() {
        LambdaExpr expr = Expr.lambda("x",
            Expr.multiply(Expr.var("x"), Expr.literal(2)));

        assertEquals(1, expr.parameters().size());
        assertTrue(expr.parameters().get(0).hasInferredType());
    }

    // ========================================================================
    // Array Expressions
    // ========================================================================

    @Test
    void arrayInit_createsArrayInitExpression() {
        ArrayInitExpr expr = Expr.arrayInit(Types.INT,
            Expr.literal(1), Expr.literal(2), Expr.literal(3));

        assertEquals(Types.INT, expr.componentType());
        assertEquals(3, expr.elements().size());
    }

    @Test
    void arrayInit_withList_createsArrayInitExpression() {
        ArrayInitExpr expr = Expr.arrayInit(Types.STRING,
            List.of(Expr.literal("a"), Expr.literal("b")));

        assertEquals(2, expr.elements().size());
    }

    // ========================================================================
    // Convenience Methods
    // ========================================================================

    @Test
    void isNull_createsNullCheck() {
        BinaryExpr expr = Expr.isNull(Expr.var("x"));

        assertEquals(BinaryOp.EQ, expr.operator());
        assertInstanceOf(LiteralExpr.class, expr.right());
    }

    @Test
    void isNotNull_createsNonNullCheck() {
        BinaryExpr expr = Expr.isNotNull(Expr.var("x"));

        assertEquals(BinaryOp.NE, expr.operator());
    }

    @Test
    void concat_createsAddExpression() {
        BinaryExpr expr = Expr.concat(Expr.literal("Hello "), Expr.var("name"));

        assertEquals(BinaryOp.ADD, expr.operator());
    }

    @Test
    void chain_createsChainedMethodCalls() {
        Expression expr = Expr.chain(
            Expr.var("builder"),
            "setName", List.of(Expr.literal("John")),
            "setAge", List.of(Expr.literal(30)),
            "build", List.of()
        );

        assertInstanceOf(MethodCallExpr.class, expr);
        MethodCallExpr call = (MethodCallExpr) expr;
        assertEquals("build", call.methodName());
    }
}
