package io.alnovis.protowrapper.ir.stmt;

import io.alnovis.protowrapper.ir.type.ClassType;
import io.alnovis.protowrapper.ir.type.TypeRef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CatchClause}.
 */
class CatchClauseTest {

    @Test
    void constructor_validArguments_succeeds() {
        TypeRef exceptionType = ClassType.of("java.io.IOException");
        List<Statement> body = List.of(new ReturnStmt(null));

        CatchClause clause = new CatchClause("e", exceptionType, body);

        assertEquals("e", clause.variableName());
        assertEquals(exceptionType, clause.exceptionType());
        assertEquals(1, clause.body().size());
    }

    @Test
    void constructor_nullVariableName_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new CatchClause(null, ClassType.of("java.lang.Exception"), List.of()));
    }

    @Test
    void constructor_emptyVariableName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new CatchClause("", ClassType.of("java.lang.Exception"), List.of()));
    }

    @Test
    void constructor_blankVariableName_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> new CatchClause("   ", ClassType.of("java.lang.Exception"), List.of()));
    }

    @Test
    void constructor_nullExceptionType_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new CatchClause("e", null, List.of()));
    }

    @Test
    void constructor_nullBody_treatedAsEmpty() {
        CatchClause clause = new CatchClause("e", ClassType.of("java.lang.Exception"), null);

        assertNotNull(clause.body());
        assertTrue(clause.body().isEmpty());
        assertTrue(clause.hasEmptyBody());
    }

    @Test
    void body_isImmutable() {
        List<Statement> body = new ArrayList<>();
        body.add(new ReturnStmt(null));
        CatchClause clause = new CatchClause("e", ClassType.of("java.lang.Exception"), body);

        assertThrows(UnsupportedOperationException.class,
            () -> clause.body().add(new ReturnStmt(null)));
    }

    @Test
    void hasEmptyBody_emptyBody_returnsTrue() {
        CatchClause clause = new CatchClause("e", ClassType.of("java.lang.Exception"), List.of());

        assertTrue(clause.hasEmptyBody());
    }

    @Test
    void hasEmptyBody_nonEmptyBody_returnsFalse() {
        CatchClause clause = new CatchClause("e", ClassType.of("java.lang.Exception"),
            List.of(new ReturnStmt(null)));

        assertFalse(clause.hasEmptyBody());
    }

    @Test
    void equals_sameComponents_areEqual() {
        TypeRef type = ClassType.of("java.io.IOException");
        CatchClause a = new CatchClause("e", type, List.of());
        CatchClause b = new CatchClause("e", ClassType.of("java.io.IOException"), List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentVariableName_areNotEqual() {
        TypeRef type = ClassType.of("java.io.IOException");
        CatchClause a = new CatchClause("e", type, List.of());
        CatchClause b = new CatchClause("ex", type, List.of());

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentExceptionType_areNotEqual() {
        CatchClause a = new CatchClause("e", ClassType.of("java.io.IOException"), List.of());
        CatchClause b = new CatchClause("e", ClassType.of("java.sql.SQLException"), List.of());

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentBody_areNotEqual() {
        TypeRef type = ClassType.of("java.io.IOException");
        CatchClause a = new CatchClause("e", type, List.of());
        CatchClause b = new CatchClause("e", type, List.of(new ReturnStmt(null)));

        assertNotEquals(a, b);
    }
}
