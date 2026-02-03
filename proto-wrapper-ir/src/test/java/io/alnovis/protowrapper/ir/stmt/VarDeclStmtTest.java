package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.expr.LiteralExpr;
import io.alnovis.protowrapper.ir.expr.VarRefExpr;
import io.alnovis.protowrapper.ir.type.ClassType;
import io.alnovis.protowrapper.ir.type.PrimitiveType;
import io.alnovis.protowrapper.ir.type.PrimitiveType.PrimitiveKind;
import io.alnovis.protowrapper.ir.type.TypeRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link VarDeclStmt}.
 */
class VarDeclStmtTest {

    @Test
    void constructor_withInitializer_succeeds() {
        TypeRef intType = PrimitiveType.of(PrimitiveKind.INT);
        LiteralExpr init = new LiteralExpr(0, intType);
        VarDeclStmt stmt = new VarDeclStmt("count", intType, init, false);

        assertEquals("count", stmt.name());
        assertEquals(intType, stmt.type());
        assertEquals(init, stmt.initializer());
        assertFalse(stmt.isFinal());
    }

    @Test
    void constructor_withoutInitializer_succeeds() {
        TypeRef stringType = ClassType.of("java.lang.String");
        VarDeclStmt stmt = new VarDeclStmt("name", stringType, null, false);

        assertEquals("name", stmt.name());
        assertEquals(stringType, stmt.type());
        assertNull(stmt.initializer());
    }

    @Test
    void constructor_final_succeeds() {
        TypeRef stringType = ClassType.of("java.lang.String");
        VarDeclStmt stmt = new VarDeclStmt("CONSTANT", stringType, new LiteralExpr("value", stringType), true);

        assertTrue(stmt.isFinal());
    }

    @Test
    void constructor_nullName_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new VarDeclStmt(null, PrimitiveType.of(PrimitiveKind.INT), null, false));
    }

    @Test
    void constructor_emptyName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new VarDeclStmt("", PrimitiveType.of(PrimitiveKind.INT), null, false));
    }

    @Test
    void constructor_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new VarDeclStmt("   ", PrimitiveType.of(PrimitiveKind.INT), null, false));
    }

    @Test
    void constructor_nullType_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new VarDeclStmt("x", null, null, false));
    }

    @Test
    void hasInitializer_withInitializer_returnsTrue() {
        VarDeclStmt stmt = new VarDeclStmt("x", PrimitiveType.of(PrimitiveKind.INT),
            new LiteralExpr(0, null), false);

        assertTrue(stmt.hasInitializer());
    }

    @Test
    void hasInitializer_withoutInitializer_returnsFalse() {
        VarDeclStmt stmt = new VarDeclStmt("x", PrimitiveType.of(PrimitiveKind.INT), null, false);

        assertFalse(stmt.hasInitializer());
    }

    @Test
    void equals_sameComponents_areEqual() {
        TypeRef type = PrimitiveType.of(PrimitiveKind.INT);
        VarDeclStmt a = new VarDeclStmt("x", type, null, false);
        VarDeclStmt b = new VarDeclStmt("x", type, null, false);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentName_areNotEqual() {
        TypeRef type = PrimitiveType.of(PrimitiveKind.INT);
        VarDeclStmt a = new VarDeclStmt("x", type, null, false);
        VarDeclStmt b = new VarDeclStmt("y", type, null, false);

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentFinal_areNotEqual() {
        TypeRef type = PrimitiveType.of(PrimitiveKind.INT);
        VarDeclStmt a = new VarDeclStmt("x", type, null, false);
        VarDeclStmt b = new VarDeclStmt("x", type, null, true);

        assertNotEquals(a, b);
    }

    @Test
    void implementsStatement() {
        VarDeclStmt stmt = new VarDeclStmt("x", PrimitiveType.of(PrimitiveKind.INT), null, false);

        assertInstanceOf(Statement.class, stmt);
    }
}
