package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.LiteralExpr;
import io.alnovis.protowrapper.ir.expr.VarRefExpr;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WhileStmt}.
 */
class WhileStmtTest {

    @Test
    void constructor_validArguments_succeeds() {
        VarRefExpr condition = new VarRefExpr("running");
        List<Statement> body = List.of(new ReturnStmt(null));

        WhileStmt stmt = new WhileStmt(condition, body);

        assertEquals(condition, stmt.condition());
        assertEquals(1, stmt.body().size());
    }

    @Test
    void constructor_nullCondition_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new WhileStmt(null, List.of()));
    }

    @Test
    void constructor_nullBody_treatedAsEmpty() {
        WhileStmt stmt = new WhileStmt(new VarRefExpr("cond"), null);

        assertNotNull(stmt.body());
        assertTrue(stmt.body().isEmpty());
        assertTrue(stmt.hasEmptyBody());
    }

    @Test
    void body_isImmutable() {
        List<Statement> body = new ArrayList<>();
        body.add(new ReturnStmt(null));
        WhileStmt stmt = new WhileStmt(new VarRefExpr("cond"), body);

        assertThrows(UnsupportedOperationException.class,
            () -> stmt.body().add(new ReturnStmt(null)));
    }

    @Test
    void hasEmptyBody_emptyBody_returnsTrue() {
        WhileStmt stmt = new WhileStmt(new VarRefExpr("cond"), List.of());

        assertTrue(stmt.hasEmptyBody());
    }

    @Test
    void hasEmptyBody_nonEmptyBody_returnsFalse() {
        WhileStmt stmt = new WhileStmt(new VarRefExpr("cond"), List.of(new ReturnStmt(null)));

        assertFalse(stmt.hasEmptyBody());
    }

    @Test
    void equals_sameComponents_areEqual() {
        WhileStmt a = new WhileStmt(new VarRefExpr("cond"), List.of());
        WhileStmt b = new WhileStmt(new VarRefExpr("cond"), List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentCondition_areNotEqual() {
        WhileStmt a = new WhileStmt(new VarRefExpr("x"), List.of());
        WhileStmt b = new WhileStmt(new VarRefExpr("y"), List.of());

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentBody_areNotEqual() {
        WhileStmt a = new WhileStmt(new VarRefExpr("cond"), List.of());
        WhileStmt b = new WhileStmt(new VarRefExpr("cond"), List.of(new ReturnStmt(null)));

        assertNotEquals(a, b);
    }

    @Test
    void implementsStatement() {
        WhileStmt stmt = new WhileStmt(new VarRefExpr("cond"), List.of());

        assertInstanceOf(Statement.class, stmt);
    }
}
