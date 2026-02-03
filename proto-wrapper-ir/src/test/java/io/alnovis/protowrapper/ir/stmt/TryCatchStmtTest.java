package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.type.ClassType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TryCatchStmt}.
 */
class TryCatchStmtTest {

    private CatchClause ioExceptionCatch() {
        return new CatchClause("e", ClassType.of("java.io.IOException"), List.of());
    }

    @Test
    void constructor_withCatch_succeeds() {
        List<Statement> tryBlock = List.of(new ReturnStmt(null));
        List<CatchClause> catches = List.of(ioExceptionCatch());

        TryCatchStmt stmt = new TryCatchStmt(tryBlock, catches, List.of());

        assertEquals(1, stmt.tryBlock().size());
        assertEquals(1, stmt.catchClauses().size());
        assertTrue(stmt.hasCatch());
        assertFalse(stmt.hasFinally());
    }

    @Test
    void constructor_withFinally_succeeds() {
        List<Statement> tryBlock = List.of(new ReturnStmt(null));
        List<Statement> finallyBlock = List.of(new ReturnStmt(null));

        TryCatchStmt stmt = new TryCatchStmt(tryBlock, List.of(), finallyBlock);

        assertTrue(stmt.hasFinally());
        assertFalse(stmt.hasCatch());
        assertTrue(stmt.isTryFinally());
    }

    @Test
    void constructor_withBoth_succeeds() {
        List<Statement> tryBlock = List.of(new ReturnStmt(null));
        List<CatchClause> catches = List.of(ioExceptionCatch());
        List<Statement> finallyBlock = List.of(new ReturnStmt(null));

        TryCatchStmt stmt = new TryCatchStmt(tryBlock, catches, finallyBlock);

        assertTrue(stmt.hasCatch());
        assertTrue(stmt.hasFinally());
        assertFalse(stmt.isTryFinally());
    }

    @Test
    void constructor_nullTryBlock_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new TryCatchStmt(null, List.of(ioExceptionCatch()), List.of()));
    }

    @Test
    void constructor_noCatchNoFinally_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new TryCatchStmt(List.of(), List.of(), List.of()));
    }

    @Test
    void constructor_nullCatches_treatedAsEmpty() {
        TryCatchStmt stmt = new TryCatchStmt(List.of(), null, List.of(new ReturnStmt(null)));

        assertNotNull(stmt.catchClauses());
        assertTrue(stmt.catchClauses().isEmpty());
    }

    @Test
    void constructor_nullFinally_treatedAsEmpty() {
        TryCatchStmt stmt = new TryCatchStmt(List.of(), List.of(ioExceptionCatch()), null);

        assertNotNull(stmt.finallyBlock());
        assertTrue(stmt.finallyBlock().isEmpty());
    }

    @Test
    void tryBlock_isImmutable() {
        List<Statement> tryBlock = new ArrayList<>();
        tryBlock.add(new ReturnStmt(null));
        TryCatchStmt stmt = new TryCatchStmt(tryBlock, List.of(ioExceptionCatch()), List.of());

        assertThrows(UnsupportedOperationException.class,
            () -> stmt.tryBlock().add(new ReturnStmt(null)));
    }

    @Test
    void catchClauses_isImmutable() {
        List<CatchClause> catches = new ArrayList<>();
        catches.add(ioExceptionCatch());
        TryCatchStmt stmt = new TryCatchStmt(List.of(), catches, List.of());

        assertThrows(UnsupportedOperationException.class,
            () -> stmt.catchClauses().add(ioExceptionCatch()));
    }

    @Test
    void finallyBlock_isImmutable() {
        List<Statement> finallyBlock = new ArrayList<>();
        finallyBlock.add(new ReturnStmt(null));
        TryCatchStmt stmt = new TryCatchStmt(List.of(), List.of(), finallyBlock);

        assertThrows(UnsupportedOperationException.class,
            () -> stmt.finallyBlock().add(new ReturnStmt(null)));
    }

    @Test
    void hasCatch_withCatches_returnsTrue() {
        TryCatchStmt stmt = new TryCatchStmt(List.of(), List.of(ioExceptionCatch()), List.of());

        assertTrue(stmt.hasCatch());
    }

    @Test
    void hasCatch_withoutCatches_returnsFalse() {
        TryCatchStmt stmt = new TryCatchStmt(List.of(), List.of(), List.of(new ReturnStmt(null)));

        assertFalse(stmt.hasCatch());
    }

    @Test
    void hasFinally_withFinally_returnsTrue() {
        TryCatchStmt stmt = new TryCatchStmt(List.of(), List.of(), List.of(new ReturnStmt(null)));

        assertTrue(stmt.hasFinally());
    }

    @Test
    void hasFinally_withoutFinally_returnsFalse() {
        TryCatchStmt stmt = new TryCatchStmt(List.of(), List.of(ioExceptionCatch()), List.of());

        assertFalse(stmt.hasFinally());
    }

    @Test
    void isTryFinally_onlyCatch_returnsFalse() {
        TryCatchStmt stmt = new TryCatchStmt(List.of(), List.of(ioExceptionCatch()), List.of());

        assertFalse(stmt.isTryFinally());
    }

    @Test
    void isTryFinally_onlyFinally_returnsTrue() {
        TryCatchStmt stmt = new TryCatchStmt(List.of(), List.of(), List.of(new ReturnStmt(null)));

        assertTrue(stmt.isTryFinally());
    }

    @Test
    void isTryFinally_both_returnsFalse() {
        TryCatchStmt stmt = new TryCatchStmt(List.of(),
            List.of(ioExceptionCatch()), List.of(new ReturnStmt(null)));

        assertFalse(stmt.isTryFinally());
    }

    @Test
    void catchCount_returnsCorrectCount() {
        CatchClause clause1 = new CatchClause("e1", ClassType.of("java.io.IOException"), List.of());
        CatchClause clause2 = new CatchClause("e2", ClassType.of("java.sql.SQLException"), List.of());

        TryCatchStmt stmt = new TryCatchStmt(List.of(), List.of(clause1, clause2), List.of());

        assertEquals(2, stmt.catchCount());
    }

    @Test
    void equals_sameComponents_areEqual() {
        TryCatchStmt a = new TryCatchStmt(List.of(), List.of(ioExceptionCatch()), List.of());
        TryCatchStmt b = new TryCatchStmt(List.of(),
            List.of(new CatchClause("e", ClassType.of("java.io.IOException"), List.of())),
            List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentCatches_areNotEqual() {
        TryCatchStmt a = new TryCatchStmt(List.of(), List.of(ioExceptionCatch()), List.of());
        TryCatchStmt b = new TryCatchStmt(List.of(),
            List.of(new CatchClause("ex", ClassType.of("java.io.IOException"), List.of())),
            List.of());

        assertNotEquals(a, b);
    }

    @Test
    void implementsStatement() {
        TryCatchStmt stmt = new TryCatchStmt(List.of(), List.of(ioExceptionCatch()), List.of());

        assertInstanceOf(Statement.class, stmt);
    }
}
