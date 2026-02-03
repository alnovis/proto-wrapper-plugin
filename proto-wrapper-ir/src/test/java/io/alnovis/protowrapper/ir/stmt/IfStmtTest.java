package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.BinaryExpr;
import io.alnovis.protowrapper.ir.expr.BinaryOp;
import io.alnovis.protowrapper.ir.expr.Expression;
import io.alnovis.protowrapper.ir.expr.VarRefExpr;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IfStmt}.
 */
class IfStmtTest {

    private Expression condition() {
        return new BinaryExpr(new VarRefExpr("x"), BinaryOp.GT, new VarRefExpr("y"));
    }

    @Test
    void constructor_validArguments_succeeds() {
        Expression cond = condition();
        List<Statement> thenBranch = List.of(new ReturnStmt(null));
        List<Statement> elseBranch = List.of();
        IfStmt stmt = new IfStmt(cond, thenBranch, elseBranch);

        assertEquals(cond, stmt.condition());
        assertEquals(1, stmt.thenBranch().size());
        assertTrue(stmt.elseBranch().isEmpty());
    }

    @Test
    void constructor_nullCondition_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new IfStmt(null, List.of(), List.of()));
    }

    @Test
    void constructor_nullThenBranch_treatedAsEmpty() {
        IfStmt stmt = new IfStmt(condition(), null, List.of());

        assertNotNull(stmt.thenBranch());
        assertTrue(stmt.thenBranch().isEmpty());
    }

    @Test
    void constructor_nullElseBranch_treatedAsEmpty() {
        IfStmt stmt = new IfStmt(condition(), List.of(), null);

        assertNotNull(stmt.elseBranch());
        assertTrue(stmt.elseBranch().isEmpty());
    }

    @Test
    void thenBranch_isImmutable() {
        List<Statement> thenBranch = new ArrayList<>();
        thenBranch.add(new ReturnStmt(null));
        IfStmt stmt = new IfStmt(condition(), thenBranch, List.of());

        assertThrows(UnsupportedOperationException.class,
            () -> stmt.thenBranch().add(new ReturnStmt(null)));
    }

    @Test
    void elseBranch_isImmutable() {
        List<Statement> elseBranch = new ArrayList<>();
        elseBranch.add(new ReturnStmt(null));
        IfStmt stmt = new IfStmt(condition(), List.of(), elseBranch);

        assertThrows(UnsupportedOperationException.class,
            () -> stmt.elseBranch().add(new ReturnStmt(null)));
    }

    @Test
    void hasElse_withElse_returnsTrue() {
        IfStmt stmt = new IfStmt(condition(), List.of(), List.of(new ReturnStmt(null)));

        assertTrue(stmt.hasElse());
    }

    @Test
    void hasElse_withoutElse_returnsFalse() {
        IfStmt stmt = new IfStmt(condition(), List.of(), List.of());

        assertFalse(stmt.hasElse());
    }

    @Test
    void hasNoElse_withoutElse_returnsTrue() {
        IfStmt stmt = new IfStmt(condition(), List.of(), List.of());

        assertTrue(stmt.hasNoElse());
    }

    @Test
    void isElseIf_withNestedIf_returnsTrue() {
        IfStmt inner = new IfStmt(new VarRefExpr("other"), List.of(), List.of());
        IfStmt stmt = new IfStmt(condition(), List.of(), List.of(inner));

        assertTrue(stmt.isElseIf());
    }

    @Test
    void isElseIf_withMultipleStatements_returnsFalse() {
        IfStmt stmt = new IfStmt(condition(), List.of(),
            List.of(new ReturnStmt(null), new ReturnStmt(null)));

        assertFalse(stmt.isElseIf());
    }

    @Test
    void equals_sameComponents_areEqual() {
        Expression cond = condition();
        IfStmt a = new IfStmt(cond, List.of(new ReturnStmt(null)), List.of());
        IfStmt b = new IfStmt(
            new BinaryExpr(new VarRefExpr("x"), BinaryOp.GT, new VarRefExpr("y")),
            List.of(new ReturnStmt(null)),
            List.of()
        );

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentCondition_areNotEqual() {
        IfStmt a = new IfStmt(new VarRefExpr("a"), List.of(), List.of());
        IfStmt b = new IfStmt(new VarRefExpr("b"), List.of(), List.of());

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentThenBranch_areNotEqual() {
        Expression cond = condition();
        IfStmt a = new IfStmt(cond, List.of(new ReturnStmt(null)), List.of());
        IfStmt b = new IfStmt(cond, List.of(), List.of());

        assertNotEquals(a, b);
    }

    @Test
    void implementsStatement() {
        IfStmt stmt = new IfStmt(condition(), List.of(), List.of());

        assertInstanceOf(Statement.class, stmt);
    }
}
