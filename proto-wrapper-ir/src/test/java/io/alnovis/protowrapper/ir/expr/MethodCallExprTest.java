package io.alnovis.protowrapper.ir.expr;

import io.alnovis.protowrapper.ir.type.ClassType;
import io.alnovis.protowrapper.ir.type.TypeRef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MethodCallExpr}.
 */
class MethodCallExprTest {

    @Test
    void constructor_validArguments_succeeds() {
        Expression target = new VarRefExpr("obj");
        List<Expression> args = List.of(new LiteralExpr(42, null));
        List<TypeRef> typeArgs = List.of();
        MethodCallExpr call = new MethodCallExpr(target, "method", args, typeArgs);

        assertEquals(target, call.target());
        assertEquals("method", call.methodName());
        assertEquals(1, call.arguments().size());
        assertTrue(call.typeArguments().isEmpty());
    }

    @Test
    void constructor_nullTarget_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new MethodCallExpr(null, "method", List.of(), List.of()));
    }

    @Test
    void constructor_nullMethodName_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new MethodCallExpr(new VarRefExpr("obj"), null, List.of(), List.of()));
    }

    @Test
    void constructor_emptyMethodName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new MethodCallExpr(new VarRefExpr("obj"), "", List.of(), List.of()));
    }

    @Test
    void constructor_blankMethodName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new MethodCallExpr(new VarRefExpr("obj"), "   ", List.of(), List.of()));
    }

    @Test
    void constructor_nullArguments_treatedAsEmpty() {
        MethodCallExpr call = new MethodCallExpr(new VarRefExpr("obj"), "method", null, List.of());

        assertNotNull(call.arguments());
        assertTrue(call.arguments().isEmpty());
    }

    @Test
    void constructor_nullTypeArguments_treatedAsEmpty() {
        MethodCallExpr call = new MethodCallExpr(new VarRefExpr("obj"), "method", List.of(), null);

        assertNotNull(call.typeArguments());
        assertTrue(call.typeArguments().isEmpty());
    }

    @Test
    void arguments_areImmutable() {
        List<Expression> args = new ArrayList<>();
        args.add(new LiteralExpr(1, null));
        MethodCallExpr call = new MethodCallExpr(new VarRefExpr("obj"), "method", args, List.of());

        assertThrows(UnsupportedOperationException.class,
            () -> call.arguments().add(new LiteralExpr(2, null)));
    }

    @Test
    void typeArguments_areImmutable() {
        List<TypeRef> typeArgs = new ArrayList<>();
        typeArgs.add(ClassType.of("java.lang.String"));
        MethodCallExpr call = new MethodCallExpr(new VarRefExpr("obj"), "method", List.of(), typeArgs);

        assertThrows(UnsupportedOperationException.class,
            () -> call.typeArguments().add(ClassType.of("java.lang.Integer")));
    }

    @Test
    void staticMethodCall_usesTypeRefExprAsTarget() {
        TypeRefExpr typeRef = new TypeRefExpr(ClassType.of("java.lang.Math"));
        MethodCallExpr staticCall = new MethodCallExpr(
            typeRef, "abs",
            List.of(new LiteralExpr(-1, null)),
            List.of()
        );

        assertInstanceOf(TypeRefExpr.class, staticCall.target());
        assertEquals("abs", staticCall.methodName());
    }

    @Test
    void instanceMethodCall_usesThisExprAsTarget() {
        MethodCallExpr thisCall = new MethodCallExpr(
            ThisExpr.INSTANCE, "getName",
            List.of(),
            List.of()
        );

        assertSame(ThisExpr.INSTANCE, thisCall.target());
    }

    @Test
    void genericMethodCall_hasTypeArguments() {
        TypeRef stringType = ClassType.of("java.lang.String");
        MethodCallExpr genericCall = new MethodCallExpr(
            new VarRefExpr("collections"), "emptyList",
            List.of(),
            List.of(stringType)
        );

        assertEquals(1, genericCall.typeArguments().size());
        assertEquals(stringType, genericCall.typeArguments().get(0));
    }

    @Test
    void equals_sameComponents_areEqual() {
        MethodCallExpr a = new MethodCallExpr(
            new VarRefExpr("obj"), "method",
            List.of(new LiteralExpr(1, null)),
            List.of()
        );
        MethodCallExpr b = new MethodCallExpr(
            new VarRefExpr("obj"), "method",
            List.of(new LiteralExpr(1, null)),
            List.of()
        );

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentMethodName_areNotEqual() {
        MethodCallExpr a = new MethodCallExpr(new VarRefExpr("obj"), "method1", List.of(), List.of());
        MethodCallExpr b = new MethodCallExpr(new VarRefExpr("obj"), "method2", List.of(), List.of());

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentArguments_areNotEqual() {
        MethodCallExpr a = new MethodCallExpr(
            new VarRefExpr("obj"), "method",
            List.of(new LiteralExpr(1, null)),
            List.of()
        );
        MethodCallExpr b = new MethodCallExpr(
            new VarRefExpr("obj"), "method",
            List.of(new LiteralExpr(2, null)),
            List.of()
        );

        assertNotEquals(a, b);
    }

    @Test
    void implementsExpression() {
        MethodCallExpr call = new MethodCallExpr(new VarRefExpr("obj"), "method", List.of(), List.of());

        assertInstanceOf(Expression.class, call);
    }
}
