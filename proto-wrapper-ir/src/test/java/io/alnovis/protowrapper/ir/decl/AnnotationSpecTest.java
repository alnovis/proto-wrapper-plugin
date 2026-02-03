package io.alnovis.protowrapper.ir.decl;

import io.alnovis.protowrapper.ir.type.ClassType;
import io.alnovis.protowrapper.ir.type.TypeRef;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AnnotationSpec}.
 */
class AnnotationSpecTest {

    @Test
    void constructor_validArguments_succeeds() {
        TypeRef type = ClassType.of("java.lang.Override");
        AnnotationSpec annotation = new AnnotationSpec(type, Map.of());

        assertEquals(type, annotation.type());
        assertTrue(annotation.members().isEmpty());
    }

    @Test
    void constructor_nullType_throwsException() {
        assertThrows(NullPointerException.class,
            () -> new AnnotationSpec(null, Map.of()));
    }

    @Test
    void constructor_nullMembers_treatedAsEmpty() {
        AnnotationSpec annotation = new AnnotationSpec(ClassType.of("Override"), null);

        assertNotNull(annotation.members());
        assertTrue(annotation.members().isEmpty());
    }

    @Test
    void members_isImmutable() {
        Map<String, Object> members = new HashMap<>();
        members.put("value", "test");
        AnnotationSpec annotation = new AnnotationSpec(ClassType.of("Test"), members);

        assertThrows(UnsupportedOperationException.class,
            () -> annotation.members().put("other", "value"));
    }

    @Test
    void of_type_createsMarkerAnnotation() {
        AnnotationSpec annotation = AnnotationSpec.of(ClassType.of("java.lang.Override"));

        assertTrue(annotation.isMarker());
        assertTrue(annotation.members().isEmpty());
    }

    @Test
    void of_typeAndMembers_createsAnnotation() {
        Map<String, Object> members = Map.of("value", "unchecked", "other", "option");
        AnnotationSpec annotation = AnnotationSpec.of(ClassType.of("SuppressWarnings"), members);

        assertFalse(annotation.isMarker());
        assertEquals(2, annotation.members().size());
        assertEquals("unchecked", annotation.getMember("value"));
    }

    @Test
    void withValue_createsSingleValueAnnotation() {
        AnnotationSpec annotation = AnnotationSpec.withValue(
            ClassType.of("java.lang.SuppressWarnings"), "unchecked");

        assertTrue(annotation.isSingleValue());
        assertEquals("unchecked", annotation.getMember("value"));
    }

    @Test
    void isMarker_emptyMembers_returnsTrue() {
        AnnotationSpec annotation = AnnotationSpec.of(ClassType.of("Override"));

        assertTrue(annotation.isMarker());
    }

    @Test
    void isMarker_withMembers_returnsFalse() {
        AnnotationSpec annotation = AnnotationSpec.withValue(ClassType.of("Test"), "value");

        assertFalse(annotation.isMarker());
    }

    @Test
    void isSingleValue_singleValueMember_returnsTrue() {
        AnnotationSpec annotation = AnnotationSpec.withValue(ClassType.of("Test"), "test");

        assertTrue(annotation.isSingleValue());
    }

    @Test
    void isSingleValue_multipleMembers_returnsFalse() {
        AnnotationSpec annotation = AnnotationSpec.of(ClassType.of("Test"),
            Map.of("value", "a", "other", "b"));

        assertFalse(annotation.isSingleValue());
    }

    @Test
    void isSingleValue_noMembers_returnsFalse() {
        AnnotationSpec annotation = AnnotationSpec.of(ClassType.of("Override"));

        assertFalse(annotation.isSingleValue());
    }

    @Test
    void getMember_existingMember_returnsValue() {
        AnnotationSpec annotation = AnnotationSpec.of(ClassType.of("Test"),
            Map.of("key", "value"));

        assertEquals("value", annotation.getMember("key"));
    }

    @Test
    void getMember_nonExistingMember_returnsNull() {
        AnnotationSpec annotation = AnnotationSpec.of(ClassType.of("Override"));

        assertNull(annotation.getMember("nonexistent"));
    }

    @Test
    void equals_sameComponents_areEqual() {
        TypeRef type = ClassType.of("Override");
        AnnotationSpec a = new AnnotationSpec(type, Map.of());
        AnnotationSpec b = new AnnotationSpec(type, Map.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentType_areNotEqual() {
        AnnotationSpec a = AnnotationSpec.of(ClassType.of("Override"));
        AnnotationSpec b = AnnotationSpec.of(ClassType.of("Deprecated"));

        assertNotEquals(a, b);
    }

    @Test
    void equals_differentMembers_areNotEqual() {
        TypeRef type = ClassType.of("Test");
        AnnotationSpec a = AnnotationSpec.withValue(type, "a");
        AnnotationSpec b = AnnotationSpec.withValue(type, "b");

        assertNotEquals(a, b);
    }
}
