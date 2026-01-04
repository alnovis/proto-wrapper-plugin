package space.alnovis.protowrapper.diff.model;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import space.alnovis.protowrapper.model.FieldInfo;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FieldChange record.
 */
class FieldChangeTest {

    @Test
    void isBreaking_returnsTrue_forRemovedField() {
        FieldInfo field = createField("old_field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "old_field", ChangeType.REMOVED, field, null, List.of()
        );

        assertTrue(change.isBreaking());
    }

    @Test
    void isBreaking_returnsTrue_forNumberChanged() {
        FieldInfo v1 = createField("field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("field", 2, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.NUMBER_CHANGED, v1, v2, List.of()
        );

        assertTrue(change.isBreaking());
    }

    @Test
    void isBreaking_returnsFalse_forAddedField() {
        FieldInfo field = createField("new_field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "new_field", ChangeType.ADDED, null, field, List.of()
        );

        assertFalse(change.isBreaking());
    }

    @Test
    void isBreaking_returnsFalse_forCompatibleTypeChange() {
        FieldInfo v1 = createField("field", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("field", 1, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.TYPE_CHANGED, v1, v2, List.of()
        );

        assertFalse(change.isBreaking());
    }

    @Test
    void isBreaking_returnsTrue_forIncompatibleTypeChange() {
        FieldInfo v1 = createField("field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("field", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.TYPE_CHANGED, v1, v2, List.of()
        );

        assertTrue(change.isBreaking());
    }

    @Test
    void isBreaking_returnsTrue_forRepeatedToSingularChange() {
        FieldInfo v1 = createField("field", 1, Type.TYPE_STRING, Label.LABEL_REPEATED);
        FieldInfo v2 = createField("field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.LABEL_CHANGED, v1, v2, List.of()
        );

        assertTrue(change.isBreaking());
    }

    @Test
    void isBreaking_returnsFalse_forNameChange() {
        FieldInfo v1 = createField("old_name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("new_name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "new_name", ChangeType.NAME_CHANGED, v1, v2, List.of()
        );

        assertFalse(change.isBreaking());
    }

    @ParameterizedTest
    @MethodSource("compatibleTypeConversions")
    void isCompatibleTypeChange_returnsTrue_forWideningConversions(Type from, Type to) {
        FieldInfo v1 = createField("field", 1, from, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("field", 1, to, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.TYPE_CHANGED, v1, v2, List.of()
        );

        assertTrue(change.isCompatibleTypeChange());
    }

    static Stream<Arguments> compatibleTypeConversions() {
        return Stream.of(
            // int32 widening
            Arguments.of(Type.TYPE_INT32, Type.TYPE_INT64),
            Arguments.of(Type.TYPE_INT32, Type.TYPE_SINT64),
            Arguments.of(Type.TYPE_SINT32, Type.TYPE_INT64),
            Arguments.of(Type.TYPE_SINT32, Type.TYPE_SINT64),
            // uint32 widening
            Arguments.of(Type.TYPE_UINT32, Type.TYPE_UINT64),
            Arguments.of(Type.TYPE_UINT32, Type.TYPE_INT64),
            // float widening
            Arguments.of(Type.TYPE_FLOAT, Type.TYPE_DOUBLE),
            // fixed widening
            Arguments.of(Type.TYPE_FIXED32, Type.TYPE_FIXED64),
            Arguments.of(Type.TYPE_SFIXED32, Type.TYPE_SFIXED64),
            // int-enum compatibility
            Arguments.of(Type.TYPE_INT32, Type.TYPE_ENUM),
            Arguments.of(Type.TYPE_ENUM, Type.TYPE_INT32),
            Arguments.of(Type.TYPE_INT64, Type.TYPE_ENUM),
            Arguments.of(Type.TYPE_ENUM, Type.TYPE_INT64)
        );
    }

    @Test
    void isCompatibleTypeChange_returnsFalse_forIncompatibleTypes() {
        FieldInfo v1 = createField("field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("field", 1, Type.TYPE_BOOL, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.TYPE_CHANGED, v1, v2, List.of()
        );

        assertFalse(change.isCompatibleTypeChange());
    }

    @Test
    void isCompatibleTypeChange_returnsFalse_whenV1IsNull() {
        FieldInfo v2 = createField("field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.ADDED, null, v2, List.of()
        );

        assertFalse(change.isCompatibleTypeChange());
    }

    @Test
    void isCompatibleTypeChange_returnsFalse_whenV2IsNull() {
        FieldInfo v1 = createField("field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.REMOVED, v1, null, List.of()
        );

        assertFalse(change.isCompatibleTypeChange());
    }

    @Test
    void getCompatibilityNote_returnsNull_forNonTypeChange() {
        FieldInfo field = createField("field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.ADDED, null, field, List.of()
        );

        assertNull(change.getCompatibilityNote());
    }

    @Test
    void getCompatibilityNote_returnsPluginWider_forWideningChange() {
        FieldInfo v1 = createField("field", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("field", 1, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.TYPE_CHANGED, v1, v2, List.of()
        );

        assertEquals("Plugin uses wider type (long)", change.getCompatibilityNote());
    }

    @Test
    void getCompatibilityNote_returnsPluginIntEnum_forIntEnumChange() {
        FieldInfo v1 = createField("field", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("field", 1, Type.TYPE_ENUM, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.TYPE_CHANGED, v1, v2, List.of()
        );

        assertEquals("Plugin uses int type with enum helper methods", change.getCompatibilityNote());
    }

    @Test
    void getCompatibilityNote_returnsManualConversion_forStringBytesChange() {
        FieldInfo v1 = createField("field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("field", 1, Type.TYPE_BYTES, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.TYPE_CHANGED, v1, v2, List.of()
        );

        assertEquals("Requires getBytes()/new String() conversion", change.getCompatibilityNote());
    }

    @Test
    void formatType_returnsNull_forNullField() {
        assertEquals("null", FieldChange.formatType(null));
    }

    @Test
    void formatType_returnsTypeName_forScalarTypes() {
        assertEquals("string", FieldChange.formatType(
            createField("f", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL)));
        assertEquals("int32", FieldChange.formatType(
            createField("f", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL)));
        assertEquals("int64", FieldChange.formatType(
            createField("f", 1, Type.TYPE_INT64, Label.LABEL_OPTIONAL)));
        assertEquals("bool", FieldChange.formatType(
            createField("f", 1, Type.TYPE_BOOL, Label.LABEL_OPTIONAL)));
        assertEquals("double", FieldChange.formatType(
            createField("f", 1, Type.TYPE_DOUBLE, Label.LABEL_OPTIONAL)));
        assertEquals("float", FieldChange.formatType(
            createField("f", 1, Type.TYPE_FLOAT, Label.LABEL_OPTIONAL)));
        assertEquals("bytes", FieldChange.formatType(
            createField("f", 1, Type.TYPE_BYTES, Label.LABEL_OPTIONAL)));
    }

    @Test
    void formatType_extractsSimpleName_fromFullTypeName() {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
            .setName("field")
            .setNumber(1)
            .setType(Type.TYPE_MESSAGE)
            .setTypeName(".com.example.User")
            .setLabel(Label.LABEL_OPTIONAL)
            .build();
        FieldInfo field = new FieldInfo(proto);

        assertEquals("User", FieldChange.formatType(field));
    }

    @Test
    void getSummary_formatsAddedField() {
        FieldInfo field = createField("new_field", 5, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            5, "new_field", ChangeType.ADDED, null, field, List.of()
        );

        String summary = change.getSummary();

        assertTrue(summary.contains("Added field"));
        assertTrue(summary.contains("new_field"));
        assertTrue(summary.contains("string"));
        assertTrue(summary.contains("#5"));
    }

    @Test
    void getSummary_formatsRemovedField() {
        FieldInfo field = createField("old_field", 3, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            3, "old_field", ChangeType.REMOVED, field, null, List.of()
        );

        String summary = change.getSummary();

        assertTrue(summary.contains("Removed field"));
        assertTrue(summary.contains("old_field"));
        assertTrue(summary.contains("#3"));
    }

    @Test
    void getSummary_formatsTypeChanged() {
        FieldInfo v1 = createField("field", 1, Type.TYPE_INT32, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("field", 1, Type.TYPE_INT64, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.TYPE_CHANGED, v1, v2, List.of()
        );

        String summary = change.getSummary();

        assertTrue(summary.contains("Type changed"));
        assertTrue(summary.contains("int32"));
        assertTrue(summary.contains("int64"));
    }

    @Test
    void getSummary_formatsLabelChanged() {
        FieldInfo v1 = createField("field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("field", 1, Type.TYPE_STRING, Label.LABEL_REPEATED);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.LABEL_CHANGED, v1, v2, List.of()
        );

        String summary = change.getSummary();

        assertTrue(summary.contains("Label changed"));
        assertTrue(summary.contains("singular"));
        assertTrue(summary.contains("repeated"));
    }

    @Test
    void getSummary_formatsNameChanged() {
        FieldInfo v1 = createField("old_name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("new_name", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "new_name", ChangeType.NAME_CHANGED, v1, v2, List.of()
        );

        String summary = change.getSummary();

        assertTrue(summary.contains("Renamed"));
        assertTrue(summary.contains("old_name"));
        assertTrue(summary.contains("new_name"));
    }

    @Test
    void getSummary_joinsCustomChanges_forDefaultCase() {
        FieldInfo v1 = createField("field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("field", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldChange change = new FieldChange(
            1, "field", ChangeType.MODIFIED, v1, v2,
            List.of("default changed", "options modified")
        );

        String summary = change.getSummary();

        assertTrue(summary.contains("default changed"));
        assertTrue(summary.contains("options modified"));
    }

    @Test
    void recordComponentsAreAccessible() {
        FieldInfo v1 = createField("test", 1, Type.TYPE_STRING, Label.LABEL_OPTIONAL);
        FieldInfo v2 = createField("test", 1, Type.TYPE_STRING, Label.LABEL_REPEATED);
        List<String> changes = List.of("label changed");

        FieldChange change = new FieldChange(1, "test", ChangeType.LABEL_CHANGED, v1, v2, changes);

        assertEquals(1, change.fieldNumber());
        assertEquals("test", change.fieldName());
        assertEquals(ChangeType.LABEL_CHANGED, change.changeType());
        assertEquals(v1, change.v1Field());
        assertEquals(v2, change.v2Field());
        assertEquals(changes, change.changes());
    }

    // Helper method
    private FieldInfo createField(String name, int number, Type type, Label label) {
        FieldDescriptorProto proto = FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(number)
            .setType(type)
            .setLabel(label)
            .build();
        return new FieldInfo(proto);
    }
}
