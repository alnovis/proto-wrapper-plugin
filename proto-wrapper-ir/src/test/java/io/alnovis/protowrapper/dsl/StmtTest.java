package io.alnovis.protowrapper.dsl;

import io.alnovis.protowrapper.ir.stmt.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Stmt}.
 */
class StmtTest {

    // ========================================================================
    // Return Statements
    // ========================================================================

    @Test
    void return_void_createsVoidReturn() {
        ReturnStmt stmt = Stmt.return_();

        assertFalse(stmt.hasValue());
        assertNull(stmt.value());
    }

    @Test
    void return_value_createsReturnWithValue() {
        ReturnStmt stmt = Stmt.return_(Expr.literal(42));

        assertTrue(stmt.hasValue());
        assertNotNull(stmt.value());
    }

    // ========================================================================
    // Variable Declarations
    // ========================================================================

    @Test
    void var_withInit_createsVarDecl() {
        VarDeclStmt stmt = Stmt.var(Types.INT, "count", Expr.literal(0));

        assertEquals("count", stmt.name());
        assertEquals(Types.INT, stmt.type());
        assertTrue(stmt.hasInitializer());
        assertFalse(stmt.isFinal());
    }

    @Test
    void var_withoutInit_createsVarDecl() {
        VarDeclStmt stmt = Stmt.var(Types.STRING, "name");

        assertEquals("name", stmt.name());
        assertFalse(stmt.hasInitializer());
    }

    @Test
    void finalVar_createsFinalVarDecl() {
        VarDeclStmt stmt = Stmt.finalVar(Types.STRING, "name", Expr.literal("John"));

        assertTrue(stmt.isFinal());
        assertTrue(stmt.hasInitializer());
    }

    // ========================================================================
    // Assignment Statements
    // ========================================================================

    @Test
    void assign_createsAssignStmt() {
        AssignStmt stmt = Stmt.assign(Expr.var("x"), Expr.literal(10));

        assertTrue(stmt.isVariableAssignment());
    }

    @Test
    void assign_field_createsFieldAssign() {
        AssignStmt stmt = Stmt.assign(Expr.field("name"), Expr.var("newName"));

        assertTrue(stmt.isFieldAssignment());
    }

    // ========================================================================
    // Expression Statements
    // ========================================================================

    @Test
    void expr_createsExprStmt() {
        ExpressionStmt stmt = Stmt.expr(Expr.call("doSomething"));

        assertTrue(stmt.isMethodCall());
    }

    // ========================================================================
    // If Statements
    // ========================================================================

    @Test
    void if_singleStatement_createsIfStmt() {
        IfStmt stmt = Stmt.if_(Expr.var("cond"), Stmt.return_());

        assertEquals(1, stmt.thenBranch().size());
        assertTrue(stmt.hasNoElse());
    }

    @Test
    void if_withElse_createsIfElseStmt() {
        IfStmt stmt = Stmt.if_(Expr.var("cond"),
            Stmt.return_(Expr.literal(true)),
            Stmt.return_(Expr.literal(false)));

        assertTrue(stmt.hasElse());
    }

    @Test
    void if_withLists_createsIfStmt() {
        IfStmt stmt = Stmt.if_(
            Expr.var("cond"),
            List.of(Stmt.return_()),
            List.of(Stmt.return_())
        );

        assertEquals(1, stmt.thenBranch().size());
        assertEquals(1, stmt.elseBranch().size());
    }

    // ========================================================================
    // Loop Statements
    // ========================================================================

    @Test
    void forEach_singleStatement_createsForEach() {
        ForEachStmt stmt = Stmt.forEach(Types.STRING, "item", Expr.var("items"),
            Stmt.expr(Expr.call("process", Expr.var("item"))));

        assertEquals("item", stmt.variableName());
        assertEquals(Types.STRING, stmt.variableType());
        assertEquals(1, stmt.body().size());
    }

    @Test
    void forEach_withList_createsForEach() {
        ForEachStmt stmt = Stmt.forEach(Types.INT, "num", Expr.var("numbers"),
            List.of(Stmt.return_()));

        assertEquals(1, stmt.body().size());
    }

    @Test
    void while_singleStatement_createsWhile() {
        WhileStmt stmt = Stmt.while_(Expr.call("hasNext"), Stmt.return_());

        assertEquals(1, stmt.body().size());
    }

    @Test
    void while_withList_createsWhile() {
        WhileStmt stmt = Stmt.while_(Expr.var("running"),
            List.of(Stmt.return_()));

        assertEquals(1, stmt.body().size());
    }

    @Test
    void for_createsForStmt() {
        ForStmt stmt = Stmt.for_(
            Stmt.var(Types.INT, "i", Expr.literal(0)),
            Expr.lt(Expr.var("i"), Expr.literal(10)),
            Expr.var("i"),
            Stmt.return_()
        );

        assertTrue(stmt.hasInitialization());
        assertTrue(stmt.hasCondition());
        assertTrue(stmt.hasUpdate());
    }

    @Test
    void for_withList_createsForStmt() {
        ForStmt stmt = Stmt.for_(
            null, Expr.literal(true), null,
            List.of(Stmt.break_())
        );

        assertFalse(stmt.hasInitialization());
        assertFalse(stmt.hasUpdate());
    }

    @Test
    void forEver_createsInfiniteLoop() {
        ForStmt stmt = Stmt.forEver(Stmt.break_());

        assertTrue(stmt.isInfinite());
        assertFalse(stmt.hasInitialization());
        assertFalse(stmt.hasUpdate());
    }

    // ========================================================================
    // Exception Handling
    // ========================================================================

    @Test
    void throw_createsThrowStmt() {
        ThrowStmt stmt = Stmt.throw_(
            Expr.new_(Types.type("java.lang.RuntimeException")));

        assertTrue(stmt.throwsNewException());
    }

    @Test
    void tryCatch_withCatches_createsTryCatch() {
        CatchClause clause = new CatchClause("e",
            Types.type("java.io.IOException"), List.of(Stmt.return_()));
        TryCatchStmt stmt = Stmt.tryCatch(List.of(Stmt.return_()), List.of(clause));

        assertTrue(stmt.hasCatch());
        assertFalse(stmt.hasFinally());
    }

    @Test
    void tryCatch_withFinally_createsTryCatchFinally() {
        CatchClause clause = new CatchClause("e",
            Types.type("Exception"), List.of());
        TryCatchStmt stmt = Stmt.tryCatch(
            List.of(Stmt.return_()),
            List.of(clause),
            List.of(Stmt.return_())
        );

        assertTrue(stmt.hasCatch());
        assertTrue(stmt.hasFinally());
    }

    @Test
    void tryFinally_createsTryFinally() {
        TryCatchStmt stmt = Stmt.tryFinally(
            List.of(Stmt.return_()),
            List.of(Stmt.return_())
        );

        assertTrue(stmt.isTryFinally());
        assertFalse(stmt.hasCatch());
    }

    @Test
    void tryCatch_simple_createsSimpleTryCatch() {
        TryCatchStmt stmt = Stmt.tryCatch(
            Stmt.return_(),
            Types.type("Exception"), "e",
            Stmt.return_()
        );

        assertEquals(1, stmt.catchCount());
    }

    // ========================================================================
    // Control Flow
    // ========================================================================

    @Test
    void break_createsBreakStmt() {
        BreakStmt stmt = Stmt.break_();

        assertSame(BreakStmt.INSTANCE, stmt);
        assertTrue(stmt.isUnlabeled());
    }

    @Test
    void break_labeled_createsLabeledBreak() {
        BreakStmt stmt = Stmt.break_("outer");

        assertTrue(stmt.isLabeled());
        assertEquals("outer", stmt.label());
    }

    @Test
    void continue_createsContinueStmt() {
        ContinueStmt stmt = Stmt.continue_();

        assertSame(ContinueStmt.INSTANCE, stmt);
        assertTrue(stmt.isUnlabeled());
    }

    @Test
    void continue_labeled_createsLabeledContinue() {
        ContinueStmt stmt = Stmt.continue_("outer");

        assertTrue(stmt.isLabeled());
        assertEquals("outer", stmt.label());
    }

    // ========================================================================
    // Block Statements
    // ========================================================================

    @Test
    void block_varargs_createsBlock() {
        BlockStmt stmt = Stmt.block(
            Stmt.var(Types.INT, "x", Expr.literal(1)),
            Stmt.return_(Expr.var("x"))
        );

        assertEquals(2, stmt.size());
    }

    @Test
    void block_list_createsBlock() {
        BlockStmt stmt = Stmt.block(List.of(Stmt.return_()));

        assertEquals(1, stmt.size());
    }

    @Test
    void emptyBlock_createsEmptyBlock() {
        BlockStmt stmt = Stmt.emptyBlock();

        assertTrue(stmt.isEmpty());
    }

    // ========================================================================
    // Convenience Methods
    // ========================================================================

    @Test
    void requireNonNull_createsNullCheck() {
        IfStmt stmt = Stmt.requireNonNull(Expr.var("name"), "name");

        assertFalse(stmt.hasElse());
        assertEquals(1, stmt.thenBranch().size());
        assertInstanceOf(ThrowStmt.class, stmt.thenBranch().get(0));
    }

    @Test
    void call_thisMethod_createsCallStmt() {
        ExpressionStmt stmt = Stmt.call("doSomething", Expr.var("x"));

        assertTrue(stmt.isMethodCall());
    }

    @Test
    void call_targetMethod_createsCallStmt() {
        ExpressionStmt stmt = Stmt.call(Expr.var("list"), "add", Expr.var("item"));

        assertTrue(stmt.isMethodCall());
    }
}
