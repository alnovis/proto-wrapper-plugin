package io.alnovis.protowrapper.ir.stmt;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BlockStmt}.
 */
class BlockStmtTest {

    @Test
    void constructor_withStatements_succeeds() {
        List<Statement> stmts = List.of(new ReturnStmt(null), new ReturnStmt(null));
        BlockStmt block = new BlockStmt(stmts);

        assertEquals(2, block.statements().size());
        assertEquals(2, block.size());
    }

    @Test
    void constructor_nullList_treatedAsEmpty() {
        BlockStmt block = new BlockStmt(null);

        assertNotNull(block.statements());
        assertTrue(block.statements().isEmpty());
        assertTrue(block.isEmpty());
    }

    @Test
    void constructor_emptyList_createsEmptyBlock() {
        BlockStmt block = new BlockStmt(List.of());

        assertTrue(block.isEmpty());
        assertEquals(0, block.size());
    }

    @Test
    void statements_isImmutable() {
        List<Statement> stmts = new ArrayList<>();
        stmts.add(new ReturnStmt(null));
        BlockStmt block = new BlockStmt(stmts);

        assertThrows(UnsupportedOperationException.class,
            () -> block.statements().add(new ReturnStmt(null)));
    }

    @Test
    void empty_createsEmptyBlock() {
        BlockStmt block = BlockStmt.empty();

        assertTrue(block.isEmpty());
        assertEquals(0, block.size());
    }

    @Test
    void of_varargs_createsBlock() {
        BlockStmt block = BlockStmt.of(
            new ReturnStmt(null),
            new ReturnStmt(null)
        );

        assertEquals(2, block.size());
        assertFalse(block.isEmpty());
    }

    @Test
    void of_emptyVarargs_createsEmptyBlock() {
        BlockStmt block = BlockStmt.of();

        assertTrue(block.isEmpty());
    }

    @Test
    void isEmpty_emptyBlock_returnsTrue() {
        BlockStmt block = new BlockStmt(List.of());

        assertTrue(block.isEmpty());
    }

    @Test
    void isEmpty_nonEmptyBlock_returnsFalse() {
        BlockStmt block = new BlockStmt(List.of(new ReturnStmt(null)));

        assertFalse(block.isEmpty());
    }

    @Test
    void size_returnsCorrectCount() {
        BlockStmt block = new BlockStmt(List.of(
            new ReturnStmt(null),
            new ReturnStmt(null),
            new ReturnStmt(null)
        ));

        assertEquals(3, block.size());
    }

    @Test
    void equals_sameStatements_areEqual() {
        BlockStmt a = new BlockStmt(List.of(new ReturnStmt(null)));
        BlockStmt b = new BlockStmt(List.of(new ReturnStmt(null)));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentStatements_areNotEqual() {
        BlockStmt a = new BlockStmt(List.of(new ReturnStmt(null)));
        BlockStmt b = new BlockStmt(List.of());

        assertNotEquals(a, b);
    }

    @Test
    void equals_bothEmpty_areEqual() {
        BlockStmt a = BlockStmt.empty();
        BlockStmt b = new BlockStmt(null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void implementsStatement() {
        BlockStmt block = BlockStmt.empty();

        assertInstanceOf(Statement.class, block);
    }
}
