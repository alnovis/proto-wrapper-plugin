package space.alnovis.protowrapper.model;

import java.util.*;

/**
 * Represents a merged field from multiple protocol versions.
 *
 * <p>Contains the unified field definition with type information
 * that works across all versions where this field is present.</p>
 *
 * <p>Can be created via constructor (mutable) or via Builder (immutable):</p>
 * <pre>
 * // Mutable approach (legacy)
 * MergedField field = new MergedField(fieldInfo, "v1");
 * field.addVersion("v2", fieldInfo2);
 *
 * // Immutable approach (preferred)
 * MergedField field = MergedField.builder()
 *     .addVersionField("v1", fieldInfo1)
 *     .addVersionField("v2", fieldInfo2)
 *     .build();
 * </pre>
 */
public class MergedField {

    /**
     * Type of conflict between field types across versions.
     */
    public enum ConflictType {
        /** No type conflict - same type in all versions */
        NONE,
        /** int ↔ enum conflict (convertible via getValue/forNumber) */
        INT_ENUM,
        /** Type widening: int → long, int → double (safe conversion) */
        WIDENING,
        /** Type narrowing: long → int, double → int (lossy conversion) */
        NARROWING,
        /** string ↔ bytes conflict (convertible via getBytes/new String) */
        STRING_BYTES,
        /** Primitive to message: int → SomeMessage (not convertible) */
        PRIMITIVE_MESSAGE,
        /** Other incompatible types: string ↔ int, etc. (not convertible) */
        INCOMPATIBLE;

        /**
         * Check if this conflict type can be safely converted.
         * @return true if conversion is possible without data loss
         */
        public boolean isConvertible() {
            return this == NONE || this == INT_ENUM || this == WIDENING || this == STRING_BYTES;
        }

        /**
         * Check if builder setters should be skipped for this conflict type.
         * For the hybrid approach, ALL type conflicts result in skipped builder setters
         * because the unified type cannot be directly passed to version-specific proto builders.
         * @return true if setters should not be generated
         */
        public boolean shouldSkipBuilderSetter() {
            return this != NONE;
        }
    }

    private final String name;
    private final String javaName;
    private final int number;
    private final String javaType;
    private final String getterType;
    private final boolean optional;
    private final boolean repeated;
    private final boolean primitive;
    private final boolean message;
    private final boolean isEnum;
    private final Set<String> presentInVersions;
    private final Map<String, FieldInfo> versionFields; // Version -> original field
    private final ConflictType conflictType;
    private final Map<String, String> typesPerVersion; // Version -> javaType

    /**
     * Create a new MergedField from a FieldInfo.
     * Use {@link #addVersion(String, FieldInfo)} to add more versions.
     *
     * @param field Field info from first version
     * @param version Version identifier
     * @deprecated Use {@link #builder()} for immutable construction
     */
    @Deprecated
    public MergedField(FieldInfo field, String version) {
        this.name = field.getProtoName();
        this.javaName = field.getJavaName();
        this.number = field.getNumber();
        this.javaType = field.getJavaType();
        this.getterType = field.getGetterType();
        this.optional = field.isOptional();
        this.repeated = field.isRepeated();
        this.primitive = field.isPrimitive();
        this.message = field.isMessage();
        this.isEnum = field.isEnum();
        this.presentInVersions = new LinkedHashSet<>();
        this.presentInVersions.add(version);
        this.versionFields = new LinkedHashMap<>();
        this.versionFields.put(version, field);
        this.conflictType = ConflictType.NONE;
        this.typesPerVersion = new LinkedHashMap<>();
        this.typesPerVersion.put(version, field.getJavaType());
    }

    /**
     * Private constructor for Builder.
     */
    private MergedField(Builder builder) {
        FieldInfo firstField = builder.versionFields.values().iterator().next();
        this.name = firstField.getProtoName();
        this.javaName = firstField.getJavaName();
        this.number = firstField.getNumber();
        this.javaType = builder.resolvedJavaType != null ? builder.resolvedJavaType : firstField.getJavaType();
        this.getterType = builder.resolvedGetterType != null ? builder.resolvedGetterType : firstField.getGetterType();
        this.optional = firstField.isOptional();
        this.repeated = firstField.isRepeated();
        this.primitive = firstField.isPrimitive();
        this.message = firstField.isMessage();
        this.isEnum = firstField.isEnum();
        this.presentInVersions = Collections.unmodifiableSet(new LinkedHashSet<>(builder.versionFields.keySet()));
        this.versionFields = Collections.unmodifiableMap(new LinkedHashMap<>(builder.versionFields));
        this.conflictType = builder.conflictType != null ? builder.conflictType : ConflictType.NONE;
        this.typesPerVersion = Collections.unmodifiableMap(new LinkedHashMap<>(builder.typesPerVersion));
    }

    /**
     * Create a new builder for MergedField.
     *
     * @return New Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Add a version to this field.
     *
     * @param version Version identifier
     * @param field Field info for this version
     * @deprecated Use {@link #builder()} for immutable construction
     */
    @Deprecated
    public void addVersion(String version, FieldInfo field) {
        presentInVersions.add(version);
        versionFields.put(version, field);
    }

    /**
     * Builder for creating immutable MergedField instances.
     */
    public static class Builder {
        private final Map<String, FieldInfo> versionFields = new LinkedHashMap<>();
        private final Map<String, String> typesPerVersion = new LinkedHashMap<>();
        private String resolvedJavaType;
        private String resolvedGetterType;
        private ConflictType conflictType;

        /**
         * Add a version field.
         *
         * @param version Version identifier
         * @param field Field info for this version
         * @return This builder
         */
        public Builder addVersionField(String version, FieldInfo field) {
            versionFields.put(version, field);
            typesPerVersion.put(version, field.getJavaType());
            return this;
        }

        /**
         * Set the resolved Java type (for type conflict resolution).
         *
         * @param javaType Resolved Java type
         * @return This builder
         */
        public Builder resolvedJavaType(String javaType) {
            this.resolvedJavaType = javaType;
            return this;
        }

        /**
         * Set the resolved getter type (for type conflict resolution).
         *
         * @param getterType Resolved getter type
         * @return This builder
         */
        public Builder resolvedGetterType(String getterType) {
            this.resolvedGetterType = getterType;
            return this;
        }

        /**
         * Set the conflict type for this field.
         *
         * @param conflictType Type of conflict between versions
         * @return This builder
         */
        public Builder conflictType(ConflictType conflictType) {
            this.conflictType = conflictType;
            return this;
        }

        /**
         * Build the immutable MergedField.
         *
         * @return New MergedField instance
         * @throws IllegalStateException if no version fields were added
         */
        public MergedField build() {
            if (versionFields.isEmpty()) {
                throw new IllegalStateException("At least one version field must be added");
            }
            return new MergedField(this);
        }
    }

    public String getName() {
        return name;
    }

    public String getJavaName() {
        return javaName;
    }

    public int getNumber() {
        return number;
    }

    public String getJavaType() {
        return javaType;
    }

    public String getGetterType() {
        return getterType;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isRepeated() {
        return repeated;
    }

    public boolean isPrimitive() {
        return primitive;
    }

    public boolean isMessage() {
        return message;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public Set<String> getPresentInVersions() {
        return Collections.unmodifiableSet(presentInVersions);
    }

    public Map<String, FieldInfo> getVersionFields() {
        return Collections.unmodifiableMap(versionFields);
    }

    /**
     * Get the conflict type for this field.
     * @return The type of conflict, or NONE if no conflict
     */
    public ConflictType getConflictType() {
        return conflictType;
    }

    /**
     * Check if this field has a type conflict between versions.
     * @return true if field types differ across versions
     */
    public boolean hasTypeConflict() {
        return conflictType != ConflictType.NONE;
    }

    /**
     * Check if builder setters should be skipped for this field.
     * @return true if the conflict type requires skipping builder setters
     */
    public boolean shouldSkipBuilderSetter() {
        return conflictType.shouldSkipBuilderSetter();
    }

    /**
     * Get the Java type for a specific version.
     * @param version Version identifier
     * @return Java type for that version, or null if not present
     */
    public String getTypeForVersion(String version) {
        return typesPerVersion.get(version);
    }

    /**
     * Get all unique types across all versions.
     * @return Set of unique Java type names
     */
    public Set<String> getAllTypes() {
        return new LinkedHashSet<>(typesPerVersion.values());
    }

    /**
     * Get types per version map.
     * @return Unmodifiable map of version to Java type
     */
    public Map<String, String> getTypesPerVersion() {
        return Collections.unmodifiableMap(typesPerVersion);
    }

    /**
     * Get the full nested type path from the proto type name.
     * E.g., for type ".example.proto.v1.Order.ShippingInfo" with package "example.proto.v1"
     * returns "Order.ShippingInfo".
     *
     * @param protoPackage The proto package to strip (e.g., "example.proto.v1")
     * @return Full nested type path or simple name if not nested
     */
    public String getNestedTypePath(String protoPackage) {
        // Get the original field info to access the full type name
        if (versionFields.isEmpty()) {
            return javaType;
        }
        FieldInfo firstField = versionFields.values().iterator().next();
        return firstField.extractNestedTypePath(protoPackage);
    }

    /**
     * Get the raw proto type name from the first version's field info.
     * @return Raw proto type name (e.g., ".example.proto.v1.Order.ShippingInfo")
     */
    public String getRawTypeName() {
        if (versionFields.isEmpty()) {
            return null;
        }
        FieldInfo firstField = versionFields.values().iterator().next();
        return firstField.getTypeName();
    }

    public String getGetterName() {
        String prefix = javaType.equals("boolean") || javaType.equals("Boolean") ? "is" : "get";
        return prefix + capitalize(javaName);
    }

    public String getExtractMethodName() {
        return "extract" + capitalize(javaName);
    }

    public String getExtractHasMethodName() {
        return "extractHas" + capitalize(javaName);
    }

    /**
     * Get extract method name for enum value (INT_ENUM conflicts).
     */
    public String getExtractEnumMethodName() {
        return "extract" + capitalize(javaName) + "Enum";
    }

    /**
     * Get extract method name for bytes value (STRING_BYTES conflicts).
     */
    public String getExtractBytesMethodName() {
        return "extract" + capitalize(javaName) + "Bytes";
    }

    /**
     * Get extract method name for message value (PRIMITIVE_MESSAGE conflicts).
     */
    public String getExtractMessageMethodName() {
        return "extract" + capitalize(javaName) + "Message";
    }

    // ==================== Builder Method Names ====================

    /**
     * Get doSet method name for builder.
     */
    public String getDoSetMethodName() {
        return "doSet" + capitalize(javaName);
    }

    /**
     * Get doClear method name for builder.
     */
    public String getDoClearMethodName() {
        return "doClear" + capitalize(javaName);
    }

    /**
     * Get doAdd method name for repeated field builder.
     */
    public String getDoAddMethodName() {
        return "doAdd" + capitalize(javaName);
    }

    /**
     * Get doAddAll method name for repeated field builder.
     */
    public String getDoAddAllMethodName() {
        return "doAddAll" + capitalize(javaName);
    }

    /**
     * Get setter method name for builder interface.
     */
    public String getSetterMethodName() {
        return "set" + capitalize(javaName);
    }

    /**
     * Get clear method name for builder interface.
     */
    public String getClearMethodName() {
        return "clear" + capitalize(javaName);
    }

    /**
     * Get add method name for repeated field builder interface.
     */
    public String getAddMethodName() {
        return "add" + capitalize(javaName);
    }

    /**
     * Get addAll method name for repeated field builder interface.
     */
    public String getAddAllMethodName() {
        return "addAll" + capitalize(javaName);
    }

    /**
     * Get has method name for optional fields.
     */
    public String getHasMethodName() {
        return "has" + capitalize(javaName);
    }

    /**
     * Get supports method name for version-specific fields.
     */
    public String getSupportsMethodName() {
        return "supports" + capitalize(javaName);
    }

    /**
     * Get the capitalized java name (useful for building method names).
     */
    public String getCapitalizedName() {
        return capitalize(javaName);
    }

    /**
     * Whether this field needs has-check pattern (optional primitive).
     */
    public boolean needsHasCheck() {
        return optional && primitive;
    }

    /**
     * Whether this field exists in all versions.
     */
    public boolean isUniversal(Set<String> allVersions) {
        return presentInVersions.containsAll(allVersions);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public String toString() {
        String conflictInfo = conflictType != ConflictType.NONE
                ? ", conflict=" + conflictType
                : "";
        return String.format("MergedField[%s:%s #%d, versions=%s%s]",
            name, javaType, number, presentInVersions, conflictInfo);
    }
}
