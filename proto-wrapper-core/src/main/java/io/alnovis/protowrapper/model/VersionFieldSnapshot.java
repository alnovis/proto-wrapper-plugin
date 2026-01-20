package io.alnovis.protowrapper.model;

import java.util.Objects;

/**
 * Immutable snapshot of field information for a specific version.
 *
 * <p>This record consolidates repeated version field lookups and null-checks
 * into a single queryable object. Instead of scattered code like:</p>
 *
 * <pre>{@code
 * FieldInfo versionField = field.getVersionFields().get(version);
 * String versionType = versionField != null ? versionField.getJavaType() : "double";
 * boolean isEnum = versionField != null && versionField.isEnum();
 * }</pre>
 *
 * <p>Use a snapshot:</p>
 *
 * <pre>{@code
 * VersionFieldSnapshot snapshot = VersionFieldSnapshot.of(field, version);
 * String versionType = snapshot.javaTypeOr("double");
 * boolean isEnum = snapshot.isEnum();
 * }</pre>
 *
 * <h2>Usage Patterns</h2>
 *
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * // Get snapshot for current version
 * VersionFieldSnapshot snapshot = VersionFieldSnapshot.of(field, ctx.requireVersion());
 *
 * // Check presence
 * if (snapshot.isPresent()) {
 *     // Use version-specific properties
 *     String javaName = snapshot.javaName();
 * }
 * }</pre>
 *
 * <h3>With Fallback Values</h3>
 * <pre>{@code
 * // Get type with fallback
 * String type = snapshot.javaTypeOr("double");
 *
 * // Get javaName with fallback to merged field's name
 * String name = snapshot.javaNameOr(field.getJavaName());
 * }</pre>
 *
 * <h3>Type Checking</h3>
 * <pre>{@code
 * if (snapshot.isEnum()) { ... }
 * if (snapshot.isRepeated()) { ... }
 * if (snapshot.supportsHasMethod()) { ... }
 * }</pre>
 *
 * @param version the version identifier (never null)
 * @param fieldInfo the FieldInfo for this version (nullable)
 *
 * @since 1.6.5
 * @see MergedField#getFieldForVersion(String)
 */
public record VersionFieldSnapshot(
        String version,
        FieldInfo fieldInfo
) {

    /**
     * Creates a snapshot with validation.
     *
     * @param version the version identifier (must not be null)
     * @param fieldInfo the FieldInfo for this version (nullable)
     */
    public VersionFieldSnapshot {
        Objects.requireNonNull(version, "version must not be null");
    }

    // ==================== Factory Methods ====================

    /**
     * Create a snapshot for a specific version.
     *
     * @param field the merged field
     * @param version the version identifier
     * @return the snapshot for that version
     */
    public static VersionFieldSnapshot of(MergedField field, String version) {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(version, "version must not be null");
        return new VersionFieldSnapshot(version, field.getVersionFields().get(version));
    }

    /**
     * Create a snapshot that represents an absent field.
     *
     * @param version the version identifier
     * @return a snapshot with no field info
     */
    public static VersionFieldSnapshot absent(String version) {
        return new VersionFieldSnapshot(version, null);
    }

    // ==================== Presence Checks ====================

    /**
     * Check if the field is present in this version.
     *
     * @return true if fieldInfo is not null
     */
    public boolean isPresent() {
        return fieldInfo != null;
    }

    /**
     * Check if the field is absent in this version.
     *
     * @return true if fieldInfo is null
     */
    public boolean isAbsent() {
        return fieldInfo == null;
    }

    // ==================== Type Properties ====================

    /**
     * Get the Java type for this version's field.
     *
     * @return the Java type, or null if field is absent
     */
    public String javaType() {
        return fieldInfo != null ? fieldInfo.getJavaType() : null;
    }

    /**
     * Get the Java type with a fallback for absent fields.
     *
     * @param defaultType the fallback type if field is absent
     * @return the Java type, or defaultType if absent
     */
    public String javaTypeOr(String defaultType) {
        return fieldInfo != null ? fieldInfo.getJavaType() : defaultType;
    }

    /**
     * Get the type name (for message/enum types).
     *
     * @return the type name, or null if absent or not a message/enum
     */
    public String typeName() {
        return fieldInfo != null ? fieldInfo.getTypeName() : null;
    }

    /**
     * Check if this version's field is an enum type.
     *
     * @return true if present and is an enum
     */
    public boolean isEnum() {
        return fieldInfo != null && fieldInfo.isEnum();
    }

    /**
     * Check if this version's field is a message type.
     *
     * @return true if present and is a message
     */
    public boolean isMessage() {
        return fieldInfo != null && fieldInfo.isMessage();
    }

    /**
     * Check if this version's field is a primitive type.
     *
     * @return true if present and is a primitive
     */
    public boolean isPrimitive() {
        return fieldInfo != null && fieldInfo.isPrimitive();
    }

    // ==================== Label Properties ====================

    /**
     * Check if this version's field is repeated.
     *
     * @return true if present and is repeated
     */
    public boolean isRepeated() {
        return fieldInfo != null && fieldInfo.isRepeated();
    }

    /**
     * Check if this version's field is optional.
     *
     * @return true if present and is optional
     */
    public boolean isOptional() {
        return fieldInfo != null && fieldInfo.isOptional();
    }

    // ==================== Map Properties ====================

    /**
     * Check if this version's field is a map.
     *
     * @return true if present and is a map
     */
    public boolean isMap() {
        return fieldInfo != null && fieldInfo.isMap();
    }

    /**
     * Get the map info for this version's field.
     *
     * @return the MapInfo, or null if absent or not a map
     */
    public MapInfo mapInfo() {
        return fieldInfo != null ? fieldInfo.getMapInfo() : null;
    }

    // ==================== Name Properties ====================

    /**
     * Get the Java name for this version's field.
     *
     * @return the Java name, or null if absent
     */
    public String javaName() {
        return fieldInfo != null ? fieldInfo.getJavaName() : null;
    }

    /**
     * Get the Java name with a fallback for absent fields.
     *
     * @param defaultName the fallback name if field is absent
     * @return the Java name, or defaultName if absent
     */
    public String javaNameOr(String defaultName) {
        return fieldInfo != null ? fieldInfo.getJavaName() : defaultName;
    }

    /**
     * Get the proto name for this version's field.
     *
     * @return the proto name, or null if absent
     */
    public String protoName() {
        return fieldInfo != null ? fieldInfo.getProtoName() : null;
    }

    // ==================== Has Method Support ====================

    /**
     * Check if this version's field supports has*() method.
     *
     * @return true if present and supports has method
     */
    public boolean supportsHasMethod() {
        return fieldInfo != null && fieldInfo.supportsHasMethod();
    }

    // ==================== Oneof Properties ====================

    /**
     * Check if this version's field is in a oneof.
     *
     * @return true if present and in a oneof
     */
    public boolean isInOneof() {
        return fieldInfo != null && fieldInfo.isInOneof();
    }

    /**
     * Get the oneof name for this version's field.
     *
     * @return the oneof name, or null if absent or not in oneof
     */
    public String oneofName() {
        return fieldInfo != null ? fieldInfo.getOneofName() : null;
    }

    // ==================== Nested Type Extraction ====================

    /**
     * Extract the nested type path for this version's field.
     *
     * @param protoPackage the proto package to strip
     * @return the nested type path, or null if absent
     */
    public String extractNestedTypePath(String protoPackage) {
        return fieldInfo != null ? fieldInfo.extractNestedTypePath(protoPackage) : null;
    }

    // ==================== Type Comparisons ====================

    /**
     * Check if the Java type equals a specific type string.
     *
     * <p>Useful for type-specific code generation:</p>
     * <pre>{@code
     * if (snapshot.javaTypeIs("float") || snapshot.javaTypeIs("Float")) {
     *     // narrowing conversion needed
     * }
     * }</pre>
     *
     * @param type the type to compare against
     * @return true if present and Java type equals the specified type
     */
    public boolean javaTypeIs(String type) {
        return fieldInfo != null && type.equals(fieldInfo.getJavaType());
    }

    /**
     * Check if the Java type is one of several types.
     *
     * <p>Useful for type-specific code generation:</p>
     * <pre>{@code
     * if (snapshot.javaTypeIsOneOf("float", "Float")) {
     *     // narrowing conversion needed
     * }
     * }</pre>
     *
     * @param types the types to check against
     * @return true if present and Java type is one of the specified types
     */
    public boolean javaTypeIsOneOf(String... types) {
        if (fieldInfo == null) return false;
        String javaType = fieldInfo.getJavaType();
        for (String type : types) {
            if (type.equals(javaType)) return true;
        }
        return false;
    }

    /**
     * Check if this field requires narrowing conversion from double to float.
     *
     * @return true if present and type is float/Float
     */
    public boolean needsNarrowingFromDouble() {
        return javaTypeIsOneOf("float", "Float");
    }

    /**
     * Check if this field requires narrowing conversion from long to int.
     *
     * @return true if present and type is int/Integer
     */
    public boolean needsNarrowingFromLong() {
        return javaTypeIsOneOf("int", "Integer");
    }
}
