package io.alnovis.protowrapper.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FieldConstraints}.
 *
 * @since 2.3.0
 */
@DisplayName("FieldConstraints")
class FieldConstraintsTest {

    @Test
    @DisplayName("none() returns empty constraints")
    void noneReturnsEmptyConstraints() {
        FieldConstraints constraints = FieldConstraints.none();

        assertFalse(constraints.notNull());
        assertFalse(constraints.valid());
        assertNull(constraints.min());
        assertNull(constraints.max());
        assertNull(constraints.pattern());
        assertNull(constraints.sizeMin());
        assertNull(constraints.sizeMax());
        assertFalse(constraints.hasAnyConstraint());
    }

    @Test
    @DisplayName("hasAnyConstraint() returns true when notNull is set")
    void hasAnyConstraintWhenNotNull() {
        FieldConstraints constraints = FieldConstraints.builder()
                .notNull(true)
                .build();

        assertTrue(constraints.hasAnyConstraint());
    }

    @Test
    @DisplayName("hasAnyConstraint() returns true when valid is set")
    void hasAnyConstraintWhenValid() {
        FieldConstraints constraints = FieldConstraints.builder()
                .valid(true)
                .build();

        assertTrue(constraints.hasAnyConstraint());
    }

    @Test
    @DisplayName("hasAnyConstraint() returns true when min is set")
    void hasAnyConstraintWhenMin() {
        FieldConstraints constraints = FieldConstraints.builder()
                .min(0L)
                .build();

        assertTrue(constraints.hasAnyConstraint());
    }

    @Test
    @DisplayName("hasAnyConstraint() returns true when max is set")
    void hasAnyConstraintWhenMax() {
        FieldConstraints constraints = FieldConstraints.builder()
                .max(100L)
                .build();

        assertTrue(constraints.hasAnyConstraint());
    }

    @Test
    @DisplayName("hasAnyConstraint() returns true when pattern is set")
    void hasAnyConstraintWhenPattern() {
        FieldConstraints constraints = FieldConstraints.builder()
                .pattern("^[a-z]+$")
                .build();

        assertTrue(constraints.hasAnyConstraint());
    }

    @Test
    @DisplayName("hasAnyConstraint() returns true when sizeMin is set")
    void hasAnyConstraintWhenSizeMin() {
        FieldConstraints constraints = FieldConstraints.builder()
                .sizeMin(1)
                .build();

        assertTrue(constraints.hasAnyConstraint());
    }

    @Test
    @DisplayName("hasAnyConstraint() returns true when sizeMax is set")
    void hasAnyConstraintWhenSizeMax() {
        FieldConstraints constraints = FieldConstraints.builder()
                .sizeMax(100)
                .build();

        assertTrue(constraints.hasAnyConstraint());
    }

    @Test
    @DisplayName("hasSizeConstraint() returns true when sizeMin or sizeMax is set")
    void hasSizeConstraint() {
        FieldConstraints withMin = FieldConstraints.builder().sizeMin(1).build();
        FieldConstraints withMax = FieldConstraints.builder().sizeMax(100).build();
        FieldConstraints withBoth = FieldConstraints.builder().sizeMin(1).sizeMax(100).build();
        FieldConstraints withNeither = FieldConstraints.none();

        assertTrue(withMin.hasSizeConstraint());
        assertTrue(withMax.hasSizeConstraint());
        assertTrue(withBoth.hasSizeConstraint());
        assertFalse(withNeither.hasSizeConstraint());
    }

    @Test
    @DisplayName("hasRangeConstraint() returns true when min or max is set")
    void hasRangeConstraint() {
        FieldConstraints withMin = FieldConstraints.builder().min(0L).build();
        FieldConstraints withMax = FieldConstraints.builder().max(100L).build();
        FieldConstraints withBoth = FieldConstraints.builder().min(0L).max(100L).build();
        FieldConstraints withNeither = FieldConstraints.none();

        assertTrue(withMin.hasRangeConstraint());
        assertTrue(withMax.hasRangeConstraint());
        assertTrue(withBoth.hasRangeConstraint());
        assertFalse(withNeither.hasRangeConstraint());
    }

    @Test
    @DisplayName("builder creates constraints with all fields")
    void builderCreatesConstraintsWithAllFields() {
        FieldConstraints constraints = FieldConstraints.builder()
                .notNull(true)
                .valid(true)
                .min(0L)
                .max(100L)
                .pattern("^[a-z]+$")
                .sizeMin(1)
                .sizeMax(50)
                .build();

        assertTrue(constraints.notNull());
        assertTrue(constraints.valid());
        assertEquals(0L, constraints.min());
        assertEquals(100L, constraints.max());
        assertEquals("^[a-z]+$", constraints.pattern());
        assertEquals(1, constraints.sizeMin());
        assertEquals(50, constraints.sizeMax());
    }

    @Test
    @DisplayName("merge combines constraints with OR for boolean fields")
    void mergeCombinesBooleansWithOr() {
        FieldConstraints a = FieldConstraints.builder().notNull(true).build();
        FieldConstraints b = FieldConstraints.builder().valid(true).build();

        FieldConstraints merged = a.merge(b);

        assertTrue(merged.notNull());
        assertTrue(merged.valid());
    }

    @Test
    @DisplayName("merge takes larger min value")
    void mergeTakesLargerMin() {
        FieldConstraints a = FieldConstraints.builder().min(10L).build();
        FieldConstraints b = FieldConstraints.builder().min(20L).build();

        FieldConstraints merged = a.merge(b);

        assertEquals(20L, merged.min());
    }

    @Test
    @DisplayName("merge takes smaller max value")
    void mergeTakesSmallerMax() {
        FieldConstraints a = FieldConstraints.builder().max(100L).build();
        FieldConstraints b = FieldConstraints.builder().max(50L).build();

        FieldConstraints merged = a.merge(b);

        assertEquals(50L, merged.max());
    }

    @Test
    @DisplayName("merge takes larger sizeMin value")
    void mergeTakesLargerSizeMin() {
        FieldConstraints a = FieldConstraints.builder().sizeMin(5).build();
        FieldConstraints b = FieldConstraints.builder().sizeMin(10).build();

        FieldConstraints merged = a.merge(b);

        assertEquals(10, merged.sizeMin());
    }

    @Test
    @DisplayName("merge takes smaller sizeMax value")
    void mergeTakesSmallerSizeMax() {
        FieldConstraints a = FieldConstraints.builder().sizeMax(100).build();
        FieldConstraints b = FieldConstraints.builder().sizeMax(50).build();

        FieldConstraints merged = a.merge(b);

        assertEquals(50, merged.sizeMax());
    }

    @Test
    @DisplayName("merge prefers this.pattern over other.pattern")
    void mergePrefersThisPattern() {
        FieldConstraints a = FieldConstraints.builder().pattern("^[a-z]+$").build();
        FieldConstraints b = FieldConstraints.builder().pattern("^[0-9]+$").build();

        FieldConstraints merged = a.merge(b);

        assertEquals("^[a-z]+$", merged.pattern());
    }

    @Test
    @DisplayName("merge takes other.pattern when this.pattern is null")
    void mergeTakesOtherPatternWhenThisIsNull() {
        FieldConstraints a = FieldConstraints.none();
        FieldConstraints b = FieldConstraints.builder().pattern("^[0-9]+$").build();

        FieldConstraints merged = a.merge(b);

        assertEquals("^[0-9]+$", merged.pattern());
    }

    @Test
    @DisplayName("merge with null returns this")
    void mergeWithNullReturnsThis() {
        FieldConstraints a = FieldConstraints.builder().notNull(true).build();

        FieldConstraints merged = a.merge(null);

        assertSame(a, merged);
    }

    @Test
    @DisplayName("merge handles null values in numeric fields")
    void mergeHandlesNullNumericValues() {
        FieldConstraints a = FieldConstraints.builder().min(10L).build();
        FieldConstraints b = FieldConstraints.none();

        FieldConstraints merged = a.merge(b);

        assertEquals(10L, merged.min());
        assertNull(merged.max());
    }
}
