package io.alnovis.protowrapper.ir.decl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Modifier}.
 */
class ModifierTest {

    @Test
    void keyword_public_returnsCorrectKeyword() {
        assertEquals("public", Modifier.PUBLIC.keyword());
    }

    @Test
    void keyword_private_returnsCorrectKeyword() {
        assertEquals("private", Modifier.PRIVATE.keyword());
    }

    @Test
    void keyword_protected_returnsCorrectKeyword() {
        assertEquals("protected", Modifier.PROTECTED.keyword());
    }

    @Test
    void keyword_static_returnsCorrectKeyword() {
        assertEquals("static", Modifier.STATIC.keyword());
    }

    @Test
    void keyword_final_returnsCorrectKeyword() {
        assertEquals("final", Modifier.FINAL.keyword());
    }

    @Test
    void keyword_abstract_returnsCorrectKeyword() {
        assertEquals("abstract", Modifier.ABSTRACT.keyword());
    }

    @Test
    void keyword_default_returnsCorrectKeyword() {
        assertEquals("default", Modifier.DEFAULT.keyword());
    }

    @Test
    void keyword_synchronized_returnsCorrectKeyword() {
        assertEquals("synchronized", Modifier.SYNCHRONIZED.keyword());
    }

    @Test
    void keyword_volatile_returnsCorrectKeyword() {
        assertEquals("volatile", Modifier.VOLATILE.keyword());
    }

    @Test
    void keyword_transient_returnsCorrectKeyword() {
        assertEquals("transient", Modifier.TRANSIENT.keyword());
    }

    @Test
    void keyword_sealed_returnsCorrectKeyword() {
        assertEquals("sealed", Modifier.SEALED.keyword());
    }

    @Test
    void keyword_nonSealed_returnsCorrectKeyword() {
        assertEquals("non-sealed", Modifier.NON_SEALED.keyword());
    }

    @Test
    void isAccessModifier_public_returnsTrue() {
        assertTrue(Modifier.PUBLIC.isAccessModifier());
    }

    @Test
    void isAccessModifier_private_returnsTrue() {
        assertTrue(Modifier.PRIVATE.isAccessModifier());
    }

    @Test
    void isAccessModifier_protected_returnsTrue() {
        assertTrue(Modifier.PROTECTED.isAccessModifier());
    }

    @Test
    void isAccessModifier_static_returnsFalse() {
        assertFalse(Modifier.STATIC.isAccessModifier());
    }

    @Test
    void isAccessModifier_final_returnsFalse() {
        assertFalse(Modifier.FINAL.isAccessModifier());
    }

    @Test
    void isAccessModifier_abstract_returnsFalse() {
        assertFalse(Modifier.ABSTRACT.isAccessModifier());
    }

    @Test
    void enumValues_containsExpectedModifiers() {
        Modifier[] modifiers = Modifier.values();

        assertTrue(modifiers.length >= 12);
        assertNotNull(Modifier.valueOf("PUBLIC"));
        assertNotNull(Modifier.valueOf("PRIVATE"));
        assertNotNull(Modifier.valueOf("PROTECTED"));
        assertNotNull(Modifier.valueOf("STATIC"));
        assertNotNull(Modifier.valueOf("FINAL"));
        assertNotNull(Modifier.valueOf("ABSTRACT"));
    }
}
