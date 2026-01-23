package io.alnovis.protowrapper.diff.model;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import io.alnovis.protowrapper.model.FieldInfo;

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
     *
     * @return true if this is a breaking change
     */
    public boolean isBreaking() {
        return switch (changeType) {
            case REMOVED -> true;
            case NUMBER_CHANGED -> !isRenumberedByMapping();
            case TYPE_CHANGED -> !isCompatibleTypeChange();
            case LABEL_CHANGED -> isBreakingLabelChange();
            default -> false;
        };
    }

    /**
     * Returns true if this field was renumbered via a configured field mapping.
     * A mapped renumber has NUMBER_CHANGED type with both v1 and v2 fields present
     * and the same proto name (indicating it's the same logical field at different numbers).
     *
     * @return true if this is a mapped renumber (not breaking)
     */
    public boolean isRenumberedByMapping() {
        return changeType == ChangeType.NUMBER_CHANGED &&
               v1Field != null && v2Field != null &&
               v1Field.getProtoName().equals(v2Field.getProtoName());
    }

    /**
     * Returns a description of the renumbering for display purposes.
     *
     * @return formatted string like "#17 -> #15", or null if not a renumber
     */
    public String getRenumberDescription() {
        if (changeType != ChangeType.NUMBER_CHANGED || v1Field == null || v2Field == null) {
            return null;
        }
        return String.format("#%d -> #%d", v1Field.getNumber(), v2Field.getNumber());
    }

    /**
     * Returns true if this is a compatible type change (widening conversion).
     *
     * @return true if the type change is compatible
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
        return from == Type.TYPE_SFIXED32 && to == Type.TYPE_SFIXED64;
    }

    /**
     * Check if int <-> enum conversion is compatible.
     * This is a common pattern and generally safe.
     * All integer types (signed/unsigned, 32/64-bit) are compatible with enums.
     */
    private boolean isIntEnumCompatible(Type t1, Type t2) {
        boolean t1IsInt = isIntegerType(t1);
        boolean t2IsInt = isIntegerType(t2);
        boolean t1IsEnum = t1 == Type.TYPE_ENUM;
        boolean t2IsEnum = t2 == Type.TYPE_ENUM;

        return (t1IsInt && t2IsEnum) || (t1IsEnum && t2IsInt);
    }

    /**
     * Check if type is any integer type (32-bit or 64-bit, signed or unsigned).
     */
    private boolean isIntegerType(Type t) {
        return t == Type.TYPE_INT32 || t == Type.TYPE_INT64 ||
               t == Type.TYPE_UINT32 || t == Type.TYPE_UINT64 ||
               t == Type.TYPE_SINT32 || t == Type.TYPE_SINT64 ||
               t == Type.TYPE_FIXED32 || t == Type.TYPE_FIXED64 ||
               t == Type.TYPE_SFIXED32 || t == Type.TYPE_SFIXED64;
    }

    /**
     * Check if label change is breaking.
     */
    private boolean isBreakingLabelChange() {
        if (v1Field == null || v2Field == null) {
            return false;
        }

        // repeated <-> singular is breaking
        // optional -> required is breaking (for existing messages without the field)
        // But proto3 doesn't have required, so this is mainly proto2 concern
        return v1Field.isRepeated() != v2Field.isRepeated();
    }

    /**
     * Returns a compatibility description if this is a type change.
     *
     * @return the compatibility note, or null if not applicable
     */
    public String getCompatibilityNote() {
        if (changeType != ChangeType.TYPE_CHANGED && changeType != ChangeType.LABEL_CHANGED) {
            return null;
        }

        TypeConflictType conflict = getTypeConflictType();
        return conflict.getPluginNote();
    }

    /**
     * Determines the type of conflict for this field change.
     * This matches the conflict detection logic in VersionMerger.
     *
     * @return the type conflict classification
     */
    public TypeConflictType getTypeConflictType() {
        if (v1Field == null || v2Field == null) {
            return TypeConflictType.NONE;
        }

        // Check for repeated/singular conflict first
        if (v1Field.isRepeated() != v2Field.isRepeated()) {
            return TypeConflictType.REPEATED_SINGLE;
        }

        // Check for optional/required conflict
        if (v1Field.isOptional() != v2Field.isOptional()) {
            // Only return OPTIONAL_REQUIRED if there's no type change
            if (v1Field.getJavaType().equals(v2Field.getJavaType())) {
                return TypeConflictType.OPTIONAL_REQUIRED;
            }
        }

        Type t1 = v1Field.getType();
        Type t2 = v2Field.getType();

        // Same proto type - check if it's message/enum with different names
        if (t1 == t2) {
            if (t1 == Type.TYPE_ENUM && !v1Field.getJavaType().equals(v2Field.getJavaType())) {
                return TypeConflictType.ENUM_ENUM;
            }
            return TypeConflictType.NONE;
        }

        // int ↔ enum conflict
        if (isIntEnumCompatible(t1, t2)) {
            return TypeConflictType.INT_ENUM;
        }

        // Signed/unsigned conflict (int32 ↔ uint32, etc.)
        if (isSignedUnsignedConflict(t1, t2)) {
            return TypeConflictType.SIGNED_UNSIGNED;
        }

        // Integer widening (int32 → int64)
        if (isWideningConversion(t1, t2)) {
            return TypeConflictType.WIDENING;
        }

        // Integer narrowing (int64 → int32)
        if (isNarrowingConversion(t1, t2)) {
            return TypeConflictType.NARROWING;
        }

        // Float/double conversion
        if (isFloatDoubleConversion(t1, t2)) {
            return TypeConflictType.FLOAT_DOUBLE;
        }

        // String ↔ bytes
        if (isStringBytesConflict(t1, t2)) {
            return TypeConflictType.STRING_BYTES;
        }

        // Primitive ↔ message
        if (isPrimitiveMessageConflict(t1, t2)) {
            return TypeConflictType.PRIMITIVE_MESSAGE;
        }

        return TypeConflictType.INCOMPATIBLE;
    }

    /**
     * Check for signed/unsigned integer conflict.
     */
    private boolean isSignedUnsignedConflict(Type t1, Type t2) {
        Set<Type> signed32 = Set.of(Type.TYPE_INT32, Type.TYPE_SINT32, Type.TYPE_SFIXED32);
        Set<Type> unsigned32 = Set.of(Type.TYPE_UINT32, Type.TYPE_FIXED32);
        Set<Type> signed64 = Set.of(Type.TYPE_INT64, Type.TYPE_SINT64, Type.TYPE_SFIXED64);
        Set<Type> unsigned64 = Set.of(Type.TYPE_UINT64, Type.TYPE_FIXED64);

        // 32-bit signed ↔ unsigned
        if ((signed32.contains(t1) && unsigned32.contains(t2)) ||
            (unsigned32.contains(t1) && signed32.contains(t2))) {
            return true;
        }

        // 64-bit signed ↔ unsigned
        return (signed64.contains(t1) && unsigned64.contains(t2)) ||
               (unsigned64.contains(t1) && signed64.contains(t2));
    }

    /**
     * Check for widening conversion (int32 → int64, uint32 → uint64).
     */
    private boolean isWideningConversion(Type from, Type to) {
        Set<Type> int32Types = Set.of(Type.TYPE_INT32, Type.TYPE_SINT32, Type.TYPE_SFIXED32,
                                       Type.TYPE_UINT32, Type.TYPE_FIXED32);
        Set<Type> int64Types = Set.of(Type.TYPE_INT64, Type.TYPE_SINT64, Type.TYPE_SFIXED64,
                                       Type.TYPE_UINT64, Type.TYPE_FIXED64);

        return int32Types.contains(from) && int64Types.contains(to);
    }

    /**
     * Check for narrowing conversion (int64 → int32).
     */
    private boolean isNarrowingConversion(Type from, Type to) {
        Set<Type> int32Types = Set.of(Type.TYPE_INT32, Type.TYPE_SINT32, Type.TYPE_SFIXED32,
                                       Type.TYPE_UINT32, Type.TYPE_FIXED32);
        Set<Type> int64Types = Set.of(Type.TYPE_INT64, Type.TYPE_SINT64, Type.TYPE_SFIXED64,
                                       Type.TYPE_UINT64, Type.TYPE_FIXED64);

        return int64Types.contains(from) && int32Types.contains(to);
    }

    /**
     * Check for float ↔ double conversion.
     */
    private boolean isFloatDoubleConversion(Type t1, Type t2) {
        return (t1 == Type.TYPE_FLOAT && t2 == Type.TYPE_DOUBLE) ||
               (t1 == Type.TYPE_DOUBLE && t2 == Type.TYPE_FLOAT);
    }

    /**
     * Check for string ↔ bytes conflict.
     */
    private boolean isStringBytesConflict(Type t1, Type t2) {
        return (t1 == Type.TYPE_STRING && t2 == Type.TYPE_BYTES) ||
               (t1 == Type.TYPE_BYTES && t2 == Type.TYPE_STRING);
    }

    /**
     * Check for primitive ↔ message conflict.
     */
    private boolean isPrimitiveMessageConflict(Type t1, Type t2) {
        boolean t1Primitive = t1 != Type.TYPE_MESSAGE && t1 != Type.TYPE_ENUM;
        boolean t2Primitive = t2 != Type.TYPE_MESSAGE && t2 != Type.TYPE_ENUM;
        boolean t1Message = t1 == Type.TYPE_MESSAGE;
        boolean t2Message = t2 == Type.TYPE_MESSAGE;

        return (t1Primitive && t2Message) || (t1Message && t2Primitive);
    }

    /**
     * Format field type for display.
     *
     * @param field the field info to format
     * @return human-readable type string
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
     *
     * @return human-readable summary of the field change
     */
    public String getSummary() {
        return switch (changeType) {
            case ADDED -> "Added field: " + fieldName + " (" + formatType(v2Field) + ", #" + fieldNumber + ")";
            case REMOVED -> "Removed field: " + fieldName + " (#" + fieldNumber + ")";
            case TYPE_CHANGED -> "Type changed: " + formatType(v1Field) + " -> " + formatType(v2Field);
            case LABEL_CHANGED -> "Label changed: " + (v1Field.isRepeated() ? "repeated" : "singular") +
                                  " -> " + (v2Field.isRepeated() ? "repeated" : "singular");
            case NAME_CHANGED -> "Renamed: " + v1Field.getProtoName() + " -> " + v2Field.getProtoName();
            case NUMBER_CHANGED -> "Renumbered: " + getRenumberDescription() +
                                   (isRenumberedByMapping() ? " (mapped)" : "");
            default -> String.join("; ", changes);
        };
    }
}
