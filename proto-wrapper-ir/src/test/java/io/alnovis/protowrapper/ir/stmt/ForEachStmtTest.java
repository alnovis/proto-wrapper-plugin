package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.VarRefExpr;
import io.alnovis.protowrapper.ir.type.ClassType;
import io.alnovis.protowrapper.ir.type.PrimitiveType;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import io.alnovis.protowrapper.ir.type.TypeRef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ForEachStmt}.
 */
class ForEachStmtTest {

    @Test
    void constructor_validArguments_succeeds() {
        TypeRef type = ClassType.of("java.lang.String");
        VarRefExpr iterable = new VarRefExpr("names");
        List<Statement> body = List.of(new ReturnStmt(null));

        ForEachStmt stmt = new ForEachStmt("name", type, iterable, body);

        assertEquals("name", stmt.variableName());
        assertEquals(type, stmt.variableType());
        assertEquals(iterable, stmt.iterable());
        assertEquals(1, stmt.body().size());
    }

    @Test
    void constructor_nullVariableName_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new ForEachStmt(null, PrimitiveType.of(PrimitiveKind.INT),
                new VarRefExpr("items"), List.of()));
    }

    @Test
    void constructor_emptyVariableName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new ForEachStmt("", PrimitiveType.of(PrimitiveKind.INT),
                new VarRefExpr("items"), List.of()));
    }

    @Test
    void constructor_blankVariableName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new ForEachStmt("   ", PrimitiveType.of(PrimitiveKind.INT),
                new VarRefExpr("items"), List.of()));
    }

    @Test
    void constructor_nullVariableType_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new ForEachStmt("item", null, new VarRefExpr("items"), List.of()));
    }

    @Test
    void constructor_nullIterable_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new ForEachStmt("item", PrimitiveType.of(PrimitiveKind.INT), null, List.of()));
    }

    @Test
    void constructor_nullBody_treatedAsEmpty() {
        ForEachStmt stmt = new ForEachStmt("item", PrimitiveType.of(PrimitiveKind.INT),
            new VarRefExpr("items"), null);

        assertNotNull(stmt.body());
        assertTrue(stmt.body().isEmpty());
        assertTrue(stmt.hasEmptyBody());
    }

    @Test
    void body_isImmutable() {
        List<Statement> body = new ArrayList<>();
        body.add(new ReturnStmt(null));
        ForEachStmt stmt = new ForEachStmt("item", PrimitiveType.of(PrimitiveKind.INT),
            new VarRefExpr("items"), body);

        assertThrows(UnsupportedOperationException.class,
            () -> stmt.body().add(new ReturnStmt(null)));
    }

    @Test
    void hasEmptyBody_emptyBody_returnsTrue() {
        ForEachStmt stmt = new ForEachStmt("item", PrimitiveType.of(PrimitiveKind.INT),
            new VarRefExpr("items"), List.of());

        assertTrue(stmt.hasEmptyBody());
    }

    @Test
    void hasEmptyBody_nonEmptyBody_returnsFalse() {
        ForEachStmt stmt = new ForEachStmt("item", PrimitiveType.of(PrimitiveKind.INT),
            new VarRefExpr("items"), List.of(new ReturnStmt(null)));

        assertFalse(stmt.hasEmptyBody());
    }

    @Test
    void equals_sameComponents_areEqual() {
        TypeRef type = PrimitiveType.of(PrimitiveKind.INT);
        VarRefExpr iterable = new VarRefExpr("items");
        ForEachStmt a = new ForEachStmt("item", type, iterable, List.of());
        ForEachStmt b = new ForEachStmt("item", type, new VarRefExpr("items"), List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentVariableName_areNotEqual() {
        TypeRef type = PrimitiveType.of(PrimitiveKind.INT);
        VarRefExpr iterable = new VarRefExpr("items");
        ForEachStmt a = new ForEachStmt("item", type, iterable, List.of());
        ForEachStmt b = new ForEachStmt("element", type, iterable, List.of());

        assertNotEquals(a, b);
    }

    @Test
    void implementsStatement() {
        ForEachStmt stmt = new ForEachStmt("item", PrimitiveType.of(PrimitiveKind.INT),
            new VarRefExpr("items"), List.of());

        assertInstanceOf(Statement.class, stmt);
    }
}
