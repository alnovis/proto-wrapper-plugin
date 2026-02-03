package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.decl.ParameterDecl;
import io.alnovis.protowrapper.ir.type.ClassType;
import io.alnovis.protowrapper.ir.type.PrimitiveType;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LambdaExpr}.
 */
class LambdaExprTest {

    @Test
    void constructor_noParameters_succeeds() {
        Expression body = new LiteralExpr("hello", ClassType.of("java.lang.String"));
        LambdaExpr lambda = new LambdaExpr(List.of(), body);

        assertTrue(lambda.parameters().isEmpty());
        assertEquals(body, lambda.body());
    }

    @Test
    void constructor_singleParameter_succeeds() {
        ParameterDecl param = new ParameterDecl("x", PrimitiveType.of(PrimitiveKind.INT), List.of(), false);
        Expression body = new BinaryExpr(
            new VarRefExpr("x"),
            BinaryOp.MUL,
            new LiteralExpr(2, PrimitiveType.of(PrimitiveKind.INT))
        );
        LambdaExpr lambda = new LambdaExpr(List.of(param), body);

        assertEquals(1, lambda.parameters().size());
        assertEquals("x", lambda.parameters().get(0).name());
    }

    @Test
    void constructor_multipleParameters_succeeds() {
        ParameterDecl paramA = new ParameterDecl("a", PrimitiveType.of(PrimitiveKind.INT), List.of(), false);
        ParameterDecl paramB = new ParameterDecl("b", PrimitiveType.of(PrimitiveKind.INT), List.of(), false);
        Expression body = new BinaryExpr(new VarRefExpr("a"), BinaryOp.ADD, new VarRefExpr("b"));
        LambdaExpr lambda = new LambdaExpr(List.of(paramA, paramB), body);

        assertEquals(2, lambda.parameters().size());
    }

    @Test
    void constructor_nullParameters_treatedAsEmpty() {
        Expression body = new LiteralExpr("hello", null);
        LambdaExpr lambda = new LambdaExpr(null, body);

        assertNotNull(lambda.parameters());
        assertTrue(lambda.parameters().isEmpty());
    }

    @Test
    void constructor_nullBody_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new LambdaExpr(List.of(), null));
    }

    @Test
    void parameters_areImmutable() {
        ParameterDecl param = new ParameterDecl("x", PrimitiveType.of(PrimitiveKind.INT), List.of(), false);
        List<ParameterDecl> params = new ArrayList<>();
        params.add(param);
        LambdaExpr lambda = new LambdaExpr(params, new VarRefExpr("x"));

        assertThrows(UnsupportedOperationException.class,
            () -> lambda.parameters().add(
                new ParameterDecl("y", PrimitiveType.of(PrimitiveKind.INT), List.of(), false)));
    }

    @Test
    void hasNoParameters_noParams_returnsTrue() {
        LambdaExpr lambda = new LambdaExpr(List.of(), new LiteralExpr("hello", null));

        assertTrue(lambda.hasNoParameters());
    }

    @Test
    void hasNoParameters_withParams_returnsFalse() {
        ParameterDecl param = new ParameterDecl("x", PrimitiveType.of(PrimitiveKind.INT), List.of(), false);
        LambdaExpr lambda = new LambdaExpr(List.of(param), new VarRefExpr("x"));

        assertFalse(lambda.hasNoParameters());
    }

    @Test
    void isSingleParameter_oneParam_returnsTrue() {
        ParameterDecl param = new ParameterDecl("x", PrimitiveType.of(PrimitiveKind.INT), List.of(), false);
        LambdaExpr lambda = new LambdaExpr(List.of(param), new VarRefExpr("x"));

        assertTrue(lambda.isSingleParameter());
    }

    @Test
    void isSingleParameter_noParams_returnsFalse() {
        LambdaExpr lambda = new LambdaExpr(List.of(), new LiteralExpr("hello", null));

        assertFalse(lambda.isSingleParameter());
    }

    @Test
    void isSingleParameter_multipleParams_returnsFalse() {
        ParameterDecl paramA = new ParameterDecl("a", PrimitiveType.of(PrimitiveKind.INT), List.of(), false);
        ParameterDecl paramB = new ParameterDecl("b", PrimitiveType.of(PrimitiveKind.INT), List.of(), false);
        LambdaExpr lambda = new LambdaExpr(List.of(paramA, paramB), new VarRefExpr("a"));

        assertFalse(lambda.isSingleParameter());
    }

    @Test
    void parameterCount_returnsCorrectCount() {
        assertEquals(0, new LambdaExpr(List.of(), new LiteralExpr(1, null)).parameterCount());

        ParameterDecl param = new ParameterDecl("x", null, List.of(), false);
        assertEquals(1, new LambdaExpr(List.of(param), new VarRefExpr("x")).parameterCount());
    }

    @Test
    void parameterWithInferredType_typeIsNull() {
        // Lambda with inferred type: x -> x * 2
        ParameterDecl param = new ParameterDecl("x", null, List.of(), false);
        LambdaExpr lambda = new LambdaExpr(List.of(param), new VarRefExpr("x"));

        assertTrue(param.hasInferredType());
        assertNull(lambda.parameters().get(0).type());
    }

    @Test
    void parameterWithExplicitType_typeIsNotNull() {
        // Lambda with explicit type: (int x) -> x * 2
        ParameterDecl param = new ParameterDecl("x", PrimitiveType.of(PrimitiveKind.INT), List.of(), false);
        LambdaExpr lambda = new LambdaExpr(List.of(param), new VarRefExpr("x"));

        assertFalse(param.hasInferredType());
        assertNotNull(lambda.parameters().get(0).type());
    }

    @Test
    void equals_sameComponents_areEqual() {
        ParameterDecl param = new ParameterDecl("x", PrimitiveType.of(PrimitiveKind.INT), List.of(), false);
        LambdaExpr a = new LambdaExpr(List.of(param), new VarRefExpr("x"));
        LambdaExpr b = new LambdaExpr(
            List.of(new ParameterDecl("x", PrimitiveType.of(PrimitiveKind.INT), List.of(), false)),
            new VarRefExpr("x")
        );

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentBody_areNotEqual() {
        ParameterDecl param = new ParameterDecl("x", null, List.of(), false);
        LambdaExpr a = new LambdaExpr(List.of(param), new VarRefExpr("x"));
        LambdaExpr b = new LambdaExpr(List.of(param), new VarRefExpr("y"));

        assertNotEquals(a, b);
    }

    @Test
    void implementsExpression() {
        LambdaExpr lambda = new LambdaExpr(List.of(), new LiteralExpr(1, null));

        assertInstanceOf(Expression.class, lambda);
    }
}
