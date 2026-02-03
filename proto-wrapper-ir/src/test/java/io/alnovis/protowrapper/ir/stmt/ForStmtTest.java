package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.BinaryExpr;
import io.alnovis.protowrapper.ir.expr.BinaryOp;
import io.alnovis.protowrapper.ir.expr.LiteralExpr;
import io.alnovis.protowrapper.ir.expr.VarRefExpr;
import io.alnovis.protowrapper.ir.type.PrimitiveType;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ForStmt}.
 */
class ForStmtTest {

    @Test
    void constructor_allParts_succeeds() {
        VarDeclStmt init = new VarDeclStmt("i", PrimitiveType.of(PrimitiveKind.INT),
            new LiteralExpr(0, null), false);
        BinaryExpr condition = new BinaryExpr(new VarRefExpr("i"), BinaryOp.LT, new VarRefExpr("n"));
        VarRefExpr update = new VarRefExpr("i");
        List<Statement> body = List.of(new ReturnStmt(null));

        ForStmt stmt = new ForStmt(init, condition, update, body);

        assertEquals(init, stmt.initialization());
        assertEquals(condition, stmt.condition());
        assertEquals(update, stmt.update());
        assertEquals(1, stmt.body().size());
    }

    @Test
    void constructor_nullInitialization_succeeds() {
        ForStmt stmt = new ForStmt(null, new VarRefExpr("cond"), null, List.of());

        assertNull(stmt.initialization());
        assertFalse(stmt.hasInitialization());
    }

    @Test
    void constructor_nullCondition_createsInfiniteLoop() {
        ForStmt stmt = new ForStmt(null, null, null, List.of());

        assertNull(stmt.condition());
        assertFalse(stmt.hasCondition());
        assertTrue(stmt.isInfinite());
    }

    @Test
    void constructor_nullUpdate_succeeds() {
        ForStmt stmt = new ForStmt(null, new VarRefExpr("cond"), null, List.of());

        assertNull(stmt.update());
        assertFalse(stmt.hasUpdate());
    }

    @Test
    void constructor_nullBody_treatedAsEmpty() {
        ForStmt stmt = new ForStmt(null, null, null, null);

        assertNotNull(stmt.body());
        assertTrue(stmt.body().isEmpty());
        assertTrue(stmt.hasEmptyBody());
    }

    @Test
    void body_isImmutable() {
        List<Statement> body = new ArrayList<>();
        body.add(new ReturnStmt(null));
        ForStmt stmt = new ForStmt(null, null, null, body);

        assertThrows(UnsupportedOperationException.class,
            () -> stmt.body().add(new ReturnStmt(null)));
    }

    @Test
    void hasInitialization_withInit_returnsTrue() {
        VarDeclStmt init = new VarDeclStmt("i", PrimitiveType.of(PrimitiveKind.INT), null, false);
        ForStmt stmt = new ForStmt(init, null, null, List.of());

        assertTrue(stmt.hasInitialization());
    }

    @Test
    void hasCondition_withCondition_returnsTrue() {
        ForStmt stmt = new ForStmt(null, new VarRefExpr("cond"), null, List.of());

        assertTrue(stmt.hasCondition());
        assertFalse(stmt.isInfinite());
    }

    @Test
    void hasUpdate_withUpdate_returnsTrue() {
        ForStmt stmt = new ForStmt(null, null, new VarRefExpr("i"), List.of());

        assertTrue(stmt.hasUpdate());
    }

    @Test
    void isInfinite_noCondition_returnsTrue() {
        ForStmt stmt = new ForStmt(null, null, null, List.of());

        assertTrue(stmt.isInfinite());
    }

    @Test
    void isInfinite_withCondition_returnsFalse() {
        ForStmt stmt = new ForStmt(null, new LiteralExpr(true, null), null, List.of());

        assertFalse(stmt.isInfinite());
    }

    @Test
    void hasEmptyBody_emptyBody_returnsTrue() {
        ForStmt stmt = new ForStmt(null, null, null, List.of());

        assertTrue(stmt.hasEmptyBody());
    }

    @Test
    void hasEmptyBody_nonEmptyBody_returnsFalse() {
        ForStmt stmt = new ForStmt(null, null, null, List.of(new ReturnStmt(null)));

        assertFalse(stmt.hasEmptyBody());
    }

    @Test
    void equals_sameComponents_areEqual() {
        ForStmt a = new ForStmt(null, new VarRefExpr("x"), null, List.of());
        ForStmt b = new ForStmt(null, new VarRefExpr("x"), null, List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentCondition_areNotEqual() {
        ForStmt a = new ForStmt(null, new VarRefExpr("x"), null, List.of());
        ForStmt b = new ForStmt(null, new VarRefExpr("y"), null, List.of());

        assertNotEquals(a, b);
    }

    @Test
    void equals_allNulls_areEqual() {
        ForStmt a = new ForStmt(null, null, null, null);
        ForStmt b = new ForStmt(null, null, null, List.of());

        assertEquals(a, b);
    }

    @Test
    void implementsStatement() {
        ForStmt stmt = new ForStmt(null, null, null, List.of());

        assertInstanceOf(Statement.class, stmt);
    }
}
