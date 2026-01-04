package space.alnovis.protowrapper.diff.model;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import space.alnovis.protowrapper.model.FieldInfo;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a change to a field between two schema versions.
 *
 * @param fieldNumber The field number (consistent across versions if renamed)
 * @param fieldName   The field name (from v2 if modified, v1 if removed)
 * @param changeType  The type of change detected
 * @param v1Field     The field in source version (null if added)
 * @param v2Field     The field in target version (null if removed)
 * @param changes     Human-readable list of specific changes
 */
public record FieldChange(
    int fieldNumber,
    String fieldName,
    ChangeType changeType,
    FieldInfo v1Field,
    FieldInfo v2Field,
    List<String> changes
) {

    // Compatible widening conversions (source type -> allowed target types)
    private static final Set<Type> INT32_COMPATIBLE = Set.of(
        Type.TYPE_INT32, Type.TYPE_INT64, Type.TYPE_SINT32, Type.TYPE_SINT64
    );

    private static final Set<Type> INT64_COMPATIBLE = Set.of(
        Type.TYPE_INT64, Type.TYPE_SINT64
    );

    private static final Set<Type> UINT32_COMPATIBLE = Set.of(
        Type.TYPE_UINT32, Type.TYPE_UINT64, Type.TYPE_INT64
    );

    private static final Set<Type> FLOAT_COMPATIBLE = Set.of(
        Type.TYPE_FLOAT, Type.TYPE_DOUBLE
    );

    /**
     * Returns true if this field change is a breaking change.
     */
    public boolean isBreaking() {
        return switch (changeType) {
            case REMOVED, NUMBER_CHANGED -> true;
            case TYPE_CHANGED -> !isCompatibleTypeChange();
            case LABEL_CHANGED -> isBreakingLabelChange();
            default -> false;
        };
    }

    /**
     * Returns true if this is a compatible type change (widening conversion).
     */
    public boolean isCompatibleTypeChange() {
        if (v1Field == null || v2Field == null) {
            return false;
        }

        Type t1 = v1Field.getType();
        Type t2 = v2Field.getType();

        if (t1 == t2) {
            // Same type but maybe different type name (enum/message)
            return Objects.equals(v1Field.getTypeName(), v2Field.getTypeName());
        }

        // Check for compatible numeric conversions
        return isCompatibleNumericConversion(t1, t2) ||
               isIntEnumCompatible(t1, t2);
    }

    /**
     * Check if numeric conversion is compatible (widening).
     */
    private boolean isCompatibleNumericConversion(Type from, Type to) {
        // int32 can widen to int64
        if (from == Type.TYPE_INT32 && INT32_COMPATIBLE.contains(to)) {
            return true;
        }
        if (from == Type.TYPE_SINT32 && INT32_COMPATIBLE.contains(to)) {
            return true;
        }

        // uint32 can widen to uint64 or int64
        if (from == Type.TYPE_UINT32 && UINT32_COMPATIBLE.contains(to)) {
            return true;
        }

        // float can widen to double
        if (from == Type.TYPE_FLOAT && FLOAT_COMPATIBLE.contains(to)) {
            return true;
        }

        // fixed32 can widen to fixed64
        if (from == Type.TYPE_FIXED32 && to == Type.TYPE_FIXED64) {
            return true;
        }
        if (from == Type.TYPE_SFIXED32 && to == Type.TYPE_SFIXED64) {
            return true;
        }

        return false;
    }

    /**
     * Check if int <-> enum conversion is compatible.
     * This is a common pattern and generally safe.
     */
    private boolean isIntEnumCompatible(Type t1, Type t2) {
        boolean t1IsInt = t1 == Type.TYPE_INT32 || t1 == Type.TYPE_INT64;
        boolean t2IsInt = t2 == Type.TYPE_INT32 || t2 == Type.TYPE_INT64;
        boolean t1IsEnum = t1 == Type.TYPE_ENUM;
        boolean t2IsEnum = t2 == Type.TYPE_ENUM;

        return (t1IsInt && t2IsEnum) || (t1IsEnum && t2IsInt);
    }

    /**
     * Check if label change is breaking.
     */
    private boolean isBreakingLabelChange() {
        if (v1Field == null || v2Field == null) {
            return false;
        }

        // repeated <-> singular is breaking
        if (v1Field.isRepeated() != v2Field.isRepeated()) {
            return true;
        }

        // optional -> required is breaking (for existing messages without the field)
        // But proto3 doesn't have required, so this is mainly proto2 concern
        return false;
    }

    /**
     * Returns a compatibility description if this is a type change.
     */
    public String getCompatibilityNote() {
        if (changeType != ChangeType.TYPE_CHANGED) {
            return null;
        }

        if (isCompatibleTypeChange()) {
            if (isIntEnumCompatible(v1Field.getType(), v2Field.getType())) {
                return "Compatible via INT_ENUM conversion";
            }
            return "Compatible widening conversion";
        }

        return "Incompatible type change";
    }

    /**
     * Format field type for display.
     */
    public static String formatType(FieldInfo field) {
        if (field == null) {
            return "null";
        }

        String typeName = field.getTypeName();
        if (typeName != null && !typeName.isEmpty()) {
            // Extract simple name from full path
            int lastDot = typeName.lastIndexOf('.');
            return lastDot >= 0 ? typeName.substring(lastDot + 1) : typeName;
        }

        return switch (field.getType()) {
            case TYPE_DOUBLE -> "double";
            case TYPE_FLOAT -> "float";
            case TYPE_INT64 -> "int64";
            case TYPE_UINT64 -> "uint64";
            case TYPE_INT32 -> "int32";
            case TYPE_FIXED64 -> "fixed64";
            case TYPE_FIXED32 -> "fixed32";
            case TYPE_BOOL -> "bool";
            case TYPE_STRING -> "string";
            case TYPE_BYTES -> "bytes";
            case TYPE_UINT32 -> "uint32";
            case TYPE_SFIXED32 -> "sfixed32";
            case TYPE_SFIXED64 -> "sfixed64";
            case TYPE_SINT32 -> "sint32";
            case TYPE_SINT64 -> "sint64";
            default -> field.getType().name();
        };
    }

    /**
     * Returns a summary of the change for display.
     */
    public String getSummary() {
        return switch (changeType) {
            case ADDED -> "Added field: " + fieldName + " (" + formatType(v2Field) + ", #" + fieldNumber + ")";
            case REMOVED -> "Removed field: " + fieldName + " (#" + fieldNumber + ")";
            case TYPE_CHANGED -> "Type changed: " + formatType(v1Field) + " -> " + formatType(v2Field);
            case LABEL_CHANGED -> "Label changed: " + (v1Field.isRepeated() ? "repeated" : "singular") +
                                  " -> " + (v2Field.isRepeated() ? "repeated" : "singular");
            case NAME_CHANGED -> "Renamed: " + v1Field.getProtoName() + " -> " + v2Field.getProtoName();
            default -> String.join("; ", changes);
        };
    }
}
